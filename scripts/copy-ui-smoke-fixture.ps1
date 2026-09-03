param(
    [Parameter(Mandatory)][string]$Source,
    [Parameter(Mandatory)][string]$Destination,
    [Parameter(Mandatory)][string]$Target,
    [switch]$VanillaMetadata
)
$ErrorActionPreference = 'Stop'
Copy-Item -LiteralPath $Source -Destination $Destination -Recurse
if ($Target -eq '1.20.1-fabric' -or ($Target -eq '1.20.1-forge' -and $VanillaMetadata)) {
    # Keep the marked Forge layout, but use native Fabric world metadata.
    # Forge's level.dat requires Blood Magic dimensions unavailable on Fabric.
    $metadata = Join-Path (Split-Path -Parent $PSScriptRoot) 'versions/1.20.1-fabric/run/saves/ae2-crafting-time/level.dat'
    Copy-Item -LiteralPath $metadata -Destination (Join-Path $Destination 'level.dat') -Force
    Copy-Item -LiteralPath $metadata -Destination (Join-Path $Destination 'level.dat_old') -Force
}
