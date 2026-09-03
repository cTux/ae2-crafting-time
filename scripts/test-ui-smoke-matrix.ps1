$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-matrix-' + [guid]::NewGuid().ToString('N'))
$scripts = Join-Path $temp 'scripts'
New-Item -ItemType Directory -Path $scripts -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke-matrix.ps1'), (Join-Path $PSScriptRoot 'release-matrix.json') -Destination $scripts
$runner = Join-Path $scripts 'run-ui-smoke-matrix.ps1'
$runnerText = Get-Content -LiteralPath $runner -Raw
$runnerText.Replace('$commit = & git -C $root rev-parse HEAD', '$commit = ''fixture-commit''') | Set-Content -LiteralPath $runner
@'
param([string]$Target,[switch]$Latest)
[pscustomobject]@{projectId='ae2';name='AE2';disposition='DIRECT_UI';scenario='standard-ae2';reason='';result='NOT_RUN'}
'@ | Set-Content (Join-Path $scripts 'get-ui-smoke-coverage.ps1')
@'
param([string]$Target,[switch]$Latest,[switch]$ResolveOnly,[switch]$Packaged,[string]$RuntimeDirectory)
if (-not $ResolveOnly -or -not $Packaged) { throw 'Guest build path selected' }
if ($Target -eq '1.20.1-fabric') { throw 'intentional resolution failure' }
New-Item -ItemType Directory -Path (Join-Path $RuntimeDirectory 'mods') -Force | Out-Null
'@ | Set-Content (Join-Path $scripts 'run-client.ps1')
@'
param([string]$Target,[switch]$Latest,[string]$Scenario,[string]$BundleDirectory,[string]$PreparedLaunchRoot,[string]$GuestSourceRoot)
$profile=if($Latest){'latest'}else{'compatible'}
$live=Join-Path (Split-Path -Parent $PSScriptRoot) "build/ui-smoke/$Target/$profile/$Scenario"
New-Item -ItemType Directory -Path "$live/evidence/standard-ae2" -Force | Out-Null
@{phase='passed';message=''} | ConvertTo-Json | Set-Content "$live/status.json"
if ($env:AE2CT_UNCONFIRMED_EXIT) {
    @{phase='failed';message='termination failed';pid=123;exitCode=$null} | ConvertTo-Json | Set-Content "$live/status.json"
}
@{result='PASS'} | ConvertTo-Json | Set-Content "$live/evidence/standard-ae2/result.json"
'@ | Set-Content (Join-Path $scripts 'invoke-ui-smoke-codexvm.ps1')
try {
    foreach ($latest in @($false,$true)) {
        $arguments = @('-NoProfile','-File',(Join-Path $scripts 'run-ui-smoke-matrix.ps1'))
        if ($latest) { $arguments += '-Latest' }
        & powershell.exe @arguments
        $exitCode = $LASTEXITCODE
        if (($exitCode -eq 0) -ne $latest) { throw 'Compatible failures and latest diagnostics have the same exit behavior' }
        $profile = if ($latest) { 'latest' } else { 'compatible' }
        $report = Get-ChildItem (Join-Path $temp 'build/ui-smoke/campaigns') -File -Recurse -Filter result.json |
            Where-Object { $_.Directory.Name -eq $profile } | Select-Object -Last 1
        $results = (Get-Content $report.FullName -Raw | ConvertFrom-Json).results
        if ($results.Count -ne 4 -or $results[3].target -ne '26.1.2-neoforge' -or $results[3].result -ne 'PASS') {
            throw 'An earlier failure skipped a later required target'
        }
        $expected = if ($latest) { 'DIAGNOSTIC_FAILURE' } else { 'FAIL_SETUP' }
        if ($results[1].result -ne $expected -or $results[1].message -notlike '*intentional resolution failure*') {
            throw 'Failure classification or evidence was lost'
        }
    }
    $env:AE2CT_UNCONFIRMED_EXIT = '1'
    try {
        & powershell.exe -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1')
        if ($LASTEXITCODE -ne 1) { throw 'Unconfirmed client exit must fail the compatible campaign' }
        $report = Get-ChildItem (Join-Path $temp 'build/ui-smoke/campaigns') -File -Recurse -Filter result.json |
            Where-Object { $_.Directory.Name -eq 'compatible' } | Sort-Object FullName | Select-Object -Last 1
        $results = (Get-Content $report.FullName -Raw | ConvertFrom-Json).results
        if ($results.Count -ne 1 -or $results[0].message -notlike '*exit is unconfirmed*') {
            throw 'The matrix launched another client after unconfirmed termination'
        }
    } finally { Remove-Item Env:\AE2CT_UNCONFIRMED_EXIT -ErrorAction SilentlyContinue }
    Write-Host 'UI smoke matrix checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if ($resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
