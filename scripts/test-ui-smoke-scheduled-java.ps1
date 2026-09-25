$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ui-smoke-scheduled-java.ps1')

$full = @(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc)
if (Compare-Object @(1, 2) $full -SyncWindow 0) { throw 'Full relaunch does not require exactly two Java launches' }
$status = @(Get-UiSmokeJavaLaunchPhases -Scenario standard-status-controls)
if (Compare-Object @(1, 2) $status -SyncWindow 0) { throw 'Status amounts do not require exactly two Java launches' }
$badge = @(Get-UiSmokeJavaLaunchPhases -Scenario badge-background)
if (Compare-Object @(1, 2) $badge -SyncWindow 0) { throw 'Badge background does not require exactly two Java launches' }
$suite = @(Get-UiSmokeJavaLaunchPhases -Scenario suite -ContainsCpuList)
if (Compare-Object @(1, 2) $suite -SyncWindow 0) { throw 'A suite containing the CPU-list case does not require two Java launches' }
$resume = @(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc -ResumeOnly)
if (Compare-Object @(2) $resume -SyncWindow 0) { throw 'Resume-only does not require exactly one phase-2 Java launch' }
if (@(Get-UiSmokeJavaLaunchPhases -Scenario cpu-list-total-ttc -PrepareOnly).Count -ne 0) {
    throw 'Prepare-only unexpectedly launches Java'
}

$nonInteractive = Get-UiSmokeScheduledJavaStartParameters -Executable 'java.exe' -Arguments '@args' `
    -WorkingDirectory 'runtime' -TaskName 'task' -InteractiveUser 'Codex' -InteractiveToken $null
if ($nonInteractive.ContainsKey('InteractiveToken')) {
    throw 'Non-interactive scheduled Java retained an empty token argument'
}
$interactive = Get-UiSmokeScheduledJavaStartParameters -Executable 'java.exe' -Arguments '@args' `
    -WorkingDirectory 'runtime' -TaskName 'task' -InteractiveUser 'Codex' -InteractiveToken ('d' * 64)
if ($interactive.InteractiveToken -cne ('d' * 64)) {
    throw 'Interactive scheduled Java lost its token argument'
}

$token = 'd' * 64
$pipeName = 'ae2ct-test-' + [guid]::NewGuid().ToString('N')
$pipe = [IO.Pipes.NamedPipeServerStream]::new($pipeName, [IO.Pipes.PipeDirection]::Out, 1,
    [IO.Pipes.PipeTransmissionMode]::Byte, [IO.Pipes.PipeOptions]::Asynchronous)
try {
    $expectedHash = ([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash(
        [Text.Encoding]::UTF8.GetBytes($token)))).Replace('-', '').ToLowerInvariant()
    $childArguments = "-NoProfile -NonInteractive -Command `"`$hash = ([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes(`$env:AE2CT_TEST_DRIVER_TOKEN)))).Replace('-', '').ToLowerInvariant(); if (`$hash -ceq '$expectedHash') { exit 0 } else { exit 9 }`""
    $action = New-UiSmokeScheduledJavaTaskAction -Executable (Get-Process -Id $PID).Path `
        -Arguments $childArguments -PipeName $pipeName
    if ($action.Argument -match [regex]::Escape($token)) { throw 'Scheduled action persisted the interactive token' }
    $relay = Start-Process -FilePath $action.Execute -ArgumentList $action.Argument -PassThru -WindowStyle Hidden
    $connected = $pipe.WaitForConnectionAsync()
    if (!$connected.Wait(5000)) { throw 'Scheduled token relay did not connect' }
    $writer = [IO.StreamWriter]::new($pipe, [Text.UTF8Encoding]::new($false), 1024, $true)
    $writer.WriteLine($token)
    $writer.Flush()
    $writer.Dispose()
    if (!$relay.WaitForExit(5000) -or $relay.ExitCode -ne 0) {
        throw 'Scheduled child did not inherit a valid interactive token'
    }
} finally {
    $pipe.Dispose()
    if ($relay -and !$relay.HasExited) { $relay.Kill() }
}

$processes = @(
    [pscustomobject]@{phase=1;pid=10;startedAt='2026-09-10T00:00:00Z';exitCode=0;exitedAt='2026-09-10T00:01:00Z'},
    [pscustomobject]@{phase=2;pid=11;startedAt='2026-09-10T00:02:00Z';exitCode=0;exitedAt='2026-09-10T00:03:00Z'}
)
Assert-UiSmokeJavaPhaseIdentities -Processes $processes -ExpectedPhases $full -FinalApproval

$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-relaunch-evidence-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp -Force | Out-Null
try {
    $configPath = Join-Path $temp 'client.toml'
    $continuationPath = Join-Path $temp 'status-continuation.json'
    [IO.File]::WriteAllText($configPath, 'compactStatusAmounts = false')
    $continuation = @{schema=1;world='world';campaign='campaign';configSha256=(Get-FileHash $configPath).Hash}
    $continuation | ConvertTo-Json | Set-Content -LiteralPath $continuationPath
    Assert-UiSmokeStatusContinuation -Path $continuationPath -ConfigPath $configPath -World world -CampaignId campaign
    foreach ($field in @('schema', 'world', 'campaign', 'configSha256')) {
        $changed = $continuation.Clone()
        $changed[$field] = 'wrong'
        $changed | ConvertTo-Json | Set-Content -LiteralPath $continuationPath
        $refused = $false
        try { Assert-UiSmokeStatusContinuation -Path $continuationPath -ConfigPath $configPath -World world -CampaignId campaign }
        catch { $refused = $_.Exception.Message -like '*does not match*' }
        if (!$refused) { throw "Status continuation accepted changed $field" }
    }
    $continuation | ConvertTo-Json | Set-Content -LiteralPath $continuationPath
    $refused = $false
    try { Assert-UiSmokeStatusContinuation -Path $continuationPath -ConfigPath "$configPath.missing" -World world -CampaignId campaign }
    catch { $refused = $_.Exception.Message -like '*does not match*' }
    if (!$refused) { throw 'Status continuation accepted a missing config' }
    [IO.File]::WriteAllText($configPath, 'compactStatusAmounts = true')
    $refused = $false
    try { Assert-UiSmokeStatusContinuation -Path $continuationPath -ConfigPath $configPath -World world -CampaignId campaign }
    catch { $refused = $_.Exception.Message -like '*does not match*' }
    if (!$refused) { throw 'Status continuation accepted a changed config' }
    $capturePaths = foreach ($capture in @('status-saved-off','status-relaunch-off','status-relaunch-on','status-relaunch-restored')) {
        foreach ($extension in @('png','json')) {
            $capturePath = Join-Path $temp "$capture.$extension"
            [IO.File]::WriteAllText($capturePath, 'fixture')
            $capturePath
        }
    }
    Assert-UiSmokeStatusRelaunchCaptures -Evidence $temp
    $badgeCapturePaths = foreach ($capture in @('status-badge-off-small','status-badge-off-auto',
            'badge-saved-off','badge-relaunch-off','badge-reset-group','badge-reset-all','badge-relaunch-restored')) {
        foreach ($extension in @('png','json')) {
            $capturePath = Join-Path $temp "$capture.$extension"
            [IO.File]::WriteAllText($capturePath, 'fixture')
            $capturePath
        }
    }
    Assert-UiSmokeBadgeRelaunchCaptures -Evidence $temp
    foreach ($capturePath in $badgeCapturePaths) {
        Remove-Item -LiteralPath $capturePath
        $refused = $false
        try { Assert-UiSmokeBadgeRelaunchCaptures -Evidence $temp }
        catch { $refused = $_.Exception.Message -like '*capture is missing*' }
        if (!$refused) { throw "Missing badge capture accepted: $capturePath" }
        [IO.File]::WriteAllText($capturePath, 'fixture')
    }
    foreach ($target in @('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')) {
        $names = @()
        switch ($target) {
            '1.20.1-forge' { $names = @('mana','chemical') }
            '1.20.1-fabric' { $names = @('mana') }
            '1.21.1-neoforge' { $names = @('chemical') }
        }
        @{schema=1;appbotLoaded=('mana' -in $names);appmekLoaded=('chemical' -in $names);
            captured=@($names)} | ConvertTo-Json -Depth 3 |
            Set-Content -LiteralPath (Join-Path $temp 'status-addon-keys.json')
        foreach ($name in @('mana','chemical')) {
            foreach ($capture in @("status-addon-$name", "status-addon-$name-tooltip")) {
                foreach ($extension in @('png','json')) {
                    [IO.File]::WriteAllText((Join-Path $temp "$capture.$extension"), 'fixture')
                }
            }
        }
        Assert-UiSmokeStatusAddonKeys -Evidence $temp -Target $target
    }
    foreach ($capturePath in $capturePaths) {
        Remove-Item -LiteralPath $capturePath
        $refused = $false
        try { Assert-UiSmokeStatusRelaunchCaptures -Evidence $temp }
        catch { $refused = $_.Exception.Message -like '*capture is missing*' }
        if (!$refused) { throw "Missing status capture accepted: $capturePath" }
        [IO.File]::WriteAllText($capturePath, 'fixture')
    }
    Assert-UiSmokeJavaPhaseIdentities -Processes $processes -ExpectedPhases $status -FinalApproval
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
if ($runner -notmatch '\$tokenParameters = @\{\}' -or
        $runner -notmatch 'if \(\$Interactive\) \{ \$tokenParameters\.InteractiveToken = \$env:AE2CT_TEST_DRIVER_TOKEN \}' -or
        $runner -notmatch '-InteractiveUser \$InteractiveUser `\s*@tokenParameters') {
    throw 'Noninteractive scheduled Java must omit the optional token while interactive Java passes it through validation'
}
try {
    Start-UiSmokeScheduledJava -Executable missing-java.exe -Arguments missing -WorkingDirectory . `
        -TaskName test -InteractiveUser Codex -InteractiveToken invalid
    throw 'Invalid interactive token was accepted'
} catch {
    if ($_.Exception.Message -eq 'Invalid interactive token was accepted' -or
            $_.Exception.Message -notmatch 'InteractiveToken') { throw }
}
if ($runner -notmatch "if \(\`$Scenario -in @\('cpu-list-total-ttc', 'standard-status-controls', 'recurrent-plan', 'stored-variant-plan', 'delayed-resource-icons', 'appmek-resource-icons'\) -or\s*\`$selectedCases -contains 'recurrent-plan' -or \`$selectedCases -contains 'stored-variant-plan'\) \{\s*\`$progressPath") {
    throw 'The progress watchdog does not cover every CPU-list process'
}
$running = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test -ProcessLookup {
    param($id)
    if ($id -eq 42) { [pscustomobject]@{ Id = $id } }
}
if ($running.state -ne 'running') { throw 'A present scheduled Java process was reported missing' }
$finishing = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test `
    -ProcessLookup { param($id) $null } -TaskLookup { param($name) [pscustomobject]@{State='Running'} }
if ($finishing.state -ne 'finishing') {
    throw 'An exact scheduled Java task still finishing after its child exited was reported missing'
}
$exited = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test `
    -ProcessLookup { param($id) $null } -TaskLookup { param($name) [pscustomobject]@{State='Ready'} } `
    -InfoLookup { param($task) [pscustomobject]@{LastTaskResult=0} }
if ($exited.state -ne 'exited' -or $exited.exitCode -ne 0) { throw 'A completed scheduled Java task was not accepted' }
$unsignedFailure = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test `
    -ProcessLookup { param($id) $null } -TaskLookup { param($name) [pscustomobject]@{State='Ready'} } `
    -InfoLookup { param($task) [pscustomobject]@{LastTaskResult=[uint32]::MaxValue} }
if ($unsignedFailure.state -ne 'exited' -or $unsignedFailure.exitCode -ne -1) {
    throw 'An unsigned scheduled-task failure result was not preserved as its signed Java exit code'
}
$disappeared = Get-UiSmokeScheduledJavaProcessState -ProcessId 42 -TaskName test `
    -ProcessLookup { param($id) $null } -TaskLookup { param($name) $null }
if ($disappeared.state -ne 'disappeared') { throw 'A disappeared scheduled Java process was not detected' }
if ($runner -notmatch 'throw "Scheduled UI-smoke client process \$\(\$process\.Id\) disappeared' -or
        $runner -notmatch 'Write-Status ''validating'' "client phase \$phase exited; validating evidence"') {
    throw 'The runner does not fail closed and publish post-exit status'
}

Write-Host 'UI smoke scheduled-Java checks passed'
