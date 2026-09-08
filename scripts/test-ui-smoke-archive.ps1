$ErrorActionPreference='Stop'
$temp=Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-archive-' + [guid]::NewGuid().ToString('N'))
$campaign=Join-Path $temp 'campaign'
$archive=Join-Path $temp 'archive'
New-Item -ItemType Directory -Path $campaign | Out-Null
function Assert($condition,$message) {if(!$condition){throw $message}}
try {
    & "$PSScriptRoot/complete-ui-smoke-evidence.ps1" -ArchiveRoot $archive -Preflight
    Assert (@(Get-ChildItem $archive -Force).Count -eq 0) 'Preflight must leave no probe'
    @{schema=1;runId='test-run';commit='test-sha';results=@(@{target='1.20.1-forge';graph='primary';required=$true;result='FAIL_SETUP';report=(Join-Path $campaign 'forge');cases=@(@{scenario='craft-plan'})})} |
        ConvertTo-Json -Depth 8 | Set-Content (Join-Path $campaign 'result.json')
    $result=& "$PSScriptRoot/complete-ui-smoke-evidence.ps1" -CampaignDirectory $campaign -ArchiveRoot $archive
    Assert ($result.overall -eq 'FAIL' -and $result.archiveResult -eq 'PASS') ("Failed campaign must stay failed but archive: " + ($result | ConvertTo-Json -Depth 8 -Compress))
    Assert (Test-Path (Join-Path $result.archive 'report.md')) 'Archive report missing'
    $manifest=Get-Content (Join-Path $result.archive 'manifest.json') -Raw | ConvertFrom-Json
    foreach($file in $manifest) {Assert ((Get-FileHash (Join-Path $result.archive $file.path)).Hash -eq $file.sha256) 'Archived hash mismatch'}
    $rejected=$false
    try {& "$PSScriptRoot/complete-ui-smoke-evidence.ps1" -CampaignDirectory $campaign -ArchiveRoot $archive} catch {$rejected=$true}
    Assert $rejected 'Finalized attempts must never be overwritten'
    $other=Join-Path $temp 'second'
    New-Item -ItemType Directory $other | Out-Null
    Copy-Item (Join-Path $campaign 'result.json') (Join-Path $other 'result.json')
    $nested=Join-Path $other 'nested'
    $rejected=$false
    try {& "$PSScriptRoot/complete-ui-smoke-evidence.ps1" -CampaignDirectory $other -ArchiveRoot $nested} catch {$rejected=$true}
    Assert $rejected 'Recursive archive destination must be rejected'
    $blocked=Join-Path $temp 'file-not-directory'
    Set-Content $blocked 'fixture'
    $rejected=$false
    try {& "$PSScriptRoot/complete-ui-smoke-evidence.ps1" -ArchiveRoot $blocked -Preflight} catch {$rejected=$true}
    Assert $rejected 'Unwritable archive must fail preflight'
    Add-Type -AssemblyName System.Drawing
    foreach($mode in @('review','automatic','mismatch','missing-pid','unconfirmed-exit','diagnostic')) {
        $sample=Join-Path $temp $mode
        $graph=Join-Path $sample 'forge'
        $evidence=Join-Path $graph 'run/evidence'
        $scripts=Join-Path $sample 'scripts'
        $references=Join-Path $sample 'test-fixtures/ui-smoke-visuals'
        New-Item -ItemType Directory -Path $evidence,$scripts,$references,(Join-Path $graph 'bundle') -Force|Out-Null
        $world='ae2ct-' + [guid]::NewGuid().ToString('N')
        $bitmap=[Drawing.Bitmap]::new(2,2)
        try {$bitmap.Save((Join-Path $evidence 'checkpoint.png'),[Drawing.Imaging.ImageFormat]::Png); $bitmap.Save((Join-Path $references 'baseline.png'),[Drawing.Imaging.ImageFormat]::Png)} finally {$bitmap.Dispose()}
        $image=Join-Path $evidence 'checkpoint.png'
        $snapshot=@{screen='fixture';screenWidth=2;screenHeight=2;guiScale=1;gui=@{x=0;y=0;width=2;height=2};capture=@{schema=1;id="$world/checkpoint.png";world=$world;scenario='advancedae-cpu';profile='compatible';frame=1;width=2;height=2;renderer='fixture';sha256=(Get-FileHash $image).Hash}}
        $snapshot|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence 'checkpoint.json')
        @{schema=1;complete=$true;result='PASS';target='1.20.1-forge';profile='compatible';scenario='advancedae-cpu';language='en_us';screenshots=@('checkpoint.png');checks=@{}}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence 'result.json')
        @{schema=1;cases=@(@{scenario='advancedae-cpu';world=$world})}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence 'suite-plan.json')
        @{schema=1;target='1.20.1-forge';graphSha256='graph';fixtureSha256='fixture';resourceSha256='resource';os='fixture';gpuDriver='fixture'}|ConvertTo-Json|Set-Content (Join-Path $graph 'environment.json')
        '{}'|Set-Content (Join-Path $graph 'bundle/expected-adapters.json')
        $status=@{pid=123;exitCode=0}
        if($mode -eq 'missing-pid'){$status.pid=$null}
        if($mode -eq 'unconfirmed-exit'){$status.exitCode=$null}
        $status|ConvertTo-Json|Set-Content (Join-Path $graph 'run/status.json')
        $required=$mode -ne 'diagnostic'
        @{schema=1;runId=$mode;commit='fixture';results=@(@{target='1.20.1-forge';profile='compatible';graph='primary';required=$required;result='PASS';report=$graph;cases=@(@{scenario='advancedae-cpu'})})}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $sample 'result.json')
        $catalogue=Join-Path $scripts 'contracts.json'
        @{schema=1;checkpoints=@()}|ConvertTo-Json|Set-Content $catalogue
        if($mode -in @('automatic','mismatch')) {
            $visual=@(& "$PSScriptRoot/test-ui-smoke-visuals.ps1" -Evidence $evidence -Target 1.20.1-forge -Profile compatible -Scenarios advancedae-cpu -SuitePlan (Join-Path $evidence 'suite-plan.json') -EnvironmentFile (Join-Path $graph 'environment.json') -ContractsFile $catalogue -OutputDirectory (Join-Path $graph 'visuals'))
            @{schema=1;checkpoints=@(@{scenario='advancedae-cpu';image='checkpoint.png';environmentId=$visual[0].environmentId;disposition='automatic';revision=1;qualification='synthetic gate regression';regions=@(@{baseline='baseline.png';sha256=(Get-FileHash (Join-Path $references 'baseline.png')).Hash;rect=@(0,0,2,2);masks=@()})})}|ConvertTo-Json -Depth 12|Set-Content $catalogue
            if($mode -eq 'mismatch') {
                $changed=[Drawing.Bitmap]::new(2,2)
                try {$changed.SetPixel(0,0,[Drawing.Color]::Red);$changed.Save($image,[Drawing.Imaging.ImageFormat]::Png)} finally {$changed.Dispose()}
                $snapshot.capture.sha256=(Get-FileHash $image).Hash
                $snapshot|ConvertTo-Json -Depth 8|Set-Content (Join-Path $evidence 'checkpoint.json')
            }
        }
        $result=& "$PSScriptRoot/complete-ui-smoke-evidence.ps1" -CampaignDirectory $sample -ArchiveRoot $archive -ContractsFile $catalogue
        $expected=switch($mode){'review'{'REVIEW_REQUIRED'} 'automatic'{'PASS'} 'diagnostic'{'PASS'} default{'FAIL'}}
        Assert ($result.overall -eq $expected) ("$mode must produce $expected, got " + ($result|ConvertTo-Json -Depth 12 -Compress))
        Assert (Test-Path (Join-Path $result.archive 'forge/bundle/expected-adapters.json')) 'Archive must retain adapter validation contract'
    }
    Write-Host 'PASS: archive preflight, gate verdicts, cleanup, manifests and immutable attempts'
} finally {
    $resolved=[IO.Path]::GetFullPath($temp)
    if (!(Split-Path $resolved -Leaf).StartsWith('ae2ct-archive-') -or !$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()),[StringComparison]::OrdinalIgnoreCase)) {throw 'Unsafe archive test cleanup'}
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
