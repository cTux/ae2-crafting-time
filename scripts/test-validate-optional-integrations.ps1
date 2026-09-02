$ErrorActionPreference = "Stop"
$validator = Join-Path $PSScriptRoot "validate-optional-integrations.ps1"
& $validator

$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([IO.Path]::GetTempPath()) ("ae2ct-consistency-" + [guid]::NewGuid().ToString("N"))
try {
    foreach ($path in @(
        "scripts\release-matrix.json", "scripts\run-client-versions.json", "docs/dependencies.md",
        "versions\1.20.1-forge\src\main\resources\META-INF\mods.toml",
        "versions\1.20.1-fabric\src\main\resources\fabric.mod.json",
        "versions\1.21.1-neoforge\src\main\resources\META-INF\neoforge.mods.toml",
        "versions\26.1.2-neoforge\src\main\resources\META-INF\neoforge.mods.toml"
    )) {
        $destination = Join-Path $temp $path
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $root $path) -Destination $destination
    }
    $coveragePath = Join-Path $temp "docs\dependencies.md"
    foreach ($case in @(
        @{ Path = 'versions\1.20.1-forge\src\main\resources\META-INF\mods.toml'; Before = '[20.3.0,)'; After = '[20.3.0,20.4.0]'; Target = '1.20.1-forge'; Mod = 'neoecoae' },
        @{ Path = 'versions\1.20.1-forge\src\main\resources\META-INF\mods.toml'; Before = '[20.3.0,)'; After = '[20.3.0]'; Target = '1.20.1-forge'; Mod = 'neoecoae' },
        @{ Path = 'versions\1.20.1-fabric\src\main\resources\fabric.mod.json'; Before = '>=1.5.0'; After = '>=1.5.0 <1.6.0'; Target = '1.20.1-fabric'; Mod = 'appbot' }
    )) {
        $path = Join-Path $temp $case.Path
        $original = Get-Content -LiteralPath $path -Raw
        if (-not $original.Contains($case.Before)) { throw 'Policy test did not match its input' }
        $original.Replace($case.Before, $case.After) | Set-Content -LiteralPath $path -Encoding UTF8
        try {
            & $validator -Root $temp
            throw 'Expected a rejected optional version cap'
        } catch {
            if ($_.Exception.Message -ne "$($case.Target) optional dependency $($case.Mod) must use an open-ended minimum version") { throw }
        } finally {
            $original | Set-Content -LiteralPath $path -Encoding UTF8
        }
    }
    $original = Get-Content -LiteralPath $coveragePath -Raw
    $addonRow = (Get-Content -LiteralPath $coveragePath | Where-Object { $_ -like '| Applied Mekanistics*' }) -join ""
    foreach ($case in @(
        @{ Before = '`15.3.3-forge`'; After = '`wrong`'; Expected = '1.20.1-forge coverage/version pin mismatch for ae2wtlib' },
        @{ Before = '`>=1.4.3`'; After = '`>=0.0.0`'; Expected = 'docs/dependencies.md omits 1.20.1-forge minimum 1.4.3 for appmek' },
        @{ Before = $addonRow; After = "$addonRow`n$addonRow"; Expected = 'Duplicate docs/dependencies.md row for appmek' }
    )) {
        if (-not $case.Before -or -not $original.Contains($case.Before)) { throw 'Matrix test did not match its input' }
        $original.Replace($case.Before, $case.After) | Set-Content -LiteralPath $coveragePath -Encoding UTF8
        try {
            & $validator -Root $temp
            throw 'Expected a rejected dependency matrix'
        } catch {
            if ($_.Exception.Message -ne $case.Expected) { throw }
        }
    }
    Write-Host "optional-integration consistency validator checks passed"
} finally {
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
