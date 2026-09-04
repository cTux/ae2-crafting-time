$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-plan-' + [guid]::NewGuid().ToString('N'))
$planner = Join-Path $PSScriptRoot 'get-ui-smoke-plan.ps1'
New-Item -ItemType Directory -Path $temp | Out-Null
function Invoke-FixtureGit([string[]]$Arguments) {
    & git -C $temp @Arguments | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Fixture Git failed: $Arguments" }
}
function Put([string]$Path, [string]$Text) {
    $file = Join-Path $temp $Path
    New-Item -ItemType Directory -Path (Split-Path $file) -Force | Out-Null
    [IO.File]::WriteAllText($file, $Text, [Text.UTF8Encoding]::new($false))
}
function Plan { & $planner -Changed -BaseRef baseline -Repository $temp }
function Assert([bool]$Condition, [string]$Message) { if (!$Condition) { throw $Message } }
function Reject([scriptblock]$Action, [string]$Message) {
    $failed = $false
    try { & $Action | Out-Null } catch { $failed = $true }
    Assert $failed $Message
}
function Clean {
    Invoke-FixtureGit @('reset','--hard','baseline')
    # Git clean is confined to this newly created disposable repository.
    Invoke-FixtureGit @('clean','-fd')
}
try {
    Invoke-FixtureGit @('init','-b','main')
    Invoke-FixtureGit @('config','user.name','Smoke plan test')
    Invoke-FixtureGit @('config','user.email','smoke-test@example.invalid')
    Invoke-FixtureGit @('config','core.hooksPath','disabled-hooks')
    Invoke-FixtureGit @('config','core.protectNTFS','false')
    Put 'README.md' 'baseline'
    $lang = 'shared/src/main/resources/assets/ae2craftingtime/lang/en_us.json'
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"DELAYED","other":"value"}'
    Put 'versions/1.20.1-forge/src/main/Old.java' 'old'
    Invoke-FixtureGit @('add','.')
    Invoke-FixtureGit @('commit','-m','fixture')
    Invoke-FixtureGit @('tag','baseline')
    Assert ((Plan).result -eq 'NOT_REQUIRED') 'Empty diff must not launch'
    # Git can contain names that Windows cannot materialize. Use the index so
    # this exercises actual NUL-delimited Git output on Windows as well.
    $blob = & git -C $temp rev-parse baseline:README.md
    $newlinePath = "unknown`nline.java"
    Invoke-FixtureGit @('-c','core.protectNTFS=false','update-index','--add','--cacheinfo',"100644,$blob,$newlinePath")
    $newlinePlan = Plan
    Assert ($newlinePlan.targets.Count -eq 4) 'Newline filename must select all targets'
    Assert ($newlinePath -cin $newlinePlan.changes.path) 'Newline filename must remain one exact path'
    Invoke-FixtureGit @('update-index','--force-remove','--',$newlinePath)
    Clean
    Put 'docs/new.md' 'docs'
    Put 'docs/StallDiagnostic.java' 'documented example'
    Put 'shared/src/test/java/Test.java' 'tests'
    Assert ((Plan).result -eq 'NOT_REQUIRED') 'Docs and tests require no runtime'
    Clean
    foreach ($pair in @(
        @('versions/1.20.1-forge/src/main/New.java',1),
        @('shared/src/mc1201/java/Packet.java',3),
        @('shared/src/mc2612/java/Packet.java',1),
        @('shared/src/neoforge/java/Packet.java',2),
        @('shared/src/testDriver1201/java/StandardAe2Scenario.java',4),
        @('shared/src/testDriverAddons/java/Fixture.java',2),
        @('other/StallDiagnostic.java',4),
        @('unknown space ü.txt',4))) {
        Put $pair[0] 'changed'
        $plan = Plan
        Assert ($plan.targets.Count -eq $pair[1]) "Wrong ownership: $($pair[0])"
        Assert (@($plan.targets | Where-Object mode -ne 'full').Count -eq 0) 'Unknown runtime must widen'
        Clean
    }
    Put 'shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/NoSpaceScenario.java' 'shared fixture'
    $sharedFixture = Plan
    Assert ($sharedFixture.targets.Count -eq 3 -and '26.1.2-neoforge' -notin $sharedFixture.targets.target) 'Replaced fixture must exclude 26.1.2'
    Clean
    Put 'versions/26.1.2-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/NoSpaceScenario.java' 'native fixture'
    $nativeFixture = Plan
    Assert ($nativeFixture.targets.Count -eq 1 -and $nativeFixture.targets[0].target -eq '26.1.2-neoforge') 'Native replacement must select only 26.1.2'
    Clean
    Put 'shared/src/main/java/com/ctux/ae2craftingtime/core/StallDiagnostic.java' 'delayed'
    $plan = Plan
    Assert ($plan.targets.Count -eq 4) 'Dedicated delayed file must reach all targets'
    Assert (@($plan.targets | Where-Object { $_.graphs.Count -ne 1 }).Count -eq 0) 'Focused core cases must keep one compatible graph'
    Assert (@($plan.targets | Where-Object { $_.cases.Count -ne 1 -or $_.cases[0] -ne 'delayed-status' }).Count -eq 0) 'Dedicated file must narrow'
    Put 'versions/1.20.1-forge/src/main/Packet.java' 'packet'
    $plan = Plan
    Assert (($plan.targets | Where-Object target -eq '1.20.1-forge').mode -eq 'full') 'Broad rule must dominate only its target'
    Assert (@($plan.targets | Where-Object mode -eq 'focused').Count -eq 3) 'Mixed changes must union'
    Clean
    Put 'shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java' 'only delayed method changed'
    Assert ((Plan).targets[0].cases.Count -eq 8) 'Never narrow mixed renderers by keywords'
    Clean
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"late","other":"value"}'
    Assert ((Plan).targets[0].cases[0] -eq 'delayed-status') 'English delayed value must narrow'
    $saved = Plan
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"later","other":"value"}'
    Reject { & $planner -Changed -BaseRef baseline -Repository $temp -ExpectedFingerprint $saved.fingerprint } 'Changed local content must invalidate plan'
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"late","other":"staged"}'
    Invoke-FixtureGit @('add','--',$lang)
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"late","other":"value"}'
    Assert ((Plan).targets[0].mode -eq 'full') 'Index and worktree key changes must union'
    Clean
    foreach ($invalid in @('{', '{"key":"one","key":"two"}', '{"key":2}')) {
        Put $lang $invalid
        Reject { Plan } 'Malformed or duplicate language JSON must fail'
    }
    Clean
    Put $lang "{`n  `"other`": `"value`", `"text.ae2craftingtime.ttc_delayed`": `"DELAYED`"`n}"
    Assert ((Plan).result -eq 'NOT_REQUIRED') 'Language formatting must not select UI'
    Clean
    New-Item -ItemType Directory -Path (Join-Path $temp 'versions/1.20.1-fabric/src/main') -Force | Out-Null
    Invoke-FixtureGit @('mv','versions/1.20.1-forge/src/main/Old.java','versions/1.20.1-fabric/src/main/Renamed.java')
    Assert ((Plan).targets.Count -eq 2) 'Cross-target rename must retain both owners'
    Clean
    Invoke-FixtureGit @('rm','versions/1.20.1-forge/src/main/Old.java')
    Assert ((Plan).targets[0].target -eq '1.20.1-forge') 'Deleted path must retain ownership'
    Clean
    Put 'shared/src/main/java/com/ctux/ae2craftingtime/core/StallDiagnostic.java' 'committed'
    Invoke-FixtureGit @('add','.')
    Invoke-FixtureGit @('commit','-m','changed')
    Assert ((Plan).targets[0].cases[0] -eq 'delayed-status') 'Committed change missing'
    $saved = Plan
    Invoke-FixtureGit @('commit','--allow-empty','-m','head change')
    Reject { & $planner -Changed -BaseRef baseline -Repository $temp -ExpectedFingerprint $saved.fingerprint } 'Changed HEAD must invalidate plan'
    Clean
    Reject { & $planner -Changed -BaseRef missing-ref -Repository $temp } 'Missing base must fail'
    Reject { & $planner -Changed -Repository $temp -Target '1.20.1-forge' } 'Changed target override must fail'
    Reject { & $planner -Changed -Repository $temp -Scenario delayed-status } 'Changed scenario override must fail'
    Reject { & $planner -Changed -Repository $temp -Latest } 'Changed latest override must fail'
    Reject { & $planner -Interactive -Repository $temp -Target '1.20.1-forge' -Scenario standard-ae2 } 'Interactive group must fail'
    $manual = & $planner -Repository $temp -Target '1.20.1-forge' -Scenario delayed-status
    Assert ($manual.mode -eq 'manual' -and $manual.targets.Count -eq 1) 'Manual scope must remain labelled manual'
    $full = & $planner -Repository $temp
    Assert ($full.targets[0].graphs.Count -eq 2 -and $full.targets[0].graphs[1].cases.Count -eq 2) 'Full Forge must schedule its separate newest-adapter graph'
    Assert ($full.targets[0].cases.Count -eq 34) 'Expanded Forge suite must contain 34 leaves'
    Assert ($full.targets[1].cases.Count -eq 16) 'Expanded Fabric suite must contain 16 leaves'
    Assert ($full.targets[2].cases.Count -eq 30) 'Expanded NeoForge suite must contain 30 leaves'
    Assert ($full.targets[3].cases.Count -eq 19) 'Expanded 26.1.2 suite must contain 19 leaves'
    Invoke-FixtureGit @('checkout','-b','conflict-side')
    Put 'README.md' 'theirs'
    Invoke-FixtureGit @('add','.')
    Invoke-FixtureGit @('commit','-m','theirs')
    Invoke-FixtureGit @('checkout','main')
    Put 'README.md' 'ours'
    Invoke-FixtureGit @('add','.')
    Invoke-FixtureGit @('commit','-m','ours')
    & git -C $temp merge conflict-side 2>&1 | Out-Null
    Assert ($LASTEXITCODE -ne 0) 'Fixture must contain a real conflict'
    Reject { Plan } 'Unmerged files must fail preflight'
    Invoke-FixtureGit @('merge','--abort')
    Clean
    Reject { & $planner -Repository (Join-Path $temp 'missing') } 'Native command failure cannot mean NOT_REQUIRED'
    $maps = Join-Path $temp 'maps'
    New-Item -ItemType Directory -Path $maps | Out-Null
    Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.json' | Copy-Item -Destination $maps
    $saved = & $planner -Repository $temp -MatrixDirectory $maps
    $rulePath = Join-Path $maps 'ui-smoke-impact.json'
    $original = Get-Content -LiteralPath $rulePath -Raw
    $rules = $original | ConvertFrom-Json
    $rules.behavior[0].reason = 'updated rule'
    $rules | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $rulePath
    Reject { & $planner -Repository $temp -MatrixDirectory $maps -ExpectedFingerprint $saved.fingerprint } 'Rule changes must invalidate plans'
    foreach ($mutation in @('id','pattern','target','case')) {
        $rules = $original | ConvertFrom-Json
        switch ($mutation) {
            id { $rules.behavior[0].id = $rules.ownership[0].id }
            pattern { $rules.behavior[0].pattern = '[' }
            target { $rules.ownership[0].targets = @('unknown-target') }
            case { $rules.behavior[0].cases = @('unknown-case') }
        }
        $rules | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $rulePath
        Reject { & $planner -Repository $temp -MatrixDirectory $maps } "Invalid rule $mutation must fail"
    }
    Set-Content -LiteralPath $rulePath -Value $original
    $groupPath = Join-Path $maps 'ui-smoke-groups.json'
    $original = Get-Content -LiteralPath $groupPath -Raw
    foreach ($member in @('standard-ae2','unknown-case','delayed-status')) {
        $groups = $original | ConvertFrom-Json
        $groups.groups.'standard-ae2' += $member
        $groups | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $groupPath
        Reject { & $planner -Repository $temp -MatrixDirectory $maps } 'Nested, missing and duplicate leaves must fail'
    }
    Write-Host 'PASS: diff ownership, union, language keys, aliases, manual modes and freshness'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -notmatch '^ae2ct-plan-[a-f0-9]{32}$') { throw 'Unsafe test cleanup' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
