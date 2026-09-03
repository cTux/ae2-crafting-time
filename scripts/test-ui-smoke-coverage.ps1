$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-coverage-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
try {
    Get-ChildItem -LiteralPath $PSScriptRoot -File -Filter '*.json' | Copy-Item -Destination $temp
    $targets = @(Get-Content (Join-Path $temp 'release-matrix.json') -Raw | ConvertFrom-Json)
    foreach ($target in $targets.id) {
        foreach ($latest in @($false, $true)) {
            $rows = @(& (Join-Path $PSScriptRoot 'get-ui-smoke-coverage.ps1') -Target $target -Latest:$latest -MatrixDirectory $temp)
            if (-not $rows.Count -or @($rows | Where-Object { -not $_.disposition -or -not $_.scenario }).Count) {
                throw 'Every selected project must have explicit coverage'
            }
            $expanded = $rows | Where-Object name -eq 'Expanded AE'
            if ($target -eq '1.20.1-forge') {
                $neoeco = $rows | Where-Object projectId -eq 'udZtKfzP'
                if ($neoeco.disposition -ne 'FOCUSED_BEHAVIOR' -or $neoeco.result -ne 'NOT_RUN' -or -not $neoeco.reason) {
                    throw 'Newest NeoEco requires separate runtime proof, never a passed or excluded legacy campaign'
                }
            }
            if ($expanded -and (($expanded.disposition -eq 'EXCLUDED') -eq $latest)) { throw 'Latest exclusion leaked from compatible' }
        }
    }
    function Assert-Rejected([string]$expected) {
        try {
            & (Join-Path $PSScriptRoot 'get-ui-smoke-coverage.ps1') -Target '1.20.1-forge' -MatrixDirectory $temp | Out-Null
        } catch {
            if ($_.Exception.Message -like "*$expected*") { return }
            throw
        }
        throw "Accepted invalid coverage: $expected"
    }
    $coveragePath = Join-Path $temp 'ui-smoke-coverage.json'
    $original = Get-Content -LiteralPath $coveragePath -Raw
    $coverage = $original | ConvertFrom-Json
    $coverage.'1.20.1-forge'.udZtKfzP.reason = ''
    $coverage | ConvertTo-Json -Depth 10 | Set-Content $coveragePath
    Assert-Rejected 'Focused project lacks a reason'
    $coverage = $original | ConvertFrom-Json
    $coverage.'1.20.1-forge'.psobject.Properties.Remove('a1RwDz90')
    $coverage | ConvertTo-Json -Depth 10 | Set-Content $coveragePath
    Assert-Rejected 'coverage declarations'
    $coverage = $original | ConvertFrom-Json
    $coverage.'1.20.1-forge'.a1RwDz90.disposition = 'PASS'
    $coverage | ConvertTo-Json -Depth 10 | Set-Content $coveragePath
    Assert-Rejected 'Invalid coverage disposition'
    $coverage = $original | ConvertFrom-Json
    $coverage.'1.20.1-forge'.a1RwDz90.scenario = 'missing-screen'
    $coverage | ConvertTo-Json -Depth 10 | Set-Content $coveragePath
    Assert-Rejected 'MISSING_FIXTURE'
    Set-Content $coveragePath $original
    @($targets | Select-Object -Skip 1) | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $temp 'release-matrix.json')
    Assert-Rejected 'target matrices differ'
    Write-Host 'UI smoke coverage checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if ($resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
