param(
    [switch]$Latest,
    [switch]$Interactive
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root "versions\1.20.1-forge\run\saves\ae2-crafting-time"
$profile = if ($Latest) { "latest" } else { "compatible" }
$base = Join-Path $root "build\ui-smoke\1.20.1-forge\$profile"
$runtime = Join-Path $base "runtime"
$evidence = Join-Path $base "evidence"
$world = "ae2ct-$([guid]::NewGuid().ToString('N'))"
$worldCopy = Join-Path $runtime "saves\$world"
$stdout = Join-Path $base "launcher.stdout.log"
$stderr = Join-Path $base "launcher.stderr.log"

function Get-TreeHash([string]$path) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $text = (Get-ChildItem -LiteralPath $path -File -Recurse | Sort-Object FullName | ForEach-Object {
            "$($_.FullName.Substring($path.Length))|$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)"
        }) -join "`n"
        return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))) -replace '-', '')
    } finally { $sha.Dispose() }
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
if (Test-Path -LiteralPath $resolvedBase) { Remove-Item -LiteralPath $resolvedBase -Recurse -Force }
New-Item -ItemType Directory -Path (Split-Path -Parent $worldCopy), $evidence -Force | Out-Null
Copy-Item -LiteralPath $source -Destination $worldCopy -Recurse
$markerPath = Join-Path $worldCopy ".ae2-crafting-time-test-fixture.json"
$marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
$marker.disposableWorldId = $world
[IO.File]::WriteAllText($markerPath, ($marker | ConvertTo-Json -Depth 10), [Text.UTF8Encoding]::new($false))
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
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "run-client.ps1"),
    "-Target", "1.20.1-forge", "-RuntimeDirectory", $runtime,
    "-DriverScenario", "craft-plan", "-DriverOutputDirectory", $evidence, "-DriverWorld", $world,
    "--no-daemon"
)
if ($Latest) { $arguments += "-Latest" }
if ($Interactive) { $arguments += "-Interactive" }

$previousToken = $env:AE2CT_TEST_DRIVER_TOKEN
if ($Interactive) {
    if ($previousToken -and $previousToken -notmatch '^[a-f0-9]{64}$') {
        throw "AE2CT_TEST_DRIVER_TOKEN must be 256-bit lowercase hex"
    }
    if (-not $previousToken) {
        $bytes = [byte[]]::new(32)
        [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
        $env:AE2CT_TEST_DRIVER_TOKEN = ([BitConverter]::ToString($bytes) -replace '-', '').ToLowerInvariant()
    }
} else {
    Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN -ErrorAction SilentlyContinue
}

try {
    $process = Start-Process -FilePath "powershell.exe" -ArgumentList $arguments -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $null = $process.Handle
    $timeout = if ($Interactive) { [TimeSpan]::FromMinutes(30) } else { [TimeSpan]::FromMinutes(8) }
    if (-not $process.WaitForExit([int]$timeout.TotalMilliseconds)) {
        $null = $process.CloseMainWindow()
        if (-not $process.WaitForExit(10000)) { & taskkill.exe /PID $process.Id /T /F | Out-Null }
        throw "UI-smoke client exceeded $($timeout.TotalMinutes) minutes"
    }
    if ($process.ExitCode -ne 0) {
        throw "UI-smoke $profile-profile setup/startup failed with launcher exit $($process.ExitCode); see $stderr"
    }

    $resultPath = Join-Path $evidence "result.json"
    if (-not (Test-Path -LiteralPath $resultPath -PathType Leaf)) { throw "Missing atomic result.json" }
    $result = Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
    if ($result.result -eq "FAIL" -and $result.failure) {
        throw "UI-smoke driver failed: step=$($result.failure.step) code=$($result.failure.code) expected=$($result.failure.expected) observed=$($result.failure.observed)"
    }
    $modVersion = ((Get-Content -LiteralPath (Join-Path $root "gradle.properties")) |
        Where-Object { $_ -match '^modVersion=' } | Select-Object -First 1) -replace '^modVersion=', ''
    $driverName = "ae2-crafting-time-$modVersion-forge-1.20.1-test-driver.jar"
    $requiredChecks = @("screen", "ttc-row", "total-ttc", "sort-cycle", "tooltip", "layout")
    if ($result.schema -ne 1 -or -not $result.complete -or $result.result -ne "PASS" -or
            $result.driver -ne $driverName -or $result.target -ne "1.20.1-forge" -or
            $result.profile -ne $profile -or $result.scenario -ne "craft-plan") {
        throw "Invalid UI-smoke result identity or completion state"
    }
    $actualChecks = @($result.checks.psobject.Properties.Name)
    if (Compare-Object $requiredChecks $actualChecks -SyncWindow 0) { throw "Invalid UI-smoke check set" }
    foreach ($check in $requiredChecks) { if (-not $result.checks.$check) { throw "Failed UI-smoke check: $check" } }
    foreach ($screenshot in @("craft-plan.png", "craft-plan-tooltip.png")) {
        if ($screenshot -notin $result.screenshots -or -not (Test-Path -LiteralPath (Join-Path $evidence $screenshot))) {
            throw "Missing required screenshot $screenshot"
        }
    }
    $manifest = Join-Path $runtime "resolved-mods\.ae2-crafting-time-run-mods.json"
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
    Write-Host "UI smoke passed: $evidence"
} finally {
    if ($previousToken) { $env:AE2CT_TEST_DRIVER_TOKEN = $previousToken }
    else { Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN -ErrorAction SilentlyContinue }
    if (Test-Path -LiteralPath $worldCopy) { Remove-Item -LiteralPath $worldCopy -Recurse -Force }
    if ((Get-TreeHash $source) -ne $sourceHash) { throw "Tracked source fixture changed during UI smoke" }
}
