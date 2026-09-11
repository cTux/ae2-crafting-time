$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ui-smoke-scheduled-java.ps1')

$full = @(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc)
if (Compare-Object @(1, 2) $full -SyncWindow 0) { throw 'Full relaunch does not require exactly two Java launches' }
$suite = @(Get-UiSmokeJavaLaunchPhases -Scenario suite -ContainsCpuList)
if (Compare-Object @(1, 2) $suite -SyncWindow 0) { throw 'A suite containing the CPU-list case does not require two Java launches' }
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
$running = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test -ProcessLookup {
    param($id)
    if ($id -eq 42) { [pscustomobject]@{ Id = $id } }
}
if ($running.state -ne 'running') { throw 'A present scheduled Java process was reported missing' }
$exited = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test `
    -ProcessLookup { param($id) $null } -TaskLookup { param($name) [pscustomobject]@{State='Ready'} } `
    -InfoLookup { param($task) [pscustomobject]@{LastTaskResult=0} }
if ($exited.state -ne 'exited' -or $exited.exitCode -ne 0) { throw 'A completed scheduled Java task was not accepted' }
$disappeared = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test `
    -ProcessLookup { param($id) $null } -TaskLookup { param($name) $null }
if ($disappeared.state -ne 'disappeared') { throw 'A disappeared scheduled Java process was not detected' }
if ($runner -notmatch 'throw "Scheduled UI-smoke client process \$\(\$process\.Id\) disappeared' -or
        $runner -notmatch 'Write-Status ''validating'' "client phase \$phase exited; validating evidence"') {
    throw 'The runner does not fail closed and publish post-exit status'
}

Write-Host 'UI smoke scheduled-Java checks passed'
