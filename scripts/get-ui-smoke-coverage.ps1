param(
    [Parameter(Mandatory)][string]$Target,
    [switch]$Latest,
    [string]$MatrixDirectory = $PSScriptRoot
)
$ErrorActionPreference = 'Stop'
$release = Get-Content -LiteralPath (Join-Path $MatrixDirectory 'release-matrix.json') -Raw | ConvertFrom-Json
$clients = Get-Content -LiteralPath (Join-Path $MatrixDirectory 'run-client-versions.json') -Raw | ConvertFrom-Json
if (Compare-Object @($release.id) @($clients.id)) { throw 'Release and client target matrices differ' }
$client = $clients | Where-Object id -eq $Target
if (-not $client) { throw "Unknown coverage target $Target" }
$coverage = (Get-Content -LiteralPath (Join-Path $MatrixDirectory 'ui-smoke-coverage.json') -Raw | ConvertFrom-Json).$Target
$suiteName = if ($Target -eq '26.1.2-neoforge') { 'neoforge-26.1.2' } else { $Target.Split('-')[1] }
$scenarios = Get-Content -LiteralPath (Join-Path $MatrixDirectory "ui-smoke-$suiteName-suite.json") -Raw | ConvertFrom-Json
if ('standard-ae2' -notin $scenarios) { throw 'Missing required standard-ae2 scenario' }
$projects = @($client.projects) + @($client.curseforge | Where-Object { $_ })
if (Compare-Object @($projects | ForEach-Object { [string]$_.project_id }) @($coverage.psobject.Properties.Name)) {
    throw "Missing or stale coverage declarations for $Target"
}
foreach ($project in $projects) {
    $id = [string]$project.project_id
    $entry = $coverage.$id
    if ($entry.disposition -notin @('DIRECT_UI', 'DIRECT_BEHAVIOR', 'FOCUSED_BEHAVIOR', 'COEXISTENCE', 'TOOLING')) {
        throw "Invalid coverage disposition: $id"
    }
    $disposition = $entry.disposition
    $reason = if ($disposition -eq 'FOCUSED_BEHAVIOR') { $entry.reason } else { '' }
    if ($disposition -eq 'FOCUSED_BEHAVIOR' -and -not $reason) { throw "Focused project lacks a reason: $id" }
    $replacement = $client.curseforge | Where-Object replaces_project_id -eq $id
    if ($replacement) { $disposition = 'EXCLUDED'; $reason = "Replaced by $($replacement.project_id)" }
    elseif (-not $Latest -and $project.compatible -is [bool] -and -not $project.compatible) {
        $disposition = 'EXCLUDED'; $reason = $project.reason
        if (-not $reason) { throw "Excluded project lacks a reason: $id" }
    }
    if ($disposition -notin @('EXCLUDED', 'FOCUSED_BEHAVIOR') -and $entry.scenario -notin $scenarios) {
        throw "MISSING_FIXTURE: $id requires $($entry.scenario)"
    }
    [pscustomobject]@{ projectId = $id; name = $project.name; disposition = $disposition
        cases = @(& (Join-Path $PSScriptRoot 'expand-ui-smoke-groups.ps1') -Target $Target -Scenarios $entry.scenario -MatrixDirectory $MatrixDirectory)
        scenario = $entry.scenario; reason = $reason; result = $(if ($disposition -eq 'EXCLUDED') { 'NOT_APPLICABLE' } else { 'NOT_RUN' }) }
}
