function Read-DedicatedArchiveJson($Archive, [string]$Name) {
    $entry = $Archive.GetEntry($Name)
    if (!$entry -or $entry.Length -gt 10MB) { throw 'Official installer metadata is missing or oversized' }
    $stream = $entry.Open()
    $reader = [IO.StreamReader]::new($stream)
    try { return $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose(); $stream.Dispose() }
}

function Get-DedicatedInstallMetadata([string]$Installer, $Graph, [string]$Report, $Budget) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $documents = [Collections.Generic.List[object]]::new()
    $fabricChecks = @()
    $archive = [IO.Compression.ZipFile]::OpenRead($Installer)
    try {
        if ($Graph.target -ne '1.20.1-fabric') {
            $documents.Add([ordered]@{name='install_profile.json';data=(Read-DedicatedArchiveJson $archive 'install_profile.json')})
            $documents.Add([ordered]@{name='version.json';data=(Read-DedicatedArchiveJson $archive 'version.json')})
        }
    } finally { $archive.Dispose() }
    $manifestPath = Join-Path $Report 'minecraft-versions.json'
    Receive-DedicatedFile 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json' $manifestPath $Budget 10MB | Out-Null
    $manifest = Read-DedicatedJson $manifestPath
    $version = @($manifest.versions | Where-Object id -ceq $Graph.minecraft)
    if ($version.Count -ne 1) { throw 'Official Minecraft version metadata does not contain the exact target' }
    $versionPath = Join-Path $Report 'minecraft-version.json'
    Receive-DedicatedFile ([uri]$version[0].url) $versionPath $Budget 10MB | Out-Null
    Assert-DedicatedDigest $versionPath SHA1 $version[0].sha1
    $minecraft = Read-DedicatedJson $versionPath
    if ($minecraft.id -cne $Graph.minecraft -or $minecraft.javaVersion.majorVersion -ne $Graph.java -or !$minecraft.downloads.server.sha1) {
        throw 'Minecraft version, Java or server metadata mismatch'
    }
    Assert-DedicatedUri ([uri]$minecraft.downloads.server.url)
    $documents.Add([ordered]@{name='minecraft-version.json';data=$minecraft})
    foreach ($document in $documents) {
        foreach ($library in @($document.data.libraries)) {
            if ($library.url) { Assert-DedicatedUri ([uri]$library.url) }
            foreach ($artifact in @($library.downloads.artifact) + @($library.downloads.classifiers.PSObject.Properties.Value)) {
                if ($artifact.url) { Assert-DedicatedUri ([uri]$artifact.url) }
            }
        }
    }
    if ($Graph.target -eq '1.20.1-fabric') {
        $fabricPath = Join-Path $Report 'fabric-server-meta.json'
        Receive-DedicatedFile ([uri]"https://meta.fabricmc.net/v2/versions/loader/$($Graph.minecraft)/$($Graph.loader)/server/json") $fabricPath $Budget 10MB | Out-Null
        $fabric = Read-DedicatedJson $fabricPath
        if ($fabric.id -cne "fabric-loader-$($Graph.loader)-$($Graph.minecraft)") { throw 'Fabric server metadata identity mismatch' }
        foreach ($library in $fabric.libraries) { Assert-DedicatedUri ([uri]$library.url) }
        $documents.Add([ordered]@{name='fabric-server-meta.json';data=$fabric})
        $index = 0
        $fabricChecks = @(foreach ($library in $fabric.libraries) {
            if ($library.name -cnotmatch '^[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9_.+-]+$') { throw 'Unsupported Fabric library coordinate' }
            $parts = $library.name.Split(':')
            $path = $parts[0].Replace('.','/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar'
            $checksum = Join-Path $Report "fabric-library-$index.sha1"; $index++
            $receipt = Receive-DedicatedFile ([uri]($library.url.TrimEnd('/') + '/' + $path + '.sha1')) $checksum $Budget 10MB
            $digest = [IO.File]::ReadAllText($checksum).Trim()
            if ($digest -cnotmatch '^[a-fA-F0-9]{40}$') { throw 'Fabric upstream library checksum is missing or malformed' }
            [ordered]@{path=('libraries/' + $path);sha1=$digest;checksum=$receipt}
        })
        $documents.Add([ordered]@{name='fabric-library-checksums.json';data=$fabricChecks})
    }
    $identities = @($documents | ForEach-Object {
        $path = Join-Path $Report $_.name
        if (!(Test-Path -LiteralPath $path)) { $_.data | ConvertTo-Json -Depth 60 | Set-Content -LiteralPath $path -Encoding UTF8 }
        [ordered]@{name=$_.name;sha256=(Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant()}
    })
    return [ordered]@{documents=$documents.ToArray();identities=$identities;server=$minecraft.downloads.server;fabricChecks=$fabricChecks}
}

function Get-DedicatedInstallerArguments([string]$Installer, $Graph, [string]$Staging) {
    $arguments = @("-Duser.home=$(Join-Path $Staging '.installer-home')", "-Djava.io.tmpdir=$(Join-Path $Staging '.installer-temp')",'-jar',$Installer)
    if ($Graph.target -eq '1.20.1-fabric') {
        return $arguments + @('server','-mcversion',$Graph.minecraft,'-loader',$Graph.loader,'-downloadMinecraft','-dir',$Staging)
    }
    return $arguments + @('--installServer',$Staging)
}

function Assert-DedicatedInstallBounds([long]$Files, [long]$Bytes, [long]$LargestFile,
        [long]$StdoutBytes, [long]$StderrBytes, [double]$ElapsedSeconds) {
    if ($Files -gt 2048 -or $Bytes -gt 4GB -or $LargestFile -gt 512MB -or
            $StdoutBytes -gt 10MB -or $StderrBytes -gt 10MB) { throw 'Official installer exceeded its disk/file/log bound' }
    if ($ElapsedSeconds -ge 900) { throw 'Official installer exceeded 15 minutes' }
}

function Get-DedicatedProcessStartMilliseconds([DateTime]$StartedAt) {
    # Process.StartTime retains 100 ns ticks; Win32_Process.CreationDate loses
    # sub-microsecond digits. Compare their UTC Unix millisecond identities,
    # including ledger readback, without a sliding timestamp tolerance.
    return ([DateTimeOffset]$StartedAt.ToUniversalTime()).ToUnixTimeMilliseconds()
}

function Test-DedicatedProcessIdentity([int]$ProcessId, [DateTime]$StartedAt,
        [int]$ExpectedProcessId, [DateTime]$ExpectedStartedAt) {
    return $ProcessId -gt 0 -and $ProcessId -eq $ExpectedProcessId -and
        (Get-DedicatedProcessStartMilliseconds $StartedAt) -eq (Get-DedicatedProcessStartMilliseconds $ExpectedStartedAt)
}

function Stop-DedicatedInstallerProcess([Diagnostics.Process]$Process) {
    if (!$Process.HasExited) {
        try { Stop-Process -InputObject $Process -Force -ErrorAction Stop }
        catch { if (!$Process.HasExited) { throw } }
    }
    if (!$Process.WaitForExit(10000)) { throw 'Owned installer process did not exit; staging retained' }
}

function Invoke-DedicatedInstaller([string]$Java, [string]$Installer, $Graph, [string]$Staging, [string]$Report) {
    Assert-DedicatedPath $Staging -Tree | Out-Null
    foreach ($name in @('.installer-home','.installer-temp')) { New-Item -ItemType Directory -Path (Join-Path $Staging $name) | Out-Null }
    $arguments = Get-DedicatedInstallerArguments $Installer $Graph $Staging
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $Java
    $start.Arguments = ($arguments | ForEach-Object { '"' + ($_ -replace '(\\*)"','$1$1\"' -replace '(\\+)$','$1$1') + '"' }) -join ' '
    $start.WorkingDirectory = $Staging
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    $start.EnvironmentVariables.Clear()
    foreach ($name in @('SystemRoot','WINDIR','COMSPEC','PROCESSOR_ARCHITECTURE','NUMBER_OF_PROCESSORS')) {
        $value = [Environment]::GetEnvironmentVariable($name)
        if ($value) { $start.EnvironmentVariables[$name] = $value }
    }
    $start.EnvironmentVariables['PATH'] = (Split-Path -Parent $Java) + ';' + (Join-Path $env:SystemRoot 'System32')
    $start.EnvironmentVariables['TEMP'] = Join-Path $Staging '.installer-temp'
    $start.EnvironmentVariables['TMP'] = Join-Path $Staging '.installer-temp'
    $start.EnvironmentVariables['USERPROFILE'] = Join-Path $Staging '.installer-home'
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $start
    $owned = [Collections.Generic.List[object]]::new()
    $stdout = [IO.File]::Create((Join-Path $Report 'installer.stdout.log'))
    $stderr = [IO.File]::Create((Join-Path $Report 'installer.stderr.log'))
    $clock = [Diagnostics.Stopwatch]::StartNew()
    $started = $false
    try {
        if (!$process.Start()) { throw 'Official installer did not start' }
        $started = $true
        $owned.Add([pscustomobject]@{pid=$process.Id;startTime=$process.StartTime.ToUniversalTime()})
        $outTask = $process.StandardOutput.BaseStream.CopyToAsync($stdout)
        $errTask = $process.StandardError.BaseStream.CopyToAsync($stderr)
        do {
            $snapshot = @(Get-CimInstance Win32_Process)
            do {
                $added = $false
                foreach ($child in $snapshot) {
                    if ($child.ProcessId -in $owned.pid -or $child.ParentProcessId -notin $owned.pid) { continue }
                    $parent = @($owned | Where-Object pid -eq $child.ParentProcessId)[0]
                    $nativeParent = @($snapshot | Where-Object ProcessId -eq $parent.pid)
                    if ($nativeParent.Count -ne 1 -or !(Test-DedicatedProcessIdentity $nativeParent[0].ProcessId `
                            $nativeParent[0].CreationDate $parent.pid $parent.startTime)) {
                        throw 'Installer parent process ownership changed'
                    }
                    if ((Get-DedicatedProcessStartMilliseconds $child.CreationDate) -lt
                            (Get-DedicatedProcessStartMilliseconds $parent.startTime)) { continue }
                    $owned.Add([pscustomobject]@{pid=[int]$child.ProcessId;startTime=$child.CreationDate.ToUniversalTime()})
                    $added = $true
                }
            } while ($added)
            $owned | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Report 'installer-processes.json') -Encoding UTF8
            Assert-DedicatedPath $Staging -Tree | Out-Null
            $entries = @(Get-ChildItem -LiteralPath $Staging -Recurse -File -Force)
            $bytes = ($entries | Measure-Object Length -Sum).Sum
            Assert-DedicatedInstallBounds $entries.Count $bytes (($entries | Measure-Object Length -Maximum).Maximum) `
                $stdout.Length $stderr.Length $clock.Elapsed.TotalSeconds
            if (!$process.HasExited) { $process.WaitForExit(1000) | Out-Null }
            elseif (!$outTask.IsCompleted -or !$errTask.IsCompleted) { Start-Sleep -Milliseconds 1000 }
        } while (!$process.HasExited -or !$outTask.IsCompleted -or !$errTask.IsCompleted)
        $outTask.GetAwaiter().GetResult(); $errTask.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw "Official installer exited $($process.ExitCode)" }
        return [ordered]@{exitCode=0;milliseconds=$clock.ElapsedMilliseconds;processes=$owned.ToArray()}
    } finally {
        if ($started) {
            foreach ($identity in @($owned.ToArray()) | Sort-Object startTime -Descending) {
                $live = Get-Process -Id $identity.pid -ErrorAction SilentlyContinue
                if (!$live) { continue }
                if (!(Test-DedicatedProcessIdentity $live.Id $live.StartTime $identity.pid $identity.startTime)) {
                    throw 'Installer process ownership changed; cleanup refused'
                }
                Stop-DedicatedInstallerProcess $live
            }
        }
        $stdout.Dispose(); $stderr.Dispose(); $process.Dispose()
    }
}

function Assert-DedicatedInstalledTree([string]$Staging, $Graph, $Metadata) {
    $tree = Get-DedicatedTree $Staging 2048
    $launcher = Join-Path $Staging $Graph.launcher
    if (!(Test-Path -LiteralPath $launcher -PathType Leaf)) { throw 'Official installation omitted its expected launcher' }
    if (@($tree.files | Where-Object {$_.path -match '(?i)(^|/)(level\.dat|auth\.json|accounts\.json|launcher_accounts\.json)$|ae2-crafting-time-.*\.jar$'}).Count) {
        throw 'Official installation contains a world, credential file or product artifact'
    }
    if (Test-Path -LiteralPath (Join-Path $Staging 'eula.txt')) {
        if ([IO.File]::ReadAllText((Join-Path $Staging 'eula.txt')) -match '(?im)^\s*eula\s*=\s*true') { throw 'A source must not contain EULA acceptance' }
    }
    if ($Graph.target -eq '1.20.1-fabric') {
        $archive = [IO.Compression.ZipFile]::OpenRead($launcher)
        try {
            $manifest = $archive.GetEntry('META-INF/MANIFEST.MF')
            if (!$manifest -or $manifest.Length -gt 64KB) { throw 'Fabric launcher manifest is missing or oversized' }
            $reader = [IO.StreamReader]::new($manifest.Open())
            try { $text = $reader.ReadToEnd() -replace '\r?\n ','' } finally { $reader.Dispose() }
            if ($text -notmatch '(?m)^Class-Path: ([^\r\n]+)' -or
                    $text -notmatch '(?m)^Main-Class: net\.fabricmc\.loader\.(?:impl\.)?launch\.server\.FabricServerLauncher\r?$') {
                throw 'Unsupported Fabric launcher manifest'
            }
            $classPath = [regex]::Match($text,'(?m)^Class-Path: ([^\r\n]+)').Groups[1].Value.Split(' ')
            if (@(Compare-Object @($classPath | Sort-Object) @($Metadata.fabricChecks.path | Sort-Object)).Count) {
                throw 'Fabric launcher classpath differs from its official library graph'
            }
        } finally { $archive.Dispose() }
        foreach ($library in $Metadata.fabricChecks) {
            Assert-DedicatedDigest (Join-Path $Staging $library.path) SHA1 $library.sha1
        }
    } else {
        $arguments = [IO.File]::ReadAllText($launcher)
        if ($arguments -match '(?i)-javaagent:|-agentpath:|-agentlib:|-Xbootclasspath|@[A-Za-z]|[A-Za-z]:[\\/]|\.\.[\\/]') {
            throw 'Installed launch arguments contain an outside path or executable override'
        }
        $references = @([regex]::Matches($arguments,'libraries[/\\][^\s;"'']+\.jar') | ForEach-Object Value | Select-Object -Unique)
        if (!$references.Count) { throw 'Installed launch arguments contain no verified libraries' }
        foreach ($reference in $references) {
            $file = Join-Path $Staging $reference
            Assert-DedicatedPath $file | Out-Null
            if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw 'Installed launch dependency is missing' }
        }
    }
    foreach ($document in $Metadata.documents) {
        foreach ($library in @($document.data.libraries)) {
            $artifact = $library.downloads.artifact
            if (!$artifact.path -or !$artifact.sha1) { continue }
            $relative = [string]$artifact.path
            if ([IO.Path]::IsPathRooted($relative) -or $relative -match '(^|[/\\])\.\.([/\\]|$)') { throw 'Official metadata contains an escaping library path' }
            $file = Join-Path $Staging ('libraries/' + $relative)
            if (Test-Path -LiteralPath $file -PathType Leaf) { Assert-DedicatedDigest $file SHA1 $artifact.sha1 }
        }
    }
    $serverCandidates = @($tree.files | Where-Object {$_.path -match '(^|/)server\.jar$|(^|/)server-[^/]+\.jar$|minecraft_server\.[^/]+\.jar$'})
    $verified = @($serverCandidates | Where-Object {
        (Get-FileHash -LiteralPath (Join-Path $Staging $_.path) -Algorithm SHA1).Hash -ieq $Metadata.server.sha1
    })
    if (!$verified.Count) { throw 'Installed Minecraft server does not match official version metadata' }
}
