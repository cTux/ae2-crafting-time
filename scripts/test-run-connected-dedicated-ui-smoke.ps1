$ErrorActionPreference = 'Stop'
$refusedEula = $false
try {
    & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory 'C:\absent-source' -PreparedLaunch 'C:\absent-launch' -BundleDirectory 'C:\absent-bundle' -ReportDirectory 'C:\absent-report'
} catch { $refusedEula = $_.Exception.Message -like '*AcceptMinecraftEula*' }
if (!$refusedEula) { throw 'Runner must refuse missing EULA consent before accessing or writing input paths' }
$runnerText = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Raw
. (Join-Path $PSScriptRoot 'dedicated-source-contract.ps1')
$parseErrors = $null
$runnerAst = [Management.Automation.Language.Parser]::ParseInput($runnerText, [ref]$null, [ref]$parseErrors)
if ($parseErrors.Count) { throw 'Connected runner has invalid PowerShell syntax' }
$clientInvocation = $runnerAst.Find({ param($node)
    $node -is [Management.Automation.Language.CommandAst] -and
        $node.Extent.Text -match '^& .+run-ui-smoke\.ps1'
}, $true)
if (!$clientInvocation) { throw 'Connected runner has no synchronous client invocation' }
for ($ancestor = $clientInvocation.Parent; $ancestor; $ancestor = $ancestor.Parent) {
    if ($ancestor -is [Management.Automation.Language.IfStatementAst] -and
            $ancestor.Clauses[0].Item1.Extent.Text -match 'Scenario') {
        throw 'Shared client launch must not be conditional on the recurrence scenario'
    }
}
$clientRunner = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke.ps1') -Raw
if ($clientRunner -notmatch "else \{ 'ae2-crafting-time-smoke-client.lock' \}" -or
        $clientRunner -notmatch "Scenario -eq 'crafting-suspension' -and \`$DedicatedAddress -and \`$Role" -or
        $clientRunner -notmatch '"OpenOrCreate", "ReadWrite", "None"') {
    throw 'Only the two-client suspension scenario may use per-role smoke-client locks'
}
if ($runnerText -notmatch "ValidateSet\('cpu-list-total-ttc','recurrent-plan','stored-variant-plan','delayed-resource-icons','appmek-resource-icons','crafting-suspension'\)" -or
        $runnerText -notmatch "Ae2ctAlpha" -or $runnerText -notmatch "Ae2ctBeta" -or
        $runnerText -notmatch 'Start-Job' -or $runnerText -match 'recurrent-role-processes.json' -or
        $runnerText -match '22 \* 1024 \* 1024') {
    throw 'Connected runner must retain bounded Alpha execution and isolate two-client suspension orchestration'
}
if ($runnerText -notmatch 'if \(\$Scenario -eq ''crafting-suspension''\)' -or
        $runnerText -notmatch 'Invoke-SuspensionClients \$connectionEpoch 1' -or
        $runnerText -notmatch 'Invoke-SuspensionClients \$phase2Epoch 2' -or
        $runnerText -notmatch 'suspension-continuation.json' -or
        $runnerText -notmatch 'phase1\.suspensionEvidence\.jobId' -or
        $runnerText -notmatch 'phase2\.suspensionEvidence\.jobId' -or
        $runnerText -notmatch 'Stop-OwnedSuspensionClients' -or
        $runnerText -notmatch 'StartTime\.ToUniversalTime\(\)\.Ticks') {
    throw 'Suspension connected branch lost same-world restart, identity, or owned-client cleanup'
}
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
if ($runnerText -notmatch '\[ValidateRange\(1, 1800\)\]\[int\]\$ServerStartupTimeoutSeconds = 300' -or
        $runnerText -notmatch 'AddSeconds\(\$ServerStartupTimeoutSeconds\)' -or
        $runnerText -notmatch 'StartupTimeoutSeconds=\$ServerStartupTimeoutSeconds') {
    throw 'Connected server and client startup must retain the same bounded configurable deadline'
}
if ($runnerText -notmatch "\`$clientParameters\.RuntimeDirectory = Join-Path \`$attemptReport 'runtime'") {
    throw 'Connected client runtime must stay on the report-owned guest-local filesystem'
}
if ($runnerText.IndexOf("-SimpleMatch ']: Done ('", [StringComparison]::Ordinal) -lt 0 -or
        $runnerText.IndexOf('Invoke-SuspensionClients $connectionEpoch 1', [StringComparison]::Ordinal) -lt
            $runnerText.IndexOf("-SimpleMatch ']: Done ('", [StringComparison]::Ordinal) -or
        $runnerText.LastIndexOf("'run-ui-smoke.ps1'", [StringComparison]::Ordinal) -lt
            $runnerText.IndexOf("-SimpleMatch ']: Done ('", [StringComparison]::Ordinal)) {
    throw 'Connected client must wait for the dedicated server startup-complete marker'
}
if ($runnerText -notmatch '\$maxAttempts = 3' -or
        $runnerText -notmatch 'FailOnInitialDisconnect=\$true' -or
        $runnerText -notmatch "startup-failure-cap-exhausted" -or
        $runnerText -notmatch "'UI-smoke phase 1 watchdog: initial-disconnect'" -or
        $runnerText -notmatch "'UI-smoke phase 1 watchdog: startup-timeout'" -or
        $runnerText -match '\$progress\.checkpoint -match ''\^state=STARTING') {
    throw 'Connected runner omitted the bounded startup-only disconnect retry contract'
}
if ($runnerText.LastIndexOf('Stop-OwnedSuspensionClients', [StringComparison]::Ordinal) -gt
        $runnerText.LastIndexOf('& robocopy.exe', [StringComparison]::Ordinal) -or
        $runnerText.LastIndexOf('$serverProcess.Dispose()', [StringComparison]::Ordinal) -gt
            $runnerText.LastIndexOf('& robocopy.exe', [StringComparison]::Ordinal)) {
    throw 'Connected server archive must follow owned-process shutdown'
}
function Invoke-StartupRetryPolicy([object[]]$observations) {
    $attempt = 0
    foreach ($observation in $observations) {
        $attempt++
        if ($observation -eq 'PASS') { return [pscustomobject]@{ attempts=$attempt; result='pass' } }
        $startupFailure = $observation.message -in @('UI-smoke phase 1 watchdog: initial-disconnect',
            'UI-smoke phase 1 watchdog: startup-timeout')
        if (!$startupFailure) { return [pscustomobject]@{ attempts=$attempt; result='fail-closed' } }
        if ($attempt -eq 3) { return [pscustomobject]@{ attempts=$attempt; result='cap-exhausted' } }
    }
    return [pscustomobject]@{ attempts=$attempt; result='pass' }
}
$transient = Invoke-StartupRetryPolicy @(
    [pscustomobject]@{ message='UI-smoke phase 1 watchdog: initial-disconnect'; checkpoint='state=STARTING phase=PREPARE fixture=new cpu-list=INITIAL screen=net.minecraft.client.gui.screens.ProgressScreen' },
    'PASS')
if ($transient.attempts -ne 2 -or $transient.result -ne 'pass') { throw 'Immediate disconnect did not consume exactly one retry' }
$coldTimeout = Invoke-StartupRetryPolicy @(
    [pscustomobject]@{ message='UI-smoke phase 1 watchdog: startup-timeout'; checkpoint='state=STARTING phase=SETUP resource-case=0 stage=0 fixture-phase=pending' },
    'PASS')
if ($coldTimeout.attempts -ne 2 -or $coldTimeout.result -ne 'pass') { throw 'Cold pre-fixture timeout did not consume exactly one retry' }
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
$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ('f' + [Guid]::NewGuid().ToString('N').Substring(0,8))
$retainedWorkspaces = @()
$tooLongTemp = $null
$originalTmp = $env:TMP; $originalTemp = $env:TEMP
try {
    New-Item -ItemType Directory -Path $temporary,$fixtureRoot | Out-Null
    $global:connectedSetupLaunches = 0
    function Start-Process { $global:connectedSetupLaunches++; throw 'Unexpected process launch during connected setup checks' }
    $source = Join-Path $fixtureRoot 'source'
    $bundle = Join-Path $temporary 'matching bundle'
    $report = Join-Path $temporary 'report with spaces'
    $javaHome = Join-Path $temporary 'java home'
    New-Item -ItemType Directory -Path (Join-Path $source 'libraries/net/minecraftforge/forge/1.20.1-47.4.10'),(Join-Path $source 'mods'),(Join-Path $bundle 'mods'),(Join-Path $javaHome 'bin') -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $source 'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt') -Value 'fixture'
    $deepRelative = 'libraries/com/google/errorprone/error_prone_annotations/2.1.3/error_prone_annotations-2.1.3.jar'
    $predictedLiveDeep = Join-Path (Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-' + ('0' * 32))) ('server-1.20.1-forge/' + $deepRelative)
    if ($predictedLiveDeep.Length -ge 260) { throw 'Test host has no short usable temporary server path' }
    $deepSource = Join-Path $source $deepRelative
    New-Item -ItemType Directory -Path (Split-Path -Parent $deepSource) -Force | Out-Null
    [IO.File]::WriteAllBytes($deepSource, [byte[]](0,1,2,127,255))
    (Get-Item -LiteralPath $deepSource).IsReadOnly = $true
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
    $archive = Get-Content -LiteralPath (Join-Path $report 'connected-server-archive.json') -Raw | ConvertFrom-Json
    if (!(Get-Item -LiteralPath $sourceMarker).IsReadOnly -or
            (Get-Item -LiteralPath (Join-Path $plan.archiveServer '.ae2-crafting-time-dedicated-fixture.json')).IsReadOnly) {
        throw 'Runner did not preserve a read-only source while making its disposable copy writable'
    }
    (Get-Item -LiteralPath $sourceMarker).IsReadOnly = $false
    if ($plan.sourceServer -eq $plan.disposableServer -or $plan.disposableServer.StartsWith($report) -or
            $plan.archiveServer -cne (Join-Path $report 'runtime/server-1.20.1-forge') -or
            $archive.status -cne 'archived' -or $archive.workspaceRetained -or
            (Test-Path -LiteralPath $plan.disposableServer)) { throw 'Plan did not archive and remove its short disposable server copy' }
    $longReportParent = Join-Path $fixtureRoot '1.20.1-forge/compatible/cpu-list-total-ttc'
    $longReport = Join-Path $longReportParent ('c' * (185 - $longReportParent.Length - 1))
    $oldDestination = Join-Path (Join-Path $longReport 'runtime/server-1.20.1-forge') $deepRelative
    if ($longReport.Length -gt 200 -or (Join-Path $longReport 'connected-server-archive.json').Length -ge 260 -or
            $oldDestination.Length -lt 260) {
        throw 'Long-report control did not reproduce the former path limit with a usable report root'
    }
    $oldRuntime = Join-Path ($longReport + '-old') 'runtime'
    New-Item -ItemType Directory -Path $oldRuntime -Force | Out-Null
    $oldCopyError = $null
    try { Copy-Item -LiteralPath $source -Destination (Join-Path $oldRuntime 'server-1.20.1-forge') -Recurse }
    catch { $oldCopyError = $_ }
    if (!$oldCopyError) { throw 'Original report-owned staging unexpectedly passed the legacy path-limit control' }
    & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $longReport `
        -JavaHome $javaHome -PlanOnly
    $longPlan = Get-Content -LiteralPath (Join-Path $longReport 'connected-runner-plan.json') -Raw | ConvertFrom-Json
    $longArchive = Get-Content -LiteralPath (Join-Path $longReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
    $inspection = Join-Path $fixtureRoot 'inspection'
    & robocopy.exe $longPlan.archiveServer $inspection /E /R:0 /W:0 | Out-Null
    if ($LASTEXITCODE -gt 7) { throw 'Could not inspect the long report archive' }
    if ($longArchive.status -cne 'archived' -or $longArchive.workspaceRetained -or
            (Test-Path -LiteralPath $longPlan.disposableServer) -or
            (Get-FileHash -LiteralPath (Join-Path $inspection $deepRelative)).Hash -cne
                (Get-FileHash -LiteralPath $deepSource).Hash -or
            !(Get-Item -LiteralPath $deepSource).IsReadOnly -or
            (Get-Item -LiteralPath (Join-Path $inspection $deepRelative)).IsReadOnly) {
        throw 'Long report did not retain the same server bytes and preserve source attributes'
    }
    $sealedSource = Join-Path $fixtureRoot 'sealed'
    Copy-Item -LiteralPath $source -Destination $sealedSource -Recurse
    $seal = Get-DedicatedTree $sealedSource
    $sealPath = Join-Path $sealedSource 'source-seal.json'
    $seal | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $sealPath
    $sealedMarkerPath = Join-Path $sealedSource '.ae2-crafting-time-dedicated-fixture.json'
    $sealedMarker = Get-Content -LiteralPath $sealedMarkerPath -Raw | ConvertFrom-Json
    $sealedMarker | Add-Member -NotePropertyName provisioning -NotePropertyValue ([ordered]@{
        schema=1;sourceKey=('a' * 64);sealPath='source-seal.json'
        sealSha256=(Get-FileHash -LiteralPath $sealPath).Hash
    })
    $sealedMarker | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $sealedMarkerPath
    $sealedMarkerHash = (Get-FileHash -LiteralPath $sealedMarkerPath).Hash
    $sealedSealHash = (Get-FileHash -LiteralPath $sealPath).Hash
    $sealedReport = Join-Path $temporary 'sealed-report'
    & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $sealedSource -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $sealedReport `
        -JavaHome $javaHome -PlanOnly
    $sealedArchive = Get-Content -LiteralPath (Join-Path $sealedReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
    if ($sealedArchive.status -cne 'archived' -or
            (Get-FileHash -LiteralPath $sealedMarkerPath).Hash -cne $sealedMarkerHash -or
            (Get-FileHash -LiteralPath $sealPath).Hash -cne $sealedSealHash -or
            (Get-FileHash -LiteralPath (Join-Path $sealedArchive.archiveServer 'source-seal.json')).Hash -cne $sealedSealHash) {
        throw 'Sealed source changed or its copy was not retained'
    }
    $setupReport = Join-Path $temporary 'setup-failure-report'
    $setupError = $null
    try {
        & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
            -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $setupReport `
            -JavaHome $javaHome -ObservationMode -NativeWorldDirectory (Join-Path $temporary 'missing-native') -PlanOnly
    } catch { $setupError = $_ }
    $setupArchive = Get-Content -LiteralPath (Join-Path $setupReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
    if (!$setupError -or $setupError.Exception.Message -notmatch 'marked stopped native world' -or
            $setupArchive.status -cne 'archived' -or $setupArchive.workspaceRetained -or
            !(Test-Path -LiteralPath $setupArchive.archiveServer)) {
        throw 'Setup failure lost its original error or server archive'
    }
    $global:partialSource = $source
    function Copy-Item {
        [CmdletBinding()]param([string]$LiteralPath,[string]$Destination,[switch]$Recurse)
        if ($LiteralPath -ceq $global:partialSource -and $Recurse) {
            New-Item -ItemType Directory -Path $Destination | Out-Null
            Set-Content -LiteralPath (Join-Path $Destination 'partial.txt') -Value 'recoverable partial copy'
            throw 'injected partial copy failure'
        }
        Microsoft.PowerShell.Management\Copy-Item @PSBoundParameters
    }
    try {
        $partialReport = Join-Path $temporary 'partial-copy-report'
        $partialError = $null
        try {
            & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $partialReport `
                -JavaHome $javaHome -PlanOnly
        } catch { $partialError = $_ }
        $partialArchive = Get-Content -LiteralPath (Join-Path $partialReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
        if (!$partialError -or $partialError.Exception.Message -notmatch 'injected partial copy failure' -or
                $partialArchive.status -cne 'archived' -or $partialArchive.workspaceRetained -or
                (Get-Content -LiteralPath (Join-Path $partialArchive.archiveServer 'partial.txt')) -cne 'recoverable partial copy') {
            throw 'Partial-copy failure did not retain evidence and its first error'
        }
    } finally { Remove-Item Function:\Copy-Item -ErrorAction SilentlyContinue; Remove-Variable partialSource -Scope Global -ErrorAction SilentlyContinue }
    function robocopy.exe { $global:LASTEXITCODE = $global:archiveExit; New-Item -ItemType Directory -Path $args[1] -Force | Out-Null }
    try {
        foreach ($code in @(-1,8)) {
            $global:archiveExit = $code
            $failedReport = Join-Path $temporary "archive-failure-$code-report"
            $archiveError = $null
            try {
                & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                    -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $failedReport `
                    -JavaHome $javaHome -PlanOnly
            } catch { $archiveError = $_ }
            $failedArchive = Get-Content -LiteralPath (Join-Path $failedReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
            $retainedWorkspaces += $failedArchive.workspace
            if (!$archiveError -or $archiveError.Exception.Message -notmatch 'archive/cleanup failed' -or
                    $failedArchive.status -cne 'failed' -or !$failedArchive.workspaceRetained -or
                    !(Test-Path -LiteralPath (Join-Path (Join-Path $failedArchive.workspace 'server-1.20.1-forge') $deepRelative))) {
                throw 'Failed archive did not retain and report the recoverable server workspace'
            }
        }
    } finally { Remove-Item Function:\robocopy.exe -ErrorAction SilentlyContinue; Remove-Variable archiveExit -Scope Global -ErrorAction SilentlyContinue }
    function robocopy.exe {
        & (Join-Path $env:WINDIR 'System32/robocopy.exe') @args | Out-Null
        $receipt = Join-Path (Split-Path -Parent (Split-Path -Parent $args[1])) 'connected-server-archive.json'
        New-Item -ItemType Directory -Path $receipt | Out-Null
    }
    try {
        $receiptReport = Join-Path $temporary 'receipt-failure-report'
        $receiptError = $null
        try {
            & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $receiptReport `
                -JavaHome $javaHome -ObservationMode -NativeWorldDirectory (Join-Path $temporary 'missing-native') -PlanOnly
        } catch { $receiptError = $_ }
        if (!$receiptError -or $receiptError.Exception.Message -notmatch 'marked stopped native world' -or
                $receiptError.Exception.Message -notmatch 'receipt:' -or
                !(Test-Path -LiteralPath (Join-Path $receiptReport 'runtime/server-1.20.1-forge'))) {
            throw 'Archive receipt failure masked the original setup failure or lost the server archive'
        }
    } finally { Remove-Item Function:\robocopy.exe -ErrorAction SilentlyContinue }
    function New-Item {
        [CmdletBinding()]param([string]$ItemType,[string]$Path,[switch]$Force)
        if ($Path -match '[\\/]ae2ct-[a-f0-9]{32}$') {
            Microsoft.PowerShell.Management\New-Item @PSBoundParameters | Out-Null
            $global:collisionWorkspace = $Path
            Set-Content -LiteralPath (Join-Path $Path 'foreign.txt') -Value 'another owner'
            throw 'injected workspace collision'
        }
        Microsoft.PowerShell.Management\New-Item @PSBoundParameters
    }
    try {
        $collisionReport = Join-Path $temporary 'collision-report'
        $collisionError = $null
        try {
            & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $collisionReport `
                -JavaHome $javaHome -AcceptMinecraftEula
        } catch { $collisionError = $_ }
        $collisionArchive = Get-Content -LiteralPath (Join-Path $collisionReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
        if (!$collisionError -or $collisionError.Exception.Message -notmatch 'injected workspace collision' -or
                $collisionArchive.status -cne 'not-created' -or
                (Get-Content -LiteralPath (Join-Path $global:collisionWorkspace 'foreign.txt')) -cne 'another owner') {
            throw 'Foreign workspace collision was mutated or its original error was lost'
        }
    } finally {
        Remove-Item Function:\New-Item -ErrorAction SilentlyContinue
        if ($global:collisionWorkspace -and $global:collisionWorkspace.StartsWith([IO.Path]::GetTempPath(), [StringComparison]::OrdinalIgnoreCase) -and
                (Test-Path -LiteralPath (Join-Path $global:collisionWorkspace 'foreign.txt'))) {
            Remove-Item -LiteralPath $global:collisionWorkspace -Recurse -Force
        }
        Remove-Variable collisionWorkspace -Scope Global -ErrorAction SilentlyContinue
    }
    $linkedTemp = Join-Path ([IO.Path]::GetTempPath()) ('j' + [Guid]::NewGuid().ToString('N').Substring(0,8))
    New-Item -ItemType Junction -Path $linkedTemp -Target $fixtureRoot | Out-Null
    $env:TMP = $linkedTemp; $env:TEMP = $linkedTemp
    try {
        $linkedReport = Join-Path $temporary 'linked-temp-refused'
        $linkedError = $null
        try {
            & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $linkedReport `
                -JavaHome $javaHome -AcceptMinecraftEula
        } catch { $linkedError = $_ }
        $linkedArchive = Get-Content -LiteralPath (Join-Path $linkedReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
        if (!$linkedError -or $linkedError.Exception.Message -notmatch 'linked ancestor' -or
                $linkedArchive.status -cne 'not-created' -or
                !(Test-Path -LiteralPath $source)) {
            throw 'Linked temporary ancestor was not refused before launch'
        }
    } finally {
        $env:TMP = $originalTmp; $env:TEMP = $originalTemp
        if (Test-Path -LiteralPath $linkedTemp) { [IO.Directory]::Delete($linkedTemp) }
    }
    $tooLongTemp = Join-Path ([IO.Path]::GetTempPath()) ('temp-root-' + ('x' * 100))
    New-Item -ItemType Directory -Path $tooLongTemp | Out-Null
    $env:TMP = $tooLongTemp; $env:TEMP = $tooLongTemp
    try {
        $longTempReport = Join-Path $temporary 'long-temp-refused'
        $longTempError = $null
        try {
            & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $longTempReport `
                -JavaHome $javaHome -AcceptMinecraftEula
        } catch { $longTempError = $_ }
        $longTempArchive = Get-Content -LiteralPath (Join-Path $longTempReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
        if (!$longTempError -or $longTempError.Exception.Message -notmatch 'path exceeds' -or
                $longTempArchive.status -cne 'not-created' -or $longTempArchive.workspaceRetained) {
            throw 'Unsuitable temp root was not refused before a Java launch'
        }
    } finally { $env:TMP = $originalTmp; $env:TEMP = $originalTemp }
    function robocopy.exe {
        & (Join-Path $env:WINDIR 'System32/robocopy.exe') @args | Out-Null
        $global:LASTEXITCODE = 7
        $owner = Join-Path (Split-Path -Parent $args[0]) 'ownership.json'
        $record = Get-Content -LiteralPath $owner -Raw | ConvertFrom-Json
        $record.campaignId = 'foreign'
        $record | ConvertTo-Json | Set-Content -LiteralPath $owner
    }
    try {
        $cleanupReport = Join-Path $temporary 'cleanup-refused-report'
        $cleanupError = $null
        try {
            & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
                -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $cleanupReport `
                -JavaHome $javaHome -PlanOnly
        } catch { $cleanupError = $_ }
        $cleanupArchive = Get-Content -LiteralPath (Join-Path $cleanupReport 'connected-server-archive.json') -Raw | ConvertFrom-Json
        $retainedWorkspaces += $cleanupArchive.workspace
        if (!$cleanupError -or $cleanupError.Exception.Message -notmatch 'ownership changed before cleanup' -or
                $cleanupArchive.status -cne 'failed' -or !$cleanupArchive.workspaceRetained -or
                !(Test-Path -LiteralPath $cleanupArchive.archiveServer)) {
            throw 'Cleanup did not refuse changed ownership after retaining the archive'
        }
    } finally { Remove-Item Function:\robocopy.exe -ErrorAction SilentlyContinue }
    if ($global:connectedSetupLaunches -ne 0) { throw 'Connected setup failure launched a process' }
    Remove-Item Function:\Start-Process
    Remove-Variable connectedSetupLaunches -Scope Global
    $finalizerAst = $runnerAst.Find({ param($node)
        $node -is [Management.Automation.Language.TryStatementAst] -and $node.Finally -and
            $node.Finally.Extent.Text -match '^\{\s*\$archiveStatus ='
    }, $true)
    if (!$finalizerAst) { throw 'Connected archive finalizer was not found' }
    $finalizerText = $finalizerAst.Finally.Extent.Text
    $finalizer = [scriptblock]::Create($finalizerText.Substring(1, $finalizerText.Length - 2))
    $shutdownAst = $runnerAst.Find({ param($node)
        $node -is [Management.Automation.Language.TryStatementAst] -and $node.Finally -and
            $node.Finally.Extent.Text -match '^\{\s*try\s*\{\s*if \(\$Scenario'
    }, $true)
    if (!$shutdownAst) { throw 'Connected process cleanup finalizer was not found' }
    $shutdownText = $shutdownAst.Finally.Extent.Text
    $shutdownFinalizer = [scriptblock]::Create($shutdownText.Substring(1, $shutdownText.Length - 2))
    foreach ($shutdownCase in @('running','exited','null','failed','foreign','no-server')) {
        $report = Join-Path $temporary "shutdown-$shutdownCase"
        $workspace = Join-Path $fixtureRoot "shutdown-$shutdownCase"
        $resolvedServer = Join-Path $workspace 'server-1.20.1-forge'
        $runtimeRoot = Join-Path $report 'runtime'
        $archiveServer = Join-Path $runtimeRoot 'server-1.20.1-forge'
        $ownerPath = Join-Path $workspace 'ownership.json'
        $Target = '1.20.1-forge'; $campaignId = 'shutdown-check'
        $workspaceAllocated = $true; $runFailure = $null; $archiveFailure = $null; $shutdownFailure = $null
        New-Item -ItemType Directory -Path $report,$resolvedServer | Out-Null
        Set-Content -LiteralPath (Join-Path $resolvedServer 'evidence.txt') -Value 'retained'
        [ordered]@{schema=1;campaignId=$campaignId;report=$report;server=$resolvedServer} |
            ConvertTo-Json | Set-Content -LiteralPath $ownerPath
        $global:connectedShutdownEvents = [Collections.Generic.List[string]]::new()
        $serverProcess = [pscustomobject]@{HasExited=($shutdownCase -eq 'exited')}
        $serverProcess | Add-Member ScriptMethod Kill { $global:connectedShutdownEvents.Add('kill') }
        $serverProcess | Add-Member ScriptMethod WaitForExit { $global:connectedShutdownEvents.Add('wait'); $this.HasExited = $true }
        $serverProcess | Add-Member ScriptMethod Dispose { $global:connectedShutdownEvents.Add('dispose') }
        if ($shutdownCase -in @('null','foreign','no-server')) { $serverProcess = $null }
        if ($shutdownCase -eq 'foreign') {
            $foreignOwner = Get-Content -LiteralPath $ownerPath -Raw | ConvertFrom-Json
            $foreignOwner.campaignId = 'another-campaign'
            $foreignOwner | ConvertTo-Json | Set-Content -LiteralPath $ownerPath
        }
        if ($shutdownCase -eq 'no-server') { Remove-Item -LiteralPath $resolvedServer -Recurse -Force }
        if ($shutdownCase -eq 'failed') {
            $Scenario = 'crafting-suspension'
            function Stop-OwnedSuspensionClients { throw 'client shutdown unconfirmed' }
            try { . $shutdownFinalizer }
            finally { Remove-Item Function:\Stop-OwnedSuspensionClients }
            if ($runFailure -or !$shutdownFailure -or $shutdownFailure.Exception.Message -cne 'client shutdown unconfirmed') {
                throw 'Successful run body did not attribute its cleanup failure to shutdown'
            }
        }
        function robocopy.exe {
            $global:connectedShutdownEvents.Add('archive')
            & (Join-Path $env:WINDIR 'System32/robocopy.exe') @args | Out-Null
        }
        $finalizerError = $null
        try { & $finalizer } catch { $finalizerError = $_ }
        finally { Remove-Item Function:\robocopy.exe }
        $shutdownReceipt = Get-Content -LiteralPath (Join-Path $report 'connected-server-archive.json') -Raw | ConvertFrom-Json
        $expectedEvents = switch ($shutdownCase) {
            'running' { 'kill,wait,dispose,archive' }
            'exited' { 'dispose,archive' }
            'null' { 'archive' }
            'failed' { 'kill,wait,dispose' }
            'foreign' { '' }
            'no-server' { '' }
        }
        if (($global:connectedShutdownEvents -join ',') -cne $expectedEvents) { throw "Invalid shutdown/archive ordering: $shutdownCase" }
        if ($shutdownCase -in @('failed','foreign')) {
            if (!$finalizerError -or !$shutdownReceipt.workspaceRetained -or $shutdownReceipt.status -cne 'failed') {
                throw 'Unconfirmed owned-client shutdown did not retain the workspace'
            }
        } elseif ($shutdownCase -eq 'no-server') {
            if ($finalizerError -or !$shutdownReceipt.workspaceRetained -or $shutdownReceipt.status -cne 'no-server-tree') {
                throw 'Workspace without a server tree was not retained'
            }
        } elseif ($finalizerError -or $shutdownReceipt.workspaceRetained -or $shutdownReceipt.status -cne 'archived') {
            throw "Stopped process did not archive and clean up: $shutdownCase"
        }
        Remove-Variable connectedShutdownEvents -Scope Global
    }
    $report = $plan.archiveServer | Split-Path -Parent | Split-Path -Parent
    if (!$plan.campaignId -or !$plan.connectionEpoch -or !$plan.relaunch.required -or $plan.relaunch.minimumProcesses -ne 2) {
        throw 'Connected plan omitted the campaign-bound two-process relaunch contract'
    }
    $resourceReport = Join-Path $temporary 'resource-plan'
    & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target 1.20.1-forge `
        -ServerDirectory $source -PreparedLaunch $prepared -BundleDirectory $bundle -ReportDirectory $resourceReport `
        -JavaHome $javaHome -Scenario delayed-resource-icons -ResourceFixtureOnly -PlanOnly
    $resourcePlan = Get-Content -LiteralPath (Join-Path $resourceReport 'connected-runner-plan.json') -Raw | ConvertFrom-Json
    if ($resourcePlan.resourceFixture -cnotmatch '^[a-f0-9]{32}$' -or
            !($resourcePlan.arguments -contains "-Dae2craftingtime.test.resourceFixture=$($resourcePlan.resourceFixture)")) {
        throw 'Connected resource plan omitted its explicit launch fixture identity'
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
    $serverProperties = Get-Content -LiteralPath (Join-Path $plan.archiveServer 'server.properties')
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
        $recurrenceReport = Join-Path $temporary ('recurrent-' + $case.target)
        & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target $case.target -Scenario recurrent-plan `
            -ServerDirectory $caseSource -PreparedLaunch $casePrepared -BundleDirectory $caseBundle `
            -ReportDirectory $recurrenceReport -JavaHome $javaHome -PlanOnly
        $recurrencePlan = Get-Content -LiteralPath (Join-Path $recurrenceReport 'connected-runner-plan.json') -Raw | ConvertFrom-Json
        if ($recurrencePlan.relaunch.required -or $recurrencePlan.relaunch.minimumProcesses -ne 1 -or
                !($recurrencePlan.arguments -contains '-Dae2ct.testDriver.serverScenario=recurrent-plan-connected')) {
            throw 'Recurrence plan retained CPU relaunch or server scenario metadata'
        }
        $variantReport = Join-Path $temporary ('variant-' + $case.target)
        & (Join-Path $PSScriptRoot 'run-connected-dedicated-ui-smoke.ps1') -Target $case.target -Scenario stored-variant-plan `
            -ServerDirectory $caseSource -PreparedLaunch $casePrepared -BundleDirectory $caseBundle `
            -ReportDirectory $variantReport -JavaHome $javaHome -PlanOnly
        $variantPlan = Get-Content -LiteralPath (Join-Path $variantReport 'connected-runner-plan.json') -Raw | ConvertFrom-Json
        if ($variantPlan.relaunch.required -or $variantPlan.relaunch.minimumProcesses -ne 1 -or
                !($variantPlan.arguments -contains '-Dae2ct.testDriver.serverScenario=stored-variant-plan-connected')) {
            throw 'Variant plan retained CPU relaunch or server scenario metadata'
        }
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
    Remove-Item Function:\Start-Process -ErrorAction SilentlyContinue
    Remove-Variable connectedSetupLaunches,connectedShutdownEvents -Scope Global -ErrorAction SilentlyContinue
    $env:TMP = $originalTmp; $env:TEMP = $originalTemp
    foreach ($owned in $retainedWorkspaces) {
        if ($owned -and $owned.StartsWith([IO.Path]::GetTempPath(), [StringComparison]::OrdinalIgnoreCase) -and
                (Test-Path -LiteralPath (Join-Path $owned 'ownership.json'))) {
            Remove-Item -LiteralPath $owned -Recurse -Force
        }
    }
    if (Test-Path -LiteralPath $fixtureRoot) {
        $emptyFixture = Join-Path $temporary 'empty-cleanup'
        New-Item -ItemType Directory -Path $emptyFixture -Force | Out-Null
        # Robocopy removes the deliberately overlong archive paths before legacy PowerShell cleanup.
        & (Join-Path $env:WINDIR 'System32/robocopy.exe') $emptyFixture $fixtureRoot /MIR /R:0 /W:0 /XJ | Out-Null
        if ($LASTEXITCODE -lt 0 -or $LASTEXITCODE -gt 7) { throw 'Could not clean up long-path test fixtures' }
        Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
    }
    if ($tooLongTemp -and (Test-Path -LiteralPath $tooLongTemp)) { Remove-Item -LiteralPath $tooLongTemp -Recurse -Force }
    $resolved = [IO.Path]::GetFullPath($temporary); $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if ($resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $resolved)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
