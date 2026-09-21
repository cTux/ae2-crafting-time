$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-bundle-cache-' + [guid]::NewGuid().ToString('N'))
$cache = Join-Path $temporary 'cache'
New-Item -ItemType Directory -Path (Join-Path $cache 'mods') -Force | Out-Null
try {
    Set-Content -LiteralPath (Join-Path $cache 'mods/mod.jar') -Value 'immutable artifact'
    $arguments = @{ CacheDirectory=$cache; HeadSha=('2' * 40); Fingerprint='fingerprint-a'; Target='1.20.1-forge'; Profile='compatible'; GraphId='primary' }
    try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Seal;throw 'Sealed a missing adapter contract' }
    catch { if($_.Exception.Message -eq 'Sealed a missing adapter contract'){throw} }
    $adapters=Join-Path $cache 'expected-adapters.json'
    [IO.File]::WriteAllText($adapters,'{}')
    & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Seal | Out-Null
    $first = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    $second = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    if (!$first.reused -or $first.bundleSha256 -ne $second.bundleSha256) { throw 'Exact immutable bundle was not reused' }
    $identity=Get-Content (Join-Path $cache 'bundle-identity.json') -Raw|ConvertFrom-Json
    if(@($identity.artifacts|Where-Object path -CEQ 'expected-adapters.json').Count -ne 1){throw 'Adapter contract was not sealed'}
    Remove-Item -LiteralPath $adapters
    try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse;throw 'Reused a missing adapter contract' }
    catch { if($_.Exception.Message -eq 'Reused a missing adapter contract'){throw} }
    [IO.File]::WriteAllText($adapters,'{"appmek":"invented"}')
    try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse;throw 'Reused tampered adapter expectations' }
    catch { if($_.Exception.Message -eq 'Reused tampered adapter expectations'){throw} }
    [IO.File]::WriteAllText($adapters,'{}')
    $restored=& (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    if($restored.bundleSha256 -ne $first.bundleSha256){throw 'Exact adapter restoration changed bundle identity'}
    $identityPath=Join-Path $cache 'bundle-identity.json';$originalIdentity=[IO.File]::ReadAllText($identityPath)
    $identity.artifacts=@($identity.artifacts|Where-Object path -CNE 'expected-adapters.json')
    [IO.File]::WriteAllText($identityPath,($identity|ConvertTo-Json -Depth 8))
    try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse;throw 'Reused an adapter contract omitted from seal artifacts' }
    catch { if($_.Exception.Message -eq 'Reused an adapter contract omitted from seal artifacts'){throw} }
    [IO.File]::WriteAllText($identityPath,$originalIdentity)
    try {
        & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse -BaseOnly | Out-Null
        throw 'A catalogue bundle was reused as a base-only bundle'
    } catch { if ($_.Exception.Message -eq 'A catalogue bundle was reused as a base-only bundle') { throw } }
    Add-Content -LiteralPath (Join-Path $cache 'mods/mod.jar') -Value 'tampered'
    try {
        & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse | Out-Null
        throw 'Tampered bundle cache was reused'
    } catch { if ($_.Exception.Message -eq 'Tampered bundle cache was reused') { throw } }
    Write-Host 'UI smoke bundle-cache checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temporary)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -cnotmatch '^ae2ct-bundle-cache-[a-f0-9]{32}$') { throw 'Unsafe bundle-cache test cleanup' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
