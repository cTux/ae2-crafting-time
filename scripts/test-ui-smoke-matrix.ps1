$ErrorActionPreference = 'Stop'
$shell = (Get-Process -Id $PID).Path
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-matrix-' + [guid]::NewGuid().ToString('N'))
$scripts = Join-Path $temp 'scripts'
function Assert-Rejected([scriptblock]$Action, [string]$Message) {
    try { & $Action; throw "DID_NOT_REJECT: $Message" }
    catch { if ($_.Exception.Message -like 'DID_NOT_REJECT:*') { throw } }
}
function Assert-ProcessRejected([string[]]$Arguments, [string]$Reason) {
    $savedPreference=$ErrorActionPreference
    try { $ErrorActionPreference='Continue';$output = (& $shell @Arguments 2>&1) -join "`n" }
    finally { $ErrorActionPreference=$savedPreference }
    if ($LASTEXITCODE -eq 0 -or $output -notlike "*$Reason*") {
        throw "Subprocess did not reject with '$Reason': exit=$LASTEXITCODE output=$output"
    }
}
New-Item -ItemType Directory -Path $scripts -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke-matrix.ps1'), (Join-Path $PSScriptRoot 'release-matrix.json') -Destination $scripts
foreach ($file in @('get-ui-smoke-plan.ps1','get-ui-smoke-results.ps1','expand-ui-smoke-groups.ps1','run-client-versions.json',
        'use-ui-smoke-bundle-cache.ps1',
        'ui-smoke-impact.json','ui-smoke-groups.json','ui-smoke-coverage.json','ui-smoke-forge-suite.json',
        'ui-smoke-fabric-suite.json','ui-smoke-neoforge-suite.json','ui-smoke-neoforge-26.1.2-suite.json')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination $scripts
}
& git -C $temp init --quiet
& git -C $temp -c user.name=Test -c user.email=test@example.invalid -c core.hooksPath=disabled-hooks commit --allow-empty -qm fixture
if ($LASTEXITCODE -ne 0) { throw 'Could not initialize matrix fixture' }
@'
param([string]$Target,[string]$BundleDirectory,[switch]$BaseOnly,[string[]]$ProjectId,[switch]$ValidateOnly)
$path=Join-Path $BundleDirectory 'expected-adapters.json'
if($ValidateOnly){
    if(!(Test-Path -LiteralPath $path) -or !(Test-Path -LiteralPath (Join-Path $BundleDirectory 'bundle-identity.json'))){throw 'Copied bundle was not sealed with adapter expectations'}
}else{
    if(Test-Path -LiteralPath (Join-Path $BundleDirectory 'bundle-identity.json')){throw 'Adapter producer tried to mutate a sealed cache'}
    [IO.File]::WriteAllText($path,'{}')
}
'@ | Set-Content (Join-Path $scripts 'prepare-ui-smoke-adapters.ps1')
@'
param([string]$Target,[switch]$Latest)
[pscustomobject]@{projectId='ae2';name='AE2';disposition='DIRECT_UI';scenario='standard-plan-controls';reason='';result='NOT_RUN'}
'@ | Set-Content (Join-Path $scripts 'get-ui-smoke-coverage.ps1')
@'
param([string]$Target,[switch]$Latest,[switch]$ResolveOnly,[switch]$Packaged,[string]$RuntimeDirectory,[string[]]$ProjectId,[switch]$BaseOnly)
if (-not $ResolveOnly -or -not $Packaged) { throw 'Guest build path selected' }
$graph = Split-Path -Leaf $RuntimeDirectory
[pscustomobject]@{target=$Target;graph=$graph;baseOnly=[bool]$BaseOnly;projectId=@($ProjectId)} | ConvertTo-Json -Compress |
    Add-Content $env:AE2CT_RESOLVER_CALLS
if ($Target -eq '1.20.1-fabric') { throw 'intentional resolution failure' }
New-Item -ItemType Directory -Path (Join-Path $RuntimeDirectory 'mods') -Force | Out-Null
'@ | Set-Content (Join-Path $scripts 'run-client.ps1')
@'
param([string]$Target,[switch]$Latest,[string]$Scenario,[string]$BundleDirectory,[string]$PreparedLaunchRoot,[string]$GuestSourceRoot,[string]$CasesBase64,[string[]]$ProjectId,[switch]$BaseOnly,[switch]$Interactive,[switch]$ResourceFixtureOnly,[int]$StartupTimeoutSeconds)
$sealed=Get-Content (Join-Path $BundleDirectory 'bundle-identity.json') -Raw|ConvertFrom-Json
$null=& (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') -Mode Reuse -CacheDirectory $BundleDirectory -HeadSha $sealed.headSha -Fingerprint $sealed.fingerprint -Target $Target -Profile $sealed.profile -GraphId $sealed.graphId -BaseOnly:$BaseOnly
$profile=if($Latest){'latest'}else{'compatible'}
$live=Join-Path (Split-Path -Parent $PSScriptRoot) "build/ui-smoke/$Target/$profile/$Scenario"
$cases = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($CasesBase64)) | ConvertFrom-Json
$contracts = (Get-Content (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json).cases
foreach ($case in $cases) {
    $evidence = if ($cases.Count -eq 1) { "$live/evidence" } else { "$live/evidence/$case" }
    New-Item -ItemType Directory -Path $evidence -Force | Out-Null
    $checks = [ordered]@{}
    foreach ($check in $contracts.$case.checks) { $checks[$check] = $true }
    $images = if ($contracts.$case) { @($contracts.$case.screenshots) } else { @('fixture.png') }
    foreach ($image in $images) {
        Set-Content (Join-Path $evidence $image) 'fixture-image'
        @{screen='fixture-screen';screenWidth=100;screenHeight=100;guiScale=2;gui=@{x=0;y=0;width=100;height=100}} | ConvertTo-Json | Set-Content (Join-Path $evidence $image.Replace('.png','.json'))
    }
    @{schema=1;complete=$true;target=$Target;profile=$profile;scenario=$case;language='en_us';result='PASS';checks=$checks;screenshots=@($images)} |
        ConvertTo-Json -Depth 6 | Set-Content "$evidence/result.json"
}
@{phase='passed';message='';pid=123;exitCode=0} | ConvertTo-Json | Set-Content "$live/status.json"
if ($env:AE2CT_INNER_FAIL) {
    $exitCode = if ($null -ne $env:AE2CT_FAILURE_EXIT_CODE) { [int]$env:AE2CT_FAILURE_EXIT_CODE } else { 17 }
    $statusCase = if ($env:AE2CT_STATUS_CASE) { $env:AE2CT_STATUS_CASE } else { 'current' }
    $status = [ordered]@{schema=2;runId='failed-run';target=$Target;profile=$profile;scenario=$Scenario;phase='failed';message="native failure $exitCode";pid=123;exitCode=$exitCode;startedAt=[DateTime]::UtcNow.ToString('o')}
    switch ($statusCase) {
        'equal' { $status.startedAt = $started }
        'equal-offset' { $status.startedAt = ([DateTimeOffset]$started).ToOffset([TimeSpan]::FromHours(3)).ToString('o') }
        'older-tick' { $status.startedAt = ([DateTimeOffset]$started).AddTicks(-1).ToString('o') }
        'missing-start' { $null = $status.Remove('startedAt') }
        'malformed-start' { $status.startedAt = 'not-a-date' }
        'wrong-target' { $status.target = '1.20.1-fabric' }
        'wrong-profile' { $status.profile = if ($profile -eq 'latest') { 'compatible' } else { 'latest' } }
        'wrong-scenario' { $status.scenario = 'craft-plan' }
        'existing' {
            New-Item -ItemType Directory -Path (Join-Path $report 'run') -Force | Out-Null
            [IO.File]::WriteAllBytes((Join-Path $report 'run/existing.bin'), [byte[]](9,8,7))
        }
    }
    $failureEvidence = if ($cases.Count -eq 1) { "$live/evidence" } else { "$live/evidence/$($cases[0])" }
    New-Item -ItemType Directory -Path $failureEvidence,"$live/logs" -Force | Out-Null
    [IO.File]::WriteAllBytes((Join-Path $failureEvidence 'failure.png'), [byte[]](137,80,78,71,13,10,26,10,1,2,3))
    [IO.File]::WriteAllBytes((Join-Path $failureEvidence 'failure.json'), [Text.Encoding]::UTF8.GetBytes('{"failure":"native"}'))
    [IO.File]::WriteAllBytes((Join-Path $failureEvidence 'result.json'), [Text.Encoding]::UTF8.GetBytes('{"schema":1,"complete":true,"result":"FAIL"}'))
    [IO.File]::WriteAllBytes("$live/logs/client.log", [Text.Encoding]::UTF8.GetBytes('native failure log'))
    if ($statusCase -eq 'missing-status') {
        Remove-Item -LiteralPath "$live/status.json" -ErrorAction SilentlyContinue
    } elseif ($statusCase -eq 'malformed-status') {
        Set-Content -LiteralPath "$live/status.json" -Value '{' -NoNewline
    } else {
        $status | ConvertTo-Json | Set-Content "$live/status.json"
    }
    throw "dispatch observed child exit $exitCode"
}
if ($env:AE2CT_STATUS_GAP) {
    Remove-Item -LiteralPath "$live/status.json"
    Start-Job -ArgumentList "$live/status.json" -ScriptBlock {
        param($path)
        Start-Sleep -Seconds 4
        @{phase='passed';message='';pid=123;exitCode=0} | ConvertTo-Json | Set-Content -LiteralPath $path
    } | Out-Null
}
if ($env:AE2CT_UNCONFIRMED_EXIT) {
    @{phase='failed';message='termination failed';pid=123;exitCode=$null} | ConvertTo-Json | Set-Content "$live/status.json"
}
'@ | Set-Content (Join-Path $scripts 'invoke-ui-smoke-codexvm.ps1')
@'
param([string]$Target,[string]$ArtifactHashes,[string]$Evidence,[string]$FixtureDirectory)
@{schema=1}
'@ | Set-Content (Join-Path $scripts 'get-ui-smoke-environment.ps1')
@'
param([string]$CampaignDirectory,[string]$ArchiveRoot,[switch]$Preflight)
if ($Preflight) { return }
$raw=Get-Content (Join-Path $CampaignDirectory 'result.json') -Raw | ConvertFrom-Json
@{overall=$(if(@($raw.results | Where-Object { $_.required -and $_.result -ne 'PASS' }).Count){'FAIL'}else{'PASS'});archive='fixture'}
'@ | Set-Content (Join-Path $scripts 'complete-ui-smoke-evidence.ps1')

Set-Content -LiteralPath (Join-Path $temp '.gitignore') 'build/'
try {
    $productionPreview = & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -PlanOnly `
        -Target 1.20.1-forge -Scenario delayed-resource-icons
    if ($LASTEXITCODE -ne 0 -or (($productionPreview -join "`n") | ConvertFrom-Json).targets[0].cases[0] -cne 'delayed-resource-icons') {
        throw 'Production resource icon plan was not preserved'
    }
    Assert-ProcessRejected @('-NoProfile','-File',(Join-Path $scripts 'run-ui-smoke-matrix.ps1'),'-PlanOnly',
        '-Target','1.20.1-forge','-Scenario','waiting-status','-ResourceFixtureOnly') 'ResourceFixtureOnly requires'
    Assert-ProcessRejected @('-NoProfile','-File',(Join-Path $scripts 'run-ui-smoke-matrix.ps1'),'-PlanOnly',
        '-Target','26.1.2-neoforge','-Scenario','appmek-resource-icons','-ResourceFixtureOnly') 'supported only'
    $resourcePreview = & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -PlanOnly `
        -Target 1.20.1-forge -Scenario delayed-resource-icons -ResourceFixtureOnly
    if ($LASTEXITCODE -ne 0 -or (($resourcePreview -join "`n") | ConvertFrom-Json).targets[0].cases[0] -cne 'delayed-resource-icons') {
        throw 'Explicit native resource fixture plan was not preserved'
    }
    $preview = & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -PlanOnly
    if ($LASTEXITCODE -ne 0 -or (Test-Path -LiteralPath (Join-Path $temp 'build'))) { throw 'Plan-only built or dispatched a client' }
    $previewPlan = ($preview -join "`n") | ConvertFrom-Json
    if ($previewPlan.targets.Count -ne 4 -or $previewPlan.mode -ne 'manual') { throw 'Plan-only lost explicit full scope' }
    foreach ($latest in @($false,$true)) {
        $resolverCalls = $temp + '-resolver-calls.jsonl'
        $env:AE2CT_RESOLVER_CALLS = $resolverCalls
        Remove-Item $resolverCalls -ErrorAction SilentlyContinue
        $arguments = @('-NoProfile','-File',(Join-Path $scripts 'run-ui-smoke-matrix.ps1'))
        if ($latest) { $arguments += '-Latest' }
        & $shell @arguments
        $exitCode = $LASTEXITCODE
        if (($exitCode -eq 0) -ne $latest) { throw 'Compatible failures and latest diagnostics have the same exit behavior' }
        $profile = if ($latest) { 'latest' } else { 'compatible' }
        $report = Get-ChildItem (Join-Path $temp 'build/ui-smoke/campaigns') -File -Recurse -Filter result.json |
            Where-Object { $_.Directory.Name -eq $profile } | Select-Object -Last 1
        $results = (Get-Content $report.FullName -Raw | ConvertFrom-Json).results
        $expectedPlan = if ($latest) {
            (& $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -Latest -PlanOnly) -join "`n" | ConvertFrom-Json
        } else { $previewPlan }
        $expectedGraphs = @($expectedPlan.targets | ForEach-Object { $targetId=$_.target; $_.graphs | ForEach-Object { "$targetId/$($_.id)" } })
        $observedGraphs = @($results | ForEach-Object { "$($_.target)/$($_.graph)" })
        if (Compare-Object $expectedGraphs $observedGraphs -SyncWindow 0) { throw 'An earlier failure skipped or reordered a planned graph' }
        $resolver = @(Get-Content $resolverCalls | ForEach-Object { $_ | ConvertFrom-Json })
        $expectedResolution = @($expectedPlan.targets | ForEach-Object { $targetId=$_.target; $_.graphs | ForEach-Object {
            "$targetId/$($_.id)/$([bool]$_.baseOnly)/$(@($_.projectId) -join ',')" } })
        $observedResolution = @($resolver | ForEach-Object { "$($_.target)/$($_.graph)/$([bool]$_.baseOnly)/$(@($_.projectId) -join ',')" })
        if (Compare-Object $expectedResolution $observedResolution -SyncWindow 0) { throw 'Resolver arguments drifted from exact planner graph metadata' }
        if ($results[-1].target -ne '26.1.2-neoforge' -or $results[-1].result -ne 'PASS') { throw ("Last planned target did not finish: " + ($results[-1] | Select-Object target,graph,result,message,cases | ConvertTo-Json -Depth 4 -Compress)) }
        $expected = if ($latest) { 'DIAGNOSTIC_FAILURE' } else { 'FAIL_SETUP' }
        $fabricResult = @($results | Where-Object target -eq '1.20.1-fabric')
        $expectedFabric = @($expectedPlan.targets | Where-Object target -eq '1.20.1-fabric' | ForEach-Object graphs)
        if ($fabricResult.Count -ne $expectedFabric.Count -or
                @($fabricResult | Where-Object { $_.result -ne $expected -or $_.message -notlike '*intentional resolution failure*' }).Count -ne 0) {
            throw 'Failure classification or evidence was lost'
        }
    }
    $env:AE2CT_STATUS_GAP = '1'
    try {
        & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -Target 1.20.1-forge -Scenario waiting-status
        if ($LASTEXITCODE -ne 0) { throw 'Transient status replacement must not lose the running client' }
    } finally { Remove-Item Env:\AE2CT_STATUS_GAP -ErrorAction SilentlyContinue }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'complete-ui-smoke-evidence.ps1'), (Join-Path $PSScriptRoot 'ui-smoke-visuals.json') -Destination $scripts -Force
    $archiveRoot = Join-Path $temp 'archive'
    $env:AE2CT_UNCONFIRMED_EXIT = '1'
    try {
        & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -ArchiveRoot $archiveRoot
        if ($LASTEXITCODE -ne 1) { throw 'Unconfirmed client exit must fail the compatible campaign' }
        $report = Get-ChildItem (Join-Path $temp 'build/ui-smoke/campaigns') -File -Recurse -Filter result.json |
            Where-Object { $_.Directory.Name -eq 'compatible' } | Sort-Object LastWriteTimeUtc | Select-Object -Last 1
        $results = (Get-Content $report.FullName -Raw | ConvertFrom-Json).results
        if ($results.Count -ne 1 -or $results[0].message -notlike '*exit is unconfirmed*') {
            throw 'The matrix launched another client after unconfirmed termination'
        }
        $coverage = Get-Content (Join-Path $results[0].report 'coverage.json') -Raw | ConvertFrom-Json
        if ($coverage.result -ne 'PASS') { throw 'A failed run erased its completed scenario outcome' }
        $gate = Get-Content (Join-Path $report.Directory.FullName 'gate.json') -Raw | ConvertFrom-Json
        if ($gate.cleanupResult -ne 'FAIL' -or $gate.archiveResult -ne 'PASS') { throw 'Unconfirmed exit cleanup or archive evidence was lost' }
    } finally { Remove-Item Env:\AE2CT_UNCONFIRMED_EXIT -ErrorAction SilentlyContinue }

    function Invoke-FailedMatrixCase([string]$statusCase, [int]$exitCode = 17, [switch]$Latest) {
        $env:AE2CT_INNER_FAIL = '1'
        $env:AE2CT_STATUS_CASE = $statusCase
        $env:AE2CT_FAILURE_EXIT_CODE = [string]$exitCode
        try {
            $arguments = @('-NoProfile','-File',(Join-Path $scripts 'run-ui-smoke-matrix.ps1'),'-Target','1.20.1-forge','-Scenario','waiting-status','-ArchiveRoot',$archiveRoot)
            if ($Latest) { $arguments += '-Latest' }
            $null = & $shell @arguments
            $matrixExit = $LASTEXITCODE
        } finally {
            Remove-Item Env:\AE2CT_INNER_FAIL,Env:\AE2CT_STATUS_CASE,Env:\AE2CT_FAILURE_EXIT_CODE -ErrorAction SilentlyContinue
        }
        $profile = if ($Latest) { 'latest' } else { 'compatible' }
        $report = Get-ChildItem (Join-Path $temp 'build/ui-smoke/campaigns') -File -Recurse -Filter result.json |
            Where-Object { $_.Directory.Name -eq $profile } | Sort-Object LastWriteTimeUtc | Select-Object -Last 1
        $result = (Get-Content $report.FullName -Raw | ConvertFrom-Json).results[0]
        [pscustomobject]@{exitCode=$matrixExit;campaign=$report.Directory.FullName;result=$result;gate=(Get-Content (Join-Path $report.Directory.FullName 'gate.json') -Raw | ConvertFrom-Json)}
    }

    foreach ($case in @(
        @{status='current';exit=0;latest=$false},
        @{status='equal';exit=17;latest=$false},
        @{status='equal-offset';exit=17;latest=$true}
    )) {
        $run = Invoke-FailedMatrixCase -statusCase $case.status -exitCode $case.exit -Latest:([bool]$case.latest)
        $expectedResult = if ($case.latest) { 'DIAGNOSTIC_FAILURE' } else { 'FAIL' }
        $expectedExit = if ($case.latest) { 0 } else { 1 }
        $retainedStatus = Get-Content (Join-Path $run.result.report 'run/status.json') -Raw | ConvertFrom-Json
        if ($run.exitCode -ne $expectedExit -or $run.result.result -ne $expectedResult -or $run.result.message -ne "native failure $($case.exit)" -or
                $retainedStatus.pid -ne 123 -or $retainedStatus.exitCode -ne $case.exit -or $run.result.cases[0].result -ne 'FAIL') {
            throw "A $($case.status) failing child hid its status, classification, PID, exit code or failed leaf"
        }
        foreach ($relative in @('status.json','evidence/failure.png','evidence/failure.json','evidence/result.json','logs/client.log')) {
            $campaignFile = Join-Path $run.result.report "run/$relative"
            $liveFile = Join-Path $temp "build/ui-smoke/1.20.1-forge/$(if($case.latest){'latest'}else{'compatible'})/waiting-status/$relative"
            $archiveRelative = ([IO.Path]::GetFullPath($campaignFile)).Substring(([IO.Path]::GetFullPath($run.campaign)).Length).TrimStart('/','\').Replace('\','/')
            $archiveFile = Join-Path $run.gate.archive $archiveRelative
            if ((Get-FileHash $campaignFile).Hash -cne (Get-FileHash $liveFile).Hash -or (Get-FileHash $campaignFile).Hash -cne (Get-FileHash $archiveFile).Hash) {
                throw "Failed evidence bytes changed while retaining $relative"
            }
            $manifest = Get-Content (Join-Path $run.gate.archive 'manifest.json') -Raw | ConvertFrom-Json
            if (!($manifest | Where-Object { $_.path -ceq $archiveRelative -and $_.sha256 -ceq (Get-FileHash $campaignFile).Hash })) {
                throw "Archive manifest lost $relative"
            }
        }
    }

    foreach ($statusCase in @('older-tick','missing-status','malformed-status','missing-start','malformed-start','wrong-target','wrong-profile','wrong-scenario')) {
        $run = Invoke-FailedMatrixCase -statusCase $statusCase
        if (Test-Path -LiteralPath (Join-Path $run.result.report 'run')) { throw "Rejected $statusCase status was retained" }
        if ($run.result.message -notlike '*dispatch observed child exit 17*') { throw "Rejected $statusCase status replaced the dispatch failure" }
    }

    $run = Invoke-FailedMatrixCase -statusCase existing
    $existing = Join-Path $run.result.report 'run/existing.bin'
    $existingChanged = !(Test-Path -LiteralPath $existing)
    if (!$existingChanged) { $existingChanged = @(Compare-Object ([IO.File]::ReadAllBytes($existing)) ([byte[]](9,8,7)) -SyncWindow 0).Count -ne 0 }
    if ($existingChanged -or (Test-Path -LiteralPath (Join-Path $run.result.report 'run/status.json'))) {
        throw 'Current fallback replaced existing campaign evidence'
    }
    Write-Host 'UI smoke matrix checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if ($resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
