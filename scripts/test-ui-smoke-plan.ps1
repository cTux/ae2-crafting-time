$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-plan-' + [guid]::NewGuid().ToString('N'))
$planner = Join-Path $PSScriptRoot 'get-ui-smoke-plan.ps1'
New-Item -ItemType Directory -Path $temp | Out-Null
function Git([string[]]$Arguments) {
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
    Git @('reset','--hard','baseline')
    # Git clean is confined to this newly created disposable repository.
    Git @('clean','-fd')
}
try {
    Git @('init','-b','main')
    Git @('config','user.name','Smoke plan test')
    Git @('config','user.email','smoke-test@example.invalid')
    Git @('config','core.hooksPath','disabled-hooks')
    Put 'README.md' 'baseline'
    $lang = 'shared/src/main/resources/assets/ae2craftingtime/lang/en_us.json'
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"DELAYED","other":"value"}'
    Put 'versions/1.20.1-forge/src/main/Old.java' 'old'
    Git @('add','.')
    Git @('commit','-m','fixture')
    Git @('tag','baseline')
    Assert ((Plan).result -eq 'NOT_REQUIRED') 'Empty diff must not launch'
    Put 'docs/new.md' 'docs'
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
        @('unknown space ü.txt',4))) {
        Put $pair[0] 'changed'
        $plan = Plan
        Assert ($plan.targets.Count -eq $pair[1]) "Wrong ownership: $($pair[0])"
        Assert (@($plan.targets | Where-Object mode -ne 'full').Count -eq 0) 'Unknown runtime must widen'
        Clean
    }
    Put 'shared/src/main/java/StallDiagnostic.java' 'delayed'
    $plan = Plan
    Assert ($plan.targets.Count -eq 4) 'Dedicated delayed file must reach all targets'
    Assert (@($plan.targets | Where-Object { $_.cases.Count -ne 1 -or $_.cases[0] -ne 'delayed-status' }).Count -eq 0) 'Dedicated file must narrow'
    Put 'versions/1.20.1-forge/src/main/Packet.java' 'packet'
    $plan = Plan
    Assert (($plan.targets | Where-Object target -eq '1.20.1-forge').mode -eq 'full') 'Broad rule must dominate only its target'
    Assert (@($plan.targets | Where-Object mode -eq 'focused').Count -eq 3) 'Mixed changes must union'
    Clean
    Put 'shared/src/mcCommon/java/CraftingStatusTableRendererMixin.java' 'only delayed method changed'
    Assert ((Plan).targets[0].cases.Count -eq 8) 'Never narrow mixed renderers by keywords'
    Clean
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"late","other":"value"}'
    Assert ((Plan).targets[0].cases[0] -eq 'delayed-status') 'English delayed value must narrow'
    $saved = Plan
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"later","other":"value"}'
    Reject { & $planner -Changed -BaseRef baseline -Repository $temp -ExpectedFingerprint $saved.fingerprint } 'Changed local content must invalidate plan'
    Put $lang '{"text.ae2craftingtime.ttc_delayed":"late","other":"staged"}'
    Git @('add','--',$lang)
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
    Git @('mv','versions/1.20.1-forge/src/main/Old.java','versions/1.20.1-fabric/src/main/Renamed.java')
    Assert ((Plan).targets.Count -eq 2) 'Cross-target rename must retain both owners'
    Clean
    Git @('rm','versions/1.20.1-forge/src/main/Old.java')
    Assert ((Plan).targets[0].target -eq '1.20.1-forge') 'Deleted path must retain ownership'
    Clean
    Put 'shared/src/main/java/StallDiagnostic.java' 'committed'
    Git @('add','.')
    Git @('commit','-m','changed')
    Assert ((Plan).targets[0].cases[0] -eq 'delayed-status') 'Committed change missing'
    $saved = Plan
    Git @('commit','--allow-empty','-m','head change')
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
    Assert ($full.targets[0].cases.Count -eq 34) 'Expanded Forge suite must contain 34 leaves'
    Assert ($full.targets[1].cases.Count -eq 16) 'Expanded Fabric suite must contain 16 leaves'
    Assert ($full.targets[2].cases.Count -eq 30) 'Expanded NeoForge suite must contain 30 leaves'
    Assert ($full.targets[3].cases.Count -eq 19) 'Expanded 26.1.2 suite must contain 19 leaves'
    Write-Host 'PASS: diff ownership, union, language keys, aliases, manual modes and freshness'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -notmatch '^ae2ct-plan-[a-f0-9]{32}$') { throw 'Unsafe test cleanup' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
