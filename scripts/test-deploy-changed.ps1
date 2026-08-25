param(
    [string]$StatePath = (Join-Path $env:TEMP "ae2-crafting-time-release-test-state.json")
)

$ErrorActionPreference = "Stop"

Remove-Item -LiteralPath $StatePath -Force -ErrorAction SilentlyContinue
$versionPath = "$StatePath.version"
Set-Content -LiteralPath $versionPath -Value "modVersion=1.0.4" -Encoding UTF8

$releaseDryRun = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" `
    -StatePath $StatePath `
    -VersionPath $versionPath `
    -Deploy `
    -DryRun `
    -ModrinthProjectId test-project `
    -CurseProjectId 1591476
if ($LASTEXITCODE -ne 0 -or ($releaseDryRun -join "`n") -notmatch 'dry-run GitHub Release: 1\.20\.1 Forge 1\.0\.4, 1\.20\.1 Fabric 1\.0\.4, 1\.21\.1 NeoForge 1\.0\.4') {
    throw "Release dry run did not create the expected GitHub Release metadata"
}
if (($releaseDryRun -join "`n") -notmatch 'dry-run next development version: 1\.0\.5') {
    throw "Release dry run did not advance the development version"
}

$first = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" `
    -StatePath $StatePath `
    -VersionPath $versionPath
if ($LASTEXITCODE -ne 0 -or ($first -join "`n") -notmatch 'build 1\.20\.1-forge: 1\.0\.4') {
    throw "First release run did not build 1.20.1-forge"
}
if (($first -join "`n") -notmatch 'build 1\.21\.1-neoforge: 1\.0\.4') {
    throw "First release run did not build 1.21.1-neoforge"
}
if (($first -join "`n") -notmatch 'build 1\.20\.1-fabric: 1\.0\.4') {
    throw "First release run did not build 1.20.1-fabric"
}

$partialStatePath = "$StatePath.partial"
Copy-Item -LiteralPath $StatePath -Destination $partialStatePath -Force
$partialState = Get-Content -LiteralPath $partialStatePath -Raw | ConvertFrom-Json
$partialState.'1.20.1-fabric'.fingerprint = "changed"
$partialState | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $partialStatePath -Encoding UTF8
Set-Content -LiteralPath $versionPath -Value "modVersion=1.0.5" -Encoding UTF8
$partial = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" `
    -StatePath $partialStatePath `
    -VersionPath $versionPath `
    -Deploy `
    -DryRun `
    -ModrinthProjectId test-project `
    -CurseProjectId 1591476
$partialOutput = $partial -join "`n"
if ($LASTEXITCODE -ne 0 -or $partialOutput -notmatch 'dry-run GitHub Release: 1\.20\.1 Fabric 1\.0\.5') {
    throw "Partial release did not publish only the affected jar at the development version"
}
if ($partialOutput -notmatch 'dry-run GitHub assets: ae2-crafting-time-1\.0\.4-forge-1\.20\.1\.jar, ae2-crafting-time-1\.0\.5-fabric-1\.20\.1\.jar, ae2-crafting-time-1\.0\.4-neoforge-1\.21\.1\.jar') {
    throw "Partial release did not attach every latest jar to GitHub"
}
if ($partialOutput -notmatch 'dry-run Modrinth version: 1\.20\.1-fabric-1\.0\.5') {
    throw "Partial release did not use a loader-qualified Modrinth version number"
}
if ($partialOutput -notmatch 'dry-run CurseForge versions: 1\.20\.1, Fabric, Client, Server') {
    throw "Partial release did not include CurseForge environment versions"
}
Remove-Item -LiteralPath $partialStatePath -Force -ErrorAction SilentlyContinue

$second = & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\deploy-changed.ps1" `
    -StatePath $StatePath `
    -VersionPath $versionPath
if ($LASTEXITCODE -ne 0 -or ($second -join "`n") -notmatch 'skip 1\.20\.1-forge: unchanged at 1\.0\.4') {
    throw "Second release run did not skip unchanged 1.20.1-forge"
}
if (($second -join "`n") -notmatch 'skip 1\.21\.1-neoforge: unchanged at 1\.0\.4') {
    throw "Second release run did not skip unchanged 1.21.1-neoforge"
}
if (($second -join "`n") -notmatch 'skip 1\.20\.1-fabric: unchanged at 1\.0\.4') {
    throw "Second release run did not skip unchanged 1.20.1-fabric"
}

Remove-Item -LiteralPath $StatePath -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $versionPath -Force -ErrorAction SilentlyContinue
Write-Host "release script check passed"
