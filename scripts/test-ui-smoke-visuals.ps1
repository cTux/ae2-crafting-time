param(
    [Parameter(Mandatory)][string]$Evidence,
    [Parameter(Mandatory)][string]$Target,
    [Parameter(Mandatory)][string]$Profile,
    [Parameter(Mandatory)][string[]]$Scenarios,
    [Parameter(Mandatory)][string]$SuitePlan,
    [Parameter(Mandatory)][string]$EnvironmentFile,
    [string]$ContractsFile = (Join-Path $PSScriptRoot 'ui-smoke-visuals.json'),
    [Parameter(Mandatory)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
function Read-Rect($value) {
    if ($value -isnot [array] -or $value.Count -ne 4) { throw 'Rectangle needs four integer coordinates' }
    foreach ($number in $value) {
        if ($number -isnot [int] -and $number -isnot [long] -or $number -lt 0 -or $number -gt [int]::MaxValue) { throw 'Invalid rectangle coordinate' }
    }
    return ,([int[]]$value)
}
Add-Type -AssemblyName System.Drawing
if (-not ('SmokePixels' -as [type])) { Add-Type -Path (Join-Path $PSScriptRoot 'ui-smoke-image.cs') -ReferencedAssemblies System.Drawing }
$contracts = Get-Content -LiteralPath $ContractsFile -Raw | ConvertFrom-Json
$plan = Get-Content -LiteralPath $SuitePlan -Raw | ConvertFrom-Json
$environment = Get-Content -LiteralPath $EnvironmentFile -Raw | ConvertFrom-Json
if ($contracts.schema -ne 1 -or $contracts.checkpoints -isnot [array] -or $plan.schema -ne 1 -or $plan.cases -isnot [array] -or $environment.schema -ne 1) { throw 'Unsupported visual input schema' }
foreach ($field in @('target','graphSha256','fixtureSha256','resourceSha256','os','gpuDriver')) {
    if (!$environment.$field) { throw "Missing render environment: $field" }
}
if ($environment.target -cne $Target) { throw 'Wrong render environment target' }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$ids = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$contractIds = @($contracts.checkpoints | ForEach-Object { "$($_.scenario)/$($_.image)/$($_.environmentId)" })
if (@($contractIds | Select-Object -Unique).Count -ne $contractIds.Count) { throw 'Duplicate visual contracts' }
$outcomes = foreach ($scenario in $Scenarios) {
    $directory = if ($Scenarios.Count -eq 1) { $Evidence } else { Join-Path $Evidence $scenario }
    $case = @($plan.cases | Where-Object scenario -CEQ $scenario)
    if ($case.Count -ne 1) { throw "Missing or duplicate suite case: $scenario" }
    $result = Get-Content -LiteralPath (Join-Path $directory 'result.json') -Raw | ConvertFrom-Json
    if ($result.target -cne $Target -or $result.profile -cne $Profile -or $result.scenario -cne $scenario -or
            $result.screenshots -isnot [array] -or !$result.screenshots.Count) { throw 'Mismatched visual result identity' }
    foreach ($image in $result.screenshots) {
        $verdict = 'FAIL'; $reason = ''; $bitmap = $null; $regions = @(); $environmentId = $null
        try {
            if ($image -isnot [string] -or $image -cnotmatch '^[a-z0-9][a-z0-9-]*\.png$') { throw 'Invalid image name' }
            $file = Join-Path $directory $image
            $snapshot = Get-Content -LiteralPath (Join-Path $directory ($image.Replace('.png','.json'))) -Raw | ConvertFrom-Json
            $capture = $snapshot.capture
            if ($capture.schema -ne 1 -or $capture.world -cne $case[0].world -or $capture.scenario -cne $scenario -or
                    $capture.profile -cne $Profile -or $capture.id -cne "$($case[0].world)/$image" -or
                    !$ids.Add($capture.id) -or $capture.frame -isnot [long] -and $capture.frame -isnot [int] -or
                    $capture.frame -lt 1 -or !$capture.renderer) { throw 'Invalid or stale capture identity' }
            if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ine $capture.sha256) { throw 'PNG hash does not match captured frame' }
            $bitmap = [SmokePixels]::Read($file)
            if ($bitmap.Width -ne $capture.width -or $bitmap.Height -ne $capture.height) { throw 'Framebuffer dimensions differ from PNG' }
            $identity = [ordered]@{target=$Target;graphSha256=$environment.graphSha256;fixtureSha256=$environment.fixtureSha256
                resourceSha256=$environment.resourceSha256;os=$environment.os;gpuDriver=$environment.gpuDriver
                renderer=$capture.renderer;width=$capture.width;height=$capture.height;guiScale=$snapshot.guiScale;language=$result.language}
            $json = $identity | ConvertTo-Json -Compress
            $sha = [Security.Cryptography.SHA256]::Create()
            try { $environmentId = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($json)))).Replace('-','').ToLowerInvariant() }
            finally { $sha.Dispose() }
            $matches = @($contracts.checkpoints | Where-Object { $_.scenario -ceq $scenario -and $_.image -ceq $image -and $_.environmentId -ceq $environmentId })
            $verdict = 'REVIEW_REQUIRED'; $reason = 'No qualified baseline for this checkpoint and render environment'
            if ($matches.Count -eq 1) {
                $contract = $matches[0]
                if ($contract.disposition -cnotin @('automatic','review')) { throw 'Unknown visual disposition' }
                if ($contract.disposition -ceq 'automatic') {
                    if (!$contract.revision -or !$contract.qualification -or !$contract.regions.Count) { throw 'Automatic contract lacks qualification or regions' }
                    $index = 0
                    foreach ($region in $contract.regions) {
                        $index++
                        if ($region.baseline -cnotmatch '^[a-zA-Z0-9_-]+\.png$') { throw 'Baseline must be a local PNG basename' }
                        $baselineRoot = Join-Path (Split-Path -Parent $ContractsFile) '../test-fixtures/ui-smoke-visuals'
                        $baseline = Join-Path $baselineRoot $region.baseline
                        if ((Get-FileHash -LiteralPath $baseline -Algorithm SHA256).Hash -ine $region.sha256) { throw 'Approved baseline hash mismatch' }
                        $masks = @()
                        foreach ($mask in $region.masks) {
                            if (!$mask.reason -or !$mask.assertion -or $result.checks.($mask.assertion) -isnot [bool] -or !$result.checks.($mask.assertion)) { throw 'Mask lacks a passing independent assertion' }
                            $masks += ,(Read-Rect $mask.rect)
                        }
                        $reference = [SmokePixels]::Read($baseline)
                        try {
                            $diff = Join-Path $OutputDirectory "$scenario-$($image.Replace('.png',''))-$index.diff.png"
                            $changed = [SmokePixels]::Compare($bitmap,$reference,(Read-Rect $region.rect),[int[][]]$masks,$diff)
                            $regions += [ordered]@{index=$index;changedPixels=$changed;baseline=$region.baseline}
                            if ($changed -gt 0) { throw "Visual mismatch in region $index ($changed pixels)" }
                        } finally { $reference.Dispose() }
                    }
                    $verdict = 'PASS'; $reason = ''
                }
            }
        } catch { $verdict = 'FAIL'; $reason = $_.Exception.Message }
        finally { if ($bitmap) { $bitmap.Dispose() } }
        [ordered]@{scenario=$scenario;image=$image;result=$verdict;reason=$reason;environmentId=$environmentId;regions=$regions}
    }
}
[ordered]@{schema=1;checkpoints=@($outcomes)} | ConvertTo-Json -Depth 20 |
    Set-Content -LiteralPath (Join-Path $OutputDirectory 'visual-results.json') -Encoding UTF8
$outcomes
