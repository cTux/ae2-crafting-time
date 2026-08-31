# Mod automation coverage

This table combines the supported dependency ranges with the current TestDriver
coverage. `Yes (base)` means the Forge 1.20.1 driver exercises the dependency as
part of its standard AE2 scenario. `Yes (scenario)` means it has a dedicated
optional-mod scenario. `No` means the dependency is supported for that target,
but no TestDriver covers it. `—` means the dependency is not part of that target.

| Mod Name | 1.20.1-forge | 1.20.1-forge TestDriver | 1.20.1-fabric | 1.20.1-fabric TestDriver | 1.21.1-neoforge | 1.21.1-neoforge TestDriver | 26.1.2-neoforge | 26.1.2-neoforge TestDriver |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Minecraft (required) | `[1.20.1,1.21)` | Yes (base) | `1.20.1` | No | `[1.21.1]` | No | `[26.1.2]` | No |
| Java (required) | `>=17` | Yes (base) | `>=17` | No | `>=21` | No | `>=25` | No |
| Forge (required) | `>=47.1.3` | Yes (base) | — | — | — | — | — | — |
| Fabric Loader (required) | — | — | `>=0.14.21` | No | — | — | — | — |
| Fabric API (required) | — | — | `>=0.83.1` | No | — | — | — | — |
| NeoForge (required) | — | — | — | — | `>=21.1.1` | No | `>=26.1.2.71` | No |
| Applied Energistics 2 (required) | `>=15.0.10 <16.0.0` | Yes (base) | `>=15.0.10 <16.0.0` | No | `>=19.0.24 <20.0.0` | No | `>=26.1.10-beta <27.0.0` | No |
| AE2: Crafting Tree (optional) | `>=1.0.1` | No | `>=1.0.1` | No | `>=1.0.1` | No | — | — |
| Applied Mekanistics (optional) | `>=1.4.0` | No | — | — | `>=1.6.0` | No | — | — |
| AdvancedAE (optional) | Not declared; fixture `1.3.6-1.20.1` | Yes (scenario) | — | — | `>=1.6.11` | No | `>=26.1.7` | No |
| ME Requester (optional) | `>=1.20.1` | No | `>=1.20.1` | No | `>=1.21.1` | No | — | — |
| Neo ECO AE Extension (optional) | `>=20.3.0 <20.4.0` | Yes (scenario) | — | — | — | — | — | — |
| AE2 Lightning Tech (optional) | `>=2.1.0-beta.2` | No | — | — | — | — | — | — |

The supported targets and public dependency ranges come from
[`scripts/release-matrix.json`](../scripts/release-matrix.json), loader metadata,
and [`DEPENDENCIES.md`](../DEPENDENCIES.md). TestDriver coverage comes from the
implemented scenarios under `versions/1.20.1-forge/src/testDriver`; AdvancedAE's
Forge fixture is development coverage, not a declared Forge dependency.
