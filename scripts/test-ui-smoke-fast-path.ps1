$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-fast-smoke-' + [guid]::NewGuid().ToString('N'))
$resume = Join-Path $temporary 'resume'
$world = Join-Path $temporary 'world'
$evidence = Join-Path $temporary 'evidence'
$bundle = Join-Path $temporary 'bundle'
New-Item -ItemType Directory -Path $world, $evidence, (Join-Path $bundle 'mods') -Force | Out-Null
try {
    $worldId = 'ae2ct-' + 'a' * 32
    @{schema=1;scenario='craft-plan';sourceFixtureId='ae2-crafting-time';disposableWorldId=$worldId;
        dependencyMode='base';dependencyCatalogueSha256=('0' * 64)} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $world '.ae2-crafting-time-test-fixture.json') -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $world 'level.dat') -Value 'world'
    @{schema=1;phase='relaunch-ready';world=$worldId;epoch='campaign-a';serverState='{}';checks=@('same-jvm-clear');screenshots=@('before.png');serverSequence=8;clientSequence=9} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidence 'cpu-list-continuation.json') -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $evidence 'before.png') -Value 'image'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/mod.jar') -Value 'artifact'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/I.txt') -Value 'upper'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/i.txt') -Value 'lower'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/İ.txt') -Value 'upper-dotted'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/ı.txt') -Value 'lower-dotless'
    '["mod.jar"]' | Set-Content -LiteralPath (Join-Path $bundle 'mods/.ae2-crafting-time-run-mods.json')
    @{schema=1;target='1.20.1-forge';profile='compatible';java=17;loader='1.20.1-47.4.10';dependencyMode='base'} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $bundle 'profile.json') -Encoding UTF8
    $catalogueLine = "mod.jar|$((Get-FileHash -LiteralPath (Join-Path $bundle 'mods/mod.jar') -Algorithm SHA256).Hash)"
    $catalogueAlgorithm = [Security.Cryptography.SHA256]::Create()
    try { $catalogueHash = ([BitConverter]::ToString($catalogueAlgorithm.ComputeHash([Text.Encoding]::UTF8.GetBytes($catalogueLine))) -replace '-','') }
    finally { $catalogueAlgorithm.Dispose() }
    $markerPath = Join-Path $world '.ae2-crafting-time-test-fixture.json'
    $marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
    $marker.dependencyCatalogueSha256 = 'F' * 64
    [IO.File]::WriteAllText($markerPath, ($marker | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
    $incompatibleResume = Join-Path $temporary 'incompatible-resume'
    try {
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-resume.ps1') -Mode Capture -ResumeDirectory $incompatibleResume `
            -WorldDirectory $world -EvidenceDirectory $evidence -BundleDirectory $bundle -Target '1.20.1-forge' `
            -Profile compatible -Scenario cpu-list-total-ttc -CampaignId campaign-a -HeadSha ('1' * 40) | Out-Null
        throw 'Incompatible dependency identity launched Minecraft'
    } catch {
        if ($_.Exception.Message -eq 'Incompatible dependency identity launched Minecraft') { throw }
    }
    $marker.dependencyCatalogueSha256 = $catalogueHash
    [IO.File]::WriteAllText($markerPath, ($marker | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))

    $originalCulture = [Threading.Thread]::CurrentThread.CurrentCulture
    [Threading.Thread]::CurrentThread.CurrentCulture = [Globalization.CultureInfo]::GetCultureInfo('en-US')
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-resume.ps1') -Mode Capture -ResumeDirectory $resume `
        -WorldDirectory $world -EvidenceDirectory $evidence -BundleDirectory $bundle -Target '1.20.1-forge' `
        -Profile compatible -Scenario cpu-list-total-ttc -CampaignId campaign-a -HeadSha ('1' * 40) | Out-Null
    [Threading.Thread]::CurrentThread.CurrentCulture = [Globalization.CultureInfo]::GetCultureInfo('tr-TR')
    $restored = & (Join-Path $PSScriptRoot 'prepare-ui-smoke-resume.ps1') -Mode Restore -ResumeDirectory $resume `
        -BundleDirectory $bundle -Target '1.20.1-forge' -Profile compatible -Scenario cpu-list-total-ttc -HeadSha ('1' * 40)
    [Threading.Thread]::CurrentThread.CurrentCulture = $originalCulture
    if ($restored.phase -ne 2 -or $restored.finalApproval -ne $false -or $restored.world -ne $worldId -or
            $restored.campaignId -ne 'campaign-a' -or $restored.launchCount -ne 1) {
        throw 'Resume restore did not remain a one-launch non-final phase-2 diagnostic'
    }
    $again = & (Join-Path $PSScriptRoot 'prepare-ui-smoke-resume.ps1') -Mode Restore -ResumeDirectory $resume `
        -BundleDirectory $bundle -Target '1.20.1-forge' -Profile compatible -Scenario cpu-list-total-ttc -HeadSha ('1' * 40)
    if ($again.bundleSha256 -ne $restored.bundleSha256) { throw 'Exact artifact bundle was not reusable' }
    $runnerText = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke.ps1') -Raw
    $resumeValidationIndex = $runnerText.IndexOf("'prepare-ui-smoke-resume.ps1') -Mode Restore", [StringComparison]::Ordinal)
    $scheduledLaunchIndex = $runnerText.IndexOf('Start-UiSmokeScheduledJava', [StringComparison]::Ordinal)
    if ($resumeValidationIndex -lt 0 -or $scheduledLaunchIndex -lt 0 -or $resumeValidationIndex -gt $scheduledLaunchIndex) {
        throw 'Resume compatibility validation does not precede scheduled Java launch'
    }
    if (!$runnerText.Contains('$deadline = [DateTime]::UtcNow.Add($timeout)') -or
            !$runnerText.Contains('while ([DateTime]::UtcNow -lt $deadline)')) {
        throw 'The UI-smoke phase no longer has a bounded absolute deadline'
    }
    Add-Content -LiteralPath (Join-Path $resume 'evidence/cpu-list-continuation.json') -Value 'tampered'
    try {
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-resume.ps1') -Mode Restore -ResumeDirectory $resume `
            -BundleDirectory $bundle -Target '1.20.1-forge' -Profile compatible -Scenario cpu-list-total-ttc -HeadSha ('1' * 40) | Out-Null
        throw 'Tampered continuation was accepted'
    } catch {
        if ($_.Exception.Message -eq 'Tampered continuation was accepted') { throw }
    }

    . (Join-Path $PSScriptRoot 'ui-smoke-progress.ps1')
    $now = [DateTime]::Parse('2026-09-10T00:01:00Z').ToUniversalTime()
    $healthy = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-2) -CheckpointAt $now.AddSeconds(-10) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30
    if ($healthy) { throw 'Healthy progress was rejected' }
    if ((Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-16) -CheckpointAt $now.AddSeconds(-1) `
            -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30) -ne 'no-callback') { throw 'No-callback stall was not detected' }
    if ((Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-31) `
            -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30) -ne 'no-checkpoint') { throw 'No-checkpoint stall was not detected' }
    $starting = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-60) -CheckpointAt $now.AddSeconds(-60) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30 -ProcessId 42 -ProgressProcessId 41 `
        -CallbackSequence 0 -StartedAt $now.AddSeconds(-60) -StartupTimeoutSeconds 120
    if ($starting) { throw 'Stale retained progress bypassed the startup grace period' }
    $noCallback = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-180) -CheckpointAt $now.AddSeconds(-180) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30 -ProcessId 42 -ProgressProcessId 41 `
        -CallbackSequence 0 -StartedAt $now.AddSeconds(-121) -StartupTimeoutSeconds 120
    if ($noCallback -ne 'no-callback') { throw 'A client with no current-process callback did not fail after startup grace' }
    $driverStarting = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-5) -CheckpointAt $now.AddSeconds(-5) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 0 -StartedAt $now.AddSeconds(-121) -StartupTimeoutSeconds 120
    if ($driverStarting) { throw 'A loaded current-process driver did not receive its callback grace period' }
    $driverNoCallback = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-121) -CheckpointAt $now.AddSeconds(-121) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 30 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 0 -StartedAt $now.AddSeconds(-121) -StartupTimeoutSeconds 120
    if ($driverNoCallback -ne 'no-callback') { throw 'A loaded driver without callbacks did not fail after startup grace' }
    $placing = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-61) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 60 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 2000 -StartedAt $now.AddSeconds(-119) -StartupTimeoutSeconds 120 `
        -Checkpoint 'state=WORLD_READY phase=PREPARE fixture=placing cpu-list=INITIAL screen=none'
    if ($placing) { throw 'Healthy fixture preparation triggered the active-scenario checkpoint watchdog' }
    $placingExpired = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-61) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 60 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 2000 -StartedAt $now.AddSeconds(-121) -StartupTimeoutSeconds 120 `
        -Checkpoint 'state=WORLD_READY phase=PREPARE fixture=placing cpu-list=INITIAL screen=none'
    if ($placingExpired -ne 'startup-timeout') { throw 'Fixture preparation escaped the absolute startup deadline' }
    $activeLive = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-61) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 60 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 2000 -StartedAt $now.AddSeconds(-121) -StartupTimeoutSeconds 120 `
        -Checkpoint 'state=WORLD_READY phase=ACTIVE fixture=ready cpu-list=RELAUNCH_OPEN screen=none'
    if ($activeLive) { throw 'Fresh current-process callbacks did not keep the active phase alive' }
    $disconnected = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-1) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 60 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 2001 -StartedAt $now.AddSeconds(-10) -StartupTimeoutSeconds 120 `
        -Checkpoint 'state=WORLD_READY phase=ACTIVE fixture=ready cpu-list=REJOIN_REQUEST screen=net.minecraft.client.gui.screens.DisconnectedScreen'
    if ($disconnected -ne 'terminal-disconnect') { throw 'Fresh callbacks kept a terminal disconnect alive' }
    $fabricDisconnected = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-1) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 60 -ProcessId 42 -ProgressProcessId 42 `
        -CallbackSequence 2001 -StartedAt $now.AddSeconds(-10) -StartupTimeoutSeconds 120 `
        -Checkpoint 'state=WORLD_READY phase=ACTIVE fixture=ready cpu-list=REJOIN_REQUEST screen=net.minecraft.class_435'
    if ($fabricDisconnected -ne 'terminal-disconnect') { throw 'Fabric 1.20.1 intermediary disconnect remained live' }
    $staleDisconnect = Get-UiSmokeProgressDecision -Now $now -CallbackAt $now.AddSeconds(-1) -CheckpointAt $now.AddSeconds(-1) `
        -CallbackTimeoutSeconds 15 -CheckpointTimeoutSeconds 60 -ProcessId 42 -ProgressProcessId 41 `
        -CallbackSequence 2001 -StartedAt $now.AddSeconds(-10) -StartupTimeoutSeconds 120 `
        -Checkpoint 'state=WORLD_READY phase=ACTIVE fixture=ready cpu-list=REJOIN_REQUEST screen=net.minecraft.client.gui.screens.DisconnectedScreen'
    if ($staleDisconnect) { throw 'Stale retained disconnect state failed the current-process guard' }
    Write-Host 'UI smoke fast-path checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temporary)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -cnotmatch '^ae2ct-fast-smoke-[a-f0-9]{32}$') { throw 'Unsafe fast-path test cleanup' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
