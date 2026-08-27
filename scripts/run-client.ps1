param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")]
    [string]$Target,
    [switch]$ResolveOnly,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
$api = "https://api.modrinth.com/v2"
$profiles = @{
    "1.20.1-forge" = [pscustomobject]@{
        Module = "mc_1_20_1_forge"; Game = "1.20.1"; Loader = "forge"
        Projects = @("Ck4E7v7R", "a1RwDz90", "IiATswDj", "E6BFl96N", "u6dRKJwZ")
    }
    "1.20.1-fabric" = [pscustomobject]@{
        Module = "fabric_1_20_1"; Game = "1.20.1"; Loader = "fabric"
        Projects = @("E6BFl96N", "u6dRKJwZ")
    }
    "1.21.1-neoforge" = [pscustomobject]@{
        Module = "mc_1_21_1_neoforge"; Game = "1.21.1"; Loader = "neoforge"
        Projects = @("Ck4E7v7R", "a1RwDz90", "IiATswDj", "rxYaglEe", "E6BFl96N", "u6dRKJwZ")
    }
    "26.1.2-neoforge" = [pscustomobject]@{
        Module = "mc_26_1_2_neoforge"; Game = "26.1.2"; Loader = "neoforge"
        Projects = @("rxYaglEe", "u6dRKJwZ")
    }
}
$provided = [Collections.Generic.HashSet[string]]::new([string[]]@("XxWD5pD3", "P7dR8mSH"))
$visited = [Collections.Generic.HashSet[string]]::new()
$managed = [Collections.Generic.List[string]]::new()
$profile = $profiles[$Target]
$root = Split-Path -Parent $PSScriptRoot
$run = Join-Path $root "versions\$Target\run"
$mods = Join-Path $run $(if ($Target -eq "1.20.1-forge") { "resolved-mods" } else { "mods" })
$manifest = Join-Path $mods ".ae2-crafting-time-run-mods.json"
New-Item -ItemType Directory -Path $mods -Force | Out-Null

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

function Get-CompatibleVersion([string]$projectId, [string]$versionId) {
    if ($versionId) {
        return Invoke-RestMethod -Uri "$api/version/$versionId"
    }
    $game = [uri]::EscapeDataString("[`"$($profile.Game)`"]")
    $loader = [uri]::EscapeDataString("[`"$($profile.Loader)`"]")
    $versions = @(Invoke-RestMethod -Uri "$api/project/$projectId/version?game_versions=$game&loaders=$loader")
    $release = $versions | Where-Object version_type -eq "release" | Select-Object -First 1
    if ($release) { return $release }
    return $versions | Select-Object -First 1
}

function Install-File($file) {
    $destination = Join-Path $mods $file.filename
    $expected = $file.hashes.sha512
    if (-not (Test-Path -LiteralPath $destination) -or
            (Get-FileHash -LiteralPath $destination -Algorithm SHA512).Hash.ToLowerInvariant() -ne $expected) {
        $download = "$destination.download"
        Invoke-WebRequest -UseBasicParsing -Uri $file.url -OutFile $download
        if ((Get-FileHash -LiteralPath $download -Algorithm SHA512).Hash.ToLowerInvariant() -ne $expected) {
            Remove-Item -LiteralPath $download -Force
            throw "Hash mismatch for $($file.filename)"
        }
        Move-Item -LiteralPath $download -Destination $destination -Force
    }
    $managed.Add($file.filename)
    Write-Host "mod $($file.filename)"
}

function Install-Project([string]$projectId, [string]$versionId = "") {
    if ($provided.Contains($projectId) -or -not $visited.Add($projectId)) { return }
    $version = Get-CompatibleVersion $projectId $versionId
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

foreach ($projectId in $profile.Projects) { Install-Project $projectId }

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
    & (Join-Path $root "gradlew.bat") ":$($profile.Module):runClient" @GradleArgs
    exit $LASTEXITCODE
}
