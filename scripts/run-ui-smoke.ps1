param(
    [ValidateSet("1.20.1-forge", "1.20.1-fabric")][string]$Target = "1.20.1-forge",
    [switch]$Latest,
    [switch]$Interactive,
    [ValidatePattern("^(suite|craft-plan|crafting-tree-screen|merequester-screen|ae2networkanalyser-screen|aeinfinitybooster-terminal|ae2importexportcard-terminal|ae2(?:wcwt|wtlib)-terminal|[a-z0-9]+(?:-[a-z0-9]+)*-cpu)$")][string]$Scenario = "craft-plan",
    [string[]]$ProjectId,
    [string]$ReportDirectory
)

$ErrorActionPreference = "Stop"
if ($Scenario -eq "suite" -and ($Interactive -or $Latest -or $ProjectId)) {
    throw "The prepared suite requires the full compatible profile and non-interactive execution"
}
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root "versions\1.20.1-forge\run\saves\ae2-crafting-time"
$loader = $Target.Substring("1.20.1-".Length)
$modsDirectory = if ($Target -eq "1.20.1-forge") { "resolved-mods" } else { "mods" }
$profile = if ($Latest) { "latest" } else { "compatible" }
$base = Join-Path $root "build\ui-smoke\$Target\$profile"
$report = if ($ReportDirectory) { [IO.Path]::GetFullPath($ReportDirectory) } else { Join-Path $base $Scenario }
$runtime = Join-Path $base "runtime"
$evidence = Join-Path $report "evidence"
$world = "ae2ct-$([guid]::NewGuid().ToString('N'))"
$worldCopy = Join-Path $runtime "saves\$world"
$worldCopies = @($worldCopy)
$stdout = Join-Path $report "launcher.stdout.log"
$stderr = Join-Path $report "launcher.stderr.log"
$statusPath = Join-Path $report "status.json"
$runId = [guid]::NewGuid().ToString("N")
$startedAt = [DateTime]::UtcNow.ToString("o")
$process = $null

function Get-TreeHash([string]$path) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $text = (Get-ChildItem -LiteralPath $path -File -Recurse | Sort-Object FullName | ForEach-Object {
            "$($_.FullName.Substring($path.Length))|$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)"
        }) -join "`n"
        return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))) -replace '-', '')
    } finally { $sha.Dispose() }
}

function Write-Status([string]$phase, [string]$message = "", [Nullable[int]]$exitCode = $null) {
    $status = [ordered]@{
        schema = 1; runId = $runId; target = $Target; profile = $profile; scenario = $Scenario
        phase = $phase; pid = $(if ($process) { $process.Id } else { $null }); exitCode = $exitCode
        startedAt = $startedAt; updatedAt = [DateTime]::UtcNow.ToString("o"); javaHome = $env:JAVA_HOME
        stagedRoot = $root; stdout = $stdout; stderr = $stderr; evidence = $evidence; message = $message
    }
    $temporary = "$statusPath.$runId.tmp"
    [IO.File]::WriteAllText($temporary, ($status | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temporary -Destination $statusPath -Force
}

if (-not (Test-Path -LiteralPath (Join-Path $source ".ae2-crafting-time-test-fixture.json") -PathType Leaf)) {
    throw "Missing tracked Forge 1.20.1 test fixture"
}
$sourceMarker = Get-Content -LiteralPath (Join-Path $source ".ae2-crafting-time-test-fixture.json") -Raw | ConvertFrom-Json
if ($sourceMarker.schema -ne 1 -or $sourceMarker.scenario -ne "craft-plan" -or
        $sourceMarker.sourceFixtureId -ne "ae2-crafting-time" -or $sourceMarker.disposableWorldId -ne "SOURCE_ONLY") {
    throw "Tracked source fixture marker is invalid or executable"
}
$sourceHash = Get-TreeHash $source
$buildRoot = [IO.Path]::GetFullPath((Join-Path $root "build\ui-smoke"))
$resolvedBase = [IO.Path]::GetFullPath($base)
if (-not $resolvedBase.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "UI-smoke output escapes build directory"
}
New-Item -ItemType Directory -Path $base, $report, (Split-Path -Parent $worldCopy) -Force | Out-Null
try {
    $runtimeLock = [IO.File]::Open((Join-Path $base "runtime.lock"), "OpenOrCreate", "ReadWrite", "None")
} catch {
    throw "Another $profile UI-smoke scenario is already using this workspace runtime"
}
if (Test-Path -LiteralPath $evidence) { Remove-Item -LiteralPath $evidence -Recurse -Force }
if ($Scenario -ne "suite") { New-Item -ItemType Directory -Path $evidence -Force | Out-Null }
Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue
Write-Status "preparing"
if ($Scenario -eq "suite") {
    $scenarios = Get-Content -LiteralPath (Join-Path $PSScriptRoot "ui-smoke-$loader-suite.json") -Raw | ConvertFrom-Json
    $suite = & (Join-Path $PSScriptRoot "prepare-ui-smoke-suite.ps1") -RuntimeDirectory $runtime -OutputDirectory $evidence -Scenarios $scenarios
    $world = $suite.world
    $plan = Get-Content -LiteralPath (Join-Path $evidence "suite-plan.json") -Raw | ConvertFrom-Json
    $worldCopies = @($plan.cases | ForEach-Object { Join-Path $runtime "saves\$($_.world)" })
} else {
    Copy-Item -LiteralPath $source -Destination $worldCopy -Recurse
    $markerPath = Join-Path $worldCopy ".ae2-crafting-time-test-fixture.json"
    $marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
    $marker.disposableWorldId = $world
    [IO.File]::WriteAllText($markerPath, ($marker | ConvertTo-Json -Depth 10), [Text.UTF8Encoding]::new($false))
}
[IO.File]::WriteAllText((Join-Path $runtime "options.txt"), @"
version:3465
fullscreen:false
overrideWidth:854
overrideHeight:480
guiScale:2
lang:en_us
maxFps:60
pauseOnLostFocus:false
soundCategory_master:0.0
"@, [Text.UTF8Encoding]::new($false))

$arguments = @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$(Join-Path $PSScriptRoot 'run-client.ps1')`"",
    "-Target", $Target, "-RuntimeDirectory", "`"$runtime`"",
    "-DriverScenario", $Scenario, "-DriverOutputDirectory", "`"$evidence`"", "-DriverWorld", $world,
    "--no-daemon"
)
if ($Latest) { $arguments += "-Latest" }
if ($Interactive) { $arguments += "-Interactive" }
if ($ProjectId) { $arguments += @("-ProjectId") + $ProjectId }

$previousToken = $env:AE2CT_TEST_DRIVER_TOKEN
if ($Interactive) {
    if ($previousToken -and $previousToken -notmatch '^[a-f0-9]{64}$') {
        throw "AE2CT_TEST_DRIVER_TOKEN must be 256-bit lowercase hex"
    }
    if (-not $previousToken) {
        $bytes = [byte[]]::new(32)
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $env:AE2CT_TEST_DRIVER_TOKEN = ([BitConverter]::ToString($bytes) -replace '-', '').ToLowerInvariant()
    }
} else {
    Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN -ErrorAction SilentlyContinue
}

try {
    try {
        $process = Start-Process -FilePath "powershell.exe" -ArgumentList $arguments -PassThru -WindowStyle Hidden `
            -WorkingDirectory $root `
            -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $null = $process.Handle
        Write-Status "running"
        $timeout = if ($Interactive) { [TimeSpan]::FromMinutes(30) } elseif ($Scenario -eq "suite") { [TimeSpan]::FromMinutes(40) } else { [TimeSpan]::FromMinutes(8) }
        if (-not $process.WaitForExit([int]$timeout.TotalMilliseconds)) {
            $null = $process.CloseMainWindow()
            if (-not $process.WaitForExit(10000)) { & taskkill.exe /PID $process.Id /T /F | Out-Null }
            throw "UI-smoke client exceeded $($timeout.TotalMinutes) minutes"
        }
        if ($process.ExitCode -ne 0) {
            throw "UI-smoke $profile-profile setup/startup failed with launcher exit $($process.ExitCode); see $stderr"
        }

    $caseScenarios = @($Scenario)
    if ($Scenario -eq "suite") {
        $summary = Get-Content -LiteralPath (Join-Path $evidence "result.json") -Raw | ConvertFrom-Json
        if ($summary.schema -ne 1 -or -not $summary.complete -or $summary.result -ne "PASS" -or $summary.processId -le 0 -or
                @($summary.cases).Count -ne $scenarios.Count) { throw "Incomplete or failed UI-smoke suite" }
        for ($i = 0; $i -lt $scenarios.Count; $i++) {
            $case = $summary.cases[$i]
            if ($case.scenario -ne $scenarios[$i] -or $case.world -ne $plan.cases[$i].world -or
                    $case.result -ne "PASS" -or -not $case.startedAt -or -not $case.finishedAt) {
                throw "Invalid suite case outcome: $($scenarios[$i])"
            }
        }
        $caseScenarios = $scenarios
    }
    foreach ($caseScenario in $caseScenarios) {
        $caseEvidence = if ($Scenario -eq "suite") { Join-Path $evidence $caseScenario } else { $evidence }
        $resultPath = Join-Path $caseEvidence "result.json"
        if (-not (Test-Path -LiteralPath $resultPath -PathType Leaf)) { throw "Missing atomic result.json" }
        $result = Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
        if ($result.result -eq "FAIL" -and $result.failure) {
            throw "UI-smoke driver failed: step=$($result.failure.step) code=$($result.failure.code) expected=$($result.failure.expected) observed=$($result.failure.observed)"
        }
        $modVersion = ((Get-Content -LiteralPath (Join-Path $root "gradle.properties")) |
            Where-Object { $_ -match '^modVersion=' } | Select-Object -First 1) -replace '^modVersion=', ''
        $driverName = "ae2-crafting-time-$modVersion-$loader-1.20.1-test-driver.jar"
        $requiredChecks = if ($caseScenario -eq "crafting-tree-screen") {
            @("screen", "node-ttc", "tooltip", "layout")
        } elseif ($caseScenario -eq "ae2networkanalyser-screen") {
            @("screen", "layout")
        } elseif ($caseScenario -eq "merequester-screen") {
            @("screen", "ttc-row", "total-ttc", "layout")
        } elseif ($caseScenario -eq "aeinfinitybooster-terminal") {
            @("screen", "plan-ttc")
        } elseif ($caseScenario -like "*-terminal") {
            @("screen", "ttc-tooltip", "plan-ttc")
        } elseif ($caseScenario -ne "craft-plan") {
            @("cpu-selected", "profile-sample", "ttc-after-sample")
        } else {
            @("screen", "ttc-row", "total-ttc", "sort-cycle", "tooltip", "layout")
        }
        if ($result.schema -ne 1 -or -not $result.complete -or $result.result -ne "PASS" -or
                $result.driver -ne $driverName -or $result.target -ne $Target -or
                $result.profile -ne $profile -or $result.scenario -ne $caseScenario) {
            throw "Invalid UI-smoke result identity or completion state"
        }
        $actualChecks = @($result.checks.psobject.Properties.Name)
        if (Compare-Object $requiredChecks $actualChecks -SyncWindow 0) { throw "Invalid UI-smoke check set" }
        foreach ($check in $requiredChecks) { if (-not $result.checks.$check) { throw "Failed UI-smoke check: $check" } }
        $requiredScreenshots = if ($caseScenario -eq "crafting-tree-screen") {
            @("crafting-tree-screen.png", "crafting-tree-tooltip.png")
        } elseif ($caseScenario -eq "ae2networkanalyser-screen") {
            @("ae2networkanalyser-screen.png")
        } elseif ($caseScenario -eq "merequester-screen") {
            @("merequester-screen.png")
        } elseif ($caseScenario -like "*-terminal") {
            $prefix = $caseScenario -replace '-terminal$', ''
            @("$prefix-terminal.png", "$prefix-plan.png")
        } elseif ($caseScenario -ne "craft-plan") {
            @("$($caseScenario -replace '-cpu$', '')-profiled-plan.png")
        } else {
            @("craft-plan.png", "craft-plan-sort-1.png", "craft-plan-sort-2.png", "craft-plan-sort-3.png", "craft-plan-tooltip.png")
        }
        foreach ($screenshot in $requiredScreenshots) {
            if ($screenshot -notin $result.screenshots -or -not (Test-Path -LiteralPath (Join-Path $caseEvidence $screenshot))) {
                throw "Missing required screenshot $screenshot"
            }
        }
    }
    $manifest = Join-Path $runtime "$modsDirectory\.ae2-crafting-time-run-mods.json"
    if (-not (Test-Path -LiteralPath $manifest)) { throw "Missing managed dependency manifest" }
    $managed = Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
    if ($driverName -notin $managed) { throw "Managed dependency manifest omits $driverName" }
    Copy-Item -LiteralPath $manifest -Destination (Join-Path $evidence "resolved-mods.json")
    $latestLog = Join-Path $runtime "logs\latest.log"
    if (-not (Test-Path -LiteralPath $latestLog)) { throw "Missing launched client log" }
    Copy-Item -LiteralPath $latestLog -Destination (Join-Path $evidence "latest.log")
    $fatal = Select-String -LiteralPath $latestLog -Pattern @(
        'Exception caught from mod bus', 'Mixin apply failed ae2craftingtime.mixins.json',
        'Mixin apply failed ae2craftingtime_test_driver.mixins.json', 'MixinTransformerError',
        'Failed to load resource', 'The game crashed whilst', 'There is no mod with modId',
        "Reference map 'ae2craftingtime_test_driver.refmap.json'"
    ) -SimpleMatch
        if ($fatal) { throw "Fatal loader, mixin, resource, or crash signature in latest.log" }
    } finally {
        $latestLog = Join-Path $runtime "logs\latest.log"
        if (Test-Path -LiteralPath $latestLog) { Copy-Item -LiteralPath $latestLog -Destination (Join-Path $evidence "latest.log") -Force }
        if ($previousToken) { $env:AE2CT_TEST_DRIVER_TOKEN = $previousToken }
        else { Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN -ErrorAction SilentlyContinue }
        foreach ($copy in $worldCopies) {
            if (Test-Path -LiteralPath $copy) { Remove-Item -LiteralPath $copy -Recurse -Force }
        }
        if ((Get-TreeHash $source) -ne $sourceHash) { throw "Tracked source fixture changed during UI smoke" }
    }
    Write-Status "passed" "UI smoke passed" $process.ExitCode
    $runtimeLock.Dispose()
    Write-Host "UI smoke passed: $evidence"
} catch {
    $exitCode = if ($process -and $process.HasExited) { [Nullable[int]]$process.ExitCode } else { $null }
    Write-Status "failed" $_.Exception.Message $exitCode
    $runtimeLock.Dispose()
    throw
}
