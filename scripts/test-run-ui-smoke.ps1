$ErrorActionPreference = "Stop"
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-ui-smoke-" + [guid]::NewGuid().ToString("N"))
$scripts = Join-Path $temp "scripts"
$source = Join-Path $temp "versions\1.20.1-forge\run\saves\ae2-crafting-time"
New-Item -ItemType Directory -Path $scripts, $source -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "run-ui-smoke.ps1") -Destination (Join-Path $scripts "run-ui-smoke.ps1")
[IO.File]::WriteAllText((Join-Path $temp "gradle.properties"), "modVersion=1.0.13`n", [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $source ".ae2-crafting-time-test-fixture.json"), @'
{"schema":1,"scenario":"craft-plan","sourceFixtureId":"ae2-crafting-time","disposableWorldId":"SOURCE_ONLY",
 "terminal":{"x":1,"y":2,"z":3,"face":"SOUTH"},"outputId":"minecraft:furnace"}
'@, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $source "level.dat"), "fixture", [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $scripts "run-client.ps1"), @'
param(
    [string]$Target, [string]$RuntimeDirectory, [string]$DriverScenario,
    [string]$DriverOutputDirectory, [string]$DriverWorld, [switch]$Latest,
    [switch]$Interactive, [Parameter(ValueFromRemainingArguments = $true)][string[]]$Rest
)
$profile = if ($Latest) { "latest" } else { "compatible" }
$driver = "ae2-crafting-time-1.0.13-forge-1.20.1-test-driver.jar"
New-Item -ItemType Directory -Path $DriverOutputDirectory, (Join-Path $RuntimeDirectory "resolved-mods"),
    (Join-Path $RuntimeDirectory "logs") -Force | Out-Null
$checks = [ordered]@{ screen=$true; 'ttc-row'=$true; 'total-ttc'=$true; 'sort-cycle'=$true; tooltip=$true; layout=$true }
$result = [ordered]@{
    schema = $(if ($env:AE2CT_UI_SMOKE_TEST_MODE -eq "schema") { 2 } else { 1 })
    complete = $true; driver = $driver; target = "1.20.1-forge"; profile = $profile
    scenario = "craft-plan"; result = "PASS"; checks = $checks
    screenshots = @("craft-plan.png", "craft-plan-tooltip.png")
}
$result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $DriverOutputDirectory "result.json") -Encoding UTF8
Set-Content -LiteralPath (Join-Path $DriverOutputDirectory "craft-plan.png") -Value "png"
if ($env:AE2CT_UI_SMOKE_TEST_MODE -ne "missing-screenshot") {
    Set-Content -LiteralPath (Join-Path $DriverOutputDirectory "craft-plan-tooltip.png") -Value "png"
}
if ($Interactive -and $env:AE2CT_UI_SMOKE_TEST_MODE -eq "interactive-token" -and
        $env:AE2CT_TEST_DRIVER_TOKEN -ne ('b' * 64)) { exit 7 }
@($driver) | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RuntimeDirectory "resolved-mods\.ae2-crafting-time-run-mods.json") -Encoding UTF8
Set-Content -LiteralPath (Join-Path $RuntimeDirectory "logs\latest.log") -Value $(if ($env:AE2CT_UI_SMOKE_TEST_MODE -eq "fatal") { "Mixin apply failed ae2craftingtime.mixins.json" } else { "clean" })
'@, [Text.UTF8Encoding]::new($false))

function Invoke-Case([string]$mode, [switch]$Latest, [switch]$Interactive, [bool]$shouldPass) {
    $env:AE2CT_UI_SMOKE_TEST_MODE = $mode
    $arguments = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $scripts "run-ui-smoke.ps1"))
    if ($Latest) { $arguments += "-Latest" }
    if ($Interactive) { $arguments += "-Interactive" }
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
    Invoke-Case "pass" -shouldPass $true
    Invoke-Case "pass" -Latest -shouldPass $true
    if (-not (Test-Path -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\compatible\evidence\result.json")) -or
            -not (Test-Path -LiteralPath (Join-Path $temp "build\ui-smoke\1.20.1-forge\latest\evidence\result.json"))) {
        throw "Compatible and latest evidence was not separated"
    }
    Invoke-Case "pass" -Interactive -shouldPass $true
    $env:AE2CT_TEST_DRIVER_TOKEN = 'b' * 64
    Invoke-Case "interactive-token" -Interactive -shouldPass $true
    $env:AE2CT_TEST_DRIVER_TOKEN = 'invalid'
    Invoke-Case "pass" -Interactive -shouldPass $false
    Remove-Item Env:\AE2CT_TEST_DRIVER_TOKEN
    Invoke-Case "schema" -shouldPass $false
    Invoke-Case "missing-screenshot" -shouldPass $false
    Invoke-Case "fatal" -shouldPass $false

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
