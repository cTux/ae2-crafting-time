param(
    [string]$Target,
    [switch]$Latest,
    [switch]$Interactive,
    [string]$Scenario = 'suite',
    [string[]]$ProjectId,
    [string]$GuestSourceRoot,
    [string]$PreparedLaunchRoot = 'C:\Users\Public\Documents\AE2CraftingTimeSmoke\prepared'
)
$ErrorActionPreference = 'Stop'
if ($Scenario -eq 'suite' -and ($ProjectId -or $Interactive)) { throw 'The suite requires the full graph and non-interactive execution' }
if ($Interactive -and -not $Target) { throw 'Interactive execution requires one target' }
$root = Split-Path -Parent $PSScriptRoot
$matrix = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'release-matrix.json') -Raw | ConvertFrom-Json
if ($Target -and $Target -notin $matrix.id) { throw "Unknown target: $Target" }
$targets = @($matrix | Where-Object { -not $Target -or $_.id -eq $Target })
$profile = if ($Latest) { 'latest' } else { 'compatible' }
$runId = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$campaign = Join-Path $root "build/ui-smoke/campaigns/$runId/$profile"
New-Item -ItemType Directory -Path $campaign -Force | Out-Null
$results = @()
$commit = & git -C $root rev-parse HEAD
$stopCampaign = $false
foreach ($row in $targets) {
    $report = Join-Path $campaign $row.id
    New-Item -ItemType Directory -Path $report -Force | Out-Null
    $started = [DateTime]::UtcNow.ToString('o')
    $result = 'FAIL_SETUP'
    $message = ''
    $coverage = @()
    try {
        $coverage = @(& (Join-Path $PSScriptRoot 'get-ui-smoke-coverage.ps1') -Target $row.id -Latest:$Latest)
        $coverage | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $report 'coverage.json') -Encoding UTF8
        $cache = Join-Path $root "build/ui-smoke/bundle-cache/$($row.id)/$profile"
        & (Join-Path $PSScriptRoot 'run-client.ps1') -Target $row.id -Latest:$Latest -ResolveOnly -Packaged -RuntimeDirectory $cache -ProjectId $ProjectId
        # Guest shares may retain read handles. Each run receives an immutable bundle.
        $bundle = Join-Path $report 'bundle'
        Copy-Item -LiteralPath $cache -Destination $bundle -Recurse
        Get-ChildItem -LiteralPath (Join-Path $bundle 'mods') -Filter '*.jar' -File | ForEach-Object {
            [ordered]@{ file = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName).Hash }
        } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $report 'artifact-hashes.json') -Encoding UTF8
        $arguments = @{ Target = $row.id; Latest = $Latest; Scenario = $Scenario
            BundleDirectory = $bundle; PreparedLaunchRoot = $PreparedLaunchRoot; ProjectId = $ProjectId; Interactive = $Interactive }
        if ($GuestSourceRoot) { $arguments.GuestSourceRoot = $GuestSourceRoot }
        & (Join-Path $PSScriptRoot 'invoke-ui-smoke-codexvm.ps1') @arguments
        $live = Join-Path $root "build/ui-smoke/$($row.id)/$profile/$Scenario"
        $deadline = [DateTime]::UtcNow.AddMinutes(45)
        do {
            if ([DateTime]::UtcNow -gt $deadline) {
                $stopCampaign = $true
                & (Join-Path $PSScriptRoot 'invoke-ui-smoke-codexvm.ps1') @arguments -Stop
                throw 'Guest runner exceeded 45 minutes; stopped the recorded client and ended the campaign'
            }
            Start-Sleep -Seconds 2
            $status = Get-Content -LiteralPath (Join-Path $live 'status.json') -Raw | ConvertFrom-Json
        } while ($status.phase -in @('queued', 'preparing', 'running'))
        Copy-Item -LiteralPath $live -Destination (Join-Path $report 'run') -Recurse
        if ($status.pid -and $null -eq $status.exitCode) {
            $stopCampaign = $true
            throw 'Recorded client exit is unconfirmed; refusing to launch another client'
        }
        if ($status.phase -ne 'passed') { $result = 'FAIL'; throw $status.message }
        $result = 'PASS'
        foreach ($entry in $coverage) {
            if ($entry.result -ne 'NOT_APPLICABLE') {
                $case = Join-Path $report "run/evidence/$($entry.scenario)/result.json"
                if ($Scenario -ne 'suite') { $case = Join-Path $report 'run/evidence/result.json' }
                if (($Scenario -eq 'suite' -or $entry.scenario -eq $Scenario) -and (Test-Path -LiteralPath $case)) {
                    $entry.result = (Get-Content -LiteralPath $case -Raw | ConvertFrom-Json).result
                }
            }
        }
    } catch {
        $message = $_.Exception.Message
        if ($Latest) { $result = 'DIAGNOSTIC_FAILURE' }
        Write-Warning "$($row.id) $profile $result`: $message"
    } finally {
        $coverage | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $report 'coverage.json') -Encoding UTF8
        $results += [ordered]@{ target = $row.id; profile = $profile; result = $result; message = $message
            startedAt = $started; finishedAt = [DateTime]::UtcNow.ToString('o'); report = $report }
        [ordered]@{ schema = 1; runId = $runId; commit = $commit; results = $results } |
            ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $campaign 'result.json') -Encoding UTF8
    }
    if ($stopCampaign) { break }
}
Write-Host "UI smoke campaign: $campaign"
if (@($results | Where-Object { $_.profile -eq 'compatible' -and $_.result -ne 'PASS' }).Count) { exit 1 }
exit 0
