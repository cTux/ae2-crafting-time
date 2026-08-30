param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")]
    [string]$Target,
    [switch]$ResolveOnly,
    [string]$Root,
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
$run = Join-Path $root "versions\$Target\run"
$mods = Join-Path $run $(if ($Target -eq "1.20.1-forge") { "resolved-mods" } else { "mods" })
$manifest = Join-Path $mods ".ae2-crafting-time-run-mods.json"
New-Item -ItemType Directory -Path $mods -Force | Out-Null
$matrix = Get-Content -LiteralPath (Join-Path $PSScriptRoot "release-matrix.json") -Raw | ConvertFrom-Json
$matrixEntry = $matrix | Where-Object { $_.id -eq $Target }
$runDependencies = @($matrixEntry.runModrinthDependencies)
$projects = @($runDependencies | Select-Object -ExpandProperty project_id)
$versionPins = @{}
foreach ($dependency in $runDependencies) {
    if ($dependency.version_number) { $versionPins[$dependency.project_id] = $dependency.version_number }
}
if ($profile.Loader -ne "fabric") { $projects += "Ck4E7v7R" }
$projects += "u6dRKJwZ"

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

function Get-CompatibleVersion([string]$projectId, [string]$versionId, [string]$versionNumber = "") {
    if ($versionId) {
        return Invoke-RestMethod -Uri "$api/version/$versionId"
    }
    $game = [uri]::EscapeDataString("[`"$($profile.Game)`"]")
    $loader = [uri]::EscapeDataString("[`"$($profile.Loader)`"]")
    $versions = Invoke-RestMethod -Uri "$api/project/$projectId/version?game_versions=$game&loaders=$loader"
    if ($versionNumber) {
        $version = $versions | Where-Object { $_.version_number -eq $versionNumber } | Select-Object -First 1
        if (-not $version) { throw "No $($profile.Game) $($profile.Loader) version $versionNumber for Modrinth project $projectId" }
        return $version
    }
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

function Install-Project([string]$projectId, [string]$versionId = "", [string]$versionNumber = "") {
    if ($provided.Contains($projectId) -or -not $visited.Add($projectId)) { return }
    $version = Get-CompatibleVersion $projectId $versionId $versionNumber
    if (-not $version) {
        throw "No $($profile.Game) $($profile.Loader) version for Modrinth project $projectId"
    }
    foreach ($dependency in @($version.dependencies | Where-Object dependency_type -eq "required")) {
        Install-Project $dependency.project_id $dependency.version_id
    }
    $file = $version.files | Where-Object primary | Select-Object -First 1
    if (-not $file) { $file = $version.files | Select-Object -First 1 }
    if (-not $file) { throw "Modrinth version $($version.id) has no files" }
    Install-File $file
}

foreach ($projectId in $projects) { Install-Project $projectId "" $versionPins[$projectId] }

$loaderVersion = Get-LatestMavenVersion $profile.LoaderMetadata $profile.LoaderPrefix
$ae2Version = Get-CompatibleVersion "XxWD5pD3" ""
$runtimeArgs = @("-P$($profile.LoaderProperty)=$loaderVersion", "-P$($profile.Ae2Property)=$($ae2Version.version_number)")
Write-Host "runtime loader $loaderVersion"
Write-Host "runtime ae2 $($ae2Version.version_number)"
if ($Target -eq "1.20.1-fabric") {
    $fabricApiVersion = Get-CompatibleVersion "P7dR8mSH" ""
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
