$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ui-smoke-scheduled-java.ps1')

$full = @(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc)
if (Compare-Object @(1, 2) $full -SyncWindow 0) { throw 'Full relaunch does not require exactly two Java launches' }
$resume = @(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc -ResumeOnly)
if (Compare-Object @(2) $resume -SyncWindow 0) { throw 'Resume-only does not require exactly one phase-2 Java launch' }
if (@(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc -PrepareOnly).Count -ne 0) {
    throw 'Prepare-only unexpectedly launches Java'
}

$processes = @(
    [pscustomobject]@{phase=1;pid=10;startedAt='2026-09-10T00:00:00Z';exitCode=0;exitedAt='2026-09-10T00:01:00Z'},
    [pscustomobject]@{phase=2;pid=11;startedAt='2026-09-10T00:02:00Z';exitCode=0;exitedAt='2026-09-10T00:03:00Z'}
)
Assert-UiSmokeJavaPhaseIdentities -Processes $processes -ExpectedPhases $full -FinalApproval
try {
    Assert-UiSmokeJavaPhaseIdentities -Processes @($processes[1]) -ExpectedPhases $resume -FinalApproval
    throw 'Resume-only phase was accepted for final approval'
} catch { if ($_.Exception.Message -eq 'Resume-only phase was accepted for final approval') { throw } }
$duplicate = @($processes[0], [pscustomobject]@{phase=2;pid=10;startedAt=$processes[0].startedAt;exitCode=0;exitedAt='2026-09-10T00:03:00Z'})
try {
    Assert-UiSmokeJavaPhaseIdentities -Processes $duplicate -ExpectedPhases $full -FinalApproval
    throw 'Duplicate phase process identity was accepted'
} catch { if ($_.Exception.Message -eq 'Duplicate phase process identity was accepted') { throw } }
$runner = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke.ps1') -Raw
if ($runner -notmatch 'if \(\$Scenario -eq ''cpu-list-total-ttc''\) \{\s*\$progressPath') {
    throw 'The progress watchdog does not cover every CPU-list process'
}

Write-Host 'UI smoke scheduled-Java checks passed'
