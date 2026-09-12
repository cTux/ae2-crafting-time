$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-native-' + [guid]::NewGuid().ToString('N'))
$scripts = Join-Path $temp 'scripts'
$bundle = Join-Path $temp 'bundle'
$runtime = Join-Path $temp 'build/ui-smoke/test/runtime'
$evidence = Join-Path $temp 'evidence'
New-Item -ItemType Directory -Path $scripts, "$bundle/mods", $evidence -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'prepare-ui-smoke-launch.ps1') -Destination $scripts
Set-Content (Join-Path $scripts 'get-java-home.ps1') 'param([int]$Major); if ($Major -notin @(17,21)) { throw "wrong Java" }; "C:\Java$Major"'
try {
    $profile = @{schema=1;target='1.20.1-forge';profile='compatible';java=17;loader='47.4.10';dependencyMode='base'}
    $profile | ConvertTo-Json | Set-Content "$bundle/profile.json"
    Set-Content "$bundle/mods/mod.jar" 'unchanged artifact'
    '["mod.jar"]' | Set-Content "$bundle/mods/.ae2-crafting-time-run-mods.json"
    $launch = @{target='1.20.1-forge';java=17;guest=(Join-Path $temp 'prepared');arguments=@('-Xmx1G','-Dae2craftingtime.test.world=old',
        '-cp','C:\Native Loader\client.jar','example.Client','--version','1.20.1-forge-47.4.10',
        '--gameDir','C:\Old Game','--quickPlaySingleplayer','old')}
    $manifest = Join-Path $temp 'launch.json'
    $launch | ConvertTo-Json | Set-Content $manifest
    $parameters = @{LaunchManifest=$manifest;BundleDirectory=$bundle;RuntimeDirectory=$runtime;Target='1.20.1-forge'
        Profile='compatible';Scenario='delayed-status';World=('ae2ct-'+'a'*32);Evidence=$evidence}
    $prepared = & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters
    if ($prepared.executable -ne 'C:\Java17\bin\java.exe') { throw 'Wrong Java executable' }
    $arguments = Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw
    if ($arguments.Contains('old') -or $arguments.Contains('-Xmx1G') -or -not $arguments.Contains('-Xmx8G') -or
            -not $arguments.Contains('scenario=delayed-status') -or -not $arguments.Contains('C:\\Native Loader\\client.jar')) {
        throw 'Native launch lost the installed classpath or retained previous run arguments'
    }
    if ((Get-Content "$runtime/mods/mod.jar" -Raw) -ne (Get-Content "$bundle/mods/mod.jar" -Raw)) { throw 'Artifact changed during staging' }
    $continuation = Join-Path $evidence 'cpu-list-continuation.json'
    Set-Content -LiteralPath $continuation -Value '{"schema":1,"phase":"relaunch-ready"}'
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters -ContinuationPath $continuation -CampaignId 'campaign-a' | Out-Null
    $continuedArguments = Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw
    if (-not $continuedArguments.Contains('continuation=') -or -not $continuedArguments.Contains('campaign=campaign-a')) {
        throw 'Relaunch continuation and campaign identity were not passed to the second client'
    }
    $parameters.Scenario = 'cpu-list-total-ttc'
    $resumed = & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters -ContinuationPath $continuation `
        -CampaignId 'campaign-a' -ResumeOnly
    $resumeArguments = Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw
    if (-not $resumeArguments.Contains('resumeOnly=true') -or $resumed.finalApproval -ne $false) {
        throw 'Resume-only launch was not marked as diagnostic and non-final'
    }
    $parameters.Scenario = 'delayed-status'
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters -Interactive | Out-Null
    if (-not (Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw).Contains('interactive=true')) { throw 'Interactive mode was discarded' }
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters -ProjectId rxYaglEe | Out-Null
    if (-not (Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw).Contains('advancedStatus=true')) { throw 'AdvancedAE status mode was discarded' }
    $control = Join-Path $temp 'connected-control'
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters -DedicatedAddress '127.0.0.1:25565' -ControlDirectory $control | Out-Null
    $connectedArguments = Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw
    if (-not $connectedArguments.Contains('connectedDedicated=true') -or
            -not $connectedArguments.Contains('dedicatedAddress=127.0.0.1:25565') -or
            $connectedArguments.Contains('--quickPlayMultiplayer') -or
            $connectedArguments.Contains('--quickPlaySingleplayer')) {
        throw 'Connected dedicated launch did not defer connection until the test driver is ready'
    }
    $profile.target = '1.21.1-neoforge'; $profile.java = 21; $profile.loader = '21.1.238'
    $profile | ConvertTo-Json | Set-Content "$bundle/profile.json"
    $launch.target = '1.21.1-neoforge'; $launch.java = 21; $launch.arguments[-5] = '1.21.1-21.1.238'
    $launch | ConvertTo-Json | Set-Content $manifest
    $parameters.Target = '1.21.1-neoforge'
    $config = Join-Path $runtime 'config/fml.toml'
    $template = Join-Path $launch.guest 'config/fml.toml'
    New-Item -ItemType Directory -Path (Split-Path $config) -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path $template) -Force | Out-Null
    $configBytes = [byte[]](0, 1, 2, 255)
    $templateBytes = [byte[]](4, 5, 6, 255)
    [IO.File]::WriteAllBytes($config, $configBytes)
    [IO.File]::WriteAllBytes($template, $templateBytes)
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters | Out-Null
    if (Compare-Object $templateBytes ([IO.File]::ReadAllBytes($config)) -SyncWindow 0) { throw 'Version-matched FML config was not staged' }
    if (Compare-Object $configBytes ([IO.File]::ReadAllBytes((Join-Path $evidence 'fml-prelaunch.toml'))) -SyncWindow 0) {
        throw 'Pre-launch FML config evidence changed bytes'
    }
    Remove-Item -LiteralPath $config -Force
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters | Out-Null
    if (-not (Test-Path -LiteralPath (Join-Path $evidence 'fml-prelaunch.absent')) -or
            (Test-Path -LiteralPath (Join-Path $evidence 'fml-prelaunch.toml'))) { throw 'Missing pre-launch FML config was not recorded' }
    if (Compare-Object $templateBytes ([IO.File]::ReadAllBytes($config)) -SyncWindow 0) { throw 'Missing runtime FML config was not initialized from the prepared loader' }
    $profile.target = '1.20.1-forge'; $profile.java = 17; $profile.loader = '47.4.10'
    $profile | ConvertTo-Json | Set-Content "$bundle/profile.json"
    $launch.target = '1.20.1-forge'; $launch.java = 17; $launch.arguments[-5] = '1.20.1-forge-47.4.10'
    $launch | ConvertTo-Json | Set-Content $manifest
    $parameters.Target = '1.20.1-forge'
    function Assert-Rejected([string]$expected) {
        try { & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters | Out-Null }
        catch { if ($_.Exception.Message -like "*$expected*") { return }; throw }
        throw "Accepted invalid native setup: $expected"
    }
    $parameters.RuntimeDirectory = Join-Path $temp 'unowned'
    Assert-Rejected 'allowed runtime root'
    $parameters.RuntimeDirectory = $runtime
    $parameters.Target = '1.20.1-fabric'
    Assert-Rejected 'mismatch'
    $parameters.Target = '1.20.1-forge'
    $profile.loader = '47.9.9'
    $profile | ConvertTo-Json | Set-Content "$bundle/profile.json"
    Assert-Rejected 'Prepared loader'
    $profile.loader = '47.4.10'
    $profile | ConvertTo-Json | Set-Content "$bundle/profile.json"
    '["../escape.jar"]' | Set-Content "$bundle/mods/.ae2-crafting-time-run-mods.json"
    Assert-Rejected 'Invalid bundle filename'
    Write-Host 'Native artifact launcher checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temp)
    if ($resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
