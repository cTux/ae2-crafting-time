param(
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge")][string]$Target = "1.20.1-forge",
    [switch]$Latest,
    [switch]$Interactive,
    [switch]$Scheduled,
    [switch]$Stop,
    [ValidatePattern("^(suite|craft-plan|no-space-status|crafting-tree-screen|merequester-screen|ae2networkanalyser-screen|aeinfinitybooster-terminal|ae2importexportcard-terminal|ae2(?:wcwt|wtlib)-terminal|[a-z0-9]+(?:-[a-z0-9]+)*-cpu)$")][string]$Scenario = "craft-plan",
    [string[]]$ProjectId,
    [string]$JavaHome,
    [string]$LocalRoot,
    [string]$InteractiveUser = "Codex",
    [string]$RequestPath
)

$ErrorActionPreference = "Stop"

function Get-WorkspaceId([string]$path) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return (([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(
            [IO.Path]::GetFullPath($path).ToLowerInvariant()))) -replace '-', '').Substring(0, 12)).ToLowerInvariant()
    } finally { $sha.Dispose() }
}

function Find-SmokeJava([string]$requested) {
    $major = if ($Target -eq "1.21.1-neoforge") { 21 } else { 17 }
    $candidate = if ($requested) { $requested } else {
        @(
            Get-ChildItem -LiteralPath "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-$major*" -ErrorAction SilentlyContinue
            Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE ".gradle\jdks") -Directory -Recurse -Filter "jdk-$major*" -ErrorAction SilentlyContinue
        ) | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    $java = if ($candidate) { Join-Path $candidate "bin\java.exe" } else { "" }
    if (-not $java -or -not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "CodexVM JDK $major was not found" }
    $version = (& { $ErrorActionPreference = "Continue"; & $java -XshowSettings:properties -version 2>&1 } | Out-String)
    if ($version -notmatch "(?m)^\s*java\.version\s*=\s*$major(?:\.|\s|$)") { throw "$Target UI smoke requires JDK ${major}: $java" }
    return [IO.Path]::GetFullPath($candidate)
}

function Get-ReportDirectory([string]$sourceRoot, [bool]$latest, [string]$scenario) {
    $profile = if ($latest) { "latest" } else { "compatible" }
    return Join-Path $sourceRoot "build\ui-smoke\$Target\$profile\$scenario"
}

function Stop-Smoke([string]$report) {
    $statusPath = Join-Path $report "status.json"
    if (-not (Test-Path -LiteralPath $statusPath -PathType Leaf)) { throw "No UI-smoke status exists at $statusPath" }
    $status = Get-Content -LiteralPath $statusPath -Raw | ConvertFrom-Json
    if (-not $status.pid -or $status.phase -notin @("preparing", "running")) { throw "UI smoke is not running" }
    $running = Get-CimInstance Win32_Process -Filter "ProcessId = $($status.pid)"
    if (-not $running) { throw "Recorded UI-smoke PID $($status.pid) is no longer running" }
    if ($running.CommandLine -notlike "*$($status.stagedRoot)*run-client.ps1*") {
        throw "PID $($status.pid) does not match the recorded UI-smoke command"
    }
    & taskkill.exe /PID $status.pid /T /F | Out-Null
    Write-Host "Stopped UI-smoke process tree $($status.pid)"
}

if ($RequestPath) {
    $request = Get-Content -LiteralPath $RequestPath -Raw | ConvertFrom-Json
    $env:JAVA_HOME = $request.javaHome
    $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
    $arguments = @{ ReportDirectory = $request.reportDirectory; Scenario = $request.scenario; Target = $request.target }
    if ($request.projectId) { $arguments.ProjectId = @($request.projectId) }
    if ($request.latest) { $arguments.Latest = $true }
    if ($request.interactive) { $arguments.Interactive = $true }
    & (Join-Path $request.stagedRoot "scripts\run-ui-smoke.ps1") @arguments
    exit 0
}

$sourceRoot = Split-Path -Parent $PSScriptRoot
$workspaceId = Get-WorkspaceId $sourceRoot
$stage = if ($LocalRoot) { [IO.Path]::GetFullPath($LocalRoot) } else { Join-Path $env:PUBLIC "Documents\AE2CraftingTimeSmoke\$workspaceId" }
$report = Get-ReportDirectory $sourceRoot $Latest.IsPresent $Scenario

if ($Stop) {
    Stop-Smoke $report
    exit 0
}

$taskName = "AE2 Crafting Time UI Smoke $workspaceId"
$existing = if ($Scheduled) { Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue } else { $null }
if ($existing -and $existing.State -eq "Running") { throw "UI smoke task is already running" }

$smokeJava = Find-SmokeJava $JavaHome
New-Item -ItemType Directory -Path $stage, $report -Force | Out-Null
& robocopy.exe $sourceRoot $stage /MIR /XD .git .gradle build /XF .git /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -gt 7) { throw "Failed to stage the checkout with robocopy exit $LASTEXITCODE" }

$request = [ordered]@{
    target = $Target; stagedRoot = $stage; reportDirectory = $report; scenario = $Scenario
    projectId = @($ProjectId); latest = $Latest.IsPresent; interactive = $Interactive.IsPresent; javaHome = $smokeJava
}
$requestFile = Join-Path $stage "ui-smoke-request.json"
[IO.File]::WriteAllText($requestFile, ($request | ConvertTo-Json), [Text.UTF8Encoding]::new($false))

if ($Scheduled) {
    if (-not $existing) {
        $script = Join-Path $stage "scripts\run-ui-smoke-codexvm.ps1"
        $action = New-ScheduledTaskAction -Execute "powershell.exe" `
            -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$script`" -RequestPath `"$requestFile`""
        $principal = New-ScheduledTaskPrincipal -UserId "$env:COMPUTERNAME\$InteractiveUser" -LogonType Interactive -RunLevel Limited
        Register-ScheduledTask -TaskName $taskName -Action $action -Principal $principal | Out-Null
    }
    $queued = [ordered]@{ schema = 1; target = $Target; profile = $(if ($Latest) { "latest" } else { "compatible" })
        scenario = $Scenario; phase = "queued"; pid = $null; stagedRoot = $stage; updatedAt = [DateTime]::UtcNow.ToString("o") }
    [IO.File]::WriteAllText((Join-Path $report "status.json"), ($queued | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    Start-ScheduledTask -TaskName $taskName
    Write-Host "Queued UI smoke in the interactive Codex desktop: $report"
    exit 0
}

$env:JAVA_HOME = $smokeJava
$env:Path = "$(Join-Path $smokeJava 'bin');$env:Path"
& (Join-Path $stage "scripts\run-ui-smoke-codexvm.ps1") -RequestPath $requestFile
exit 0
