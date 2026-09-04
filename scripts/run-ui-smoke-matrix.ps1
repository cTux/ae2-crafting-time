param(
    [switch]$Changed,
    [string]$BaseRef = 'origin/master',
    [switch]$PlanOnly,
    [string]$Target,
    [switch]$Latest,
    [switch]$Interactive,
    [string]$Scenario = 'suite',
    [string[]]$ProjectId,
    [string]$GuestSourceRoot,
    [string]$PreparedLaunchRoot = 'C:\Users\Public\Documents\AE2CraftingTimeSmoke\prepared'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$planning = @{ Changed=$Changed; BaseRef=$BaseRef; Target=$Target; Latest=$Latest; Interactive=$Interactive; ProjectId=$ProjectId }
if ($PSBoundParameters.ContainsKey('Scenario')) { $planning.Scenario = $Scenario }
$plan = & (Join-Path $PSScriptRoot 'get-ui-smoke-plan.ps1') @planning
if ($PlanOnly) { $plan | ConvertTo-Json -Depth 20; return }
$targets = @($plan.targets)
$profile = if ($Latest) { 'latest' } else { 'compatible' }
$runId = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$campaign = Join-Path $root "build/ui-smoke/campaigns/$runId/$profile"
New-Item -ItemType Directory -Path $campaign -Force | Out-Null
$plan | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath (Join-Path $campaign 'selection.json') -Encoding UTF8
if ($plan.result -eq 'NOT_REQUIRED') { Write-Host 'NOT_REQUIRED: no runtime changes; normal checks still required'; return }
$results = @()
$commit = $plan.headSha
$stopCampaign = $false
foreach ($targetEntry in $targets) {
  foreach ($graph in $targetEntry.graphs) {
    $row = $targetEntry
    $runCases = @($graph.cases)
    $runLatest = $graph.profile -eq 'latest'
    $profile = $graph.profile
    $runProjects = @($graph.projectId)
    $Scenario = if ($runCases.Count -eq 1) { $runCases[0] } else { 'suite' }
    $casesBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject @($runCases) -Compress)))
    $report = Join-Path $campaign "$($row.target)/$($graph.id)"
    New-Item -ItemType Directory -Path $report -Force | Out-Null
    $started = [DateTime]::UtcNow.ToString('o')
    $result = 'FAIL_SETUP'
    $message = ''
    $coverage = @()
    $clientExitConfirmed = $true
    try {
        $coverage = @(& (Join-Path $PSScriptRoot 'get-ui-smoke-coverage.ps1') -Target $row.target -Latest:$runLatest)
        $coverage | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $report 'coverage.json') -Encoding UTF8
        $null = & (Join-Path $PSScriptRoot 'get-ui-smoke-plan.ps1') @planning -ExpectedFingerprint $plan.fingerprint
        $cache = Join-Path $root "build/ui-smoke/bundle-cache/$($row.target)/$profile/$($graph.id)"
        & (Join-Path $PSScriptRoot 'run-client.ps1') -Target $row.target -Latest:$runLatest -ResolveOnly -Packaged -RuntimeDirectory $cache -ProjectId $runProjects
        # Guest shares may retain read handles. Each run receives an immutable bundle.
        $bundle = Join-Path $report 'bundle'
        Copy-Item -LiteralPath $cache -Destination $bundle -Recurse
        Get-ChildItem -LiteralPath (Join-Path $bundle 'mods') -Filter '*.jar' -File | ForEach-Object {
            [ordered]@{ file = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName).Hash }
        } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $report 'artifact-hashes.json') -Encoding UTF8
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target $row.target -BundleDirectory $bundle
        $null = & (Join-Path $PSScriptRoot 'get-ui-smoke-plan.ps1') @planning -ExpectedFingerprint $plan.fingerprint
        $plan | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath (Join-Path $report 'selection.json') -Encoding UTF8
        $arguments = @{ Target = $row.target; Latest = $runLatest; Scenario = $Scenario
            CasesBase64 = $casesBase64; BundleDirectory = $bundle; PreparedLaunchRoot = $PreparedLaunchRoot; ProjectId = $runProjects; Interactive = $Interactive }
        if ($GuestSourceRoot) { $arguments.GuestSourceRoot = $GuestSourceRoot }
        $clientExitConfirmed = $false
        & (Join-Path $PSScriptRoot 'invoke-ui-smoke-codexvm.ps1') @arguments
        $live = Join-Path $root "build/ui-smoke/$($row.target)/$profile/$Scenario"
        $deadline = [DateTime]::UtcNow.AddMinutes(45)
        do {
            if ([DateTime]::UtcNow -gt $deadline) {
                $stopCampaign = $true
                & (Join-Path $PSScriptRoot 'invoke-ui-smoke-codexvm.ps1') @arguments -Stop
                throw 'Guest runner exceeded 45 minutes; stopped the recorded client and ended the campaign'
            }
            Start-Sleep -Seconds 2
            $status = $null
            try {
                $status = Get-Content -LiteralPath (Join-Path $live 'status.json') -Raw | ConvertFrom-Json
            } catch {
                # Shared-folder replacement can briefly hide the status file.
                # Keep the existing deadline and never interpret that gap as exit.
                Write-Verbose "Waiting for readable guest status: $($_.Exception.Message)"
            }
        } while (!$status -or !$status.phase -or $status.phase -in @('queued', 'preparing', 'running'))
        Copy-Item -LiteralPath $live -Destination (Join-Path $report 'run') -Recurse
        if ($status.pid -and $null -eq $status.exitCode) {
            $stopCampaign = $true
            throw 'Recorded client exit is unconfirmed; refusing to launch another client'
        }
        $clientExitConfirmed = $true
        if ($status.phase -ne 'passed') { $result = 'FAIL'; throw $status.message }
        $result = 'PASS'
    } catch {
        if (!$clientExitConfirmed) { $stopCampaign = $true }
        $message = $_.Exception.Message
        if ($Latest) { $result = 'DIAGNOSTIC_FAILURE' }
        Write-Warning "$($row.target) $profile $result`: $message"
    } finally {
        $leaves = @(& (Join-Path $PSScriptRoot 'get-ui-smoke-results.ps1') -Target $row.target -Profile $profile -Scenarios $runCases -Evidence (Join-Path $report 'run/evidence') -ExpectedAdapters (Join-Path $report 'bundle/expected-adapters.json'))
        $groups = @()
        $catalogue = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json
        foreach ($group in $catalogue.groups.psobject.Properties) {
            $members = @($group.Value)
            $outcomes = @($leaves | Where-Object { $_.scenario -cin $members })
            $groupResult = if (@($outcomes | Where-Object result -eq 'FAIL').Count) { 'FAIL' }
                elseif ($outcomes.Count -eq $members.Count -and @($outcomes | Where-Object result -ne 'PASS').Count -eq 0) { 'PASS' } else { 'NOT_RUN' }
            $groups += [pscustomobject]@{ scenario=$group.Name; result=$groupResult; cases=$members }
        }
        foreach ($entry in $coverage) {
            if ($entry.result -eq 'NOT_APPLICABLE') { continue }
            $outcome = @($leaves) + @($groups) | Where-Object scenario -CEQ $entry.scenario
            if ($outcome) { $entry.result = $outcome.result }
        }
        if ($result -eq 'PASS' -and @($leaves | Where-Object result -ne 'PASS').Count) {
            $result = if ($Latest) { 'DIAGNOSTIC_FAILURE' } else { 'FAIL' }
            $message = 'Selected leaf evidence is missing or invalid'
        }
        $coverage | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $report 'coverage.json') -Encoding UTF8
        $results += [ordered]@{ target = $row.target; graph = $graph.id; required = !$Latest; profile = $profile; result = $result; message = $message
            startedAt = $started; finishedAt = [DateTime]::UtcNow.ToString('o'); report = $report; mode = $(if ($graph.id -eq 'primary') { $row.mode } else { 'focused' }); cases = $leaves; groups = $groups }
        [ordered]@{ schema = 1; runId = $runId; commit = $commit; results = $results } |
            ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $campaign 'result.json') -Encoding UTF8
    }
    if ($stopCampaign) { break }
  }
  if ($stopCampaign) { break }
}
Write-Host "UI smoke campaign: $campaign"
if (@($results | Where-Object { $_.required -and $_.result -ne 'PASS' }).Count) { exit 1 }
exit 0
