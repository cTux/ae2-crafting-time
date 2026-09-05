param(
    [Parameter(Mandatory)][string]$Source,
    [Parameter(Mandatory)][string]$Destination,
    [Parameter(Mandatory)][string]$Target,
    [string]$Scenario = 'craft-plan',
    [switch]$VanillaMetadata
)
$ErrorActionPreference = 'Stop'
if (Test-Path -LiteralPath $Destination) { throw 'Fixture destination must be a new directory' }
$standardCases = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'ui-smoke-groups.json') -Raw | ConvertFrom-Json).groups.'standard-ae2'
if ($Scenario -cin $standardCases) {
    # These scenarios construct their own grids; saved chunks can contain incompatible CPU jobs.
    New-Item -ItemType Directory -Path $Destination | Out-Null
    foreach ($name in @('level.dat', '.ae2-crafting-time-test-fixture.json')) {
        Copy-Item -LiteralPath (Join-Path $Source $name) -Destination $Destination
    }
    $backup = Join-Path $Source 'level.dat_old'
    if (Test-Path -LiteralPath $backup) { Copy-Item -LiteralPath $backup -Destination $Destination }
    $worldGeneration = Join-Path $Source 'data/minecraft/world_gen_settings.dat'
    if (Test-Path -LiteralPath $worldGeneration) {
        $metadataDirectory = Join-Path $Destination 'data/minecraft'
        New-Item -ItemType Directory -Path $metadataDirectory -Force | Out-Null
        Copy-Item -LiteralPath $worldGeneration -Destination $metadataDirectory
    }
} else {
    Copy-Item -LiteralPath $Source -Destination $Destination -Recurse
}
if ($Target -eq '1.20.1-fabric' -or ($Target -eq '1.20.1-forge' -and $VanillaMetadata)) {
    # Keep the fixture coordinates, but use native Fabric world metadata.
    # Forge's level.dat requires Blood Magic dimensions unavailable on Fabric.
    $metadata = Join-Path (Split-Path -Parent $PSScriptRoot) 'versions/1.20.1-fabric/run/saves/ae2-crafting-time/level.dat'
    Copy-Item -LiteralPath $metadata -Destination (Join-Path $Destination 'level.dat') -Force
    Copy-Item -LiteralPath $metadata -Destination (Join-Path $Destination 'level.dat_old') -Force
}
