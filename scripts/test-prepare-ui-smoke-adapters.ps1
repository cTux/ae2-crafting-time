$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-adapters-' + [guid]::NewGuid().ToString('N'))
try {
    $mods = Join-Path $temp 'mods'
    New-Item -ItemType Directory -Path $mods -Force | Out-Null
    $version = ((Get-Content (Join-Path $root 'gradle.properties')) | Where-Object { $_ -match '^modVersion=' }) -replace '^modVersion=', ''
    if (!$version) { throw 'Missing modVersion' }
    Copy-Item (Join-Path $root "build/test-driver/ae2-crafting-time-$version-forge-1.20.1-test-driver.jar") $mods
    Copy-Item (Join-Path $root "dist/ae2-crafting-time-$version-forge-1.20.1.jar") $mods
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target 1.20.1-forge -BundleDirectory $temp -BaseOnly
    $base = Get-Content (Join-Path $temp 'expected-adapters.json') -Raw | ConvertFrom-Json
    if (@($base.psobject.Properties).Count -ne 0) { throw 'BaseOnly expected adapters' }
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target 1.20.1-forge -BundleDirectory $temp -ProjectId rxYaglEe
    $advanced = Get-Content (Join-Path $temp 'expected-adapters.json') -Raw | ConvertFrom-Json
    if (@($advanced.psobject.Properties).Count -ne 1 -or $advanced.advanced_ae -ne 'advanced-cpu') { throw 'AdvancedAE graph expectation' }
    & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target 1.20.1-forge -BundleDirectory $temp
    $full = Get-Content (Join-Path $temp 'expected-adapters.json') -Raw | ConvertFrom-Json
    if (@($full.psobject.Properties).Count -lt 5 -or $full.advanced_ae -ne 'advanced-cpu') { throw 'Full catalogue expectation' }
    foreach($target in @('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')) {
    foreach($native in @($true)+$(if($target -in @('1.20.1-forge','1.21.1-neoforge')){@($false)}else{@()})) {
        $parameters=@{Target=$target;BundleDirectory=$temp;BaseOnly=$native}
        if(!$native){$parameters.ProjectId=@('IiATswDj')}
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') @parameters
        $path=Join-Path $temp 'expected-adapters.json'
        if([IO.File]::ReadAllText($path) -cne '{}'){throw 'Native and focused AppMek shared hooks require an empty custom-adapter contract'}
        $before=(Get-FileHash $path).Hash
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') @parameters
        if((Get-FileHash $path).Hash -cne $before){throw 'Adapter generation is nondeterministic'}
        $cache=@{CacheDirectory=$temp;HeadSha=('a'*40);Fingerprint='adapter-test';Target=$target;Profile='compatible';GraphId='focused';BaseOnly=$native}
        $sealed=& (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @cache -Mode Seal
        $timestamp=(Get-Item $path).LastWriteTimeUtc
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') @parameters
        & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') @parameters -ValidateOnly
        $reused=& (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') @cache -Mode Reuse
        if($reused.bundleSha256 -cne $sealed.bundleSha256 -or (Get-Item $path).LastWriteTimeUtc -ne $timestamp){throw 'Sealed adapter validation mutated bundle identity'}
        if(!$native){
            try { & (Join-Path $PSScriptRoot 'prepare-ui-smoke-adapters.ps1') -Target $target -BundleDirectory $temp -ProjectId rxYaglEe;throw 'Accepted wrong focused adapter policy' }
            catch { if($_.Exception.Message -eq 'Accepted wrong focused adapter policy'){throw} }
            if((Get-FileHash $path).Hash -cne $before){throw 'Rejected focused policy mutated sealed adapters'}
        }
        Remove-Item (Join-Path $temp 'bundle-identity.json')
    }
    }
    Write-Host 'prepare-ui-smoke-adapters checks passed'
} finally {
    if (Test-Path $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
