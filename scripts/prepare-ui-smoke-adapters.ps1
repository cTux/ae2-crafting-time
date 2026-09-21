param(
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$BundleDirectory,
    [switch]$BaseOnly,
    [switch]$ValidateOnly,
    [string[]]$ProjectId
)
$ErrorActionPreference = 'Stop'
$identityPath = Join-Path $BundleDirectory 'bundle-identity.json'
if (Test-Path -LiteralPath $identityPath) {
    $identity = Get-Content -LiteralPath $identityPath -Raw | ConvertFrom-Json
    if ($identity.target -cne $Target -or [bool]$identity.baseOnly -ne [bool]$BaseOnly) { throw 'Sealed adapter graph policy mismatch' }
    $null = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') -Mode Reuse -CacheDirectory $BundleDirectory `
        -HeadSha $identity.headSha -Fingerprint $identity.fingerprint -Target $Target -Profile $identity.profile `
        -GraphId $identity.graphId -BaseOnly:$BaseOnly
    $ValidateOnly = $true
}
$mods = Join-Path $BundleDirectory 'mods'
$driver = @(Get-ChildItem -LiteralPath $mods -Filter 'ae2-crafting-time-*-test-driver.jar')
$production = @(Get-ChildItem -LiteralPath $mods -Filter 'ae2-crafting-time-*.jar' | Where-Object Name -NotLike '*-test-driver.jar')
if ($driver.Count -ne 1 -or $production.Count -ne 1) { throw 'Adapter preflight needs exact production and driver artifacts' }
$major = if ($Target -like '1.20.1-*') { 17 } elseif ($Target -eq '1.21.1-neoforge') { 21 } else { 25 }
$javaHome = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $major
$lines = & (Join-Path $javaHome 'bin/java.exe') -cp "$($driver[0].FullName);$($production[0].FullName)" com.ctux.ae2craftingtime.testdriver.SmokeAdapterCatalog $Target
if ($LASTEXITCODE -ne 0) { throw 'Packaged adapter catalogue could not be read' }
$expected = [ordered]@{}
if (!$BaseOnly) {
    foreach ($line in $lines) {
        if ($line -cnotmatch '^([a-z0-9_]+)\t([a-z0-9-]+)$' -or $expected.Contains($Matches[1])) { throw "Invalid packaged adapter entry: $line" }
        $expected[$Matches[1]] = $Matches[2]
    }
    if (!$expected.Count) { throw 'No adapter catalogue entries for target' }
    if ($ProjectId) {
        $profiles = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-client-versions.json') -Raw | ConvertFrom-Json
        $selected = @($profiles | Where-Object id -eq $Target | ForEach-Object projects |
            Where-Object project_id -in $ProjectId | ForEach-Object mod_id)
        foreach ($dependency in @($expected.Keys)) {
            if ($dependency -notin $selected) { $expected.Remove($dependency) }
        }
    }
}
$expectedPath = Join-Path $BundleDirectory 'expected-adapters.json'
if ($ValidateOnly) {
    $actual = Get-Content -LiteralPath $expectedPath -Raw | ConvertFrom-Json
    if ($null -eq $actual -or @($actual.psobject.Properties).Count -ne $expected.Count) { throw 'Sealed adapter expectations differ from the selected graph' }
    foreach ($key in $expected.Keys) {
        $property = @($actual.psobject.Properties | Where-Object Name -CEQ $key)
        if ($property.Count -ne 1 -or $property[0].Value -cne $expected[$key]) { throw 'Sealed adapter expectations differ from the selected graph' }
    }
} else {
    $keys = [string[]]@($expected.Keys); [Array]::Sort($keys,[StringComparer]::Ordinal)
    $ordered = [ordered]@{}; foreach ($key in $keys) { $ordered[$key] = $expected[$key] }
    [IO.File]::WriteAllText($expectedPath,($ordered | ConvertTo-Json -Compress),[Text.UTF8Encoding]::new($false))
}
