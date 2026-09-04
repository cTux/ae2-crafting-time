$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-matrix-' + [guid]::NewGuid().ToString('N'))
$scripts = Join-Path $temp 'scripts'
New-Item -ItemType Directory -Path $scripts -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'run-ui-smoke-matrix.ps1'), (Join-Path $PSScriptRoot 'release-matrix.json') -Destination $scripts
foreach ($file in @('get-ui-smoke-plan.ps1','get-ui-smoke-results.ps1','expand-ui-smoke-groups.ps1','run-client-versions.json',
        'ui-smoke-impact.json','ui-smoke-groups.json','ui-smoke-coverage.json','ui-smoke-forge-suite.json',
        'ui-smoke-fabric-suite.json','ui-smoke-neoforge-suite.json','ui-smoke-neoforge-26.1.2-suite.json')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination $scripts
}
& git -C $temp init --quiet
& git -C $temp -c user.name=Test -c user.email=test@example.invalid -c core.hooksPath=disabled-hooks commit --allow-empty -qm fixture
if ($LASTEXITCODE -ne 0) { throw 'Could not initialize matrix fixture' }
@'
param([string]$Target,[string]$BundleDirectory)
'{}' | Set-Content (Join-Path $BundleDirectory 'expected-adapters.json')
'@ | Set-Content (Join-Path $scripts 'prepare-ui-smoke-adapters.ps1')
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
param([string]$Target,[switch]$Latest,[string]$Scenario,[string]$BundleDirectory,[string]$PreparedLaunchRoot,[string]$GuestSourceRoot,[string]$CasesBase64)
$profile=if($Latest){'latest'}else{'compatible'}
$live=Join-Path (Split-Path -Parent $PSScriptRoot) "build/ui-smoke/$Target/$profile/$Scenario"
$cases = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($CasesBase64)) | ConvertFrom-Json
$contracts = (Get-Content (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json).cases
foreach ($case in $cases) {
    $evidence = if ($cases.Count -eq 1) { "$live/evidence" } else { "$live/evidence/$case" }
    New-Item -ItemType Directory -Path $evidence -Force | Out-Null
    $checks = [ordered]@{}
    foreach ($check in $contracts.$case.checks) { $checks[$check] = $true }
    foreach ($image in $contracts.$case.screenshots) {
        Set-Content (Join-Path $evidence $image) 'fixture-image'
        @{screen='fixture-screen';gui=@{x=0;y=0;width=100;height=100}} | ConvertTo-Json | Set-Content (Join-Path $evidence $image.Replace('.png','.json'))
    }
    @{schema=1;complete=$true;target=$Target;profile=$profile;scenario=$case;language='en_us';result='PASS';checks=$checks;screenshots=@($contracts.$case.screenshots)} |
        ConvertTo-Json -Depth 6 | Set-Content "$evidence/result.json"
}
@{phase='passed';message='';pid=123;exitCode=0} | ConvertTo-Json | Set-Content "$live/status.json"
if ($env:AE2CT_UNCONFIRMED_EXIT) {
    @{phase='failed';message='termination failed';pid=123;exitCode=$null} | ConvertTo-Json | Set-Content "$live/status.json"
}
'@ | Set-Content (Join-Path $scripts 'invoke-ui-smoke-codexvm.ps1')
Set-Content -LiteralPath (Join-Path $temp '.gitignore') 'build/'
try {
    $preview = & powershell.exe -NoProfile -File (Join-Path $scripts 'run-ui-smoke-matrix.ps1') -PlanOnly
    if ($LASTEXITCODE -ne 0 -or (Test-Path -LiteralPath (Join-Path $temp 'build'))) { throw 'Plan-only built or dispatched a client' }
    $previewPlan = ($preview -join "`n") | ConvertFrom-Json
    if ($previewPlan.targets.Count -ne 4 -or $previewPlan.mode -ne 'manual') { throw 'Plan-only lost explicit full scope' }
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
        $fabricIndex = if ($latest) { 1 } else { 2 }
        $lastIndex = if ($latest) { 3 } else { 4 }
        if ($results.Count -ne ($lastIndex + 1) -or $results[$lastIndex].target -ne '26.1.2-neoforge' -or $results[$lastIndex].result -ne 'PASS') {
            throw 'An earlier failure skipped a later required target'
        }
        $expected = if ($latest) { 'DIAGNOSTIC_FAILURE' } else { 'FAIL_SETUP' }
        if ($results[$fabricIndex].result -ne $expected -or $results[$fabricIndex].message -notlike '*intentional resolution failure*') {
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
        $coverage = Get-Content (Join-Path $results[0].report 'coverage.json') -Raw | ConvertFrom-Json
        if ($coverage.result -ne 'PASS') { throw 'A failed run erased its completed scenario outcome' }
    } finally { Remove-Item Env:\AE2CT_UNCONFIRMED_EXIT -ErrorAction SilentlyContinue }
    Write-Host 'UI smoke matrix checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if ($resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
