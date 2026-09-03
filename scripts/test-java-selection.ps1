$ErrorActionPreference = 'Stop'
$resolve = Join-Path $PSScriptRoot 'get-java-home.ps1'
$configure = Join-Path $PSScriptRoot 'set-prism-java.ps1'
$homes = @{}
foreach ($major in 17, 21, 25) { $homes[$major] = & $resolve -Major $major }
$saved = $env:JAVA_HOME_17
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-java-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp -Force | Out-Null
function Assert-Failure([scriptblock]$action, [string]$message) {
    try { & $action; throw 'Expected failure did not occur' }
    catch { if ($_.Exception.Message -notlike $message) { throw } }
}
try {
    # Registry reads are substituted so missing/user/machine cases never edit real settings.
    $userHome = $homes[17]
    $machineHome = $null
    function Get-ItemProperty {
        param($LiteralPath, $Name, $ErrorAction)
        if ($LiteralPath -eq 'HKCU:\Environment') { return [pscustomobject]@{ JAVA_HOME_17 = $userHome } }
        return [pscustomobject]@{ JAVA_HOME_17 = $machineHome }
    }
    $env:JAVA_HOME_17 = $null
    if ((& $resolve -Major 17) -ne $homes[17]) { throw 'User Java setting was not loaded' }
    $userHome = $null
    $machineHome = $homes[17]
    if ((& $resolve -Major 17) -ne $homes[17]) { throw 'Machine Java setting was not loaded' }
    $machineHome = $null
    Assert-Failure { & $resolve -Major 17 } 'Set JAVA_HOME_17*'
    $env:JAVA_HOME_17 = Join-Path $temp 'missing'
    Assert-Failure { & $resolve -Major 17 } '*has no bin\java.exe*'
    $env:JAVA_HOME_17 = $homes[21]
    Assert-Failure { & $resolve -Major 17 } '*requires JDK 17*'
    $failed = Join-Path $temp 'failed\bin'
    New-Item -ItemType Directory -Path $failed -Force | Out-Null
    Copy-Item -LiteralPath "$env:SystemRoot\System32\where.exe" -Destination (Join-Path $failed 'java.exe')
    $env:JAVA_HOME_17 = Split-Path -Parent $failed
    Assert-Failure { & $resolve -Major 17 } '*requires JDK 17*'
    $env:JAVA_HOME_17 = $homes[17]
    if ((& $resolve -Major 17) -ne $homes[17]) { throw 'Process Java setting was not used' }
    Remove-Item Function:\Get-ItemProperty

    $instance = Join-Path $temp 'test pack'
    New-Item -ItemType Directory -Path $instance -Force | Out-Null
    '{"groups":{"Codex":{"instances":["test pack"]}}}' | Set-Content (Join-Path $temp 'instgroups.json')
    $config = Join-Path $instance 'instance.cfg'
    foreach ($game in '1.20.1', '1.21.1', '26.1.2') {
        $major = @{'1.20.1'=17; '1.21.1'=21; '26.1.2'=25}[$game]
        "{`"components`": [{`"uid`":`"net.minecraft`",`"version`":`"$game`"}]}" | Set-Content (Join-Path $instance 'mmc-pack.json')
        "[General]`r`nname=Keep this`r`nJavaPath=old`r`nAutomaticJava=true`r`n" | Set-Content $config
        & $configure -InstanceDirectory $instance
        $first = Get-Content $config -Raw
        & $configure -InstanceDirectory $instance
        $expected = (Join-Path $homes[$major] 'bin\java.exe').Replace('\', '/')
        if ((Get-Content $config -Raw) -ne $first -or $first -notmatch '(?m)^name=Keep this\r?$' -or
                @($first -split "`r?`n" | Where-Object { $_ -eq "JavaPath=$expected" }).Count -ne 1 -or
                $first -notmatch '(?m)^OverrideJavaLocation=true\r?$' -or $first -notmatch '(?m)^AutomaticJava=false\r?$') {
            throw "Incorrect Prism Java settings for $game"
        }
    }
    'missing section' | Set-Content $config
    Assert-Failure { & $configure -InstanceDirectory $instance } 'Missing Prism General*'
    '{"components":[{"uid":"net.minecraft","version":"1.19.2"}]}' | Set-Content (Join-Path $instance 'mmc-pack.json')
    Assert-Failure { & $configure -InstanceDirectory $instance } 'No Java mapping*'
    '{"groups":{"Codex":{"instances":[]}}}' | Set-Content (Join-Path $temp 'instgroups.json')
    Assert-Failure { & $configure -InstanceDirectory $instance } 'Only Prism instances in the Codex group*'
    if ((Get-Content $config -Raw).Trim() -ne 'missing section') { throw 'Rejected instance was modified' }
    Write-Host 'Java selection checks passed'
} finally {
    $env:JAVA_HOME_17 = $saved
    $resolvedTemp = [IO.Path]::GetFullPath($temp)
    if (-not $resolvedTemp.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe test cleanup path' }
    Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
}
