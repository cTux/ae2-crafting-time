$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-suite-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporary | Out-Null
$runtime = Join-Path $temporary 'runtime'
$output = Join-Path $temporary 'evidence'
try {
    $result = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -RuntimeDirectory $runtime -OutputDirectory $output -Scenarios @('craft-plan','merequester-screen')
    $plan = Get-Content "$output\suite-plan.json" -Raw | ConvertFrom-Json
    if ($result.caseCount -ne 2 -or $result.world -ne $plan.cases[0].world -or $result.scenario -ne 'suite') { throw 'Wrong suite launch identity' }
    foreach ($case in $plan.cases) {
        $marker = Get-Content "$runtime\saves\$($case.world)\.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
        if ($marker.disposableWorldId -ne $case.world -or $marker.sourceFixtureId -ne 'ae2-crafting-time') { throw 'Wrong case marker' }
    }
    if ($plan.cases[0].world -eq $plan.cases[1].world) { throw 'Worlds are not isolated' }
    $fabric = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 1.20.1-fabric -RuntimeDirectory $runtime -OutputDirectory "$temporary\fabric-evidence" -Scenarios @('standard-ae2')
    $fabricSource = Join-Path (Split-Path -Parent $PSScriptRoot) 'versions/1.20.1-fabric/run/saves/ae2-crafting-time/level.dat'
    if ((Get-FileHash -LiteralPath "$runtime/saves/$($fabric.world)/level.dat").Hash -ne (Get-FileHash -LiteralPath $fabricSource).Hash) {
        throw 'Fabric retained Forge-only world metadata'
    }
    $fabricMarker = Get-Content "$runtime/saves/$($fabric.world)/.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
    if ($fabricMarker.terminal.x -ne -13 -or $fabricMarker.terminal.y -ne -59) { throw 'Fabric lost the marked fixture layout' }
    $neo = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 1.21.1-neoforge -RuntimeDirectory $runtime -OutputDirectory "$temporary\neo-evidence" -Scenarios @('craft-plan')
    $neoMarker = Get-Content "$runtime\saves\$($neo.world)\.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
    if ($neoMarker.terminal.x -ne 2 -or $neoMarker.terminal.y -ne 205) { throw 'NeoForge did not copy its native fixture' }
    $neo = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 26.1.2-neoforge -RuntimeDirectory $runtime -OutputDirectory "$temporary\neo26-evidence" -Scenarios @('craft-plan')
    $neoMarker = Get-Content "$runtime\saves\$($neo.world)\.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
    if ($neoMarker.terminal.x -ne 2 -or $neoMarker.terminal.y -ne 205) { throw 'NeoForge did not copy its native fixture' }
    foreach ($invalid in @(@('craft-plan','craft-plan'), @('../escape'))) {
        $rejected = $false
        try { & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -RuntimeDirectory $runtime -OutputDirectory "$temporary\rejected-$([guid]::NewGuid())" -Scenarios $invalid | Out-Null }
        catch { $rejected = $true }
        if (!$rejected) { throw 'Expected duplicate or invalid name rejection' }
    }
    $rejected = $false
    try { & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -RuntimeDirectory $runtime -OutputDirectory $output -Scenarios @('craft-plan') | Out-Null }
    catch { $rejected = $true }
    if (!$rejected) { throw 'Expected existing output rejection' }
    $rejected = $false
    try { & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -RuntimeDirectory $runtime -OutputDirectory "$temporary\too-many" -Scenarios (1..33 | ForEach-Object {"case-$_"}) | Out-Null }
    catch { $rejected = $true }
    if (!$rejected) { throw 'Expected bounded suite rejection' }
    Write-Host 'PASS: suite copies, launch identity, markers, unique worlds, and invalid inputs'
} finally {
    $resolved = (Resolve-Path -LiteralPath $temporary).Path
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -notmatch '^ae2ct-suite-[a-f0-9]{32}$') { throw 'Unsafe test cleanup path' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
