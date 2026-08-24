param(
    [string]$StatePath = (Join-Path $env:TEMP "ae2-crafting-time-release-test-state.json")
)

$ErrorActionPreference = "Stop"

Remove-Item -LiteralPath $StatePath -Force -ErrorAction SilentlyContinue

$first = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" -StatePath $StatePath
if ($LASTEXITCODE -ne 0 -or ($first -join "`n") -notmatch 'build 1\.20\.1-forge: 1\.0\.1') {
    throw "First release run did not build 1.20.1-forge"
}
if (($first -join "`n") -notmatch 'build 1\.21\.1-neoforge: 1\.0\.3') {
    throw "First release run did not build 1.21.1-neoforge"
}
if (($first -join "`n") -notmatch 'build 1\.20\.1-fabric: 1\.0\.1') {
    throw "First release run did not build 1.20.1-fabric"
}

$second = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" -StatePath $StatePath
if ($LASTEXITCODE -ne 0 -or ($second -join "`n") -notmatch 'skip 1\.20\.1-forge: unchanged at 1\.0\.1') {
    throw "Second release run did not skip unchanged 1.20.1-forge"
}
if (($second -join "`n") -notmatch 'skip 1\.21\.1-neoforge: unchanged at 1\.0\.3') {
    throw "Second release run did not skip unchanged 1.21.1-neoforge"
}
if (($second -join "`n") -notmatch 'skip 1\.20\.1-fabric: unchanged at 1\.0\.1') {
    throw "Second release run did not skip unchanged 1.20.1-fabric"
}

Remove-Item -LiteralPath $StatePath -Force -ErrorAction SilentlyContinue
Write-Host "release script check passed"
