# Dependencies

AE2 Crafting Time is shipped as separate jars per Minecraft loader. Use the jar
that matches the loader and Minecraft version in your instance.

## Strict Dependencies

These dependencies are required. The mod will not load without the matching row
for its target jar.

| Target jar | Required dependencies |
| --- | --- |
| `1.20.1 Forge` | Minecraft `[1.20.1,1.21)`, Forge `[47,)`, Applied Energistics 2 `[15.0.0,16.0.0)` |
| `1.20.1 Fabric` | Minecraft `1.20.1`, Fabric Loader `>=0.16.0`, Fabric API, Java `>=17`, Applied Energistics 2 `>=15.0.0` |
| `1.21.1 NeoForge` | Minecraft `[1.21.1]`, NeoForge `[21.1.1,)`, Applied Energistics 2 `[19.0.0,20.0.0)` |

## Optional Dependencies

These dependencies are detected when present. They are not required to load AE2
Crafting Time.

| Dependency | Targets | Version range | What it enables |
| --- | --- | --- | --- |
| AE2: Crafting Tree (`ae2ct`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | Forge/Fabric: `>=1.20.1-1.1.1`; NeoForge: `>=1.21.1` | TTC lines and details in AE2: Crafting Tree UI. |
| Applied Mekanistics (`appmek`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=1.4.0`; NeoForge: `>=1.6.0` | TTC profiling and display for Applied Mekanistics chemical keys. |
| ME Requester (`merequester`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | Forge/Fabric: `>=1.20.1`; NeoForge: `>=1.21.1` | TTC row labels and total TTC hints in ME Requester screens. |

Applied Mekanistics is not declared for the Fabric jar because the supported
Applied Mekanistics releases for these targets are Forge/NeoForge.

## Build Tools

Building the project requires Java 17 for the 1.20.1 modules and Java 21 for
the 1.21.1 NeoForge module.
