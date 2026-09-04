param(
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$BundleDirectory
)
$ErrorActionPreference = 'Stop'
$mods = Join-Path $BundleDirectory 'mods'
$driver = @(Get-ChildItem -LiteralPath $mods -Filter 'ae2-crafting-time-*-test-driver.jar')
$production = @(Get-ChildItem -LiteralPath $mods -Filter 'ae2-crafting-time-*.jar' | Where-Object Name -NotLike '*-test-driver.jar')
if ($driver.Count -ne 1 -or $production.Count -ne 1) { throw 'Adapter preflight needs exact production and driver artifacts' }
$major = if ($Target -like '1.20.1-*') { 17 } elseif ($Target -eq '1.21.1-neoforge') { 21 } else { 25 }
$javaHome = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $major
$lines = & (Join-Path $javaHome 'bin/java.exe') -cp "$($driver[0].FullName);$($production[0].FullName)" com.ctux.ae2craftingtime.testdriver.SmokeAdapterCatalog $Target
if ($LASTEXITCODE -ne 0) { throw 'Packaged adapter catalogue could not be read' }
$expected = [ordered]@{}
foreach ($line in $lines) {
    if ($line -cnotmatch '^([a-z0-9_]+)\t([a-z0-9-]+)$' -or $expected.Contains($Matches[1])) { throw "Invalid packaged adapter entry: $line" }
    $expected[$Matches[1]] = $Matches[2]
}
if (!$expected.Count) { throw 'No adapter catalogue entries for target' }
$expected | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $BundleDirectory 'expected-adapters.json') -Encoding UTF8
