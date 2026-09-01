$ErrorActionPreference = "Stop"
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-codexvm-smoke-" + [guid]::NewGuid().ToString("N"))
$source = Join-Path $temp "source"
$stage = Join-Path $temp "stage"
$scripts = Join-Path $source "scripts"
New-Item -ItemType Directory -Path $scripts -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "run-ui-smoke-codexvm.ps1") -Destination $scripts
[IO.File]::WriteAllText((Join-Path $scripts "run-ui-smoke.ps1"), @'
param([string]$ReportDirectory, [string]$Scenario, [switch]$Latest, [switch]$Interactive)
New-Item -ItemType Directory -Path $ReportDirectory -Force | Out-Null
[ordered]@{ scenario=$Scenario; latest=$Latest.IsPresent; interactive=$Interactive.IsPresent; javaHome=$env:JAVA_HOME } |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $ReportDirectory "wrapper-result.json") -Encoding UTF8
'@, [Text.UTF8Encoding]::new($false))

try {
    & (Join-Path $scripts "run-ui-smoke-codexvm.ps1") -LocalRoot $stage -Latest -Interactive -Scenario ae2wtlib-terminal
    $resultPath = Join-Path $source "build\ui-smoke\1.20.1-forge\latest\ae2wtlib-terminal\wrapper-result.json"
    $result = Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
    if (-not $result.latest -or -not $result.interactive -or $result.scenario -ne "ae2wtlib-terminal") {
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
    Write-Host "run-ui-smoke-codexvm checks passed"
} finally {
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
