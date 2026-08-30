param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")]
    [string]$Target,
    [switch]$Latest,
    [switch]$ResolveOnly,
    [string]$Root,
    [string]$VersionMatrix,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
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
$run = Join-Path $root "versions\$Target\$runName"
$mods = Join-Path $run $(if ($Target -eq "1.20.1-forge") { "resolved-mods" } else { "mods" })
$manifest = Join-Path $mods ".ae2-crafting-time-run-mods.json"
New-Item -ItemType Directory -Path $mods -Force | Out-Null
$matrixPath = if ($VersionMatrix) { $VersionMatrix } else { Join-Path $PSScriptRoot "run-client-versions.json" }
$matrix = Get-Content -LiteralPath $matrixPath -Raw | ConvertFrom-Json
$matrixEntry = $matrix | Where-Object { $_.id -eq $Target }
if (-not $matrixEntry) { throw "No run-client version entry for $Target" }
$projects = @($matrixEntry.projects | Where-Object { $Latest -or $_.compatible -ne $false } | Select-Object -ExpandProperty project_id)
$versionPins = @{}
foreach ($dependency in @($matrixEntry.compatible.versions)) {
    $versionPins[$dependency.project_id] = $dependency.version_id
}

if ($Target -eq "1.20.1-forge") {
    $legacyMods = Join-Path $run "mods"
    $oldManifest = Join-Path $legacyMods ".ae2-crafting-time-run-mods.json"
    if (Test-Path -LiteralPath $oldManifest) {
        foreach ($filename in @(Get-Content -LiteralPath $oldManifest -Raw | ConvertFrom-Json)) {
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
    $file = $version.files | Where-Object primary | Select-Object -First 1
    if (-not $file) { $file = $version.files | Select-Object -First 1 }
    if (-not $file) { throw "Modrinth version $($version.id) has no files" }
    Install-File $file
}

foreach ($projectId in $projects) { Install-Project $projectId }
foreach ($dependency in @($matrixEntry.curseforge | Where-Object { $_ })) {
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
$runtimeArgs = @("-P$($profile.LoaderProperty)=$loaderVersion", "-P$($profile.Ae2Property)=$($ae2Version.version_number)", "-PruntimeRunDirectory=$runName")
Write-Host "profile $(if ($Latest) { 'latest' } else { 'compatible' })"
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

if ($Target -eq "1.20.1-forge") {
    foreach ($filename in $managed) {
        Remove-Item -LiteralPath (Join-Path $legacyMods $filename) -Force -ErrorAction SilentlyContinue
    }
}

$previous = if (Test-Path -LiteralPath $manifest) { @(Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json) } else { @() }
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
[IO.File]::WriteAllText($manifest, ($managed | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
Write-Host "mod AE2 Crafting Time (Gradle source set :$($profile.Module))"

if (-not $ResolveOnly) {
    & (Join-Path $root "gradlew.bat") ":$($profile.Module):runClient" @runtimeArgs @GradleArgs
    exit $LASTEXITCODE
}
