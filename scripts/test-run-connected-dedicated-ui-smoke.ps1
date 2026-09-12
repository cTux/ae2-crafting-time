$ErrorActionPreference = 'Stop'
$runnerText = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Raw
if ($runnerText.Contains('(& $java -version 2>&1)') -or
        $runnerText -notmatch 'RedirectStandardOutput.+RedirectStandardError') {
    throw 'Connected runner must capture Java version output without promoting native stderr to a terminating error'
}
if ($runnerText -match 'ReadToEndAsync' -or
        $runnerText -notmatch '(?s)Start-Process -FilePath \$java -ArgumentList \$launchCommandLine.+-RedirectStandardOutput \$serverOut -RedirectStandardError \$serverErr') {
    throw 'Connected runner must stream dedicated output to files instead of retaining it in memory'
}
if ($runnerText -notmatch 'clientParameters\.HeadSha = \$HeadSha') {
    throw 'Connected runner must forward an explicit immutable head to the staged client runner'
}
if ($runnerText -notmatch 'clientParameters\.ScheduledJava = \$true' -or
        $runnerText -notmatch 'clientParameters\.InteractiveUser = \$InteractiveUser') {
    throw 'Connected runner must support the prepared interactive Java session used by CodexVM'
}
if ($runnerText -notmatch '\[ValidateRange\(1, 1800\)\]\[int\]\$ServerStartupTimeoutSeconds = 180' -or
        $runnerText -notmatch 'AddSeconds\(\$ServerStartupTimeoutSeconds\)') {
    throw 'Connected server startup must retain a bounded configurable deadline'
}
if ($runnerText -notmatch "\`$clientParameters\.RuntimeDirectory = Join-Path \`$attemptReport 'runtime'") {
    throw 'Connected client runtime must stay on the report-owned guest-local filesystem'
}
if ($runnerText.IndexOf("-SimpleMatch ']: Done ('", [StringComparison]::Ordinal) -lt 0 -or
        $runnerText.IndexOf("'run-ui-smoke.ps1'", [StringComparison]::Ordinal) -lt
            $runnerText.IndexOf("-SimpleMatch ']: Done ('", [StringComparison]::Ordinal)) {
    throw 'Connected client must wait for the dedicated server startup-complete marker'
}
if ($runnerText -notmatch '\$maxAttempts = 3' -or
        $runnerText -notmatch 'FailOnInitialDisconnect=\$true' -or
        $runnerText -notmatch "initial-disconnect-cap-exhausted" -or
        $runnerText -notmatch "\`$status\.message -eq 'UI-smoke phase 1 watchdog: initial-disconnect'" -or
        $runnerText -match '\$progress\.checkpoint -match ''\^state=STARTING') {
    throw 'Connected runner omitted the bounded startup-only disconnect retry contract'
}
function Invoke-StartupRetryPolicy([object[]]$observations) {
    $attempt = 0
    foreach ($observation in $observations) {
        $attempt++
        if ($observation -eq 'PASS') { return [pscustomobject]@{ attempts=$attempt; result='pass' } }
        $startupDisconnect = $observation.message -eq 'UI-smoke phase 1 watchdog: initial-disconnect'
        if (!$startupDisconnect) { return [pscustomobject]@{ attempts=$attempt; result='fail-closed' } }
        if ($attempt -eq 3) { return [pscustomobject]@{ attempts=$attempt; result='cap-exhausted' } }
    }
    return [pscustomobject]@{ attempts=$attempt; result='pass' }
}
$transient = Invoke-StartupRetryPolicy @(
    [pscustomobject]@{ message='UI-smoke phase 1 watchdog: initial-disconnect'; checkpoint='state=STARTING phase=PREPARE fixture=new cpu-list=INITIAL screen=net.minecraft.client.gui.screens.ProgressScreen' },
    'PASS')
if ($transient.attempts -ne 2 -or $transient.result -ne 'pass') { throw 'Immediate disconnect did not consume exactly one retry' }
$exhausted = Invoke-StartupRetryPolicy @(1..3 | ForEach-Object {
    [pscustomobject]@{ message='UI-smoke phase 1 watchdog: initial-disconnect'; checkpoint='state=STARTING phase=PREPARE fixture=new cpu-list=INITIAL screen=net.minecraft.client.gui.screens.ProgressScreen' }
})
if ($exhausted.attempts -ne 3 -or $exhausted.result -ne 'cap-exhausted') { throw 'Startup disconnect retry cap was not enforced' }
$started = Invoke-StartupRetryPolicy @(
    [pscustomobject]@{ message='UI-smoke phase 1 watchdog: checkpoint-stalled'; checkpoint='state=WORLD_READY phase=ACTIVE fixture=craftable cpu-list=INITIAL screen=net.minecraft.client.gui.screens.TitleScreen' })
if ($started.attempts -ne 1 -or $started.result -ne 'fail-closed') { throw 'Scenario progress incorrectly remained retryable' }
function Write-SourceMarker([string]$source, [string]$target, [int]$java, [string]$loader, [string]$launcher) {
    $dependency = Join-Path $source 'mods/dependency.jar'
    New-Item -ItemType Directory -Path (Split-Path -Parent $dependency) -Force | Out-Null
    Set-Content -LiteralPath $dependency -Value "dependency-$target"
    $relativeLauncher = $launcher.Replace('\\', '/')
    [ordered]@{
        schema=2; sourceFixtureId='ae2-crafting-time'; role='source'; target=$target
        javaMajor=$java; loader=$loader
        launcher=[ordered]@{path=$relativeLauncher;sha256=(Get-FileHash -LiteralPath (Join-Path $source $launcher) -Algorithm SHA256).Hash}
        dependencies=@([ordered]@{name='dependency.jar';sha256=(Get-FileHash -LiteralPath $dependency -Algorithm SHA256).Hash})
    } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $source '.ae2-crafting-time-dedicated-fixture.json')
}
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2 ct connected runner ' + [Guid]::NewGuid())
try {
    $source = Join-Path $temporary 'prepared server source'
    $bundle = Join-Path $temporary 'matching bundle'
    $report = Join-Path $temporary 'report with spaces'
    $javaHome = Join-Path $temporary 'java home'
    New-Item -ItemType Directory -Path (Join-Path $source 'libraries/net/minecraftforge/forge/1.20.1-47.4.10'),(Join-Path $source 'mods'),(Join-Path $bundle 'mods'),(Join-Path $javaHome 'bin') -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $source 'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt') -Value 'fixture'
    Set-Content -LiteralPath (Join-Path $bundle 'profile.json') -Value '{"target":"1.20.1-forge","java":17,"loader":"1.20.1-47.4.10"}'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/ae2-crafting-time-1-forge-1.20.1.jar') -Value 'production'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/ae2-crafting-time-1-forge-1.20.1-test-driver.jar') -Value 'driver'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/dependency.jar') -Value 'dependency-1.20.1-forge'
    Write-SourceMarker $source '1.20.1-forge' 17 '1.20.1-47.4.10' 'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt'
    Set-Content -LiteralPath (Join-Path $javaHome 'bin/java.exe') -Value 'java'
    $prepared = Join-Path $temporary 'prepared launch.json'
    Set-Content -LiteralPath $prepared -Value '{"target":"1.20.1-forge","java":17}'
    $sourceMarker = Join-Path $source '.ae2-crafting-time-dedicated-fixture.json'
    (Get-Item -LiteralPath $sourceMarker).IsReadOnly = $true
    & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $report `
        -JavaHome $javaHome -Address '127.0.0.1:25575' -PlanOnly
    $plan = Get-Content -LiteralPath (Join-Path $report 'connected-runner-plan.json') -Raw | ConvertFrom-Json
    if (!(Get-Item -LiteralPath $sourceMarker).IsReadOnly -or
            (Get-Item -LiteralPath (Join-Path $plan.disposableServer '.ae2-crafting-time-dedicated-fixture.json')).IsReadOnly) {
        throw 'Runner did not preserve a read-only source while making its disposable copy writable'
    }
    (Get-Item -LiteralPath $sourceMarker).IsReadOnly = $false
    if ($plan.sourceServer -eq $plan.disposableServer -or !$plan.disposableServer.StartsWith($report)) { throw 'Plan did not isolate a disposable server copy' }
    if (!$plan.campaignId -or !$plan.connectionEpoch -or !$plan.relaunch.required -or $plan.relaunch.minimumProcesses -ne 2) {
        throw 'Connected plan omitted the campaign-bound two-process relaunch contract'
    }
    if (!$plan.sourceIdentity.markerSha256 -or !$plan.sourceIdentity.launcherSha256 -or !$plan.sourceIdentity.javaVersion) {
        throw 'Connected plan omitted source marker, loader launcher, or Java identity'
    }
    if ($plan.sourceIdentity.loader -ne '1.20.1-47.4.10' -or $plan.sourceIdentity.javaMajor -ne 17 -or
            $plan.sourceIdentity.dependencies.Count -ne 1) {
        throw 'Connected plan omitted the verified loader, Java, or dependency identities'
    }
    $args = Get-Content -LiteralPath $plan.argumentFile
    $argsBytes = [IO.File]::ReadAllBytes($plan.argumentFile)
    if ($argsBytes.Length -ge 3 -and $argsBytes[0] -eq 0xEF -and $argsBytes[1] -eq 0xBB -and $argsBytes[2] -eq 0xBF) {
        throw 'Java argument file must use BOM-free UTF-8 under Windows PowerShell 5.1'
    }
    if (@($args | Where-Object { $_ -match 'serverResult=.*report with spaces' }).Count -ne 1 -or
            @($args | Where-Object { $_ -match 'serverControl=.*report with spaces' }).Count -ne 1) {
        throw 'Java argument file did not preserve paths with spaces'
    }
    if (Test-Path -LiteralPath (Join-Path $source 'eula.txt')) { throw 'Runner mutated its source fixture' }
    if ($plan.launchArguments.Count -ne 3 -or $plan.launchArguments[1] -ne '@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt') {
        throw 'Loader argument files must be separate launcher arguments, not nested in an argument file'
    }
    $expectedCommandLine = ($plan.launchArguments | ForEach-Object {
        '"' + ($_ -replace '(\\*)"', '$1$1\"' -replace '(\\+)$', '$1$1') + '"'
    }) -join ' '
    if ($plan.launchCommandLine -cne $expectedCommandLine -or $runnerText -match 'ArgumentList\.Add') {
        throw 'Connected runner did not render a PowerShell 5.1-compatible native argument string'
    }
    $capture = Join-Path $temporary 'capture native arguments.ps1'
    $captured = Join-Path $temporary 'captured native arguments.json'
    Set-Content -LiteralPath $capture -Value @'
param([string]$OutputPath)
[IO.File]::WriteAllText($OutputPath, ($args | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
'@
    $captureStart = [Diagnostics.ProcessStartInfo]::new()
    $captureStart.FileName = 'powershell.exe'
    $captureStart.UseShellExecute = $false
    $captureStart.Arguments = '-NoProfile -ExecutionPolicy Bypass -File "' + $capture + '" "' + $captured + '" ' + $plan.launchCommandLine
    $captureProcess = [Diagnostics.Process]::Start($captureStart)
    $captureProcess.WaitForExit()
    $capturedArguments = [object[]](Get-Content -LiteralPath $captured -Raw | ConvertFrom-Json)
    if ($captureProcess.ExitCode -ne 0 -or (Compare-Object @($plan.launchArguments) $capturedArguments -SyncWindow 0)) {
        throw "PowerShell 5.1 native child invocation changed arguments: $($capturedArguments -join ' | ')"
    }
    $captureProcess.Dispose()
    $serverProperties = Get-Content -LiteralPath (Join-Path $plan.disposableServer 'server.properties')
    if (!$serverProperties.Contains('server-port=25575')) {
        throw 'Custom server port was not applied'
    }
    if (!$serverProperties.Contains('enforce-secure-profile=false')) {
        throw 'Offline dedicated smoke must accept the prepared client profile'
    }
    foreach ($case in @(
        @{target='1.20.1-fabric'; java=17; loader='0.19.4'; launcher='fabric-server-launch.jar'},
        @{target='1.21.1-neoforge'; java=21; loader='21.1.238'; launcher='libraries/net/neoforged/neoforge/21.1.238/win_args.txt'},
        @{target='26.1.2-neoforge'; java=25; loader='26.1.2.99'; launcher='libraries/net/neoforged/neoforge/26.1.2.99/win_args.txt'})) {
        $caseSource = Join-Path $temporary ('source-' + $case.target)
        $caseBundle = Join-Path $temporary ('bundle-' + $case.target)
        $caseReport = Join-Path $temporary ('report-' + $case.target)
        $casePrepared = Join-Path $temporary ($case.target + '-launch.json')
        $caseLauncher = Join-Path $caseSource $case.launcher
        New-Item -ItemType Directory -Path (Split-Path -Parent $caseLauncher),(Join-Path $caseBundle 'mods') -Force | Out-Null
        Set-Content -LiteralPath $caseLauncher -Value 'fixture'
        $case | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $caseBundle 'profile.json')
        $case | ConvertTo-Json | Set-Content -LiteralPath $casePrepared
        $parts = $case.target.Split('-')
        foreach ($suffix in @('', '-test-driver')) {
            Set-Content -LiteralPath (Join-Path $caseBundle "mods/ae2-crafting-time-1-$($parts[1])-$($parts[0])$suffix.jar") -Value 'artifact'
        }
        Set-Content -LiteralPath (Join-Path $caseBundle 'mods/dependency.jar') -Value "dependency-$($case.target)"
        Write-SourceMarker $caseSource $case.target $case.java $case.loader $case.launcher
        & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target $case.target `
            -ServerDirectory $caseSource -PreparedLaunch $casePrepared -BundleDirectory $caseBundle `
            -ReportDirectory $caseReport -JavaHome $javaHome -PlanOnly
        $casePlan = Get-Content -LiteralPath (Join-Path $caseReport 'connected-runner-plan.json') -Raw | ConvertFrom-Json
        $expectedCount = if ($case.target -like '*-fabric') { 1 } else { 3 }
        if ($casePlan.launchArguments.Count -ne $expectedCount) { throw 'Wrong target-specific Java launch argument contract' }
        if (Test-Path -LiteralPath (Join-Path $caseSource 'server.properties')) { throw 'Runner changed a source server' }
    }
    foreach ($refusal in @(
        @{report=$report; address='127.0.0.1:25565'; expected='new report'},
        @{report=(Join-Path $source 'nested-report'); address='127.0.0.1:25565'; expected='outside'},
        @{report=(Join-Path $temporary 'remote-refused'); address='192.0.2.1:25565'; expected='loopback'})) {
        $rejected = $false
        try { & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
            -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $refusal.report `
            -JavaHome $javaHome -Address $refusal.address -PlanOnly }
        catch { $rejected = $_.Exception.Message -match $refusal.expected }
        if (!$rejected) { throw 'Connected runner did not reject an unsafe input' }
    }
    $marker = Get-Content -LiteralPath (Join-Path $source '.ae2-crafting-time-dedicated-fixture.json') -Raw | ConvertFrom-Json
    $marker.role = 'disposable'; $marker | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $source '.ae2-crafting-time-dedicated-fixture.json')
    $refused = $false
    try { & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory (Join-Path $temporary 'refused') `
        -JavaHome $javaHome -PlanOnly } catch { $refused = $_.Exception.Message -match 'marker' }
    if (!$refused) { throw 'Runner accepted an unmarked source fixture' }
    $marker.role = 'source'; $marker | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $source '.ae2-crafting-time-dedicated-fixture.json')
    Set-Content -LiteralPath (Join-Path $source 'mods/dependency.jar') -Value 'changed dependency'
    $dependencyReport = Join-Path $temporary 'dependency refused'
    $dependencyRefused = $false
    try { & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $dependencyReport `
        -JavaHome $javaHome -PlanOnly } catch { $dependencyRefused = $_.Exception.Message -match 'dependency' }
    if (!$dependencyRefused -or (Test-Path -LiteralPath $dependencyReport)) {
        throw 'Runner accepted a source dependency that no longer matches its marker and bundle'
    }
    Set-Content -LiteralPath (Join-Path $source 'mods/dependency.jar') -Value 'dependency-1.20.1-forge'
    Set-Content -LiteralPath (Join-Path $bundle 'mods/ae2-crafting-time-duplicate-forge-1.20.1.jar') -Value 'duplicate'
    $artifactReport = Join-Path $temporary 'artifact refused'
    $artifactRefused = $false
    try { & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $artifactReport `
        -JavaHome $javaHome -PlanOnly } catch { $artifactRefused = $_.Exception.Message -match 'exactly one' }
    if (!$artifactRefused -or (Test-Path -LiteralPath $artifactReport)) {
        throw 'Runner did not refuse invalid artifacts before mutating its report directory'
    }
    Write-Host 'connected dedicated runner checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temporary); $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if ($resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $resolved)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
