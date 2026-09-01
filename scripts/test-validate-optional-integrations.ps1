$ErrorActionPreference = "Stop"
$validator = Join-Path $PSScriptRoot "validate-optional-integrations.ps1"
& $validator

$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-consistency-" + [guid]::NewGuid().ToString("N"))
try {
    foreach ($path in @(
        "scripts\release-matrix.json", "scripts\run-client-versions.json", "DEPENDENCIES.md",
        "docs\mod-automation-coverage.md", "versions\1.20.1-forge\src\main\resources\META-INF\mods.toml",
        "versions\1.20.1-fabric\src\main\resources\fabric.mod.json",
        "versions\1.21.1-neoforge\src\main\resources\META-INF\neoforge.mods.toml",
        "versions\26.1.2-neoforge\src\main\resources\META-INF\neoforge.mods.toml"
    )) {
        $destination = Join-Path $temp $path
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $root $path) -Destination $destination
    }
    $coveragePath = Join-Path $temp "docs\mod-automation-coverage.md"
    (Get-Content -LiteralPath $coveragePath -Raw).Replace('`15.3.3-forge`', '`wrong`') |
        Set-Content -LiteralPath $coveragePath -Encoding UTF8
    try {
        & $validator -Root $temp
        throw "Expected a coverage/version mismatch"
    } catch {
        if ($_.Exception.Message -ne "1.20.1-forge coverage/version pin mismatch for ae2wtlib") { throw }
    }
    Write-Host "optional-integration consistency validator checks passed"
} finally {
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
