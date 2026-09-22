$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-resource-wrapper-' + [guid]::NewGuid().ToString('N'))
try {
    $scripts = Join-Path $temporary 'scripts'
    New-Item -ItemType Directory -Path $scripts -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke.ps1'),
        (Join-Path $PSScriptRoot 'resource-fixture-contract.ps1') -Destination $scripts
    @'
param([switch]$Changed,[string]$BaseRef,[switch]$PlanOnly,[string]$Target,[switch]$Latest,
    [switch]$Interactive,[switch]$ResourceFixtureOnly,[string]$Scenario,[string[]]$ProjectId,[string]$ArchiveRoot)
if ($Scenario -cne 'delayed-resource-icons' -or !$PlanOnly) {
    throw 'Wrapper did not forward the resource campaign contract'
}
[pscustomobject]@{forwarded=$true;scenario=$Scenario;resourceFixtureOnly=[bool]$ResourceFixtureOnly} |
    ConvertTo-Json | Set-Content -LiteralPath $env:AE2CT_WRAPPER_CAPTURE
'@ | Set-Content -LiteralPath (Join-Path $scripts 'run-ui-smoke-matrix.ps1')
    $env:AE2CT_WRAPPER_CAPTURE = Join-Path $temporary 'capture.json'
    $shell = (Get-Process -Id $PID).Path
    & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke.ps1') -PlanOnly -Target 1.20.1-forge `
        -Scenario delayed-resource-icons -ResourceFixtureOnly | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Resource fixture wrapper process failed' }
    $result = Get-Content -LiteralPath $env:AE2CT_WRAPPER_CAPTURE -Raw | ConvertFrom-Json
    if (!$result.forwarded -or !$result.resourceFixtureOnly) { throw 'Resource fixture wrapper forwarding failed' }
    & $shell -NoProfile -File (Join-Path $scripts 'run-ui-smoke.ps1') -PlanOnly -Target 1.20.1-forge `
        -Scenario delayed-resource-icons | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Production resource wrapper process failed' }
    $result = Get-Content -LiteralPath $env:AE2CT_WRAPPER_CAPTURE -Raw | ConvertFrom-Json
    if (!$result.forwarded -or $result.resourceFixtureOnly) { throw 'Production resource wrapper forwarding failed' }
    Write-Host 'Resource fixture wrapper forwarding test passed'
} finally {
    Remove-Item Env:\AE2CT_WRAPPER_CAPTURE -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Recurse -Force }
}
