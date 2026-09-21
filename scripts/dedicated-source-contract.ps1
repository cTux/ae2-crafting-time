# Shared validation for the fixed compatible dedicated sources and their consumer.
function Get-DedicatedSha256([string]$Text) {
    $hash = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($Text))) -replace '-','').ToLowerInvariant() }
    finally { $hash.Dispose() }
}

function Assert-DedicatedPath([string]$Path, [switch]$Tree) {
    if ($Path -notmatch '^[A-Za-z]:[\\/]' -or $Path -match '(^|[\\/])\.\.([\\/]|$)' -or $Path.Substring(2).Contains(':')) {
        throw 'Dedicated paths must be absolute local paths without traversal or alternate streams'
    }
    $full = [IO.Path]::GetFullPath($Path).TrimEnd('\','/')
    if ($full.Length -le 3) { throw 'A filesystem root is not a dedicated workspace' }
    foreach ($part in $Path.Substring(3).TrimEnd('\','/').Split([char[]]@('\','/'))) {
        if ($part -match '^(?i:con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)' -or $part.TrimEnd(' ','.') -cne $part) {
            throw 'Dedicated paths cannot name a device or an ambiguous Windows component'
        }
    }
    for ($cursor = $full; $cursor; $cursor = Split-Path -Parent $cursor) {
        if (Test-Path -LiteralPath $cursor) {
            $entry = Get-Item -LiteralPath $cursor -Force
            if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Dedicated path has a linked ancestor' }
        }
    }
    if ($Tree -and (Test-Path -LiteralPath $full)) {
        # Enumerate one directory at a time, rejecting links before descending.
        $pending = [Collections.Generic.Queue[string]]::new()
        $pending.Enqueue($full)
        while ($pending.Count) {
            $directory = $pending.Dequeue()
            foreach ($entry in Get-ChildItem -LiteralPath $directory -Force) {
                if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Dedicated tree contains a filesystem link' }
                if ($entry.PSIsContainer) { $pending.Enqueue($entry.FullName) }
                else {
                    if ($entry.LinkType) { throw 'Dedicated file is hard-linked' }
                }
            }
        }
    }
    return $full
}

function Assert-DedicatedRoots([string[]]$Roots, [string[]]$ProtectedRoots) {
    foreach ($root in $Roots) {
        Assert-DedicatedPath $root | Out-Null
        for ($ancestor = $root; $ancestor; $ancestor = Split-Path -Parent $ancestor) {
            if ((Test-Path -LiteralPath (Join-Path $ancestor '.git')) -or
                    (Test-Path -LiteralPath (Join-Path $ancestor '.ae2-crafting-time-dedicated-fixture.json'))) {
                throw 'Dedicated writable roots cannot be inside a checkout or existing source'
            }
        }
    }
    for ($i = 0; $i -lt $Roots.Count; $i++) {
        foreach ($other in @($Roots | Select-Object -Skip ($i + 1)) + $ProtectedRoots) {
            $a = [IO.Path]::GetFullPath($Roots[$i]).TrimEnd('\','/')
            $b = [IO.Path]::GetFullPath($other).TrimEnd('\','/')
            if ($a -ieq $b -or $a.StartsWith($b + '\',[StringComparison]::OrdinalIgnoreCase) -or
                    $b.StartsWith($a + '\',[StringComparison]::OrdinalIgnoreCase)) {
                throw 'Dedicated roots must be distinct and outside protected inputs'
            }
        }
    }
}

function Read-DedicatedJson([string]$Path, [long]$Limit = 10MB) {
    Assert-DedicatedPath $Path | Out-Null
    $file = Get-Item -LiteralPath $Path -Force
    if ($file.PSIsContainer -or $file.Length -gt $Limit) { throw 'Dedicated JSON is missing, non-regular or oversized' }
    $stream = [IO.File]::OpenRead($Path)
    try {
        $bytes = [byte[]]::new($Limit + 1)
        $count = 0
        do { $read = $stream.Read($bytes,$count,$bytes.Length-$count); $count += $read } while ($read -gt 0 -and $count -lt $bytes.Length)
        if ($count -gt $Limit) { throw 'Dedicated JSON exceeded its read bound' }
        $parsed = [Text.Encoding]::UTF8.GetString($bytes,0,$count).TrimStart([char]0xfeff) | ConvertFrom-Json
        return $parsed
    } finally { $stream.Dispose() }
}

function Get-DedicatedTree([string]$Root, [int]$MaxFiles = 20000, [long]$MaxBytes = 4GB) {
    $rootPath = Assert-DedicatedPath $Root -Tree
    $total = 0L
    $files = @(Get-ChildItem -LiteralPath $rootPath -File -Recurse -Force | Where-Object {
        $_.FullName -ne (Join-Path $rootPath '.ae2-crafting-time-dedicated-fixture.json') -and
        $_.FullName -ne (Join-Path $rootPath 'source-seal.json')
    } | Sort-Object FullName | ForEach-Object {
        $total += $_.Length
        if ($_.Length -gt 512MB -or $total -gt $MaxBytes) { throw 'Dedicated tree exceeds its byte bound' }
        [ordered]@{path=$_.FullName.Substring($rootPath.Length + 1).Replace('\','/');size=$_.Length
            sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
    })
    if ($files.Count -gt $MaxFiles) { throw 'Dedicated tree exceeds its file-count bound' }
    return [ordered]@{schema=1;files=$files;bytes=$total}
}

function Assert-DedicatedSeal([string]$Root) {
    Assert-DedicatedPath $Root -Tree | Out-Null
    $markerPath = Join-Path $Root '.ae2-crafting-time-dedicated-fixture.json'
    $marker = Read-DedicatedJson $markerPath
    if ($marker.schema -ne 2 -or $marker.role -ne 'source' -or $marker.sourceFixtureId -ne 'ae2-crafting-time' -or
            $marker.provisioning.schema -ne 1 -or $marker.provisioning.sealPath -cne 'source-seal.json' -or
            $marker.provisioning.sourceKey -cnotmatch '^[a-f0-9]{64}$') { throw 'Invalid provisioned source identity' }
    $sealPath = Join-Path $Root 'source-seal.json'
    if ((Get-FileHash -LiteralPath $sealPath -Algorithm SHA256).Hash -ine $marker.provisioning.sealSha256) {
        throw 'Source seal changed; quarantine the source without deleting it'
    }
    $seal = Read-DedicatedJson $sealPath
    $tree = Get-DedicatedTree $Root
    $expected = @($seal.files | ForEach-Object { "$($_.path)|$($_.size)|$($_.sha256)" })
    $actual = @($tree.files | ForEach-Object { "$($_.path)|$($_.size)|$($_.sha256)" })
    if ($seal.schema -ne 1 -or $seal.bytes -ne $tree.bytes -or $expected.Count -ne $actual.Count -or
            (($expected -join "`n") -cne ($actual -join "`n"))) {
        throw 'Source inventory changed; quarantine the source without deleting it'
    }
    return $marker
}

function Get-DedicatedGraph([string]$Target, [string]$BundleDirectory, [string]$PreparedLaunch) {
    $rows = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'release-matrix.json') -Raw | ConvertFrom-Json
    $pins = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-client-versions.json') -Raw | ConvertFrom-Json
    $row = @($rows | Where-Object id -eq $Target)
    $pin = @($pins | Where-Object id -eq $Target)
    if ($Target -notin @('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge') -or $row.Count -ne 1 -or $pin.Count -ne 1) {
        throw 'Unsupported dedicated target'
    }
    $profile = Read-DedicatedJson (Join-Path $BundleDirectory 'profile.json')
    $launch = Read-DedicatedJson $PreparedLaunch
    $identity = Read-DedicatedJson (Join-Path $BundleDirectory 'bundle-identity.json')
    $java = if ($Target -like '1.20.1-*') { 17 } elseif ($Target -eq '1.21.1-neoforge') { 21 } else { 25 }
    if ($profile.schema -ne 1 -or $profile.target -ne $Target -or $profile.profile -cne 'compatible' -or
            $profile.java -ne $java -or $profile.loader -cne $pin[0].compatible.loader_version -or
            $profile.ae2 -cne $pin[0].compatible.ae2_version -or $launch.target -ne $Target -or $launch.java -ne $java) {
        throw 'Dedicated target, compatible pins, prepared launch or Java mismatch'
    }
    Assert-DedicatedPath $BundleDirectory -Tree | Out-Null
    & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') -Mode Reuse -CacheDirectory $BundleDirectory `
        -HeadSha $identity.headSha -Fingerprint $identity.fingerprint -Target $Target -Profile compatible `
        -GraphId $identity.graphId -BaseOnly:([bool]$identity.baseOnly) | Out-Null
    $loaderVersion = $profile.loader -replace ('^' + [regex]::Escape($row[0].minecraftVersion) + '-'),''
    if (!@($launch.arguments | Where-Object { $_ -match "(^|[-])$([regex]::Escape($loaderVersion))($|[-])" }).Count) {
        throw 'Prepared client loader does not match the exact compatible loader'
    }
    $names = @(Read-DedicatedJson (Join-Path $BundleDirectory 'mods/.ae2-crafting-time-run-mods.json'))
    $files = @(Get-ChildItem -LiteralPath (Join-Path $BundleDirectory 'mods') -File -Filter '*.jar')
    if ($names.Count -ne $files.Count -or @($names | Select-Object -Unique).Count -ne $names.Count -or
            @(Compare-Object @($names | Sort-Object) @($files.Name | Sort-Object)).Count) { throw 'Bundle mod inventory mismatch' }
    $dependencies = @($files | Where-Object Name -NotLike 'ae2-crafting-time-*.jar' | Sort-Object Name)
    $chemical = @($dependencies | Where-Object Name -Like 'applied-mekanistics-*').Count -eq 1
    if ($chemical -and $Target -notin @('1.20.1-forge','1.21.1-neoforge')) { throw 'Unsupported chemical graph' }
    if ([bool]$identity.baseOnly -eq $chemical -or $profile.dependencyMode -cne $(if ($chemical) {'catalogue'} else {'base'})) {
        throw 'Dedicated graph must be explicitly native or focused AppMek'
    }
    $expected = @($(if ($Target -like '*-neoforge') {"appliedenergistics2-$($profile.ae2).jar"}
        else {"appliedenergistics2-$($row[0].loader)-$($profile.ae2).jar"}))
    if ($Target -eq '1.20.1-fabric') { $expected += "fabric-api-$($pin[0].compatible.fabric_api_version).jar" }
    else { $expected += "guideme-$(@($pin[0].compatible.versions | Where-Object project_id -eq 'Ck4E7v7R')[0].version).jar" }
    if ($chemical) {
        $appmek = @($pin[0].compatible.versions | Where-Object project_id -eq 'IiATswDj')[0].version
        $mekanism = @($pin[0].compatible.versions | Where-Object project_id -eq 'Ce6I4WUE')[0].version
        $expected += "applied-mekanistics-$appmek.jar", "Mekanism-$($row[0].minecraftVersion)-$mekanism.jar"
    }
    if (@(Compare-Object @($expected | Sort-Object) @($dependencies.Name | Sort-Object)).Count) {
        throw 'Unknown, client-only, missing or differently pinned dedicated dependency'
    }
    $dependencyIdentity = @($dependencies | ForEach-Object {
        [ordered]@{name=$_.Name;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
    })
    $installer = switch ($Target) {
        '1.20.1-forge' { "https://maven.minecraftforge.net/net/minecraftforge/forge/$($profile.loader)/forge-$($profile.loader)-installer.jar" }
        '1.20.1-fabric' { 'https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.2/fabric-installer-1.1.2.jar' }
        default { "https://maven.neoforged.net/releases/net/neoforged/neoforge/$($profile.loader)/neoforge-$($profile.loader)-installer.jar" }
    }
    $launcher = if ($Target -eq '1.20.1-fabric') {'fabric-server-launch.jar'} elseif ($Target -eq '1.20.1-forge') {
        "libraries/net/minecraftforge/forge/$($profile.loader)/win_args.txt"
    } else { "libraries/net/neoforged/neoforge/$($profile.loader)/win_args.txt" }
    return [ordered]@{target=$Target;java=$java;loader=$profile.loader;minecraft=$row[0].minecraftVersion
        graph=$(if ($chemical) {'appmek'} else {'native'});dependencies=$dependencyIdentity
        installerUrl=$installer;launcher=$launcher;bundleSha256=$identity.bundleSha256;headSha=$identity.headSha}
}
