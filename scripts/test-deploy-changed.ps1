param(
    [string]$StatePath = (Join-Path $env:TEMP "ae2-crafting-time-release-test-state.json")
)

$ErrorActionPreference = "Stop"

Remove-Item -LiteralPath $StatePath -Force -ErrorAction SilentlyContinue

$releaseDryRun = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" `
    -StatePath $StatePath `
    -Deploy `
    -DryRun `
    -ModrinthProjectId test-project `
    -CurseProjectId 1591476
if ($LASTEXITCODE -ne 0 -or ($releaseDryRun -join "`n") -notmatch 'dry-run GitHub Release: 1\.20\.1 Forge 1\.0\.1, 1\.20\.1 Fabric 1\.0\.1, 1\.21\.1 NeoForge 1\.0\.3') {
    throw "Release dry run did not create the expected GitHub Release metadata"
}

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

$partialStatePath = "$StatePath.partial"
Copy-Item -LiteralPath $StatePath -Destination $partialStatePath -Force
$partialState = Get-Content -LiteralPath $partialStatePath -Raw | ConvertFrom-Json
$partialState.'1.20.1-fabric'.fingerprint = "changed"
$partialState | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $partialStatePath -Encoding UTF8
$partial = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" `
    -StatePath $partialStatePath `
    -Deploy `
    -DryRun `
    -ModrinthProjectId test-project `
    -CurseProjectId 1591476
$partialOutput = $partial -join "`n"
if ($LASTEXITCODE -ne 0 -or $partialOutput -notmatch 'dry-run GitHub Release: 1\.20\.1 Fabric 1\.0\.2') {
    throw "Partial release did not bump only the affected jar"
}
if ($partialOutput -notmatch 'dry-run GitHub assets: ae2-crafting-time-1\.20\.1-Forge-1\.0\.1\.jar, ae2-crafting-time-1\.20\.1-Fabric-1\.0\.2\.jar, ae2-crafting-time-1\.21\.1-NeoForge-1\.0\.3\.jar') {
    throw "Partial release did not attach every latest jar to GitHub"
}
Remove-Item -LiteralPath $partialStatePath -Force -ErrorAction SilentlyContinue

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
