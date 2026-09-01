# Mod automation coverage

This table combines required dependencies, optional dependencies, every
candidate in [`DEPENDENCIES_POTENTIAL.md`](../DEPENDENCIES_POTENTIAL.md), and the
current TestDriver coverage. Version cells use the latest working version pinned
in the compatible development-client profile, which may be older than the real
latest release.

`Yes (base)` means the Forge 1.20.1 driver exercises the dependency as part of
its standard AE2 scenario. `Yes (scenario)` means it has a dedicated optional-mod
scenario. `No` means a version is known for that target, but no TestDriver covers
it. `Not pinned` means support is declared without a compatible-profile artifact.
`—` means the dependency is not part of that target.

| Mod Name | 1.20.1-forge | 1.20.1-forge TestDriver | 1.20.1-fabric | 1.20.1-fabric TestDriver | 1.21.1-neoforge | 1.21.1-neoforge TestDriver | 26.1.2-neoforge | 26.1.2-neoforge TestDriver |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Minecraft (required) | `1.20.1` | Yes (base) | `1.20.1` | No | `1.21.1` | No | `26.1.2` | No |
| Java (required) | `17` | Yes (base) | `17` | No | `21` | No | `25` | No |
| Forge (required) | `47.4.10` | Yes (base) | — | — | — | — | — | — |
| Fabric Loader (required) | — | — | `0.19.4` | No | — | — | — | — |
| Fabric API (required) | — | — | `0.92.11+1.20.1` | No | — | — | — | — |
| NeoForge (required) | — | — | — | — | `21.1.238` | No | `26.1.2.99` | No |
| Applied Energistics 2 (required) | `15.4.10` | Yes (base) | `15.1.0` | No | `19.2.17` | No | `26.1.10-beta` | No |
| ae2ct (Crafting Tree) (`ae2ct`; optional; candidate) | `1.0.1` | No | Not pinned | No | `1.0.1` | No | — | — |
| Applied Mekanistics (`appmek`; optional) | `1.4.3` | Yes (`appmek-cpu`) | — | — | `1.6.3` | No | — | — |
| AdvancedAE (`advanced_ae`; optional; candidate) | `1.3.6-1.20.1` | Yes (scenario) | — | — | `1.6.12-1.21.1` | No | `26.1.7` | No |
| ME Requester (`merequester`; optional; candidate) | `1.20.1-1.2.1+forge` | Yes (`merequester-screen`) | `1.20.1-1.1.4+fabric` | No | `1.21.1-1.4.3+neoforge` | No | — | — |
| NeoEco AE (`neoecoae`; optional; candidate) | `20.3.0` | Yes (scenario) | — | — | `21.1.1` | No | — | — |
| AE2 Lightning Tech (`ae2lightningtech`; optional; candidate) | `2.1.0-beta.2-forge.1.20.1` | No | — | — | `2.1.0-beta.2` | No | `1.0.1alpha-26.1.2neoforge` | No |
| OmniSequence: Transfinite (`molecularmanipulator`; optional; candidate) | `1.3.9-forge` | Yes (scenario) | — | — | `1.3.9-hotfix` | No | — | — |
| ExtendedAE (optional; candidate) | `1.20-1.4.18-forge` | Yes (scenario) | `1.20-1.0.2-fabric` | No | `1.21-2.2.35-neoforge` | No | `26.1-1.0.3-neoforge` | No |
| ExtendedAE-Plus (`extendedae_plus`; optional; candidate) | `1.5.5` | Yes (scenario) | — | — | `1.6.2` | No | — | — |
| BM Addon (`bmaddon`; optional) | `1.0.4` | Yes (`bmaddon-cpu`) | — | — | `BMAddon1.21.1-beta1` | No | `beta3` | No |
| Crazy AE2 Addons (`crazyae2addons`; optional) | `3.2.4` | Yes (`crazyae2addons-cpu`) | — | — | — | — | — | — |
| AE2 WCWT (`wcwt`; optional) | `1.20.1.10` | Yes (`ae2wcwt-terminal`) | — | — | `1.3.8` | No | — | — |
| AE2 Wireless Terminals (`ae2wtlib`; optional) | `15.3.3-forge` | Yes (`ae2wtlib-terminal`) | `15.2.1-fabric` | No | `19.5.1` | No | `26.1.1-beta` | No |
| MEGA Cells (`megacells`; optional) | `forge-2.4.6` | Yes (`megacells-cpu`) | `fabric-2.4.6` | No | `4.11.0` | No | — | — |
| OMNI Cells (`ae2omnicells`; optional) | `1.1.6-1.20.1-forge` | Yes (`omnicells-cpu`) | — | — | `1.1.6-1.21.1-neoforge` | No | `1.1.7-26.1.2-neoforge` | No |
| ProjectCell (`projectcell`; optional) | `1.0.1` | Yes (`projectcell-cpu`) | — | — | `1.0.3` | No | — | — |
| AppliedE / AppliedE TPS Fix (`appliede`; optional) | `0.14.7-fix2` | Yes (`appliede-cpu`) | — | — | `1.0.0-beta` | No | — | — |
| Applied Flux (`appflux`; optional) | `1.20-1.3.7-forge` | Yes (`appflux-cpu`) | — | — | `1.21-2.1.5-neoforge` | No | `26.1-1.0.1-neoforge` | No |
| Modern AE2 Additions (`mae2`; optional) | `2.0.1` | Yes (`modern-ae2-additions-cpu`) | — | — | — | — | — | — |
| AE2 Import Export Card (`ae2insertexportcard` / `ae2importexportcard`; optional) | `1.20.1-1.3.0` | Yes (`ae2importexportcard-terminal`) | — | — | `1.21.1-1.6.0` | No | `26.1.2-2.1.0` | No |
| AE2 Network Analyser (candidate) | — | — | — | — | — | — | — | — |
| AEInfinityBooster (candidate) | `1.20.1-1.0.0+20` | No | — | — | `1.21.1-1.0.0.58` | No | `26.1.2-1.0.0.57` | No |
| Applied Botanics (Fork) (candidate) | — | — | — | — | — | — | — | — |
| Advanced Peripherals (candidate) | `1.20.1-0.7.48r` | No | — | — | `1.21.1-0.8.0a` | No | — | — |
| AE2 Things (candidate) | — | — | `1.3.2` | No | — | — | — | — |
| Expanded AE (candidate) | Incompatible; not pinned | No | — | — | Incompatible; not pinned | No | — | — |

The supported targets and declared dependencies come from
[`scripts/release-matrix.json`](../scripts/release-matrix.json), loader metadata,
[`DEPENDENCIES.md`](../DEPENDENCIES.md), and
[`scripts/run-client-versions.json`](../scripts/run-client-versions.json).
TestDriver coverage comes from the implemented scenarios under
`versions/1.20.1-forge/src/testDriver`; AdvancedAE's Forge fixture is development
coverage, not a declared Forge dependency.
