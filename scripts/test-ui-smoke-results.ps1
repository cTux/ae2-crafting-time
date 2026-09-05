$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-results-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$catalogue = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json
$cases = @($catalogue.groups.'standard-ae2')
function Read-Results { @(& "$PSScriptRoot/get-ui-smoke-results.ps1" -Target 1.20.1-forge -Profile compatible -Scenarios $cases -Evidence $temp) }
function Assert([bool]$condition, [string]$message) { if (!$condition) { throw $message } }
try {
    Assert (@(Read-Results | Where-Object result -eq 'NOT_RUN').Count -eq 6) 'Missing results must remain unrun'
    foreach ($case in $cases) {
        $directory = Join-Path $temp $case
        New-Item -ItemType Directory -Path $directory | Out-Null
        $contract = $catalogue.cases.$case
        $checks = [ordered]@{}
        foreach ($check in $contract.checks) { $checks[$check] = $true }
        foreach ($image in $contract.screenshots) {
            Set-Content -LiteralPath (Join-Path $directory $image) -Value 'fixture-image'
            @{screen='fixture';gui=@{x=0;y=0;width=100;height=100}} | ConvertTo-Json |
                Set-Content -LiteralPath (Join-Path $directory $image.Replace('.png','.json'))
        }
        @{schema=1;complete=$true;target='1.20.1-forge';profile='compatible';scenario=$case;language='en_us';result='PASS';checks=$checks;screenshots=$contract.screenshots} |
            ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $directory 'result.json')
    }
    Assert (@(Read-Results | Where-Object result -eq 'PASS').Count -eq 6) 'Complete leaves must pass'
    foreach ($case in $cases) {
        $caseFile = Join-Path (Join-Path $temp $case) 'result.json'
        $valid = Get-Content -LiteralPath $caseFile -Raw
        $data = $valid | ConvertFrom-Json
        $reordered = [ordered]@{}
        foreach ($check in @($data.checks.psobject.Properties.Name | Sort-Object -Descending)) { $reordered[$check] = $true }
        $data.checks = $reordered
        $data | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $caseFile
        Assert ((Read-Results | Where-Object scenario -eq $case).result -eq 'PASS') "JSON check order must not affect $case"
        foreach ($invalid in @('true', 'false', 1, $null)) {
            $data = $valid | ConvertFrom-Json
            $data.complete = $invalid
            $data | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $caseFile
            Assert ((Read-Results | Where-Object scenario -eq $case).result -eq 'FAIL') "Non-boolean complete must fail $case"
            $data = $valid | ConvertFrom-Json
            $check = $catalogue.cases.$case.checks[0]
            $data.checks.$check = $invalid
            $data | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $caseFile
            Assert ((Read-Results | Where-Object scenario -eq $case).result -eq 'FAIL') "Non-boolean check must fail $case"
        }
        $data = $valid | ConvertFrom-Json
        $check = $catalogue.cases.$case.checks[0]
        $data.checks.psobject.Properties.Remove($check)
        $data.checks | Add-Member -NotePropertyName $check.ToUpperInvariant() -NotePropertyValue $true
        $data | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $caseFile
        Assert ((Read-Results | Where-Object scenario -eq $case).result -eq 'FAIL') "Wrong check casing must fail $case"
        Set-Content -LiteralPath $caseFile -Value $valid
    }
    $directory = Join-Path $temp 'delayed-status'
    $image = Join-Path $directory 'delayed-tooltip.png'
    Remove-Item -LiteralPath $image
    Assert ((Read-Results | Where-Object scenario -eq 'delayed-status').result -eq 'FAIL') 'Missing image must fail'
    Set-Content -LiteralPath $image -Value 'fixture-image'
    $snapshot = Join-Path $directory 'delayed-tooltip.json'
    $validSnapshot = Get-Content -LiteralPath $snapshot -Raw
    Remove-Item -LiteralPath $snapshot
    Assert ((Read-Results | Where-Object scenario -eq 'delayed-status').result -eq 'FAIL') 'Missing snapshot must fail'
    Set-Content -LiteralPath $snapshot -Value '{}'
    Assert ((Read-Results | Where-Object scenario -eq 'delayed-status').result -eq 'FAIL') 'Invalid snapshot must fail'
    Set-Content -LiteralPath $snapshot -Value $validSnapshot
    Assert ((Read-Results | Where-Object scenario -eq 'delayed-status').result -eq 'PASS') 'Restored evidence must pass before testing result fields'
    $file = Join-Path $directory 'result.json'
    $original = Get-Content -LiteralPath $file -Raw
    foreach ($field in @('schema','complete','target','profile','scenario','language','result')) {
        $data = $original | ConvertFrom-Json
        $data.$field = if ($field -eq 'schema') { 2 } elseif ($field -eq 'complete') { $false } else { 'invalid' }
        $data | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $file
        Assert ((Read-Results | Where-Object scenario -eq 'delayed-status').result -eq 'FAIL') "Invalid $field must fail"
    }
    foreach ($check in $catalogue.cases.'delayed-status'.checks) {
        foreach ($mutation in @('missing', 'false')) {
            $data = $original | ConvertFrom-Json
            if ($mutation -eq 'missing') { $data.checks.psobject.Properties.Remove($check) }
            else { $data.checks.$check = $false }
            $data | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $file
            $outcome = Read-Results | Where-Object scenario -eq 'delayed-status'
            Assert ($outcome.result -eq 'FAIL') "$mutation check must fail: $check"
            $expectedReason = if ($mutation -eq 'missing') { 'Incomplete check set' } else { "Failed check: $check" }
            Assert ($outcome.reason -eq $expectedReason) "Wrong rejection for $mutation check: $check"
        }
    }
    Set-Content -LiteralPath $file -Value $original
    Assert (@(Read-Results | Where-Object result -eq 'PASS').Count -eq 6) 'Restoring a failed leaf must restore the complete group'
    Set-Content -LiteralPath $file -Value '{'
    Assert ((Read-Results | Where-Object scenario -eq 'delayed-status').result -eq 'FAIL') 'Malformed result must fail'
    $expected = Join-Path $temp 'expected-adapters.json'
    @{neoecoae='batched-long'} | ConvertTo-Json | Set-Content -LiteralPath $expected
    $adapterResult = @{schema=1;complete=$true;target='1.20.1-forge';profile='latest';scenario='neoeco-cpu';language='en_us';result='PASS';adapters=@{neoecoae=@{variant='pending-accounting';reason='selected'}}}
    function Read-Adapter { & "$PSScriptRoot/get-ui-smoke-results.ps1" -Target 1.20.1-forge -Profile latest -Scenarios neoeco-cpu -Evidence $temp -ExpectedAdapters $expected }
    $adapterResult | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $temp 'result.json')
    Assert ((Read-Adapter).result -eq 'FAIL') 'Older adapter must fail leaf coverage'
    $adapterResult.adapters.neoecoae.variant = 'batched-long'
    $adapterResult | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $temp 'result.json')
    Assert ((Read-Adapter).result -eq 'PASS') 'Selected newest adapter must pass'
    $adapterResult.adapters.neoecoae.reason = 'missing'
    $adapterResult | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $temp 'result.json')
    Assert ((Read-Adapter).result -eq 'FAIL') 'Unselected adapter must fail'
    Write-Host 'PASS: independent evidence, missing/unrun leaves and stale identity rejection'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -notmatch '^ae2ct-results-[a-f0-9]{32}$') { throw 'Unsafe evidence test cleanup' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
