$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-bundle-cache-' + [guid]::NewGuid().ToString('N'))
$cache = Join-Path $temporary 'cache'
New-Item -ItemType Directory -Path (Join-Path $cache 'mods') -Force | Out-Null
try {
    Set-Content -LiteralPath (Join-Path $cache 'mods/mod.jar') -Value 'immutable artifact'
    $arguments = @{ CacheDirectory=$cache; HeadSha=('2' * 40); Fingerprint='fingerprint-a'; Target='1.20.1-forge'; Profile='compatible'; GraphId='primary' }
    & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Seal | Out-Null
    $first = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    $second = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    if (!$first.reused -or $first.bundleSha256 -ne $second.bundleSha256) { throw 'Exact immutable bundle was not reused' }
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
