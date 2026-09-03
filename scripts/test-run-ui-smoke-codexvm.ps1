$ErrorActionPreference = "Stop"
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-codexvm-smoke-" + [guid]::NewGuid().ToString("N"))
$source = Join-Path $temp "source"
$stage = Join-Path $temp "stage"
$scripts = Join-Path $source "scripts"
New-Item -ItemType Directory -Path $scripts -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "run-ui-smoke-codexvm.ps1") -Destination $scripts
[IO.File]::WriteAllText((Join-Path $scripts "run-ui-smoke.ps1"), @'
param([string]$Target, [string]$ReportDirectory, [string]$Scenario, [string[]]$ProjectId, [switch]$Latest, [switch]$Interactive)
New-Item -ItemType Directory -Path $ReportDirectory -Force | Out-Null
[ordered]@{ target=$Target; scenario=$Scenario; projectId=@($ProjectId); latest=$Latest.IsPresent; interactive=$Interactive.IsPresent; javaHome=$env:JAVA_HOME } |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $ReportDirectory "wrapper-result.json") -Encoding UTF8
'@, [Text.UTF8Encoding]::new($false))

try {
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Latest -Interactive `
        -Scenario aeinfinitybooster-terminal -ProjectId VQhDBNs8
    $resultPath = Join-Path $source "build\ui-smoke\1.20.1-forge\latest\aeinfinitybooster-terminal\wrapper-result.json"
    $result = Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
    if (-not $result.latest -or -not $result.interactive -or $result.scenario -ne "aeinfinitybooster-terminal" -or
            @($result.projectId).Count -ne 1 -or $result.projectId[0] -ne "VQhDBNs8") {
        throw "CodexVM wrapper dropped smoke arguments"
    }

    $cacheMarker = Join-Path $stage "build\cache-marker.txt"
    New-Item -ItemType Directory -Path (Split-Path -Parent $cacheMarker) -Force | Out-Null
    Set-Content -LiteralPath $cacheMarker -Value "keep"
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage
    if (-not (Test-Path -LiteralPath $cacheMarker)) { throw "Stable staging discarded the guest build cache" }
    if (-not (Test-Path -LiteralPath (Join-Path $source "build\ui-smoke\1.20.1-forge\compatible\craft-plan\wrapper-result.json"))) {
        throw "Default scenario report was not separated"
    }
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Scenario suite
    $suiteResult = Get-Content (Join-Path $source 'build\ui-smoke\1.20.1-forge\compatible\suite\wrapper-result.json') -Raw | ConvertFrom-Json
    if ($suiteResult.scenario -ne 'suite') { throw 'Wrapper dropped suite selection' }
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Target 1.20.1-fabric -Scenario suite
    $fabricResult = Get-Content (Join-Path $source 'build\ui-smoke\1.20.1-fabric\compatible\suite\wrapper-result.json') -Raw | ConvertFrom-Json
    if ($fabricResult.target -ne '1.20.1-fabric' -or $fabricResult.scenario -ne 'suite') { throw 'Wrapper dropped Fabric target' }
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Target 1.21.1-neoforge -Scenario suite
    $neoResult = Get-Content (Join-Path $source 'build\ui-smoke\1.21.1-neoforge\compatible\suite\wrapper-result.json') -Raw | ConvertFrom-Json
    if ($neoResult.target -ne '1.21.1-neoforge' -or $neoResult.scenario -ne 'suite' -or $neoResult.javaHome -notmatch '21') {
        throw 'Wrapper dropped NeoForge target or JDK 21'
    }
    try {
        & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Target 1.21.1-neoforge -JavaHome $fabricResult.javaHome
        throw 'Accepted Java 17 for NeoForge'
    } catch {
        if ($_.Exception.Message -notlike '*requires JDK 21*') { throw }
    }
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Target 26.1.2-neoforge -Scenario suite
    $neoResult = Get-Content (Join-Path $source 'build\ui-smoke\26.1.2-neoforge\compatible\suite\wrapper-result.json') -Raw | ConvertFrom-Json
    if ($neoResult.target -ne '26.1.2-neoforge' -or $neoResult.scenario -ne 'suite' -or $neoResult.javaHome -notmatch '21') {
        throw 'Wrapper dropped NeoForge target or JDK 21'
    }
    try {
        & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Target 26.1.2-neoforge -JavaHome $fabricResult.javaHome
        throw 'Accepted Java 17 for NeoForge'
    } catch {
        if ($_.Exception.Message -notlike '*requires JDK 21*') { throw }
    }
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Scenario no-space-status
    $focused = Get-Content (Join-Path $source 'build/ui-smoke/1.20.1-forge/compatible/no-space-status/wrapper-result.json') -Raw | ConvertFrom-Json
    if ($focused.scenario -ne 'no-space-status') { throw 'Wrapper dropped no-space scenario' }
    Write-Host "run-ui-smoke-codexvm checks passed"
} finally {
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
