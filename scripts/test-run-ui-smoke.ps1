$ErrorActionPreference = "Stop"
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct ui smoke " + [guid]::NewGuid().ToString("N"))
$scripts = Join-Path $temp "scripts"
$source = Join-Path $temp "versions\1.20.1-forge\run\saves\ae2-crafting-time"
New-Item -ItemType Directory -Path $scripts, $source -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "run-ui-smoke.ps1") -Destination (Join-Path $scripts "run-ui-smoke.ps1")
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "prepare-ui-smoke-suite.ps1"), (Join-Path $PSScriptRoot "ui-smoke-forge-suite.json"), (Join-Path $PSScriptRoot "ui-smoke-fabric-suite.json"), (Join-Path $PSScriptRoot "ui-smoke-neoforge-suite.json") -Destination $scripts
[IO.File]::WriteAllText((Join-Path $temp "gradle.properties"), "modVersion=1.1.0`n", [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $source ".ae2-crafting-time-test-fixture.json"), @'
{"schema":1,"scenario":"craft-plan","sourceFixtureId":"ae2-crafting-time","disposableWorldId":"SOURCE_ONLY",
 "terminal":{"x":1,"y":2,"z":3,"face":"SOUTH"},"outputId":"minecraft:furnace"}
'@, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $source "level.dat"), "fixture", [Text.UTF8Encoding]::new($false))
$neoFixture = Join-Path $temp "versions\1.21.1-neoforge\run\saves\ae2-crafting-time"
New-Item -ItemType Directory -Path (Split-Path $neoFixture) -Force | Out-Null
Copy-Item $source $neoFixture -Recurse
[IO.File]::WriteAllText((Join-Path $scripts "run-client.ps1"), @'
param(
    [string]$Target, [string]$RuntimeDirectory, [string]$DriverScenario,
    [string]$DriverOutputDirectory, [string]$DriverWorld, [switch]$Latest,
    [string[]]$ProjectId,
    [switch]$Interactive, [Parameter(ValueFromRemainingArguments = $true)][string[]]$Rest
)
if ([IO.Path]::GetFullPath((Get-Location).Path) -ne [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))) { exit 8 }
if ((Get-Content (Join-Path $RuntimeDirectory 'options.txt') -Raw) -notmatch '(?m)^onboardAccessibility:false\r?$') { exit 9 }
$profile = if ($Latest) { "latest" } else { "compatible" }
$loader = $Target.Substring(7)
$modsDirectory = if ($Target -eq "1.20.1-forge") { "resolved-mods" } else { "mods" }
$driver = "ae2-crafting-time-1.1.0-$loader-$($Target.Split("-")[0])-test-driver.jar"
New-Item -ItemType Directory -Path $DriverOutputDirectory, (Join-Path $RuntimeDirectory $modsDirectory),
    (Join-Path $RuntimeDirectory "logs") -Force | Out-Null
if ($DriverScenario -eq "suite") {
    $plan = Get-Content (Join-Path $DriverOutputDirectory 'suite-plan.json') -Raw | ConvertFrom-Json
    $cases = foreach ($case in $plan.cases) {
        & $PSCommandPath -Target $Target -RuntimeDirectory $RuntimeDirectory -DriverScenario $case.scenario `
            -DriverOutputDirectory (Join-Path $DriverOutputDirectory $case.scenario) -DriverWorld $case.world
        [ordered]@{scenario=$case.scenario; world=$case.world; result='PASS'; startedAt='2026-09-02T00:00:00Z'; finishedAt='2026-09-02T00:00:01Z'}
    }
    $summary = [ordered]@{schema=1;complete=$true;result='PASS';processId=$PID;cases=@($cases)}
    switch ($env:AE2CT_UI_SMOKE_TEST_MODE) {
        'suite-missing' { $summary.cases = @($cases | Select-Object -Skip 1) }
        'suite-order' { $summary.cases[0].scenario = 'wrong-cpu' }
        'suite-fail' { $summary.result = 'FAIL'; $summary.complete = $false }
        'suite-world' { $summary.cases[0].world = 'wrong-world' }
    }
    $summary | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $DriverOutputDirectory 'result.json') -Encoding UTF8
    return
}
$checks = if ($DriverScenario -eq "crafting-tree-screen") {
    [ordered]@{ screen=$true; 'node-ttc'=$true; tooltip=$true; layout=$true }
} elseif ($DriverScenario -eq "ae2networkanalyser-screen") {
    [ordered]@{ screen=$true; layout=$true }
} elseif ($DriverScenario -eq "merequester-screen") {
    [ordered]@{ screen=$true; 'ttc-row'=$true; 'total-ttc'=$true; layout=$true }
} elseif ($DriverScenario -eq "aeinfinitybooster-terminal") {
    [ordered]@{ screen=$true; 'plan-ttc'=$true }
} elseif ($DriverScenario -like "*-terminal") {
    [ordered]@{ screen=$true; 'ttc-tooltip'=$true; 'plan-ttc'=$true }
} elseif ($DriverScenario -ne "craft-plan") {
    [ordered]@{ 'cpu-selected'=$true; 'profile-sample'=$true; 'ttc-after-sample'=$true }
} else {
    [ordered]@{ screen=$true; 'ttc-row'=$true; 'total-ttc'=$true; 'sort-cycle'=$true; tooltip=$true; layout=$true }
}
$screenshots = if ($DriverScenario -eq "crafting-tree-screen") {
    @("crafting-tree-screen.png", "crafting-tree-tooltip.png")
} elseif ($DriverScenario -eq "ae2networkanalyser-screen") {
    @("ae2networkanalyser-screen.png")
} elseif ($DriverScenario -eq "merequester-screen") {
    @("merequester-screen.png")
} elseif ($DriverScenario -like "*-terminal") {
    $prefix = $DriverScenario -replace '-terminal$', ''
    @("$prefix-terminal.png", "$prefix-plan.png")
} elseif ($DriverScenario -ne "craft-plan") { @("$(($DriverScenario -replace '-cpu$', ''))-profiled-plan.png") } else { @("craft-plan.png", "craft-plan-sort-1.png", "craft-plan-sort-2.png", "craft-plan-sort-3.png", "craft-plan-tooltip.png") }
$result = [ordered]@{
    schema = $(if ($env:AE2CT_UI_SMOKE_TEST_MODE -eq "schema") { 2 } else { 1 })
    complete = $true; driver = $driver; target = $(if ($env:AE2CT_UI_SMOKE_TEST_MODE -eq "wrong-target") { "wrong" } else { $Target }); profile = $profile
    scenario = $DriverScenario; result = "PASS"; checks = $checks
    screenshots = $screenshots
}
$result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $DriverOutputDirectory "result.json") -Encoding UTF8
foreach ($screenshot in $screenshots) {
    if ($env:AE2CT_UI_SMOKE_TEST_MODE -ne "missing-screenshot" -or $screenshot -ne $screenshots[-1]) {
        Set-Content -LiteralPath (Join-Path $DriverOutputDirectory $screenshot) -Value "png"
    }
}
if ($Interactive -and $env:AE2CT_UI_SMOKE_TEST_MODE -eq "interactive-token" -and
        $env:AE2CT_TEST_DRIVER_TOKEN -ne ('b' * 64)) { exit 7 }
(@($driver) + @($ProjectId | ForEach-Object { "$_.jar" })) | ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $RuntimeDirectory "$modsDirectory\.ae2-crafting-time-run-mods.json") -Encoding UTF8
Set-Content -LiteralPath (Join-Path $RuntimeDirectory "logs\latest.log") -Value $(if ($env:AE2CT_UI_SMOKE_TEST_MODE -eq "fatal") { "Mixin apply failed ae2craftingtime.mixins.json" } elseif ($env:AE2CT_UI_SMOKE_TEST_MODE -eq "production-refmap") { "Reference map 'ae2craftingtime.refmap.json' could not be read" } else { "clean" })
'@, [Text.UTF8Encoding]::new($false))

function Invoke-Case([string]$mode, [switch]$Latest, [switch]$Interactive,
        [string]$Target = "1.20.1-forge", [string]$Scenario = "craft-plan", [string[]]$ProjectId, [string]$ReportDirectory, [bool]$shouldPass) {
    $env:AE2CT_UI_SMOKE_TEST_MODE = $mode
    $arguments = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $scripts "run-ui-smoke.ps1"))
    $arguments += @("-Target", $Target)
    if ($Latest) { $arguments += "-Latest" }
    if ($Interactive) { $arguments += "-Interactive" }
    if ($Scenario -ne "craft-plan") { $arguments += @("-Scenario", $Scenario) }
    if ($ProjectId) { $arguments += @("-ProjectId") + $ProjectId }
    if ($ReportDirectory) { $arguments += @("-ReportDirectory", $ReportDirectory) }
    $preference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & powershell.exe @arguments 2>&1
        $passed = $LASTEXITCODE -eq 0
    } finally { $ErrorActionPreference = $preference }
    if ($passed -ne $shouldPass) {
        throw "Unexpected runner result for '$mode' latest=$Latest`: $($output -join [Environment]::NewLine)"
    }
}

try {
    Invoke-Case "pass" -Target "1.20.1-fabric" -Scenario suite -shouldPass $true
    Invoke-Case "pass" -Target "1.20.1-fabric" -Latest -shouldPass $true
    Invoke-Case "wrong-target" -Target "1.20.1-fabric" -shouldPass $false
    Invoke-Case "pass" -Target "1.21.1-neoforge" -Scenario suite -shouldPass $true
    Invoke-Case "pass" -Target "1.21.1-neoforge" -Latest -shouldPass $true
    Invoke-Case "wrong-target" -Target "1.21.1-neoforge" -shouldPass $false
    Invoke-Case "missing-screenshot" -Target "1.21.1-neoforge" -Scenario suite -shouldPass $false
    Invoke-Case "pass" -Target "26.1.2-neoforge" -shouldPass $false
    Invoke-Case "pass" -shouldPass $true
    $cacheMarker = Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\runtime\cache-marker.txt"
    Set-Content -LiteralPath $cacheMarker -Value "keep"
    Invoke-Case "pass" -shouldPass $true
    if (-not (Test-Path -LiteralPath $cacheMarker)) { throw "Smoke runtime cache was discarded" }
    $status = Get-Content -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\craft-plan\status.json") -Raw | ConvertFrom-Json
    if ($status.phase -ne "passed" -or -not $status.pid -or $status.target -ne "1.20.1-forge") {
        throw "Smoke status omitted the final phase, PID, or target"
    }
    $externalReport = Join-Path $temp "shared-report"
    Invoke-Case "pass" -ReportDirectory $externalReport -shouldPass $true
    if (-not (Test-Path -LiteralPath (Join-Path $externalReport "evidence\result.json"))) {
        throw "External smoke report omitted copied evidence"
    }
    Invoke-Case "pass" -Scenario suite -shouldPass $true
    $suiteSaves = Join-Path $temp 'build\ui-smoke\1.20.1-forge\compatible\runtime\saves'
    if (@(Get-ChildItem $suiteSaves -Directory).Count) { throw 'Suite left disposable worlds behind' }
    foreach ($mode in @('suite-missing', 'suite-order', 'suite-fail', 'suite-world', 'schema', 'missing-screenshot', 'fatal')) {
        Invoke-Case $mode -Scenario suite -shouldPass $false
    }
    Invoke-Case "pass" -Scenario suite -Latest -shouldPass $false
    Invoke-Case "pass" -Scenario suite -Interactive -shouldPass $false
    Invoke-Case "pass" -Scenario suite -ProjectId E6BFl96N -shouldPass $false
    Invoke-Case "pass" -Latest -shouldPass $true
    Invoke-Case "pass" -Scenario "neoeco-cpu" -shouldPass $true
    Invoke-Case "pass" -Scenario "crafting-tree-screen" -shouldPass $true
    Invoke-Case "missing-screenshot" -Scenario "crafting-tree-screen" -shouldPass $false
    Invoke-Case "pass" -Scenario "merequester-screen" -ProjectId E6BFl96N -shouldPass $true
    Invoke-Case "pass" -Scenario "ae2importexportcard-terminal" -ProjectId qelfSMnn -shouldPass $true
    Invoke-Case "pass" -Scenario "ae2networkanalyser-screen" -ProjectId 961856 -shouldPass $true
    Invoke-Case "pass" -Scenario "aeinfinitybooster-terminal" -ProjectId VQhDBNs8 -shouldPass $true
    if (-not (Test-Path -LiteralPath $cacheMarker)) { throw "Scenario switch discarded the shared runtime" }
    Invoke-Case "pass" -Scenario "ae2wtlib-terminal" -ProjectId pNabrMMw -shouldPass $true
    $focused = Get-Content -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\ae2wtlib-terminal\evidence\resolved-mods.json") -Raw | ConvertFrom-Json
    if ("pNabrMMw.jar" -notin $focused) { throw "Smoke runner dropped the focused project" }
    Invoke-Case "pass" -Scenario "future-addon-cpu" -shouldPass $true
    if (-not (Test-Path -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\craft-plan\evidence\result.json")) -or
            -not (Test-Path -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\neoeco-cpu\evidence\result.json")) -or
            -not (Test-Path -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\latest\craft-plan\evidence\result.json"))) {
        throw "Compatible and latest evidence was not separated"
    }
    $lock = [IO.File]::Open((Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\runtime.lock"), "Open", "ReadWrite", "None")
    try { Invoke-Case "pass" -shouldPass $false } finally { $lock.Dispose() }
    Invoke-Case "pass" -Interactive -shouldPass $true
    $env:AE2CT_TEST_DRIVER_TOKEN = 'b' * 64
    Invoke-Case "interactive-token" -Interactive -shouldPass $true
    $env:AE2CT_TEST_DRIVER_TOKEN = 'invalid'
    Invoke-Case "pass" -Interactive -shouldPass $false
    Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN
    Invoke-Case "schema" -shouldPass $false
    Invoke-Case "missing-screenshot" -shouldPass $false
    Invoke-Case "production-refmap" -Target "1.20.1-fabric" -shouldPass $false
    Invoke-Case "fatal" -shouldPass $false
    $failedStatus = Get-Content -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\craft-plan\status.json") -Raw | ConvertFrom-Json
    if ($failedStatus.phase -ne "failed" -or -not $failedStatus.message) { throw "Smoke failure status was incomplete" }

    $markerPath = Join-Path $source ".ae2-crafting-time-test-fixture.json"
    $marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
    $marker.disposableWorldId = "ae2-crafting-time"
    $marker | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $markerPath -Encoding UTF8
    Invoke-Case "pass" -shouldPass $false
    Write-Host "run-ui-smoke checks passed"
} finally {
    Remove-Item Env:\AE2CT_UI_SMOKE_TEST_MODE -ErrorAction SilentlyContinue
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    $resolved = [IO.Path]::GetFullPath($temp)
    if ($resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $resolved)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
