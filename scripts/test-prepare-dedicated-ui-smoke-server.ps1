$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'dedicated-source-contract.ps1')
. (Join-Path $PSScriptRoot 'dedicated-download.ps1')
. (Join-Path $PSScriptRoot 'dedicated-install.ps1')
. (Join-Path $PSScriptRoot 'resource-prewarm-contract.ps1')
function Assert($Condition,[string]$Message) { if (!$Condition) { throw $Message } }
function Refuses([scriptblock]$Action,[string]$Message) {
    $failed=$false
    try { & $Action | Out-Null } catch { $failed=$true }
    Assert $failed $Message
}
function Json([string]$Path,$Value) { $Value | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $Path -Encoding UTF8 }
function Zip([string]$Path,[hashtable]$Entries) {
    Add-Type -AssemblyName System.IO.Compression,System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::Open($Path,[IO.Compression.ZipArchiveMode]::Create)
    try{foreach($name in $Entries.Keys){$writer=[IO.StreamWriter]::new($archive.CreateEntry($name).Open());try{$writer.Write($Entries[$name])}finally{$writer.Dispose()}}}
    finally{$archive.Dispose()}
}
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ae2ct-provisioning-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporary | Out-Null
try {
    foreach ($path in @('C:\','\\server\share\source','\\?\C:\source','C:\a\..\source','C:\source:stream','C:\con\file','C:\name.\file','relative')) {
        Refuses { Assert-DedicatedPath $path } "Accepted unsafe root $path"
    }
    Assert ((Assert-DedicatedPath $temporary) -eq $temporary) 'Local root was rejected'
    Refuses { Assert-DedicatedRoots @($temporary,(Join-Path $temporary 'inside')) @() } 'Overlapping roots accepted'
    Refuses { Assert-DedicatedRoots @($temporary) @($temporary) } 'Protected root accepted'
    Assert-DedicatedRoots @((Join-Path $temporary 'a'),(Join-Path $temporary 'b')) @((Join-Path $temporary 'protected'))
    foreach ($uri in @('http://maven.fabricmc.net/file','https://user:pass@maven.fabricmc.net/file',
            'https://maven.fabricmc.net:444/file','https://evil.invalid/file','file:///C:/file')) {
        Refuses { Assert-DedicatedUri ([uri]$uri) } 'Unsafe download origin accepted'
    }
    foreach ($hostName in @('maven.minecraftforge.net','maven.neoforged.net','maven.fabricmc.net','meta.fabricmc.net',
            'piston-meta.mojang.com','piston-data.mojang.com','launchermeta.mojang.com','launcher.mojang.com','libraries.minecraft.net')) {
        Assert-DedicatedUri ([uri]"https://$hostName/path")
    }
    Assert-DedicatedTransferBounds 512MB 2GB 512MB
    Refuses { Assert-DedicatedTransferBounds (512MB+1) 0 512MB } 'Per-file transfer cap failed'
    Refuses { Assert-DedicatedTransferBounds 1 (2GB+1) 512MB } 'Aggregate transfer cap failed'
    Assert-DedicatedInstallBounds 2048 4GB 512MB 10MB 10MB 899.99
    $nativeStart=[DateTimeOffset]::Parse('2026-09-21T10:21:01.8362606Z').UtcDateTime
    $cimStart=[DateTimeOffset]::Parse('2026-09-21T10:21:01.8362600Z').UtcDateTime
    Assert (Test-DedicatedProcessIdentity 1234 $nativeStart 1234 $cimStart) 'CIM truncation rejected the same installer parent'
    Assert (Test-DedicatedProcessIdentity 1234 $cimStart 1234 $nativeStart) 'Native cleanup rejected the same CIM-recorded child'
    Assert (Test-DedicatedProcessIdentity 1234 $nativeStart.ToLocalTime() 1234 $cimStart) 'Process identity depends on time zone representation'
    $ledger=@{pid=1234;startTime=$cimStart.ToString('o')}|ConvertTo-Json|ConvertFrom-Json
    Assert (Test-DedicatedProcessIdentity 1234 $nativeStart $ledger.pid ([DateTime]$ledger.startTime)) 'Ledger readback lost live installer ownership'
    Assert (!(Test-DedicatedProcessIdentity 1235 $nativeStart 1234 $cimStart)) 'Different installer PID accepted'
    Assert (!(Test-DedicatedProcessIdentity 0 $nativeStart 0 $cimStart)) 'Invalid installer PID accepted'
    Assert (!(Test-DedicatedProcessIdentity 1234 $nativeStart.AddSeconds(1) 1234 $cimStart)) 'Reused installer PID timestamp accepted'
    Assert (!(Test-DedicatedProcessIdentity 1234 $nativeStart.AddMilliseconds(1) 1234 $cimStart)) 'Different canonical millisecond accepted'
    $nextMillisecond=[DateTimeOffset]::Parse('2026-09-21T10:21:01.8370000Z').UtcDateTime
    Assert (!(Test-DedicatedProcessIdentity 1234 $nextMillisecond 1234 $nextMillisecond.AddTicks(-1))) 'Sliding tolerance crossed a canonical millisecond boundary'
    Assert ((Get-DedicatedProcessStartMilliseconds $cimStart) -eq (Get-DedicatedProcessStartMilliseconds $nativeStart)) 'Installer ancestry precision differs from identity checks'
    $raceProcess=Start-Process -FilePath (Get-Process -Id $PID).Path -ArgumentList '-NoProfile','-Command','Start-Sleep -Seconds 60' -WindowStyle Hidden -PassThru
    try {
        function Stop-Process { param([Diagnostics.Process]$InputObject,[switch]$Force)
            $InputObject.Kill();$InputObject.WaitForExit();throw [InvalidOperationException]::new('Process has exited') }
        Stop-DedicatedInstallerProcess $raceProcess
        Assert $raceProcess.HasExited 'Exited installer child was not accepted after the cleanup race'
    } finally {
        Remove-Item Function:Stop-Process
        if (!$raceProcess.HasExited) { $raceProcess.Kill();$raceProcess.WaitForExit() }
        $raceProcess.Dispose()
    }
    $blockedProcess=Start-Process -FilePath (Get-Process -Id $PID).Path -ArgumentList '-NoProfile','-Command','Start-Sleep -Seconds 60' -WindowStyle Hidden -PassThru
    try {
        function Stop-Process { throw [InvalidOperationException]::new('Refused to stop a live installer') }
        Refuses { Stop-DedicatedInstallerProcess $blockedProcess } 'Live installer stop failure was accepted'
        Assert (!$blockedProcess.HasExited) 'Live installer stop failure lost its process'
    } finally {
        Remove-Item Function:Stop-Process
        if (!$blockedProcess.HasExited) { $blockedProcess.Kill();$blockedProcess.WaitForExit() }
        $blockedProcess.Dispose()
    }
    foreach ($index in 0..5) {
        $values=@(2048L,4GB,512MB,10MB,10MB,899.99)
        $values[$index]++
        Refuses { Assert-DedicatedInstallBounds @values } "Installer bound $index failed"
    }
    $file=Join-Path $temporary 'data.json'; Json $file @{schema=1}
    Assert ((Read-DedicatedJson $file).schema -eq 1) 'Bounded JSON read failed'
    Refuses { Read-DedicatedJson $file 1 } 'JSON byte limit failed'
    Refuses { Read-DedicatedJson $temporary } 'Directory accepted as JSON'
    $hash=(Get-FileHash -LiteralPath $file).Hash
    Assert-DedicatedDigest $file SHA256 $hash
    Refuses { Assert-DedicatedDigest $file SHA256 ('0'*64) } 'Corrupt download accepted'
    Refuses { Assert-DedicatedDigest $file MD5 ('0'*32) } 'Weak unknown digest accepted'
    Refuses { Assert-DedicatedDigest $file SHA1 'bad' } 'Malformed digest accepted'
    $linked=Join-Path $temporary 'hardlink.json'
    New-Item -ItemType HardLink -Path $linked -Target $file | Out-Null
    Refuses { Assert-DedicatedPath $temporary -Tree } 'Hard-linked file accepted'
    Remove-Item -LiteralPath $linked
    $junction=Join-Path $temporary 'junction'; $targetDirectory=Join-Path $temporary 'junction-target'
    New-Item -ItemType Directory -Path $targetDirectory | Out-Null
    New-Item -ItemType Junction -Path $junction -Target $targetDirectory | Out-Null
    Refuses { Assert-DedicatedPath (Join-Path $junction 'new-file') } 'Linked ancestor accepted'
    Refuses { Assert-DedicatedPath $temporary -Tree } 'Linked descendant accepted'
    (Get-Item -LiteralPath $junction).Delete()

    $pins=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'run-client-versions.json') -Raw | ConvertFrom-Json
    $head='a'*40
    foreach ($target in @('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')) {
        foreach ($chemical in @($false) + $(if($target -in @('1.20.1-forge','1.21.1-neoforge')){@($true)}else{@()})) {
            $pin=@($pins | Where-Object id -eq $target)[0].compatible
            $case=Join-Path $temporary ($target + '-' + $chemical)
            $bundle=Join-Path $case 'bundle'; $mods=Join-Path $bundle 'mods'
            $javaRoot=Join-Path $case 'java'; $major=if($target -like '1.20.1-*'){17}elseif($target -eq '1.21.1-neoforge'){21}else{25}
            New-Item -ItemType Directory -Path $mods,(Join-Path $javaRoot 'bin') -Force | Out-Null
            [IO.File]::WriteAllText((Join-Path $javaRoot 'release'),"JAVA_VERSION=`"$major.0.1`"")
            [IO.File]::WriteAllText((Join-Path $javaRoot 'bin/java.exe'),'never execute PlanOnly')
            $names=@("ae2-crafting-time-test-$($target.Split('-')[1])-$($target.Split('-')[0]).jar",
                "ae2-crafting-time-test-$($target.Split('-')[1])-$($target.Split('-')[0])-test-driver.jar")
            $names+=if($target -like '*-neoforge'){"appliedenergistics2-$($pin.ae2_version).jar"}else{"appliedenergistics2-$($target.Split('-')[1])-$($pin.ae2_version).jar"}
            if($target -eq '1.20.1-fabric'){$names+="fabric-api-$($pin.fabric_api_version).jar"}
            else{$names+="guideme-$(@($pin.versions | Where-Object project_id -eq 'Ck4E7v7R')[0].version).jar"}
            if($chemical){$names+="Applied-Mekanistics-$(@($pin.versions | Where-Object project_id -eq 'IiATswDj')[0].version).jar"
                $names+="Mekanism-$($target.Split('-')[0])-$(@($pin.versions | Where-Object project_id -eq 'Ce6I4WUE')[0].version).jar"}
            foreach($name in $names){[IO.File]::WriteAllText((Join-Path $mods $name),$name)}
            Json (Join-Path $mods '.ae2-crafting-time-run-mods.json') $names
            Json (Join-Path $bundle 'profile.json') @{schema=1;target=$target;profile='compatible';java=$major
                loader=$pin.loader_version;ae2=$pin.ae2_version;dependencyMode=$(if($chemical){'catalogue'}else{'base'})}
            Json (Join-Path $bundle 'expected-adapters.json') @{}
            & (Join-Path $PSScriptRoot 'use-ui-smoke-bundle-cache.ps1') -Mode Seal -CacheDirectory $bundle -HeadSha $head `
                -Fingerprint fixed -Target $target -Profile compatible -GraphId primary -BaseOnly:(!$chemical) | Out-Null
            $preparedRoot=Join-Path $case 'prepared';New-Item -ItemType Directory -Path $preparedRoot | Out-Null
            $launch=Join-Path $preparedRoot 'launch.json'
            Json $launch @{target=$target;java=$major;arguments=@(($pin.loader_version -replace '^1\.20\.1-',''))}
            $sourceRoot=Join-Path $case 'sources';$cacheRoot=Join-Path $case 'cache';$report=Join-Path $case 'report'
            $plan=& (Join-Path $PSScriptRoot 'prepare-dedicated-ui-smoke-server.ps1') -Target $target -BundleDirectory $bundle `
                -PreparedLaunch $launch -SourceRoot $sourceRoot -CacheRoot $cacheRoot -ReportDirectory $report -JavaHome $javaRoot -PlanOnly
            Assert ($plan.graph.graph -eq $(if($chemical){'appmek'}else{'native'})) 'Wrong graph'
            Assert (!(Test-Path $sourceRoot) -and !(Test-Path $cacheRoot) -and !(Test-Path $report)) 'PlanOnly wrote directories'
            $arguments=Get-DedicatedInstallerArguments 'C:\cache\installer.jar' $plan.graph 'C:\owned\staging'
            Assert ($arguments -contains $(if($target -eq '1.20.1-fabric'){'-downloadMinecraft'}else{'--installServer'})) 'Wrong installer adapter'
            $installed=Join-Path $case 'installed';New-Item -ItemType Directory -Path (Join-Path $installed 'libraries') -Force | Out-Null
            [IO.File]::WriteAllText((Join-Path $installed 'server.jar'),'official-server')
            $library=Join-Path $installed 'libraries/native.jar';[IO.File]::WriteAllText($library,'official-library')
            $metadata=@{documents=@(@{data=@{libraries=@(@{downloads=@{artifact=@{path='native.jar';sha1=(Get-FileHash $library -Algorithm SHA1).Hash}}})}})
                fabricChecks=@(@{path='libraries/native.jar';sha1=(Get-FileHash $library -Algorithm SHA1).Hash})
                server=@{sha1=(Get-FileHash (Join-Path $installed 'server.jar') -Algorithm SHA1).Hash}}
            if($target -eq '1.20.1-fabric'){
                Zip (Join-Path $installed $plan.graph.launcher) @{'META-INF/MANIFEST.MF'="Manifest-Version: 1.0`r`nMain-Class: net.fabricmc.loader.impl.launch.server.FabricServerLauncher`r`nClass-Path: libraries/native.jar`r`n"}
            }else{
                $launcher=Join-Path $installed $plan.graph.launcher;New-Item -ItemType Directory -Path (Split-Path $launcher) -Force|Out-Null
                [IO.File]::WriteAllText($launcher,'--module-path libraries/native.jar')
            }
            Assert-DedicatedInstalledTree $installed $plan.graph $metadata
            [IO.File]::AppendAllText($library,'corrupt')
            Refuses { Assert-DedicatedInstalledTree $installed $plan.graph $metadata } 'Installed library corruption accepted'
            Refuses { & (Join-Path $PSScriptRoot 'prepare-dedicated-ui-smoke-server.ps1') -Target $target -BundleDirectory $bundle `
                -PreparedLaunch $launch -SourceRoot $sourceRoot -CacheRoot $cacheRoot -ReportDirectory $report -JavaHome $javaRoot -Offline } 'Offline cache miss executed'
            [IO.File]::AppendAllText((Join-Path $mods $names[-1]),'corrupt')
            Refuses { Get-DedicatedGraph $target $bundle $launch } 'Changed bundle accepted'
        }
    }

    $source=Join-Path $temporary 'sealed';New-Item -ItemType Directory -Path $source | Out-Null
    [IO.File]::WriteAllText((Join-Path $source 'library.jar'),'library')
    Json (Join-Path $source 'source-seal.json') (Get-DedicatedTree $source)
    Json (Join-Path $source '.ae2-crafting-time-dedicated-fixture.json') @{schema=2;role='source';sourceFixtureId='ae2-crafting-time'
        provisioning=@{schema=1;sourceKey=('a'*64);sealPath='source-seal.json';sealSha256=(Get-FileHash (Join-Path $source 'source-seal.json')).Hash}}
    Assert-DedicatedSeal $source | Out-Null
    function New-PublicationFixture([string]$Staging,[string]$FileName) {
        New-Item -ItemType Directory -Path $Staging -Force | Out-Null
        [IO.File]::WriteAllText((Join-Path $Staging $FileName),'preserve installed bytes')
        Json (Join-Path $Staging 'source-seal.json') (Get-DedicatedTree $Staging)
        Json (Join-Path $Staging '.ae2-crafting-time-dedicated-fixture.json') @{schema=2;role='source';sourceFixtureId='ae2-crafting-time'
            provisioning=@{schema=1;sourceKey=('a'*64);sealPath='source-seal.json';sealSha256=(Get-FileHash (Join-Path $Staging 'source-seal.json')).Hash}}
    }
    $publicationRoot=Join-Path $temporary 'publication';New-Item -ItemType Directory -Path $publicationRoot|Out-Null
    $destination=Join-Path $publicationRoot ('a'*64)
    $longName='x'*(260-$destination.Length-1)
    $longStaging=Join-Path $temporary 'staging-long';New-PublicationFixture $longStaging $longName
    $publication=[ordered]@{result='FAILED';source=$null;cleanup='NOT_RUN'}
    Assert ((($destination+'\'+$longName).Length) -eq 260) 'Publication boundary fixture is not exactly 260 characters'
    function Move-Item { throw 'Overlong publication reached Move-Item' }
    try {
        try { Publish-DedicatedSource $longStaging $destination $publication;throw 'Accepted 260-character published path' }
        catch { if($_.Exception.Message -notlike '*legacy MAX_PATH (260 characters)*'){throw} }
    } finally { Remove-Item Function:Move-Item }
    Assert ((Test-Path -LiteralPath $longStaging) -and !(Test-Path -LiteralPath $destination)) 'Path refusal moved or published the source'
    Assert ($publication.result -eq 'FAILED' -and !$publication.source -and $publication.cleanup -eq 'NOT_PUBLISHED') 'Path refusal reported misleading publication state'
    $shortStaging=Join-Path $temporary 'staging-short';New-PublicationFixture $shortStaging $longName.Substring(1)
    Publish-DedicatedSource $shortStaging $destination $publication
    Assert ($publication.result -eq 'PREPARED' -and $publication.cleanup -eq 'PUBLISHED' -and !$publication.quarantinedSource) '259-character publication was not accepted'
    Assert-DedicatedSeal $destination|Out-Null
    $quarantineDestination=Join-Path $publicationRoot ('b'*64)
    $quarantineStaging=Join-Path $temporary 'staging-quarantine';New-PublicationFixture $quarantineStaging 'library.jar'
    $quarantineResult=[ordered]@{result='FAILED';source=$null;cleanup='NOT_RUN'}
    $originalSeal=(Get-Command Assert-DedicatedSeal).ScriptBlock
    function Assert-DedicatedSeal([string]$Root,[switch]$AllowPendingPublication) {
        if($Root -eq $quarantineDestination){throw 'Injected post-move validation failure'}
        & $originalSeal $Root -AllowPendingPublication:$AllowPendingPublication
    }
    try { Refuses { Publish-DedicatedSource $quarantineStaging $quarantineDestination $quarantineResult } 'Post-move failure was accepted' }
    finally { Set-Item Function:Assert-DedicatedSeal -Value $originalSeal }
    Assert ($quarantineResult.result -eq 'FAILED' -and !$quarantineResult.source -and
        $quarantineResult.cleanup -eq 'RETAINED_UNVALIDATED_SOURCE' -and $quarantineResult.quarantinedSource -eq $quarantineDestination) 'Post-move failure did not identify the retained unvalidated destination'
    Assert ((Test-Path -LiteralPath $quarantineDestination) -and !(Test-Path -LiteralPath $quarantineStaging)) 'Post-move failure lost its retained source'
    Assert ((Get-Content -LiteralPath (Join-Path $quarantineDestination 'library.jar') -Raw) -eq 'preserve installed bytes') 'Quarantine changed installed evidence'
    Assert (Test-Path -LiteralPath (Join-Path $quarantineDestination '.provisioning-quarantine.json')) 'Post-move failure omitted quarantine marker'
    Refuses { Assert-DedicatedSeal $quarantineDestination } 'Quarantined publication could be reused'
    Assert-DedicatedSeal $quarantineDestination -AllowPendingPublication|Out-Null
    $writeStaging=Join-Path $temporary 'staging-marker-write';New-PublicationFixture $writeStaging 'library.jar'
    $writeDestination=Join-Path $publicationRoot ('c'*64)
    $writeResult=[ordered]@{result='FAILED';source=$null;cleanup='NOT_RUN'}
    function New-Item { throw 'Injected pending marker creation failure' }
    function Move-Item { throw 'Marker creation failure reached rename' }
    try {
        try { Publish-DedicatedSource $writeStaging $writeDestination $writeResult;throw 'Accepted pending marker creation failure' }
        catch { if($_.Exception.Message -ne 'Injected pending marker creation failure'){throw} }
    } finally { Remove-Item Function:New-Item;Remove-Item Function:Move-Item }
    Assert ((Test-Path -LiteralPath $writeStaging) -and !(Test-Path -LiteralPath $writeDestination) -and
        $writeResult.cleanup -eq 'NOT_PUBLISHED' -and !$writeResult.source) 'Marker write failure published a usable source'
    $clearStaging=Join-Path $temporary 'staging-marker-clear';New-PublicationFixture $clearStaging 'library.jar'
    $clearDestination=Join-Path $publicationRoot ('d'*64)
    $clearResult=[ordered]@{result='FAILED';source=$null;cleanup='NOT_RUN'}
    function Remove-Item { throw 'Injected pending marker clear failure' }
    try {
        try { Publish-DedicatedSource $clearStaging $clearDestination $clearResult;throw 'Accepted pending marker clear failure' }
        catch { if($_.Exception.Message -ne 'Injected pending marker clear failure'){throw} }
    } finally { Microsoft.PowerShell.Management\Remove-Item Function:Remove-Item }
    Assert ($clearResult.cleanup -eq 'RETAINED_UNVALIDATED_SOURCE' -and !$clearResult.source -and
        (Test-Path -LiteralPath (Join-Path $clearDestination '.provisioning-quarantine.json'))) 'Marker clear failure lost quarantine state'
    Assert-DedicatedSeal $clearDestination -AllowPendingPublication|Out-Null
    Refuses { Assert-DedicatedSeal $clearDestination } 'Marker clear failure allowed later reuse'
    Refuses { Get-DedicatedTree $source 0 } 'Inventory count cap failed'
    Refuses { Get-DedicatedTree $source 10 1 } 'Inventory byte cap failed'
    [IO.File]::AppendAllText((Join-Path $source 'library.jar'),'changed')
    Refuses { Assert-DedicatedSeal $source } 'Corrupt sealed tree accepted'

    $process=Get-Process -Id $PID
    $epoch=[guid]::NewGuid().ToString();$bundleDigest='b'*64
    $receipt=[pscustomobject]@{schema='1';epoch=$epoch;head=$head;bundle=$bundleDigest;pid=[string]$PID
        startTime=$process.StartTime.ToUniversalTime().ToString('o');player='446b6d0c-cadd-3e57-baf6-99d70f01a628';generation='1';serverTicks='20';worldFrames='0'}
    Assert-ResourcePrewarmReceipt $receipt $epoch $head $bundleDigest $PID $process.StartTime $true
    $timestampReceipt=$receipt.PSObject.Copy()
    foreach($representation in @($nativeStart.ToString('o'),$nativeStart,[DateTimeOffset]$nativeStart)) {
        $timestampReceipt.startTime=$representation
        Assert-ResourcePrewarmReceipt $timestampReceipt $epoch $head $bundleDigest $PID $cimStart $true
        Refuses { Assert-ResourcePrewarmReceipt $timestampReceipt $epoch $head $bundleDigest $PID $cimStart.AddMilliseconds(1) $true } 'Receipt timestamp normalization accepted a different millisecond'
    }
    foreach($key in @('schema','epoch','head','bundle','pid','startTime','player','generation','serverTicks','worldFrames')) {
        $previous=$receipt.$key;$receipt.$key='invalid'
        Refuses { Assert-ResourcePrewarmReceipt $receipt $epoch $head $bundleDigest $PID $process.StartTime $true } "Receipt field $key was not bound"
        $receipt.$key=$previous
    }
    $receipt.serverTicks='0';$receipt.worldFrames='40'
    Assert-ResourcePrewarmReceipt $receipt $epoch $head $bundleDigest $PID $process.StartTime $false
    Assert (!(Get-ResourcePrewarmWatchdog 599999 600000 $false 0 500)) 'Cold startup callback exemption failed'
    Assert ((Get-ResourcePrewarmWatchdog 600000 600000 $false 0 0) -eq 'prewarm-timeout') 'Absolute prewarm deadline failed'
    Assert (!(Get-ResourcePrewarmWatchdog 600001 600000 $true 1 20)) 'Activation did not end the cold deadline'
    Assert ((Get-ResourcePrewarmWatchdog 1 600000 $false 1 20.01) -eq 'no-callback') 'Cold callback deadline weakened'
    Assert ((Get-ResourcePrewarmWatchdog 600001 600000 $true 1 20.01) -eq 'no-callback') 'Activated callback deadline weakened'
    $control=Join-Path $temporary 'control';New-Item -ItemType Directory -Path (Join-Path $control 'prewarm') -Force|Out-Null
    $peer=Start-Process -FilePath (Get-Process -Id $PID).Path -ArgumentList '-NoProfile','-Command','Start-Sleep -Seconds 60' -WindowStyle Hidden -PassThru
    try {
        $serverReceipt=$receipt.PSObject.Copy();$serverReceipt.pid=[string]$peer.Id
        $serverReceipt.startTime=$peer.StartTime.ToUniversalTime().ToString('o');$serverReceipt.serverTicks='20';$serverReceipt.worldFrames='0'
        $update=@{Control=$control;Epoch=$epoch;Head=$head;Bundle=$bundleDigest;ServerProcessId=$peer.Id
            ServerStartedAt=$peer.StartTime;ClientProcessId=$PID;ClientStartedAt=$process.StartTime}
        Assert (!(Update-ResourcePrewarm @update)) 'Missing readiness activated'
        Json (Join-Path $control 'prewarm/server-ready.json') $serverReceipt
        Json (Join-Path $control 'prewarm/client-ready.json') $receipt
        $attemptReceipt=$receipt.PSObject.Copy();$attemptReceipt.worldFrames='0'
        Json (Join-Path $control 'prewarm/attempt.json') $attemptReceipt
        $attemptReceipt.generation='2';Json (Join-Path $control 'prewarm/attempt.json') $attemptReceipt
        Refuses { Update-ResourcePrewarm @update } 'Readiness from a different native attempt accepted'
        $attemptReceipt.generation='1';$attemptReceipt.worldFrames='40';Json (Join-Path $control 'prewarm/attempt.json') $attemptReceipt
        Refuses { Update-ResourcePrewarm @update } 'Malformed attempt readiness counters accepted'
        $attemptReceipt.worldFrames='0';Json (Join-Path $control 'prewarm/attempt.json') $attemptReceipt
        New-Item -ItemType Directory -Path (Join-Path $control 'resource')|Out-Null
        Json (Join-Path $control 'resource/state.properties') @{}
        Refuses { Update-ResourcePrewarm @update } 'Pre-arm fixture mutation accepted'
        Remove-Item -LiteralPath (Join-Path $control 'resource/state.properties')
        Assert (!(Update-ResourcePrewarm @update)) 'Arm publication pretended to be server acceptance'
        $arm=Join-Path $control 'prewarm/arm.json';$armed=Join-Path $control 'prewarm/armed.json'
        $timestamp=(Get-Item -LiteralPath $arm).LastWriteTimeUtc
        Copy-Item -LiteralPath $arm -Destination $armed
        Assert (Update-ResourcePrewarm @update) 'Matching native acceptance rejected'
        Assert ((Get-Item -LiteralPath $arm).LastWriteTimeUtc -eq $timestamp) 'Identical arm replay rewrote activation'
        $receipt.generation='2';Json (Join-Path $control 'prewarm/client-ready.json') $receipt
        Refuses { Update-ResourcePrewarm @update } 'Different connection generation accepted'
        $receipt.generation='1';Json (Join-Path $control 'prewarm/client-ready.json') $receipt
        $update.ServerStartedAt=$peer.StartTime.AddSeconds(-1)
        Refuses { Update-ResourcePrewarm @update } 'Stale process accepted'
        $update.ServerStartedAt=$peer.StartTime
        [IO.File]::AppendAllText((Join-Path $control 'prewarm/client-ready.json'),' ')
        Refuses { Update-ResourcePrewarm @update } 'Changed receipt after arm accepted'
    } finally { if (!$peer.HasExited) { $peer.Kill();$peer.WaitForExit() };$peer.Dispose() }
    Write-Host 'dedicated provisioning and prewarm contracts passed'
} finally {
    $resolved=[IO.Path]::GetFullPath($temporary)
    if (!$resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()),[StringComparison]::OrdinalIgnoreCase) -or
            [IO.Path]::GetFileName($resolved) -notlike 'ae2ct-provisioning-*') { throw 'Test cleanup escaped its owned root' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
