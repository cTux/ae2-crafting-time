$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-bundle-cache-' + [guid]::NewGuid().ToString('N'))
$cache = Join-Path $temporary 'cache'
New-Item -ItemType Directory -Path (Join-Path $cache 'mods') -Force | Out-Null
try {
    Set-Content -LiteralPath (Join-Path $cache 'mods/mod.jar') -Value 'immutable artifact'
    $arguments = @{ CacheDirectory=$cache; HeadSha=('2' * 40); Fingerprint='fingerprint-a'; Target='1.20.1-forge'; Profile='compatible'; GraphId='primary'; ProjectId=@('z','A','z') }
    try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Seal;throw 'Sealed a missing adapter contract' }
    catch { if($_.Exception.Message -eq 'Sealed a missing adapter contract'){throw} }
    $adapters=Join-Path $cache 'expected-adapters.json'
    [IO.File]::WriteAllText($adapters,'{}')
    & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Seal | Out-Null
    $first = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    $second = & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse
    if (!$first.reused -or $first.bundleSha256 -ne $second.bundleSha256) { throw 'Exact immutable bundle was not reused' }
    $identity=Get-Content (Join-Path $cache 'bundle-identity.json') -Raw|ConvertFrom-Json
    if ($identity.schema -ne 3 -or (@($identity.projectId) -cjoin ',') -cne 'A,z') { throw 'Canonical selection was not sealed' }
    $sameSelection = @{} + $arguments; $sameSelection.ProjectId = @('A','z')
    if (!(& (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @sameSelection -Mode Reuse).reused) { throw 'Order or duplicates caused another resolution' }
    $canonical = & (Join-Path $PSScriptRoot 'get-ui-smoke-project-selection.ps1') -ProjectId @('z','A','z')
    $reordered = & (Join-Path $PSScriptRoot 'get-ui-smoke-project-selection.ps1') -ProjectId @('A','z')
    $unfiltered = & (Join-Path $PSScriptRoot 'get-ui-smoke-project-selection.ps1') -ProjectId @()
    if ($canonical.digest -cne $reordered.digest -or $canonical.digest -ceq $unfiltered.digest) { throw 'Selection path digest is unstable' }
    foreach ($different in @(@{ids=@()},@{ids=@('A')},@{ids=@('a','z')},@{ids=@('A','z','other')})) {
        $wrong = @{} + $arguments; $wrong.ProjectId = $different.ids
        try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @wrong -Mode Reuse | Out-Null; throw 'Reused a different selection' }
        catch { if ($_.Exception.Message -eq 'Reused a different selection') { throw } }
    }
    foreach ($change in @(@{Target='1.20.1-fabric'},@{Profile='latest'},@{GraphId='other'})) {
        $wrong = @{} + $arguments
        foreach ($key in $change.Keys) { $wrong[$key] = $change[$key] }
        try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @wrong -Mode Reuse | Out-Null; throw 'Reused a different target, profile or graph' }
        catch { if ($_.Exception.Message -eq 'Reused a different target, profile or graph') { throw } }
    }
    $identityPath=Join-Path $cache 'bundle-identity.json';$originalIdentity=[IO.File]::ReadAllText($identityPath)
    foreach ($legacy in @('old-schema','missing-selection','scalar-selection','non-string-selection','unsorted-selection')) {
        $identity = $originalIdentity | ConvertFrom-Json
        if ($legacy -eq 'old-schema') { $identity.schema = 2 }
        elseif ($legacy -eq 'missing-selection') { $identity.psobject.Properties.Remove('projectId') }
        elseif ($legacy -eq 'scalar-selection') { $identity.projectId = 'A' }
        elseif ($legacy -eq 'non-string-selection') { $identity.projectId = @(1,'z') }
        else { $identity.projectId = @('z','A') }
        [IO.File]::WriteAllText($identityPath,($identity|ConvertTo-Json -Depth 8))
        $rejectedBytes = [IO.File]::ReadAllText($identityPath)
        try { & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @arguments -Mode Reuse | Out-Null; throw 'Reused an incomplete selection seal' }
        catch { if ($_.Exception.Message -eq 'Reused an incomplete selection seal') { throw } }
        if ([IO.File]::ReadAllText($identityPath) -cne $rejectedBytes -or [IO.File]::ReadAllText($adapters) -cne '{}') { throw 'Rejected seal changed bundle bytes' }
    }
    [IO.File]::WriteAllText($identityPath,$originalIdentity)
    $identity = $originalIdentity | ConvertFrom-Json
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
