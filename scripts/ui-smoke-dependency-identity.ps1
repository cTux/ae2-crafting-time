function Get-UiSmokeDependencyIdentity([string]$BundleDirectory) {
    $bundle = [IO.Path]::GetFullPath($BundleDirectory)
    $profilePath = Join-Path $bundle 'profile.json'
    $manifestPath = Join-Path $bundle 'mods/.ae2-crafting-time-run-mods.json'
    if (!(Test-Path -LiteralPath $profilePath -PathType Leaf) -or !(Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw 'UI-smoke bundle has no dependency identity'
    }
    $profile = Get-Content -LiteralPath $profilePath -Raw | ConvertFrom-Json
    if ($profile.dependencyMode -notin @('base','catalogue')) { throw 'UI-smoke bundle has no dependency mode' }
    $names = @((Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json).ForEach({ [string]$_ }))
    $lines = [Collections.Generic.List[string]]::new()
    foreach ($name in $names) {
        if ([IO.Path]::GetFileName($name) -ne $name -or $name -notlike '*.jar') { throw 'Invalid bundle dependency filename' }
        $path = Join-Path $bundle "mods/$name"
        if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing bundle dependency $name" }
        $lines.Add("$name|$((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash)")
    }
    $lines.Sort([StringComparer]::Ordinal)
    $algorithm = [Security.Cryptography.SHA256]::Create()
    try {
        $catalogue = ([BitConverter]::ToString($algorithm.ComputeHash([Text.Encoding]::UTF8.GetBytes($lines -join "`n"))) -replace '-','')
    } finally { $algorithm.Dispose() }
    [pscustomobject]@{ mode=[string]$profile.dependencyMode; catalogueSha256=$catalogue; files=@($names) }
}

function Assert-UiSmokeWorldDependencyIdentity([string]$MarkerPath, $DependencyIdentity) {
    if (!(Test-Path -LiteralPath $MarkerPath -PathType Leaf)) { throw 'Resume world has no disposable marker' }
    $marker = Get-Content -LiteralPath $MarkerPath -Raw | ConvertFrom-Json
    if ($marker.dependencyMode -ne $DependencyIdentity.mode -or
            $marker.dependencyCatalogueSha256 -ne $DependencyIdentity.catalogueSha256) {
        throw 'Resume world dependency identity differs from the selected bundle'
    }
    $marker
}
