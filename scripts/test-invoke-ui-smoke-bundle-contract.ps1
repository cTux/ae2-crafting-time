$ErrorActionPreference = 'Stop'
$invoke = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'invoke-ui-smoke-codexvm.ps1') -Raw
$postprocess = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke.ps1') -Raw
$prepare = $invoke.IndexOf("prepare-ui-smoke-adapters.ps1", [StringComparison]::Ordinal)
$dispatch = $invoke.IndexOf('if ($Transport -eq "OpenSSH")', [StringComparison]::Ordinal)
if ($prepare -lt 0 -or $prepare -gt $dispatch) {
    throw 'Focused bundle adapter expectations must be prepared before CodexVM dispatch'
}
$prepareCall = "& (Join-Path `$PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target `$Target -BundleDirectory `$bundlePath"
if ($invoke.IndexOf($prepareCall, [StringComparison]::Ordinal) -lt 0) {
    throw 'Focused bundle preparation must reuse the packaged adapter catalogue preflight'
}
if ($invoke.IndexOf('if (-not $Stop) {', [StringComparison]::Ordinal) -gt $prepare) {
    throw 'Cleanup-only dispatch must not depend on bundle preparation'
}
$validation = "Join-Path `$BundleDirectory 'expected-adapters.json'"
if ($postprocess.IndexOf($validation, [StringComparison]::Ordinal) -lt 0) {
    throw 'Focused bundle post-processing no longer validates expected adapters'
}
Write-Host 'invoke-ui-smoke bundle contract checks passed'
