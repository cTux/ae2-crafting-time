param(
    [string]$CampaignDirectory,
    [string]$ArchiveRoot = 'E:/games/mc-instances/.codex-test-results/ui-smoke/clients',
    [switch]$Preflight,
    [string]$ContractsFile = (Join-Path $PSScriptRoot 'ui-smoke-visuals.json')
)
$ErrorActionPreference = 'Stop'
$archiveRootPath = [IO.Path]::GetFullPath($ArchiveRoot)
New-Item -ItemType Directory -Path $archiveRootPath -Force | Out-Null
$probe = Join-Path $archiveRootPath ('.write-check-' + [guid]::NewGuid().ToString('N'))
$stream = [IO.File]::Open($probe,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
$stream.Dispose()
Remove-Item -LiteralPath $probe
if ($Preflight) { return }
$campaign = [IO.Path]::GetFullPath($CampaignDirectory)
if ($archiveRootPath.TrimEnd('/','\') -ieq $campaign.TrimEnd('/','\') -or $archiveRootPath.StartsWith($campaign.TrimEnd('/','\') + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Archive cannot be inside the campaign' }
$gatePath = Join-Path $campaign 'gate.json'
if (Test-Path -LiteralPath $gatePath) { throw 'Campaign is already finalized; preserve its gate and use a new attempt' }
$clock = [Diagnostics.Stopwatch]::StartNew()
$raw = Get-Content -LiteralPath (Join-Path $campaign 'result.json') -Raw | ConvertFrom-Json
if ($raw.schema -ne 1 -or !$raw.runId -or !$raw.results.Count) { throw 'Invalid campaign result' }
$gate = [ordered]@{schema=1;runId=$raw.runId;commit=$raw.commit;semanticResult='PASS';visualResults=@();archiveResult='FAIL';cleanupResult='PASS';overall='FAIL';reasons=@();validationMs=0;archiveMs=0}
foreach ($run in $raw.results) {
    if ($run.required -and $run.result -ne 'PASS') { $gate.semanticResult = 'FAIL' }
    try {
        $report = [IO.Path]::GetFullPath($run.report)
        if (!$report.StartsWith($campaign.TrimEnd('/','\') + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Report is outside campaign' }
        $status = Get-Content -LiteralPath (Join-Path $report 'run/status.json') -Raw | ConvertFrom-Json
        if ($run.result -eq 'PASS' -and (!$status.pid -or $null -eq $status.exitCode -or $status.exitCode -ne 0)) { $gate.cleanupResult = 'FAIL'; throw 'Passing run lacks a confirmed successful client exit' }
        if ($status.pid -and $null -eq $status.exitCode) { $gate.cleanupResult = 'FAIL'; throw 'Client exit is unconfirmed' }
        $cases = @($run.cases | ForEach-Object scenario)
        $evidence = Join-Path $report 'run/evidence'
        $suitePlan = Join-Path $evidence 'suite-plan.json'
        if (!(Test-Path -LiteralPath $suitePlan)) { throw 'Missing suite identity for visual validation' }
        $validLeaves = @(& (Join-Path $PSScriptRoot 'get-ui-smoke-results.ps1') -Target $run.target -Profile $run.profile -Scenarios $cases -Evidence $evidence -ExpectedAdapters (Join-Path $report 'bundle/expected-adapters.json'))
        if (@($validLeaves | Where-Object result -ne 'PASS').Count) { throw 'Semantic leaf validation failed' }
        $visuals = @(& (Join-Path $PSScriptRoot 'test-ui-smoke-visuals.ps1') -Target $run.target -Profile $run.profile -Scenarios $cases -Evidence $evidence -SuitePlan $suitePlan -EnvironmentFile (Join-Path $report 'environment.json') -ContractsFile $ContractsFile -OutputDirectory (Join-Path $report 'visuals'))
        $gate.visualResults += [ordered]@{target=$run.target;graph=$run.graph;required=$run.required;checkpoints=$visuals}
    } catch {
        $gate.reasons += "$($run.target)/$($run.graph): $($_.Exception.Message)"
        if ($run.required) { $gate.semanticResult = 'FAIL' }
    }
}
$gate.validationMs = $clock.ElapsedMilliseconds
$requiredVisuals = @($gate.visualResults | Where-Object required | ForEach-Object checkpoints)
$overall = if ($gate.semanticResult -ne 'PASS' -or $gate.cleanupResult -ne 'PASS' -or @($requiredVisuals | Where-Object result -eq 'FAIL').Count) { 'FAIL' }
    elseif (@($requiredVisuals | Where-Object result -eq 'REVIEW_REQUIRED').Count) { 'REVIEW_REQUIRED' } else { 'PASS' }
$attempt = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$pending = Join-Path $archiveRootPath ('.pending-' + $attempt)
$destination = Join-Path $archiveRootPath $attempt
$archiveClock = [Diagnostics.Stopwatch]::StartNew()
try {
    New-Item -ItemType Directory -Path $pending -ErrorAction Stop | Out-Null
    $manifest = @()
    $files = @(foreach ($file in Get-ChildItem -LiteralPath $campaign -Recurse -File) {
        $relative = $file.FullName.Substring($campaign.Length).TrimStart('/','\')
        $normalized = $relative.Replace('\','/')
        if ($normalized -match '(^|/)runtime(/|$)') { continue }
        if ($normalized -match '(^|/)bundle(/|$)' -and $normalized -notmatch '/bundle/(expected-adapters\.json|mods/[^/]+\.jar)$') { continue }
        [pscustomobject]@{source=$file.FullName;relative=$normalized}
    })
    $files += [pscustomobject]@{source=[IO.Path]::GetFullPath($ContractsFile);relative='visual-contracts/scripts/catalogue.json'}
    $baselineRoot = Join-Path (Split-Path -Parent $ContractsFile) '../test-fixtures/ui-smoke-visuals'
    if (Test-Path -LiteralPath $baselineRoot) {
        $files += @(Get-ChildItem -LiteralPath $baselineRoot -File -Filter '*.png' | ForEach-Object {
            [pscustomobject]@{source=$_.FullName;relative="visual-contracts/test-fixtures/ui-smoke-visuals/$($_.Name)"}
        })
    }
    foreach ($file in $files) {
        $copy = Join-Path $pending $file.relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $copy) -Force | Out-Null
        if (Test-Path -LiteralPath $copy) { throw 'Archive path collision' }
        Copy-Item -LiteralPath $file.source -Destination $copy
        $hash = (Get-FileHash -LiteralPath $file.source -Algorithm SHA256).Hash
        if ((Get-FileHash -LiteralPath $copy -Algorithm SHA256).Hash -cne $hash) { throw 'Archive copy hash mismatch' }
        $manifest += [ordered]@{path=$file.relative;sha256=$hash;bytes=(Get-Item -LiteralPath $copy).Length}
    }
    $gate.archiveMs = $archiveClock.ElapsedMilliseconds
    $gate.archiveResult = 'PASS'; $gate.overall = $overall
    $gate | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath (Join-Path $pending 'gate.json') -Encoding UTF8
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $pending 'manifest.json') -Encoding UTF8
    $reportText = @('# UI smoke result', '', "Result: $overall", "Commit: $($raw.commit)", '', '| Target / graph | Runtime result |', '|---|---|')
    foreach ($run in $raw.results) {
        $reportText += "| $($run.target) / $($run.graph) | $($run.result) |"
    }
    $reportText += @('', '| Case | Result | Evidence |', '|---|---|---|')
    foreach ($run in $raw.results) {
        $relativeReport = [IO.Path]::GetFullPath($run.report).Substring($campaign.Length).TrimStart('/','\').Replace('\','/')
        foreach ($case in $run.cases) {
            $leaf = "$relativeReport/run/evidence/" + $(if ($run.cases.Count -gt 1) { "$($case.scenario)/" } else { '' }) + 'result.json'
            $link = if (Test-Path -LiteralPath (Join-Path $pending $leaf)) { "[Result]($($leaf.Replace(' ','%20')))" } else { 'Not captured' }
            $reportText += "| $($run.target) / $($run.graph) / $($case.scenario) | $($case.result) | $link |"
        }
    }
    $reportText += @('', "Visual validation: $($gate.validationMs) ms", "Archive copy: $($gate.archiveMs) ms", '', '[Structured gate](gate.json)', '[Archive hashes](manifest.json)', '[Visual contracts](visual-contracts/scripts/catalogue.json)')
    $reportText | Set-Content -LiteralPath (Join-Path $pending 'report.md') -Encoding UTF8
    [IO.Directory]::Move($pending,$destination)
    $gate.archive = $destination
} catch { $gate.archiveResult='FAIL'; $gate.overall='FAIL'; $gate.reasons += $_.Exception.Message }
$temporary = $gatePath + '.tmp'
$gate | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $temporary -Encoding UTF8
[IO.File]::Move($temporary,$gatePath)
[pscustomobject]$gate
