$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-visual-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp,(Join-Path $temp 'scripts'),(Join-Path $temp 'test-fixtures/ui-smoke-visuals') | Out-Null
Add-Type -AssemblyName System.Drawing
function Assert($condition,$message) { if (!$condition) { throw $message } }
function Write-Json($path,$data) { ConvertTo-Json -InputObject $data -Depth 20 | Set-Content -LiteralPath $path -Encoding UTF8 }
$world = 'ae2ct-' + [guid]::NewGuid().ToString('N')
$image = Join-Path $temp 'checkpoint.png'
$sidecar = Join-Path $temp 'checkpoint.json'
$baseline = Join-Path $temp 'test-fixtures/ui-smoke-visuals/baseline.png'
$contractPath = Join-Path $temp 'scripts/contracts.json'
$environment = Join-Path $temp 'environment.json'
$plan = Join-Path $temp 'suite-plan.json'
$bitmap = [Drawing.Bitmap]::new(16,16)
try { $graphics=[Drawing.Graphics]::FromImage($bitmap); try {$graphics.Clear([Drawing.Color]::Red)} finally {$graphics.Dispose()}; $bitmap.Save($image,[Drawing.Imaging.ImageFormat]::Png); $bitmap.Save($baseline,[Drawing.Imaging.ImageFormat]::Png) } finally {$bitmap.Dispose()}
$validImage = [IO.File]::ReadAllBytes($image)
$data = @{screen='screen';menu='menu';gui=@{x=0;y=0;width=16;height=16};screenWidth=16;screenHeight=16;guiScale=1
    capture=@{schema=1;id="$world/checkpoint.png";world=$world;scenario='craft-plan';profile='compatible';frame=1;width=16;height=16;renderer='test renderer';sha256=(Get-FileHash $image).Hash}}
Write-Json $sidecar $data
Write-Json (Join-Path $temp 'result.json') @{target='1.20.1-forge';profile='compatible';scenario='craft-plan';language='en_us';screenshots=@('checkpoint.png');checks=@{clock=$true}}
Write-Json $plan @{schema=1;cases=@(@{scenario='craft-plan';world=$world})}
Write-Json $environment @{schema=1;target='1.20.1-forge';graphSha256='graph';fixtureSha256='fixture';resourceSha256='resource';os='test';gpuDriver='test'}
$contracts = @{schema=1;checkpoints=@()}
Write-Json $contractPath $contracts
function Read-Visual {
    @(& "$PSScriptRoot/test-ui-smoke-visuals.ps1" -Evidence $temp -Target 1.20.1-forge -Profile compatible -Scenarios craft-plan -SuitePlan $plan -EnvironmentFile $environment -ContractsFile $contractPath -OutputDirectory (Join-Path $temp 'visuals'))
}
try {
    $unknown = Read-Visual
    Assert ($unknown[0].result -eq 'REVIEW_REQUIRED') ("Unqualified valid screenshot must require review: " + ($unknown | ConvertTo-Json -Depth 8 -Compress))
    $contract = @{scenario='craft-plan';image='checkpoint.png';environmentId=$unknown[0].environmentId;disposition='automatic';revision=1;qualification='synthetic-negative-test';regions=@(@{baseline='baseline.png';sha256=(Get-FileHash $baseline).Hash;rect=@(0,0,16,16);masks=@()})}
    $contracts.checkpoints=@($contract); Write-Json $contractPath $contracts
    Assert ((Read-Visual)[0].result -eq 'PASS') 'Identical qualified image must pass'
    Write-Json $plan @{schema=2;cases=@(@{scenario='craft-plan';world=$world})}
    Assert ((Read-Visual)[0].result -eq 'FAIL') 'Shared-world suite must reject legacy capture IDs'
    $data.capture.schema=2; $data.capture.id="$world/craft-plan/checkpoint.png"; Write-Json $sidecar $data
    Assert ((Read-Visual)[0].result -eq 'PASS') 'Shared-world capture identity must pass'
    Write-Json $plan @{schema=1;cases=@(@{scenario='craft-plan';world=$world})}
    Assert ((Read-Visual)[0].result -eq 'PASS') 'New capture identity must work in legacy suites and single runs'
    Write-Json $plan @{schema=2;cases=@(@{scenario='craft-plan';world=$world},@{scenario='crafting-tree-screen';world=$world})}
    foreach ($scenario in @('craft-plan','crafting-tree-screen')) {
        $directory=Join-Path $temp $scenario
        New-Item -ItemType Directory -Path $directory | Out-Null
        Copy-Item $image (Join-Path $directory 'checkpoint.png')
        $data.capture.scenario=$scenario; $data.capture.id="$world/$scenario/checkpoint.png"
        Write-Json (Join-Path $directory 'checkpoint.json') $data
        Write-Json (Join-Path $directory 'result.json') @{target='1.20.1-forge';profile='compatible';scenario=$scenario;language='en_us';screenshots=@('checkpoint.png');checks=@{clock=$true}}
    }
    $shared=@(& "$PSScriptRoot/test-ui-smoke-visuals.ps1" -Evidence $temp -Target 1.20.1-forge -Profile compatible `
        -Scenarios @('craft-plan','crafting-tree-screen') -SuitePlan $plan -EnvironmentFile $environment -ContractsFile $contractPath -OutputDirectory (Join-Path $temp 'shared-visuals'))
    Assert ($shared.Count -eq 2 -and $shared[0].result -eq 'PASS' -and $shared[1].result -eq 'REVIEW_REQUIRED') 'Same image basename across shared-world cases must have independent identities'
    $data.capture.scenario='craft-plan'; $data.capture.id="$world/craft-plan/checkpoint.png"; Write-Json $sidecar $data
    Write-Json $plan @{schema=2;cases=@(@{scenario='craft-plan';world=$world})}
    $changed=[Drawing.Bitmap]::new($image)
    try {$changed.SetPixel(3,3,[Drawing.Color]::Blue); $changed.Save((Join-Path $temp 'changed.png'),[Drawing.Imaging.ImageFormat]::Png)} finally {$changed.Dispose()}
    Copy-Item (Join-Path $temp 'changed.png') $image -Force
    Assert ((Read-Visual)[0].reason -match 'hash') 'Stale capture hash must fail'
    $data.capture.sha256=(Get-FileHash $image).Hash; Write-Json $sidecar $data
    Assert ((Read-Visual)[0].reason -match 'Visual mismatch') 'Wrong pixel color must fail even with valid identity'
    Assert (Test-Path (Join-Path $temp 'visuals/craft-plan-checkpoint-1.diff.png')) 'Mismatch must save a diff'
    $contract.regions[0].masks=@(@{rect=@(3,3,1,1);reason='synthetic clock';assertion='clock'}); Write-Json $contractPath $contracts
    Assert ((Read-Visual)[0].result -eq 'PASS') 'Explicit independently asserted mask must work'
    $contract.regions[0].masks[0].assertion='missing'; Write-Json $contractPath $contracts
    Assert ((Read-Visual)[0].result -eq 'FAIL') 'Mask without assertion must fail'
    $contract.regions[0].masks=@(); Write-Json $contractPath $contracts
    [IO.File]::WriteAllBytes($image,$validImage); $data.capture.sha256=(Get-FileHash $image).Hash; Write-Json $sidecar $data
    foreach ($field in @('world','scenario','profile','id','schema','frame','width','height','renderer')) {
        $old=$data.capture[$field]; $data.capture[$field]=if($old -is [int]) {0} else {'wrong'}; Write-Json $sidecar $data
        if ($field -eq 'renderer') { Assert ((Read-Visual)[0].result -eq 'REVIEW_REQUIRED') 'New renderer must require review' }
        else { Assert ((Read-Visual)[0].result -eq 'FAIL') ("Invalid $field must fail: " + ((Read-Visual) | ConvertTo-Json -Depth 8 -Compress)) }
        $data.capture[$field]=$old
    }
    Write-Json $sidecar $data
    [IO.File]::WriteAllText($image,'not a png'); $data.capture.sha256=(Get-FileHash $image).Hash; Write-Json $sidecar $data
    Assert ((Read-Visual)[0].result -eq 'FAIL') 'Corrupt image must fail decoding'
    [IO.File]::WriteAllBytes($image,$validImage); $data.capture.sha256=(Get-FileHash $image).Hash; Write-Json $sidecar $data
    $contract.regions[0].rect=@(15,15,16,16); Write-Json $contractPath $contracts
    Assert ((Read-Visual)[0].result -eq 'FAIL') 'Out-of-frame crop must fail'
    $contract.regions[0].rect=@(0,0,16,16); $contract.regions[0].sha256='wrong'; Write-Json $contractPath $contracts
    Assert ((Read-Visual)[0].reason -match 'baseline hash') 'Corrupt baseline must fail'
    Write-Host 'PASS: visual identity, corrupt images, baseline review, pixel mismatch, masks and crop bounds'
} finally {
    $resolved=[IO.Path]::GetFullPath($temp)
    if (!(Split-Path $resolved -Leaf).StartsWith('ae2ct-visual-') -or !$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()),[StringComparison]::OrdinalIgnoreCase)) {throw 'Unsafe visual test cleanup'}
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
