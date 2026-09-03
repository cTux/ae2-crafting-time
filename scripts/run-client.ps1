param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")]
    [string]$Target,
    [switch]$Latest,
    [switch]$ResolveOnly,
    [switch]$Packaged,
    [string]$Root,
    [string]$VersionMatrix,
    [string]$RuntimeDirectory,
    [string]$DriverScenario,
    [string]$DriverOutputDirectory,
    [string]$DriverWorld,
    [string[]]$ProjectId,
    [switch]$Interactive,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
if ($Packaged -and -not $ResolveOnly) { throw 'Packaged preparation requires -ResolveOnly; launch copied artifacts in CodexVM' }
$javaHomes = @{}
foreach ($major in 17, 21, 25) {
    $javaHomes[$major] = & (Join-Path $PSScriptRoot 'get-java-home.ps1') -Major $major
    [Environment]::SetEnvironmentVariable("JAVA_HOME_$major", $javaHomes[$major], 'Process')
}
$clientJava = if ($Target -like '1.20.1-*') { 17 } elseif ($Target -eq '1.21.1-neoforge') { 21 } else { 25 }
$env:JAVA_HOME = $javaHomes[[Math]::Min($clientJava, 21)]
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
$GradleArgs += @('-Porg.gradle.java.installations.fromEnv=JAVA_HOME_17,JAVA_HOME_21,JAVA_HOME_25',
    '-Porg.gradle.java.installations.auto-detect=false', '-Porg.gradle.java.installations.auto-download=false')
Write-Host "runtime java $clientJava ($($javaHomes[$clientJava]))"
$api = "https://api.modrinth.com/v2"
$profiles = @{
    "1.20.1-forge" = [pscustomobject]@{
        Module = "mc_1_20_1_forge"; Game = "1.20.1"; Loader = "forge"
        LoaderMetadata = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
        LoaderPrefix = "1.20.1-"
        LoaderProperty = "runtimeForge1201Version"; Ae2Property = "runtimeAe2Forge1201Version"
    }
    "1.20.1-fabric" = [pscustomobject]@{
        Module = "fabric_1_20_1"; Game = "1.20.1"; Loader = "fabric"
        LoaderMetadata = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml"
        LoaderPrefix = ""
        LoaderProperty = "runtimeFabricLoader1201Version"; Ae2Property = "runtimeAe2Fabric1201Version"
    }
    "1.21.1-neoforge" = [pscustomobject]@{
        Module = "mc_1_21_1_neoforge"; Game = "1.21.1"; Loader = "neoforge"
        LoaderMetadata = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
        LoaderPrefix = "21.1."
        LoaderProperty = "runtimeNeoForge1211Version"; Ae2Property = "runtimeAe2NeoForge1211Version"
    }
    "26.1.2-neoforge" = [pscustomobject]@{
        Module = "mc_26_1_2_neoforge"; Game = "26.1.2"; Loader = "neoforge"
        LoaderMetadata = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
        LoaderPrefix = "26.1.2."
        LoaderProperty = "runtimeNeoForge2612Version"; Ae2Property = "runtimeAe2NeoForge2612Version"
    }
}
$provided = [Collections.Generic.HashSet[string]]::new([string[]]@("XxWD5pD3", "P7dR8mSH"))
$visited = [Collections.Generic.HashSet[string]]::new()
$managed = [Collections.Generic.List[string]]::new()
$profile = $profiles[$Target]
$root = if ($Root) { $Root } else { Split-Path -Parent $PSScriptRoot }
$runName = if ($Latest) { "run-latest" } else { "run" }
$run = if ($RuntimeDirectory) { [IO.Path]::GetFullPath($RuntimeDirectory) } else { Join-Path $root "versions\$Target\$runName" }
$mods = Join-Path $run $(if ($Target -eq "1.20.1-forge" -and -not $Packaged) { "resolved-mods" } else { "mods" })
$manifest = Join-Path $mods ".ae2-crafting-time-run-mods.json"
New-Item -ItemType Directory -Path $mods -Force | Out-Null
$matrixPath = if ($VersionMatrix) { $VersionMatrix } else { Join-Path $PSScriptRoot "run-client-versions.json" }
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
$matrixEntry = $matrix | Where-Object { $_.id -eq $Target }
if (-not $matrixEntry) { throw "No run-client version entry for $Target" }
$requestedProjects = @($ProjectId | Where-Object { $_ } | ForEach-Object { [string]$_ })
$availableProjects = @($matrixEntry.projects.project_id | ForEach-Object { [string]$_ }) +
    @($matrixEntry.curseforge.project_id | ForEach-Object { [string]$_ })
foreach ($requestedProject in $requestedProjects) {
    if ($requestedProject -notin $availableProjects) { throw "Unknown project $requestedProject for $Target" }
}
foreach ($replacement in @($matrixEntry.curseforge | Where-Object {
    $_.replaces_project_id -and ($requestedProjects.Count -eq 0 -or [string]$_.project_id -in $requestedProjects)
})) {
    if ($replacement.replaces_project_id -in $requestedProjects) {
        throw "Cannot load projects $($replacement.project_id) and $($replacement.replaces_project_id) together"
    }
    $null = $provided.Add([string]$replacement.replaces_project_id)
}
$projects = @($matrixEntry.projects | Where-Object {
    ($Latest -or $_.compatible -ne $false) -and
        ($requestedProjects.Count -eq 0 -or [string]$_.project_id -in $requestedProjects)
} | Select-Object -ExpandProperty project_id)
if (-not $Latest) {
    $excluded = @($matrixEntry.projects | Where-Object {
        $_.compatible -eq $false -and [string]$_.project_id -in $requestedProjects
    })
    if ($excluded) { throw "Focused project $($excluded[0].project_id) is excluded from the compatible profile" }
}
$versionPins = @{}
foreach ($dependency in @($matrixEntry.compatible.versions)) {
    $versionPins[$dependency.project_id] = $dependency.version_id
}

if ($Target -eq "1.20.1-forge") {
    $legacyMods = Join-Path $run "mods"
    $oldManifest = Join-Path $legacyMods ".ae2-crafting-time-run-mods.json"
    if (Test-Path -LiteralPath $oldManifest) {
        foreach ($filename in (Get-Content -LiteralPath $oldManifest -Raw | ConvertFrom-Json)) {
            Remove-Item -LiteralPath (Join-Path $legacyMods ([IO.Path]::GetFileName($filename))) -Force -ErrorAction SilentlyContinue
        }
        Remove-Item -LiteralPath $oldManifest -Force
    }
}

function Get-ProjectVersion([string]$projectId) {
    if (-not $Latest) {
        $versionId = $versionPins[$projectId]
        if (-not $versionId) { throw "Missing compatible version for Modrinth project $projectId" }
        return Invoke-RestMethod -Uri "$api/version/$versionId"
    }
    $game = [uri]::EscapeDataString("[`"$($profile.Game)`"]")
    $loader = [uri]::EscapeDataString("[`"$($profile.Loader)`"]")
    $versions = Invoke-RestMethod -Uri "$api/project/$projectId/version?game_versions=$game&loaders=$loader"
    return $versions[0]
}

function Get-LatestMavenVersion([string]$url, [string]$prefix) {
    [xml]$metadata = (Invoke-WebRequest -UseBasicParsing -Uri $url).Content
    $versions = @($metadata.metadata.versioning.versions.version | Where-Object { $_ -like "$prefix*" })
    if ($versions.Count -eq 0) { throw "No Maven version starts with $prefix at $url" }
    return $versions[-1]
}

function Get-Sha512([string]$path) {
    $algorithm = [Security.Cryptography.SHA512]::Create()
    $stream = [IO.File]::OpenRead($path)
    try { return (([BitConverter]::ToString($algorithm.ComputeHash($stream)) -replace "-", "").ToLowerInvariant()) }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}

function Install-File($file) {
    $destination = Join-Path $mods $file.filename
    $expected = $file.hashes.sha512
    if (-not (Test-Path -LiteralPath $destination) -or
            (Get-Sha512 $destination) -ne $expected) {
        $download = "$destination.download"
        Invoke-WebRequest -UseBasicParsing -Uri $file.url -OutFile $download
        if ((Get-Sha512 $download) -ne $expected) {
            Remove-Item -LiteralPath $download -Force
            throw "Hash mismatch for $($file.filename)"
        }
        Move-Item -LiteralPath $download -Destination $destination -Force
    }
    $managed.Add($file.filename)
    Write-Host "mod $($file.filename)"
}

function Install-Project([string]$projectId) {
    if ($provided.Contains($projectId) -or -not $visited.Add($projectId)) { return }
    $version = Get-ProjectVersion $projectId
    if (-not $version) {
        throw "No $($profile.Game) $($profile.Loader) version for Modrinth project $projectId"
    }
    foreach ($dependency in @($version.dependencies | Where-Object dependency_type -eq "required")) {
        Install-Project $dependency.project_id
    }
    $project = $matrixEntry.projects | Where-Object project_id -eq $projectId | Select-Object -First 1
    foreach ($dependency in @($project.modrinth_dependencies | Where-Object { $_ })) {
        Install-Project $dependency
    }
    $file = $version.files | Where-Object primary | Select-Object -First 1
    if (-not $file) { $file = $version.files | Select-Object -First 1 }
    if (-not $file) { throw "Modrinth version $($version.id) has no files" }
    Install-File $file
}

foreach ($projectId in $projects) { Install-Project $projectId }
if ($DriverScenario -and $requestedProjects.Count) {
    foreach ($projectId in @($matrixEntry.test_driver_projects | Where-Object { $_ })) { Install-Project $projectId }
}
$curseforgeProjects = [Collections.Generic.HashSet[string]]::new()
function Add-CurseForgeProject([string]$projectId) {
    if (-not $curseforgeProjects.Add($projectId)) { return }
    $dependency = $matrixEntry.curseforge | Where-Object { [string]$_.project_id -eq $projectId } | Select-Object -First 1
    foreach ($requiredProject in @($dependency.dependencies)) { Add-CurseForgeProject ([string]$requiredProject) }
}
if ($requestedProjects.Count) {
    foreach ($requestedProject in $requestedProjects) {
        if ($requestedProject -in @($matrixEntry.curseforge.project_id | ForEach-Object { [string]$_ })) {
            Add-CurseForgeProject $requestedProject
        }
    }
} else {
    foreach ($dependency in @($matrixEntry.curseforge | Where-Object { $_ })) {
        $null = $curseforgeProjects.Add([string]$dependency.project_id)
    }
}
foreach ($dependency in @($matrixEntry.curseforge | Where-Object { [string]$_.project_id -in $curseforgeProjects })) {
    foreach ($projectId in @($dependency.modrinth_dependencies | Where-Object { $_ })) { Install-Project $projectId }
    $file = if ($Latest) { $dependency.latest } else { $dependency.compatible }
    $fileId = [string]$file.file_id
    $group = $fileId.Substring(0, 4)
    $rest = $fileId.Substring(4)
    Install-File ([pscustomobject]@{
        filename = $file.filename
        hashes = [pscustomobject]@{ sha512 = $file.sha512 }
        url = "https://mediafilez.forgecdn.net/files/$group/$rest/$([uri]::EscapeDataString($file.filename))"
    })
}

$loaderVersion = if ($Latest) { Get-LatestMavenVersion $profile.LoaderMetadata $profile.LoaderPrefix } else { $matrixEntry.compatible.loader_version }
if ($Latest) {
    $game = [uri]::EscapeDataString("[`"$($profile.Game)`"]")
    $loader = [uri]::EscapeDataString("[`"$($profile.Loader)`"]")
    $ae2Versions = Invoke-RestMethod -Uri "$api/project/XxWD5pD3/version?game_versions=$game&loaders=$loader"
    $ae2Version = $ae2Versions[0]
} else {
    $ae2Version = Invoke-RestMethod -Uri "$api/version/$($matrixEntry.compatible.ae2_version_id)"
}
if (-not $Latest -and $ae2Version.version_number -ne $matrixEntry.compatible.ae2_version) {
    throw "AE2 version lock mismatch for $Target"
}
foreach ($dependency in @($ae2Version.dependencies | Where-Object dependency_type -eq "required")) {
    Install-Project $dependency.project_id
}
$runtimeArgs = @("-P$($profile.LoaderProperty)=$loaderVersion", "-P$($profile.Ae2Property)=$($ae2Version.version_number)", "-PruntimeRunDirectory=$run")
Write-Host "profile $(if ($Latest) { 'latest' } else { 'compatible' })"
if ($requestedProjects.Count) { Write-Host "focused projects $($requestedProjects -join ', ')" }
Write-Host "runtime loader $loaderVersion"
Write-Host "runtime ae2 $($ae2Version.version_number)"
if ($Target -eq "1.20.1-fabric") {
    if ($Latest) {
        $game = [uri]::EscapeDataString("[`"$($profile.Game)`"]")
        $loader = [uri]::EscapeDataString("[`"$($profile.Loader)`"]")
        $fabricApiVersions = Invoke-RestMethod -Uri "$api/project/P7dR8mSH/version?game_versions=$game&loaders=$loader"
        $fabricApiVersion = $fabricApiVersions[0]
    } else {
        $fabricApiVersion = Invoke-RestMethod -Uri "$api/version/$($matrixEntry.compatible.fabric_api_version_id)"
    }
    if (-not $Latest -and $fabricApiVersion.version_number -ne $matrixEntry.compatible.fabric_api_version) {
        throw "Fabric API version lock mismatch for $Target"
    }
    $runtimeArgs += "-PruntimeFabricApi1201Version=$($fabricApiVersion.version_number)"
    Write-Host "runtime fabric-api $($fabricApiVersion.version_number)"
}
if ($Target -eq "1.21.1-neoforge") {
    $runtimeArgs += "-PruntimeAe2NeoForge1211Group=org.appliedenergistics"
    $runtimeArgs += "-PruntimeLatestNeoForge1211"
    Write-Host "runtime ae2 group org.appliedenergistics"
}

if ($DriverScenario) {
    if ($Target -notin @("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge") -or -not $DriverOutputDirectory -or -not $DriverWorld) {
        throw "Test-driver scenarios require a supported target, an output directory, and a disposable world"
    }
    $runtimeArgs += "-PtestDriverScenario=$DriverScenario"
    $runtimeArgs += "-PtestDriverProfile=$(if ($Latest) { 'latest' } else { 'compatible' })"
    $runtimeArgs += "-PtestDriverOutput=$([IO.Path]::GetFullPath($DriverOutputDirectory))"
    $runtimeArgs += "-PtestDriverWorld=$DriverWorld"
    if ($Interactive) { $runtimeArgs += "-PtestDriverInteractive=true" }
}

if ($Packaged) {
    Install-File ($ae2Version.files | Where-Object primary | Select-Object -First 1)
    if ($Target -eq '1.20.1-fabric') {
        Install-File ($fabricApiVersion.files | Where-Object primary | Select-Object -First 1)
    }
    [ordered]@{ schema = 1; target = $Target; profile = $(if ($Latest) { 'latest' } else { 'compatible' })
        loader = $loaderVersion; ae2 = $ae2Version.version_number; java = $clientJava
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $run 'profile.json') -Encoding UTF8
}

if ($Target -eq "1.20.1-forge") {
    foreach ($filename in $managed) {
        Remove-Item -LiteralPath (Join-Path $legacyMods $filename) -Force -ErrorAction SilentlyContinue
    }
}

$previous = if (Test-Path -LiteralPath $manifest) { Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json } else { @() }
foreach ($filename in $previous) {
    if ($managed -notcontains $filename) {
        Remove-Item -LiteralPath (Join-Path $mods ([IO.Path]::GetFileName($filename))) -Force -ErrorAction SilentlyContinue
    }
}
foreach ($pattern in @("ae2ct-*.jar", "jei-*.jar")) {
    Get-ChildItem -Path (Join-Path $mods $pattern) -File -ErrorAction SilentlyContinue |
        Where-Object Name -NotIn $managed |
        Remove-Item -Force
}

if ($Target -in @("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")) {
    $modVersion = ((Get-Content -LiteralPath (Join-Path $root "gradle.properties")) |
        Where-Object { $_ -match '^modVersion=' } | Select-Object -First 1) -replace '^modVersion=', ''
    if (-not $modVersion) { throw "Missing modVersion in gradle.properties" }
    $driverName = "ae2-crafting-time-$modVersion-$($profile.Loader)-$($profile.Game)-test-driver.jar"
    $buildTasks = @("$($profile.Module):testDriverJar")
    if ($Packaged) { $buildTasks += "$($profile.Module):distMod" }
    & (Join-Path $root "gradlew.bat") @buildTasks @runtimeArgs @GradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Test-driver build failed" }
    $driverArtifact = Join-Path $root "build\test-driver\$driverName"
    if (-not (Test-Path -LiteralPath $driverArtifact -PathType Leaf)) {
        throw "Missing exact test-driver artifact $driverArtifact"
    }
    Get-ChildItem -LiteralPath $mods -Filter "ae2-crafting-time-*-$($profile.Loader)-$($profile.Game)-test-driver.jar" -File |
        Where-Object Name -ne $driverName | Remove-Item -Force
    Copy-Item -LiteralPath $driverArtifact -Destination (Join-Path $mods $driverName) -Force
    $managed.Add($driverName)
    if ($Packaged) {
        $productionName = "ae2-crafting-time-$modVersion-$($profile.Loader)-$($profile.Game).jar"
        Copy-Item -LiteralPath (Join-Path $root "dist\$productionName") -Destination (Join-Path $mods $productionName) -Force
        $managed.Add($productionName)
    }
    Write-Host "mod $driverName"
}
[IO.File]::WriteAllText($manifest, ($managed | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
Write-Host "mod AE2 Crafting Time (Gradle source set :$($profile.Module))"

if (-not $ResolveOnly) {
    & (Join-Path $root "gradlew.bat") ":$($profile.Module):runClient" @runtimeArgs @GradleArgs
    exit $LASTEXITCODE
}
