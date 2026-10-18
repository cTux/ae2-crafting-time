$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-selection-boundary-' + [guid]::NewGuid().ToString('N'))
$scripts = Join-Path $temporary 'scripts'
$bundle = Join-Path $temporary 'bundle'
New-Item -ItemType Directory -Path $scripts,(Join-Path $bundle 'mods') -Force | Out-Null
try {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1'),(Join-Path $PSScriptRoot 'get-ui-smoke-project-selection.ps1') -Destination $scripts
    $adapter = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Raw
    $boundary = $adapter.IndexOf('$mods = Join-Path', [StringComparison]::Ordinal)
    if ($boundary -lt 0) { throw 'Adapter preflight boundary moved' }
    [IO.File]::WriteAllText((Join-Path $scripts 'adapter-boundary.ps1'),$adapter.Substring(0,$boundary))
    [IO.File]::WriteAllText((Join-Path $bundle 'expected-adapters.json'),'{}')
    [IO.File]::WriteAllText((Join-Path $bundle 'mods/mod.jar'),'fixture')
    $seal = @{CacheDirectory=$bundle;HeadSha=('a'*40);Fingerprint='fixture';Target='1.20.1-forge';Profile='compatible';GraphId='primary';ProjectId=@('A')}
    & (Join-Path $scripts 'use-ui-smoke-bundle-cache.ps1') @seal -Mode Seal | Out-Null
    & (Join-Path $scripts 'adapter-boundary.ps1') -Target 1.20.1-forge -BundleDirectory $bundle -ProjectId A
    $before = (Get-FileHash -LiteralPath (Join-Path $bundle 'bundle-identity.json')).Hash
    try { & (Join-Path $scripts 'adapter-boundary.ps1') -Target 1.20.1-forge -BundleDirectory $bundle -ProjectId B; throw 'Adapter preflight accepted the wrong request' }
    catch { if ($_.Exception.Message -eq 'Adapter preflight accepted the wrong request') { throw } }
    if ((Get-FileHash -LiteralPath (Join-Path $bundle 'bundle-identity.json')).Hash -cne $before -or
            [IO.File]::ReadAllText((Join-Path $bundle 'expected-adapters.json')) -cne '{}') { throw 'Rejected adapter request changed sealed bytes' }
    Write-Host 'UI smoke selection boundary checks passed'
} finally {
    $resolved = [IO.Path]::GetFullPath($temporary)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()),[StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolved -Leaf) -cnotmatch '^ae2ct-selection-boundary-[a-f0-9]{32}$') { throw 'Unsafe selection test cleanup' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
