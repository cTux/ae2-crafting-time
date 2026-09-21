param(
    [Parameter(Mandatory)][ValidateSet('1.20.1-forge','1.20.1-fabric','1.21.1-neoforge','26.1.2-neoforge')][string]$Target,
    [Parameter(Mandatory)][string]$BundleDirectory,
    [Parameter(Mandatory)][string]$PreparedLaunch,
    [Parameter(Mandatory)][string]$SourceRoot,
    [Parameter(Mandatory)][string]$CacheRoot,
    [Parameter(Mandatory)][string]$ReportDirectory,
    [string]$JavaHome,
    [switch]$PlanOnly,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'dedicated-source-contract.ps1')
. (Join-Path $PSScriptRoot 'dedicated-download.ps1')
. (Join-Path $PSScriptRoot 'dedicated-install.ps1')
$source = Assert-DedicatedPath $SourceRoot -Tree
$cache = Assert-DedicatedPath $CacheRoot -Tree
$report = Assert-DedicatedPath $ReportDirectory
$bundle = Assert-DedicatedPath $BundleDirectory -Tree
$prepared = Assert-DedicatedPath $PreparedLaunch
$launch = Read-DedicatedJson $prepared
$protected = @((Split-Path -Parent $PSScriptRoot),$bundle,(Split-Path -Parent $prepared))
if ($launch.guest) { $protected += [string]$launch.guest }
Assert-DedicatedRoots @($source,$cache,$report) $protected
if (Test-Path -LiteralPath $report) { throw 'Provisioning requires new report evidence' }
$graph = Get-DedicatedGraph $Target $bundle $prepared
if (!$JavaHome) {
    $JavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME_$($graph.java)")
    if (!$JavaHome) { $JavaHome = (Get-ItemProperty -LiteralPath 'HKCU:\Environment' -ErrorAction SilentlyContinue).("JAVA_HOME_$($graph.java)") }
}
if (!$JavaHome) { throw "An existing Java $($graph.java) is required" }
$javaRoot = Assert-DedicatedPath $JavaHome
$java = Join-Path $javaRoot 'bin/java.exe'
$release = Join-Path $javaRoot 'release'
if (!(Test-Path -LiteralPath $java -PathType Leaf) -or !(Test-Path -LiteralPath $release -PathType Leaf) -or
        [IO.File]::ReadAllText($release) -notmatch "(?m)^JAVA_VERSION=`"$($graph.java)(?:\.|`"|-)" ) {
    throw 'Installed Java release identity does not match the target'
}
$inputKey = Get-DedicatedSha256 ((($graph.target,$graph.java,$graph.loader,$graph.installerUrl) -join '|') +
    "`n" + (($graph.dependencies | ForEach-Object {"$($_.name)|$($_.sha256)"}) -join "`n"))
$hit = $null
foreach ($candidate in @(Get-ChildItem -LiteralPath $source -Directory -ErrorAction SilentlyContinue)) {
    if ($candidate.Name -cnotmatch '^[a-f0-9]{64}$') { continue }
    $marker = Read-DedicatedJson (Join-Path $candidate.FullName '.ae2-crafting-time-dedicated-fixture.json')
    if ($marker.provisioning.inputKey -cne $inputKey) { continue }
    $marker = Assert-DedicatedSeal $candidate.FullName
    if ($marker.target -cne $graph.target -or $marker.javaMajor -ne $graph.java -or $marker.loader -cne $graph.loader -or
            (($marker.dependencies | ForEach-Object {"$($_.name)|$($_.sha256)"}) -join "`n") -cne
            (($graph.dependencies | ForEach-Object {"$($_.name)|$($_.sha256)"}) -join "`n")) {
        throw 'Cached source does not match the complete requested graph'
    }
    if ($marker.provisioning.sourceKey -cne $candidate.Name) { throw 'Source directory identity mismatch' }
    $hit = $candidate.FullName
    break
}
if ($Offline -and !$hit) { throw 'Offline preparation requires a complete verified sealed-source hit' }
$plan = [ordered]@{schema=1;target=$Target;graph=$graph;sourceRoot=$source;cacheRoot=$cache;reportDirectory=$report
    java=$java;sourceHit=$hit;offline=[bool]$Offline;requiredDownloads=$(if ($hit) {@()} else {@($graph.installerUrl,
        ($graph.installerUrl + '.sha256'),'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')})
    limits=[ordered]@{downloadFiles=2048;fileBytes=512MB;downloadBytes=2GB;metadataBytes=10MB
        connectSeconds=30;transferSeconds=120;transferAttempts=2;installSeconds=900;stagingBytes=4GB
        newCacheBytes=4GB;requiredFreeBytes=12GB};knownDownloadBytes=$null;maximumDownloadBytes=2GB}
if ($PlanOnly) { return [pscustomobject]$plan }
foreach ($root in @($source,$cache,$report)) { New-Item -ItemType Directory -Path $root -Force | Out-Null }
$plan | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $report 'provisioning-plan.json') -Encoding UTF8
if ($hit) {
    [ordered]@{schema=1;result='REUSED';source=$hit;sourceMarkerSha256=(Get-FileHash -LiteralPath (Join-Path $hit '.ae2-crafting-time-dedicated-fixture.json')).Hash
        inputKey=$inputKey;downloadBytes=0;installerStarted=$false;eulaWritten=$false} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $report 'provisioning-evidence.json') -Encoding UTF8
    return $hit
}
foreach ($root in @($source,$cache)) {
    $drive = [IO.DriveInfo]::new([IO.Path]::GetPathRoot($root))
    if ($drive.DriveFormat -cne 'NTFS' -or $drive.AvailableFreeSpace -lt 12GB) { throw 'Provisioning requires local NTFS and 12 GiB free' }
}
$staging = Join-Path $source ('.staging-' + [guid]::NewGuid().ToString('N'))
$budget = New-DedicatedTransferBudget
$result = [ordered]@{schema=1;result='FAILED';inputKey=$inputKey;target=$Target;graph=$graph.graph
    eulaWritten=$false;source=$null;staging=$staging;cleanup='NOT_RUN';installerStarted=$false}
New-Item -ItemType Directory -Path $staging | Out-Null
$ownership = Join-Path $staging '.provisioning-owner.json'
[ordered]@{schema=1;id=[guid]::NewGuid().ToString('N');path=$staging;pid=$PID
    startTime=(Get-Process -Id $PID).StartTime.ToUniversalTime().ToString('o')} |
    ConvertTo-Json | Set-Content -LiteralPath $ownership -Encoding UTF8
try {
    $installer = Get-DedicatedInstaller $graph.installerUrl $cache $report $budget
    $result.installer = $installer.provenance
    $result.installerCacheHit = $installer.cacheHit
    $installer.provenance | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $report 'installer-provenance.json') -Encoding UTF8
    $metadata = Get-DedicatedInstallMetadata $installer.path $graph $report $budget
    $result.metadata = $metadata.identities
    $result.installerStarted = $true
    $install = Invoke-DedicatedInstaller $java $installer.path $graph $staging $report
    $result.installation = $install
    Assert-DedicatedInstalledTree $staging $graph $metadata
    $mods = Join-Path $staging 'mods'
    New-Item -ItemType Directory -Path $mods | Out-Null
    foreach ($dependency in $graph.dependencies) {
        Assert-DedicatedPath $staging -Tree | Out-Null
        $from = Join-Path $bundle ('mods/' + $dependency.name)
        Assert-DedicatedDigest $from SHA256 $dependency.sha256
        Copy-Item -LiteralPath $from -Destination (Join-Path $mods $dependency.name)
        Assert-DedicatedDigest (Join-Path $mods $dependency.name) SHA256 $dependency.sha256
    }
    Copy-Item -LiteralPath (Join-Path $report 'installer-provenance.json') -Destination (Join-Path $staging 'installer-provenance.json')
    $key = Get-DedicatedSha256 ($inputKey + '|' + $installer.provenance.sha256 + '|' +
        (($metadata.identities | ForEach-Object {"$($_.name)|$($_.sha256)"}) -join "`n"))
    $destination = Join-Path $source $key
    if (Test-Path -LiteralPath $destination) { throw 'Source publication would overwrite an existing source' }
    Assert-DedicatedPath $staging -Tree | Out-Null
    $tree = Get-DedicatedTree $staging
    $sealText = $tree | ConvertTo-Json -Depth 8
    if ([Text.Encoding]::UTF8.GetByteCount($sealText) -gt 10MB) { throw 'Source seal exceeds 10 MiB' }
    $sealText | Set-Content -LiteralPath (Join-Path $staging 'source-seal.json') -Encoding UTF8
    $marker = [ordered]@{schema=2;sourceFixtureId='ae2-crafting-time';role='source';target=$Target
        javaMajor=$graph.java;loader=$graph.loader;dependencies=$graph.dependencies
        launcher=[ordered]@{path=$graph.launcher;sha256=(Get-FileHash -LiteralPath (Join-Path $staging $graph.launcher)).Hash}
        provisioning=[ordered]@{schema=1;inputKey=$inputKey;sourceKey=$key;sealPath='source-seal.json'
            sealSha256=(Get-FileHash -LiteralPath (Join-Path $staging 'source-seal.json')).Hash
            installer=$installer.provenance;metadata=$metadata.identities}}
    $marker | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $staging '.ae2-crafting-time-dedicated-fixture.json') -Encoding UTF8
    Assert-DedicatedSeal $staging | Out-Null
    foreach ($file in Get-ChildItem -LiteralPath $staging -File -Recurse -Force) { $file.IsReadOnly = $true }
    Move-Item -LiteralPath $staging -Destination $destination
    Assert-DedicatedSeal $destination | Out-Null
    $result.source = $destination; $result.result = 'PREPARED'; $result.cleanup = 'PUBLISHED'
    $result.sourceMarkerSha256 = (Get-FileHash -LiteralPath (Join-Path $destination '.ae2-crafting-time-dedicated-fixture.json')).Hash
} catch {
    $result.failure = $_.Exception.Message
    throw
} finally {
    $result.downloadBytes = $budget.bytes; $result.transfers = $budget.receipts.ToArray()
    if (Test-Path -LiteralPath $staging) {
        Assert-DedicatedPath $staging -Tree | Out-Null
        $processLedger = Join-Path $report 'installer-processes.json'
        if (Test-Path -LiteralPath $processLedger) {
            foreach ($identity in @(Read-DedicatedJson $processLedger 64KB)) {
                $live = Get-Process -Id $identity.pid -ErrorAction SilentlyContinue
                if ($live -and (Test-DedicatedProcessIdentity $live.Id $live.StartTime $identity.pid ([DateTime]$identity.startTime))) {
                    $result.cleanup = 'RETAINED_LIVE_INSTALLER'
                    $result | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $report 'provisioning-evidence.json') -Encoding UTF8
                    throw 'Owned installer remains live; staging cleanup refused'
                }
            }
        }
        if (!(Test-Path -LiteralPath $ownership)) {
            $result.cleanup = 'RETAINED_UNSEALED_STAGING'
        } else {
            $owner = Read-DedicatedJson $ownership 64KB
            if ($owner.path -cne $staging -or $owner.pid -ne $PID) { throw 'Staging ownership changed; cleanup refused' }
            Remove-Item -LiteralPath $staging -Recurse -Force
            $result.cleanup = 'REMOVED_OWNED_STAGING'
        }
    }
    $result | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $report 'provisioning-evidence.json') -Encoding UTF8
}
return $result.source
