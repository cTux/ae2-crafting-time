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

$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-relaunch-evidence-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp -Force | Out-Null
try {
    $control = Join-Path $temp 'state.properties'
    [IO.File]::WriteAllLines($control, @('schema=1', 'state=DONE'), [Text.UTF8Encoding]::new($false))
    $runtimeProcesses = @($processes | ForEach-Object {
        [pscustomobject]@{ phase=$_.phase; pid=$_.pid; startedAt=$_.startedAt; stdout='stdout'; stderr='stderr'
            taskName='task'; executable='java.exe'; argumentFile='args'; exitCode=$_.exitCode; exitedAt=$_.exitedAt
            runtimeObject=(Get-Item -LiteralPath $control) }
    })
    $output = Join-Path $temp 'relaunch-evidence.json'
    $watch = [Diagnostics.Stopwatch]::StartNew()
    Write-UiSmokeRelaunchEvidence -Path $output -CampaignId campaign -Target '1.20.1-forge' `
        -Profile compatible -Scenario cpu-list-total-ttc -World world -ConnectionEpoch campaign `
        -PredecessorCheckpointSha256 ('a' * 64) -Processes $runtimeProcesses `
        -Artifacts @([pscustomobject]@{name='mod.jar';sha256=('b' * 64);runtimeObject=(Get-Item $control)}) `
        -DependencyMode base -DependencyCatalogueSha256 ('c' * 64) -ControlStatePath $control -FinalApproval
    $watch.Stop()
    $written = Get-Content -LiteralPath $output -Raw | ConvertFrom-Json
    if ($watch.Elapsed.TotalSeconds -ge 2 -or $written.schema -ne 1 -or $written.launchCount -ne 2 -or
            @($written.controlState).Count -ne 2 -or $written.processes[0].psobject.Properties.Name -contains 'runtimeObject' -or
            $written.artifacts[0].psobject.Properties.Name -contains 'runtimeObject') {
        throw 'Relaunch evidence did not remain bounded and primitive'
    }
    $integratedOutput = Join-Path $temp 'integrated-relaunch-evidence.json'
    Write-UiSmokeRelaunchEvidence -Path $integratedOutput -CampaignId campaign -Target '1.20.1-forge' `
        -Profile compatible -Scenario cpu-list-total-ttc -World world -ConnectionEpoch $null `
        -PredecessorCheckpointSha256 ('a' * 64) -Processes $runtimeProcesses `
        -Artifacts @([pscustomobject]@{name='mod.jar';sha256=('b' * 64)}) -FinalApproval
    $integrated = Get-Content -LiteralPath $integratedOutput -Raw | ConvertFrom-Json
    if ($null -ne $integrated.connectionEpoch -or @($integrated.controlState).Count -ne 0 -or
            !$integrated.finalApproval -or $integrated.launchCount -ne 2) {
        throw 'Integrated relaunch evidence claimed a connected control handshake'
    }
    try {
        Write-UiSmokeRelaunchEvidence -Path $output -CampaignId campaign -Target '1.20.1-forge' `
            -Profile compatible -Scenario cpu-list-total-ttc -World world -ConnectionEpoch campaign `
            -PredecessorCheckpointSha256 ('a' * 64) -Processes $runtimeProcesses `
            -Artifacts @([pscustomobject]@{name='mod.jar';sha256=('b' * 64)}) -FinalApproval
        throw 'Dedicated relaunch evidence accepted missing control state'
    } catch {
        if ($_.Exception.Message -ne 'Relaunch evidence requires the connected control state') { throw }
    }
    [IO.File]::WriteAllText($control, ('x' * 65537), [Text.UTF8Encoding]::new($false))
    try {
        Write-UiSmokeRelaunchEvidence -Path $output -CampaignId campaign -Target '1.20.1-forge' `
            -Profile compatible -Scenario cpu-list-total-ttc -World world -ConnectionEpoch campaign `
            -PredecessorCheckpointSha256 ('a' * 64) `
            -Processes $runtimeProcesses -Artifacts @([pscustomobject]@{name='mod.jar';sha256=('b' * 64)}) `
            -ControlStatePath $control -FinalApproval
        throw 'Oversized control state was accepted'
    } catch {
        if ($_.Exception.Message -notlike 'UI-smoke control state exceeds *') { throw }
    }
} finally {
    Remove-Item -LiteralPath $temp -Recurse -Force
}
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
