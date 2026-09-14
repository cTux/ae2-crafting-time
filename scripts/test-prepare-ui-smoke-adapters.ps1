$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-adapters-' + [guid]::NewGuid().ToString('N'))
try {
    $mods = Join-Path $temp 'mods'
    New-Item -ItemType Directory -Path $mods -Force | Out-Null
    $version = ((Get-Content (Join-Path $root 'gradle.properties')) | Where-Object { $_ -match '^modVersion=' }) -replace '^modVersion=', ''
    if (!$version) { throw 'Missing modVersion' }
    Copy-Item (Join-Path $root "build/test-driver/ae2-crafting-time-$version-forge-1.20.1-test-driver.jar") $mods
    Copy-Item (Join-Path $root "dist/ae2-crafting-time-$version-forge-1.20.1.jar") $mods
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target 1.20.1-forge -BundleDirectory $temp -BaseOnly
    $base = Get-Content (Join-Path $temp 'expected-adapters.json') -Raw | ConvertFrom-Json
    if (@($base.psobject.Properties).Count -ne 0) { throw 'BaseOnly expected adapters' }
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target 1.20.1-forge -BundleDirectory $temp -ProjectId rxYaglEe
    $advanced = Get-Content (Join-Path $temp 'expected-adapters.json') -Raw | ConvertFrom-Json
    if (@($advanced.psobject.Properties).Count -ne 1 -or $advanced.advanced_ae -ne 'advanced-cpu') { throw 'AdvancedAE graph expectation' }
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target 1.20.1-forge -BundleDirectory $temp
    $full = Get-Content (Join-Path $temp 'expected-adapters.json') -Raw | ConvertFrom-Json
    if (@($full.psobject.Properties).Count -lt 5 -or $full.advanced_ae -ne 'advanced-cpu') { throw 'Full catalogue expectation' }
    Write-Host 'prepare-ui-smoke-adapters checks passed'
} finally {
    if (Test-Path $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
