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
    Assert ($result.overall -eq 'FAIL' -and $result.archiveResult -eq 'PASS') 'Failed campaign must stay failed but archive'
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
    Write-Host 'PASS: archive preflight, failure preservation, manifests and immutable attempts'
} finally {
    $resolved=[IO.Path]::GetFullPath($temp)
    if (!(Split-Path $resolved -Leaf).StartsWith('ae2ct-archive-') -or !$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()),[StringComparison]::OrdinalIgnoreCase)) {throw 'Unsafe archive test cleanup'}
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
