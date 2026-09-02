param([string]$Root)

$ErrorActionPreference = "Stop"
$Root = if ($Root) { $Root } else { Split-Path -Parent $PSScriptRoot }
$release = Get-Content -LiteralPath (Join-Path $Root "scripts\release-matrix.json") -Raw | ConvertFrom-Json
$clients = Get-Content -LiteralPath (Join-Path $Root "scripts\run-client-versions.json") -Raw | ConvertFrom-Json
if (Compare-Object @($release.id) @($clients.id)) { throw "Release and run-client target rows differ" }

function Get-TomlOptionals([string]$path) {
    $result = [ordered]@{}
    $text = Get-Content -LiteralPath $path -Raw
    foreach ($match in [regex]::Matches($text, '(?ms)^\s*\[\[dependencies(?:\.[^\]]+)?\]\]\s*(?<body>.*?)(?=^\s*\[\[|\z)')) {
        $body = $match.Groups['body'].Value
        $optional = $body -match '(?m)^\s*mandatory\s*=\s*false\s*$' -or $body -match '(?m)^\s*type\s*=\s*"OPTIONAL"\s*$'
        if (-not $optional) { continue }
        $modId = [regex]::Match($body, '(?m)^\s*modId\s*=\s*["''](?<value>[^"'']+)').Groups['value'].Value
        $range = [regex]::Match($body, '(?m)^\s*versionRange\s*=\s*["''](?<value>[^"'']+)').Groups['value'].Value
        if (-not $modId -or -not $range -or $result.Contains($modId)) { throw "Invalid or duplicate optional metadata in $path" }
        $result[$modId] = $range
    }
    return $result
}

function Get-MetadataOptionals($row) {
    if ($row.loader -eq "fabric") {
        $metadata = Get-Content -LiteralPath (Join-Path $Root "$($row.projectDir)\src\main\resources\fabric.mod.json") -Raw | ConvertFrom-Json
        $result = [ordered]@{}
        foreach ($property in $metadata.suggests.PSObject.Properties) { $result[$property.Name] = [string]$property.Value }
        return $result
    }
    $name = if ($row.loader -eq "forge") { "mods.toml" } else { "neoforge.mods.toml" }
    return Get-TomlOptionals (Join-Path $Root "$($row.projectDir)\src\main\resources\META-INF\$name")
}

function Get-Minimum([string]$range) {
    if ($range -match '^[\[(](?<value>[^,]+),') { return $Matches.value.Trim() }
    if ($range -match '>=\s*(?<value>[^\s]+)') { return $Matches.value }
    if ($range -match '^\[(?<value>[^,]+)\]$') { return $Matches.value }
    return ""
}

function Assert-SameSet($expected, $actual, [string]$context) {
    $difference = Compare-Object @($expected | Sort-Object -Unique) @($actual | Sort-Object -Unique)
    if ($difference) { throw "$context differs: $($difference.InputObject -join ', ')" }
}

$projectById = @{}
$projectByMod = @{}
foreach ($row in $clients) {
    foreach ($project in @($row.projects | Where-Object mod_id)) {
        $id = [string]$project.project_id
        if (($projectById[$id] -and $projectById[$id] -ne $project.mod_id) -or
                ($projectByMod[$project.mod_id] -and $projectByMod[$project.mod_id] -ne $id)) {
            throw "Conflicting project/mod identity for $id"
        }
        $projectById[$id] = $project.mod_id
        $projectByMod[$project.mod_id] = $id
    }
}

$dependencyRows = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $Root "DEPENDENCIES.md")) {
    if ($line -match '^\| .*?\(`(?<mod>[^`]+)`\) \| (?<targets>.*?) \| (?<range>.*?) \|') {
        if ($dependencyRows.ContainsKey($Matches.mod)) { throw "Duplicate DEPENDENCIES.md row for $($Matches.mod)" }
        $dependencyRows[$Matches.mod] = [pscustomobject]@{ Targets = $Matches.targets; Range = $Matches.range }
    }
}

$coverageRows = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $Root "docs\mod-automation-coverage.md")) {
    if ($line -match '^\| .*?\(`(?<mod>[^`]+)`[;)]') {
        $cells = @($line.Trim('|').Split('|') | ForEach-Object Trim)
        if ($coverageRows.ContainsKey($Matches.mod)) { throw "Duplicate coverage row for $($Matches.mod)" }
        $coverageRows[$Matches.mod] = $cells
    }
}

$allMetadataMods = [Collections.Generic.HashSet[string]]::new()
for ($index = 0; $index -lt $release.Count; $index++) {
    $row = $release[$index]
    $client = $clients | Where-Object id -eq $row.id | Select-Object -First 1
    $metadata = Get-MetadataOptionals $row
    foreach ($modId in $metadata.Keys) { $null = $allMetadataMods.Add($modId) }

    $label = "$($row.minecraftVersion) $($row.loaderName)"
    $documented = @($dependencyRows.Keys | Where-Object {
        $dependencyRows[$_].Targets -eq "All supported targets" -or $dependencyRows[$_].Targets -like "*$label*"
    })
    Assert-SameSet $metadata.Keys $documented "$($row.id) loader metadata and DEPENDENCIES.md"

    $released = @($row.modrinthDependencies | Where-Object dependency_type -eq "optional" | ForEach-Object {
        $modId = $projectById[[string]$_.project_id]
        if (-not $modId) { throw "Unknown optional release project $($_.project_id) in $($row.id)" }
        $modId
    })
    $curseMods = @($client.curseforge | Where-Object mod_id | ForEach-Object { [string]$_.mod_id })
    $expectedRelease = @($metadata.Keys | Where-Object { $_ -notin $curseMods })
    Assert-SameSet $expectedRelease $released "$($row.id) loader metadata and release dependencies"

    foreach ($modId in $metadata.Keys) {
        $upgradeableRange = if ($row.loader -eq "fabric") { '^>=\S+$' } else { '^\[[^,\[\]()]+,\)$' }
        if ($metadata[$modId] -notmatch $upgradeableRange) {
            throw "$($row.id) optional dependency $modId must use an open-ended minimum version"
        }
        if (-not $coverageRows[$modId]) { throw "Missing coverage row for $modId" }
        $minimum = Get-Minimum $metadata[$modId]
        if ($minimum -and $dependencyRows[$modId].Range -notlike "*$minimum*") {
            throw "DEPENDENCIES.md omits $($row.id) minimum $minimum for $modId"
        }
        $coverage = $coverageRows[$modId][1 + (2 * $index)].Trim('`')
        $project = $client.projects | Where-Object mod_id -eq $modId | Select-Object -First 1
        $curse = $client.curseforge | Where-Object mod_id -eq $modId | Select-Object -First 1
        if ($project) {
            $version = $client.compatible.versions | Where-Object project_id -eq $project.project_id | Select-Object -First 1
            if (-not $version -or $coverage -ne $version.version) { throw "$($row.id) coverage/version pin mismatch for $modId" }
        } elseif ($curse) {
            if (-not $curse.version -or $coverage -ne $curse.version) { throw "$($row.id) CurseForge coverage/version mismatch for $modId" }
        } elseif ($coverage -ne "Not pinned") {
            throw "$($row.id) declares $modId without a pin or Not pinned coverage"
        }
    }
}
Assert-SameSet $allMetadataMods $dependencyRows.Keys "Loader metadata and DEPENDENCIES.md optional integrations"
Write-Host "optional-integration consistency checks passed"
