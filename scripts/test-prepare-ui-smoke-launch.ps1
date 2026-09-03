$ErrorActionPreference = 'Stop'
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-native-' + [guid]::NewGuid().ToString('N'))
$scripts = Join-Path $temp 'scripts'
$bundle = Join-Path $temp 'bundle'
$runtime = Join-Path $temp 'build/ui-smoke/test/runtime'
$evidence = Join-Path $temp 'evidence'
New-Item -ItemType Directory -Path $scripts, "$bundle/mods", $evidence -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'prepare-ui-smoke-launch.ps1') -Destination $scripts
Set-Content (Join-Path $scripts 'get-java-home.ps1') 'param([int]$Major); if ($Major -ne 17) { throw "wrong Java" }; "C:\Java17"'
try {
    $profile = @{schema=1;target='1.20.1-forge';profile='compatible';java=17;loader='47.4.10'}
    $profile | ConvertTo-Json | Set-Content "$bundle/profile.json"
    Set-Content "$bundle/mods/mod.jar" 'unchanged artifact'
    '["mod.jar"]' | Set-Content "$bundle/mods/.ae2-crafting-time-run-mods.json"
    $launch = @{target='1.20.1-forge';java=17;arguments=@('-Xmx1G','-Dae2craftingtime.test.world=old',
        '-cp','C:\Native Loader\client.jar','example.Client','--version','1.20.1-forge-47.4.10',
        '--gameDir','C:\Old Game','--quickPlaySingleplayer','old')}
    $manifest = Join-Path $temp 'launch.json'
    $launch | ConvertTo-Json | Set-Content $manifest
    $parameters = @{LaunchManifest=$manifest;BundleDirectory=$bundle;RuntimeDirectory=$runtime;Target='1.20.1-forge'
        Profile='compatible';Scenario='standard-ae2';World=('ae2ct-'+'a'*32);Evidence=$evidence}
    $prepared = & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters
    if ($prepared.executable -ne 'C:\Java17\bin\java.exe') { throw 'Wrong Java executable' }
    $arguments = Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw
    if ($arguments.Contains('old') -or $arguments.Contains('-Xmx1G') -or -not $arguments.Contains('-Xmx8G') -or
            -not $arguments.Contains('scenario=standard-ae2') -or -not $arguments.Contains('C:\\Native Loader\\client.jar')) {
        throw 'Native launch lost the installed classpath or retained previous run arguments'
    }
    if ((Get-Content "$runtime/mods/mod.jar" -Raw) -ne (Get-Content "$bundle/mods/mod.jar" -Raw)) { throw 'Artifact changed during staging' }
    & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters -Interactive | Out-Null
    if (-not (Get-Content (Join-Path $runtime 'ui-smoke-java.args') -Raw).Contains('interactive=true')) { throw 'Interactive mode was discarded' }
    function Assert-Rejected([string]$expected) {
        try { & (Join-Path $scripts 'prepare-ui-smoke-launch.ps1') @parameters | Out-Null }
        catch { if ($_.Exception.Message -like "*$expected*") { return }; throw }
        throw "Accepted invalid native setup: $expected"
    }
    $parameters.RuntimeDirectory = Join-Path $temp 'unowned'
    Assert-Rejected 'inside build/ui-smoke'
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
