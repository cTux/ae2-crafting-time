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
    foreach ($case in $plan.cases) {
        if (!(Get-ChildItem -LiteralPath "$runtime/saves/$($case.world)/region" -Filter '*.mca')) {
            throw 'Legacy scenarios lost their fixture chunks'
        }
    }
    if ($plan.cases[0].world -eq $plan.cases[1].world) { throw 'Worlds are not isolated' }
    $fabric = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 1.20.1-fabric -RuntimeDirectory $runtime -OutputDirectory "$temporary\fabric-evidence" -Scenarios @('standard-ae2')
    if ($fabric.caseCount -ne 6) { throw 'Standard alias must expand to six worlds' }
    $fabricSource = Join-Path (Split-Path -Parent $PSScriptRoot) 'versions/1.20.1-fabric/run/saves/ae2-crafting-time/level.dat'
    if ((Get-FileHash -LiteralPath "$runtime/saves/$($fabric.world)/level.dat").Hash -ne (Get-FileHash -LiteralPath $fabricSource).Hash) {
        throw 'Fabric retained Forge-only world metadata'
    }
    $fabricPlan = Get-Content "$temporary/fabric-evidence/suite-plan.json" -Raw | ConvertFrom-Json
    foreach ($case in $fabricPlan.cases) {
        $worldPath = "$runtime/saves/$($case.world)"
        $names = @(Get-ChildItem -LiteralPath $worldPath -Force | ForEach-Object Name)
        if ($names.Count -ne 3 -or @($names | Where-Object { $_ -notin @('level.dat', 'level.dat_old', '.ae2-crafting-time-test-fixture.json') }).Count) {
            throw "Standard scenario copied saved chunks or player data: $($case.scenario)"
        }
        foreach ($name in @('level.dat', 'level.dat_old')) {
            if ((Get-FileHash -LiteralPath "$worldPath/$name").Hash -ne (Get-FileHash -LiteralPath $fabricSource).Hash) {
                throw "Standard scenario retained Forge metadata: $($case.scenario)/$name"
            }
        }
    }
    $rejected = $false
    try {
        & "$PSScriptRoot/copy-ui-smoke-fixture.ps1" -Source "$runtime/saves/$($plan.cases[0].world)" `
            -Destination "$runtime/saves/$($fabric.world)" -Target 1.20.1-fabric -Scenario delayed-status
    } catch { $rejected = $true }
    if (!$rejected) { throw 'Expected existing fixture destination rejection' }
    $fabricMarker = Get-Content "$runtime/saves/$($fabric.world)/.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
    if ($fabricMarker.terminal.x -ne -13 -or $fabricMarker.terminal.y -ne -59) { throw 'Fabric lost the fixture coordinates' }
    $fabricCraftPlan = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 1.20.1-fabric -RuntimeDirectory $runtime `
        -OutputDirectory "$temporary\fabric-craft-plan-evidence" -Scenarios @('craft-plan')
    $fabricRegion = "$runtime/saves/$($fabricCraftPlan.world)/region/r.0.0.mca"
    $fabricRegionSource = Join-Path (Split-Path -Parent $PSScriptRoot) 'versions/1.20.1-fabric/run/saves/ae2-crafting-time/region/r.0.0.mca'
    if ((Get-FileHash -LiteralPath $fabricRegion).Hash -ne (Get-FileHash -LiteralPath $fabricRegionSource).Hash) {
        throw 'Fabric craft-plan retained Forge chunks'
    }
    $neo = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 1.21.1-neoforge -RuntimeDirectory $runtime -OutputDirectory "$temporary\neo-evidence" -Scenarios @('craft-plan')
    $neoMarker = Get-Content "$runtime\saves\$($neo.world)\.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
    if ($neoMarker.terminal.x -ne 2 -or $neoMarker.terminal.y -ne 205) { throw 'NeoForge did not copy its native fixture' }
    $neo = & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -Target 26.1.2-neoforge -RuntimeDirectory $runtime -OutputDirectory "$temporary\neo26-evidence" -Scenarios @('craft-plan')
    $neoMarker = Get-Content "$runtime\saves\$($neo.world)\.ae2-crafting-time-test-fixture.json" -Raw | ConvertFrom-Json
    if ($neoMarker.terminal.x -ne 2 -or $neoMarker.terminal.y -ne 205) { throw 'NeoForge did not copy its native fixture' }
    $neoStandard = & "$PSScriptRoot/prepare-ui-smoke-suite.ps1" -Target 26.1.2-neoforge -RuntimeDirectory $runtime `
        -OutputDirectory "$temporary/neo26-standard-evidence" -Scenarios @('delayed-status')
    $neoWorld = "$runtime/saves/$($neoStandard.world)"
    $generationSource = Join-Path (Split-Path -Parent $PSScriptRoot) 'versions/26.1.2-neoforge/run/saves/ae2-crafting-time/data/minecraft/world_gen_settings.dat'
    if ((Get-FileHash -LiteralPath "$neoWorld/data/minecraft/world_gen_settings.dat").Hash -ne (Get-FileHash -LiteralPath $generationSource).Hash) {
        throw 'Standard NeoForge lost native world-generation settings'
    }
    $allowedFiles = @('level.dat', 'level.dat_old', '.ae2-crafting-time-test-fixture.json', 'world_gen_settings.dat')
    if (@(Get-ChildItem -LiteralPath $neoWorld -Recurse -Force -File | Where-Object Name -NotIn $allowedFiles).Count -or
            (Test-Path -LiteralPath "$neoWorld/region")) {
        throw 'Standard NeoForge copied saved chunks or jobs'
    }
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
    try { & "$PSScriptRoot\prepare-ui-smoke-suite.ps1" -RuntimeDirectory $runtime -OutputDirectory "$temporary\too-many" -Scenarios (1..65 | ForEach-Object {"case-$_"}) | Out-Null }
    catch { $rejected = $true }
    if (!$rejected) { throw 'Expected bounded suite rejection' }
    Write-Host 'PASS: suite copies, launch identity, markers, unique worlds, and invalid inputs'
} finally {
    $resolved = (Resolve-Path -LiteralPath $temporary).Path
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -notmatch '^ae2ct-suite-[a-f0-9]{32}$') { throw 'Unsafe test cleanup path' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
