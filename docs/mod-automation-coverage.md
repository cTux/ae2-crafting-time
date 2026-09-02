# Mod automation coverage

This table combines required dependencies, optional dependencies, every
candidate in [`DEPENDENCIES_POTENTIAL.md`](../DEPENDENCIES_POTENTIAL.md), and the
current TestDriver coverage. Version cells use the latest working version pinned
in the compatible development-client profile, which may be older than the real
latest release.

WCWT, AE2 Wireless Terminals, and Import Export Card terminal tooltips label
estimates as `TTC: ~1s` (or `TTC: Collecting`). The line comes after AE2's
amounts, craftability, and advanced details on every supported target. Other mods
can still append content afterward. Compact row badges keep their existing text.

Project Infinity 0.1 (`0.0.51.3 HOTFIX`) also uses Crazy AE2 Addons `2.6.2`,
WCWT `1.20.1.7-hotfix`, NeoEco `20.4.0`, and OmniSequence
`1.3.8-hotfix-forge`. Forge metadata admits these pack versions; the existing
scenario fixtures remain applicable. NeoEco's expected-output hook accepts
both the 20.3 accounting object and the 20.4 batched-dispatch signature.
The prepared-client pins below are unchanged. CodexVM passed dependency
validation but Minecraft's square texture probe lowered its detected limit to
`8192`, preventing the pack's `16384x8192` atlas. A guest GL allocation probe later
confirmed that SVGA3D supports that rectangular atlas; the opt-in test-driver
workaround is documented in the test-driver design. A user-approved host-GPU rerun in a disposable
copy passed the dedicated scenarios for all four exact versions above, including
fresh CPU profiling samples and visible TTC for the three CPU integrations and
terminal tooltip/plan TTC for WCWT. The original pack's mods were not upgraded
or removed. These scenario results do not imply full modpack gameplay coverage.

The subsequent CodexVM campaign `20260902T084023Z-suite` passed all 23 installed
integration scenarios in one Minecraft process, with a fresh disposable world
per case, 31 visually inspected checkpoint screenshots, and exit code 0. It used
the exact pack above with Forge `47.4.20` and the opt-in rectangular atlas probe.
All 358 third-party JAR hashes and the original source instance were unchanged.
The earlier failed VM attempt is retained separately; the successful retry
includes fixes for capture-before-render and double initialization of AE2 nodes.
Per-mod results, screenshots, shared logs, and checkpoint mappings are archived
under the campaign ID using the [smoke evidence layout](ui-smoke-evidence.md).

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
| AE2 Network Analyser (`ae2netanalyser`; optional) | `1.20-1.0.6-forge` | Yes (`ae2networkanalyser-screen`) | `1.20-1.0.1-fabric` | No | `1.21-2.1.5-neoforge` | No | `26.1-1.0.0-neoforge` (beta) | No |
| AEInfinityBooster (`aeinfinitybooster`; optional) | `1.20.1-1.0.0+20` | Yes (`aeinfinitybooster-terminal`) | — | — | `1.21.1-1.0.0.58` | No | `26.1.2-1.0.0.57` | No |
| Applied Botanics (original) (`appbot`; optional) | `1.5.2` | Yes (`appbot-cpu`: native mana storage + item craft) | `1.5.2` | No | Not pinned | No | — | — |
| Applied Botanics (Fork; alternative `appbot`) | `1.5.2` | Yes (`appbot-fork-cpu`: native mana storage + item craft) | — | — | — | — | — | — |
| Advanced Peripherals (`advancedperipherals`; optional) | `1.20.1-0.7.48r` | Yes (`advancedperipherals-cpu`: real ME Bridge API) | — | — | `1.21.1-0.8.0a` (alpha) | No | — | — |
| AE2 Things (`ae2things`; original Fabric / Forge port; optional) | `1.2.1` | Yes (`ae2things-cpu`: real DISK storage craft) | `1.3.2` | No | Not pinned (`1.4.2-beta` supported) | No | — | — |
| Expanded AE (`expandedae`; optional) | `1.2.2` (focused only; excluded from full compatible set) | Yes (`expandedae-cpu`, latest focused profile) | — | — | `2.1.1` (excluded from compatible set) | No | — | — |

Original Applied Botanics 1.21.1 support is declared from `1.6.0-alpha.3`, but
its required Botania snapshot has no matching published Modrinth release.
That runtime is not pinned or smoke-verified; no original 26.1.2 artifact exists.

Forge full clients select the fork instead of the original because their mod
IDs and filenames collide. Focused runs can select either artifact explicitly.

The supported targets and declared dependencies come from
[`scripts/release-matrix.json`](../scripts/release-matrix.json), loader metadata,
[`DEPENDENCIES.md`](../DEPENDENCIES.md), and
[`scripts/run-client-versions.json`](../scripts/run-client-versions.json).
TestDriver coverage comes from the implemented scenarios under
`versions/1.20.1-forge/src/testDriver`; AdvancedAE's Forge fixture is development
coverage, not a declared Forge dependency.
