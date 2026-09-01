param(
    [Parameter(Mandatory = $true)][string]$ProjectId,
    [string]$ReleaseMatrix = (Join-Path $PSScriptRoot "release-matrix.json"),
    [string]$RunClientMatrix = (Join-Path $PSScriptRoot "run-client-versions.json"),
    [string]$ApiBase = "https://api.modrinth.com/v2"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Compare-Version([string]$left, [string]$right) {
    $leftParts = @([regex]::Matches(($left -split '\+')[0].ToLowerInvariant(), '\d+|[a-z]+') | ForEach-Object Value)
    $rightParts = @([regex]::Matches(($right -split '\+')[0].ToLowerInvariant(), '\d+|[a-z]+') | ForEach-Object Value)
    for ($index = 0; $index -lt [Math]::Max($leftParts.Count, $rightParts.Count); $index++) {
        if ($index -ge $leftParts.Count) { return $(if ($rightParts[$index] -match '^\d+$') { -[int]$rightParts[$index] } else { 1 }) }
        if ($index -ge $rightParts.Count) { return $(if ($leftParts[$index] -match '^\d+$') { [int]$leftParts[$index] } else { -1 }) }
        $leftNumber = $leftParts[$index] -match '^\d+$'
        $rightNumber = $rightParts[$index] -match '^\d+$'
        $comparison = if ($leftNumber -and $rightNumber) {
            [int64]$leftParts[$index] - [int64]$rightParts[$index]
        } elseif ($leftNumber) { 1 } elseif ($rightNumber) { -1 } else {
            [string]::CompareOrdinal($leftParts[$index], $rightParts[$index])
        }
        if ($comparison) { return [Math]::Sign($comparison) }
    }
    return 0
}

function Test-VersionRange([string]$version, [string]$range) {
    if (-not $range -or $range -match '\$\{') { return $false }
    foreach ($alternative in ($range -split '\s*\|\|\s*')) {
        if ($alternative -match '^[\[(](?<minimum>[^,]*),(?<maximum>[^\])]*)[\])]$') {
            $minimumOk = -not $Matches.minimum -or (Compare-Version $version $Matches.minimum) -ge $(if ($alternative[0] -eq '[') { 0 } else { 1 })
            $maximumOk = -not $Matches.maximum -or (Compare-Version $version $Matches.maximum) -le $(if ($alternative[-1] -eq ']') { 0 } else { -1 })
            if ($minimumOk -and $maximumOk) { return $true }
            continue
        }
        if ($alternative -match '^\[(?<exact>[^,]+)\]$') {
            if ((Compare-Version $version $Matches.exact) -eq 0) { return $true }
            continue
        }
        $all = $true
        foreach ($condition in ($alternative -split '\s+')) {
            if (-not $condition) { continue }
            if ($condition -match '^(?<operator>>=|<=|>|<|=)?(?<expected>.+)$') {
                $comparison = Compare-Version $version $Matches.expected
                $all = $all -and $(switch ($Matches.operator) {
                    '>=' { $comparison -ge 0 }; '<=' { $comparison -le 0 }; '>' { $comparison -gt 0 }
                    '<' { $comparison -lt 0 }; default { $comparison -eq 0 }
                })
            }
        }
        if ($all) { return $true }
    }
    return $false
}

function Get-TomlRange([string]$text, [string]$modId) {
    foreach ($match in [regex]::Matches($text, '(?ms)^\s*\[\[dependencies(?:\.[^\]]+)?\]\]\s*(?<body>.*?)(?=^\s*\[\[|\z)')) {
        $body = $match.Groups['body'].Value
        $id = [regex]::Match($body, '(?m)^\s*modId\s*=\s*["''](?<value>[^"'']+)').Groups['value'].Value
        if ($id -eq $modId) {
            return [regex]::Match($body, '(?m)^\s*versionRange\s*=\s*["''](?<value>[^"'']+)').Groups['value'].Value
        }
    }
    return ""
}

function Get-ArtifactRanges([string]$path, [string]$loader) {
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        if ($loader -eq "fabric") {
            $entry = $archive.GetEntry("fabric.mod.json")
            if (-not $entry) { return $null }
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            return [pscustomobject]@{
                Minecraft = [string]$metadata.depends.minecraft
                Loader = [string]$metadata.depends.fabricloader
                Ae2 = [string]$metadata.depends.ae2
            }
        }
        $entry = $archive.GetEntry($(if ($loader -eq "forge") { "META-INF/mods.toml" } else { "META-INF/neoforge.mods.toml" }))
        if (-not $entry -and $loader -eq "neoforge") { $entry = $archive.GetEntry("META-INF/mods.toml") }
        if (-not $entry) { return $null }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $text = $reader.ReadToEnd() } finally { $reader.Dispose() }
        return [pscustomobject]@{
            Minecraft = Get-TomlRange $text "minecraft"
            Loader = Get-TomlRange $text $loader
            Ae2 = Get-TomlRange $text "ae2"
        }
    } finally { $archive.Dispose() }
}

$releaseRows = Get-Content -LiteralPath $ReleaseMatrix -Raw | ConvertFrom-Json
$runRows = Get-Content -LiteralPath $RunClientMatrix -Raw | ConvertFrom-Json
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-integration-audit-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $temp -Force | Out-Null
try {
    foreach ($row in $releaseRows) {
        $run = $runRows | Where-Object id -eq $row.id | Select-Object -First 1
        if (-not $run) { throw "Missing run-client row $($row.id)" }
        $game = [uri]::EscapeDataString("[`"$($row.minecraftVersion)`"]")
        $loader = [uri]::EscapeDataString("[`"$($row.loader)`"]")
        $versions = @(Invoke-RestMethod -Uri "$ApiBase/project/$ProjectId/version?game_versions=$game&loaders=$loader" |
            Where-Object version_type -eq "release" | Sort-Object date_published)
        $selected = $null
        $lastReason = "no stable artifact"
        foreach ($version in $versions) {
            $file = $version.files | Where-Object primary | Select-Object -First 1
            if (-not $file) { $file = $version.files | Select-Object -First 1 }
            if (-not $file) { $lastReason = "version $($version.id) has no artifact"; continue }
            $artifact = Join-Path $temp $file.filename
            Invoke-WebRequest -UseBasicParsing -Uri $file.url -OutFile $artifact
            if ($file.hashes.sha512 -and (Get-FileHash -LiteralPath $artifact -Algorithm SHA512).Hash.ToLowerInvariant() -ne $file.hashes.sha512) {
                throw "SHA-512 mismatch for Modrinth version $($version.id)"
            }
            $ranges = Get-ArtifactRanges $artifact $row.loader
            if (-not $ranges -or -not $ranges.Minecraft -or -not $ranges.Loader -or -not $ranges.Ae2) {
                $lastReason = "version $($version.id) lacks Minecraft, loader, or AE2 metadata"
                continue
            }
            $loaderVersion = [string]$run.compatible.loader_version
            if ($row.loader -eq "forge") { $loaderVersion = $loaderVersion -replace "^$([regex]::Escape($row.minecraftVersion))-", "" }
            $ae2Version = [string]$run.compatible.ae2_version
            $minecraftOk = Test-VersionRange $row.minecraftVersion $ranges.Minecraft
            $loaderOk = Test-VersionRange $loaderVersion $ranges.Loader
            $ae2Ok = Test-VersionRange $ae2Version $ranges.Ae2
            if ($minecraftOk -and $loaderOk -and $ae2Ok) {
                $selected = [pscustomobject]@{
                    Target = $row.id; Status = "SUPPORTED"; VersionId = $version.id; Version = $version.version_number
                    MinecraftRange = $ranges.Minecraft; LoaderRange = $ranges.Loader; Ae2Range = $ranges.Ae2; Reason = ""
                }
                break
            }
            $lastReason = "version $($version.id) rejects pinned metadata: minecraft=$minecraftOk loader=$loaderOk ae2=$ae2Ok"
        }
        if ($selected) { $selected } else {
            [pscustomobject]@{
                Target = $row.id; Status = "UNSUPPORTED"; VersionId = ""; Version = ""
                MinecraftRange = ""; LoaderRange = ""; Ae2Range = ""; Reason = $lastReason
            }
        }
    }
} finally {
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
