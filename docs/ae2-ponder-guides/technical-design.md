# AE2 Ponder Guides Technical Design

## Evidence And Boundary

The standalone Ponder project publishes loader artifacts for Minecraft 1.20.1
Forge/Fabric and 1.21.1 NeoForge. Its `PonderPlugin`,
`PonderSceneRegistrationHelper`, `SceneBuilder`, and `SceneBuildingUtil` APIs are
source-compatible across the official `mc1.20.1/dev` and `mc1.21.1/dev`
branches. It does not currently document a 26.1.2 artifact.

The official repository has a `mc26.1/dev` branch, but its README still lists
NeoForge 1.21.1 as the supported NeoForge setup and the official Maven has no
`Ponder-NeoForge-26.1` or `Ponder-NeoForge-26.1.2` artifact. A development
branch alone is not a published dependency contract.

AE2 exposes its public content through `AEBlocks`, `AEParts`, and `AEItems` on
the AE2 15.x and 19.x lines. Those definitions, rather than a handwritten
partial list, are the coverage boundary.

## Ownership

Use one shared client-only integration under `shared/src/mc1201`:

```text
loader client setup, only when mod id "ponder" is loaded
  -> PonderIndex.addPlugin(Ae2CraftingTimePonderPlugin for AE2 15.x or 19.x)
  -> shared scene, tag, and text registration
  -> line-specific coverage manifest
  -> shared schematic NBT and localization resources
```

The three covered loader modules only detect the optional mod and invoke the
shared registration:

- Fabric calls it from `onInitializeClient`;
- Forge calls it from `FMLClientSetupEvent`; and
- NeoForge calls it from `FMLClientSetupEvent`.

The branch that references Ponder classes is executed only after the loader
confirms `ponder` is present. No Ponder-facing class belongs to a server source
set or the 26.1.2 source graph.

Put manifests, schematics, and Ponder localization under
`shared/src/mc1201/resources`. The 1.20.1 builds already include that resource
root; add it explicitly to the 1.21.1 NeoForge `sourceSets.main.resources`.
The 26.1.2 build must continue to omit it.

## Dependencies

- Compile against `Ponder-Forge-1.20.1:1.0.92`,
  `Ponder-Fabric-1.20.1:1.0.92`, and
  `Ponder-NeoForge-1.21.1:1.0.69` from the official Create Maven.
- Declare Ponder optional in the three matching loader metadata files with a
  same-major range beginning at the compiled version.
- Add Ponder to compatible and latest development-client profiles only for
  those three targets.
- Do not add Ponder to the 26.1.2 build, metadata, client matrix, or packaged
  classes.

## Scene Structure

`Ae2CraftingTimePonderPlugin` registers one tag per player-facing category and
delegates scene construction to small category classes only when one file would
otherwise mix unrelated mechanics. Reuse a scene for behavior-equivalent
variants by registering all matching definitions with one storyboard.

Schematics live under
`assets/ae2craftingtime/ponder/<category>/<scene>.nbt`. Scene code uses AE2
registry IDs from the line-specific coverage manifest. That manifest is the
single source of those IDs: the shared plugin registers its `ResourceLocation`
entries directly, avoiding static-field differences such as AE2 15.x `CHEST`
versus AE2 19.x `ME_CHEST`. Captions use Ponder localization registration with
AE2 Crafting Time-owned keys.

Scenes are deterministic demonstrations, not simulations of live server state.
They show block placement, visible transitions, overlays, controls, and expected
flows. Networking scenes use the smallest network that proves the mechanic;
multiblock scenes reveal construction in layers; configurable devices show the
interaction and its visible consequence.

## Coverage Manifest And Validation

Add a machine-readable manifest keyed by Minecraft line and AE2 registry ID.
Each row records `guide`, `shared`, or `excluded`; shared rows name their owning
scene, and excluded rows use one of: `decorative`, `material`, `crafting_only`,
`debug`, or `internal`.

A small test loads the manifest and the checked AE2 definition snapshot for
15.x and 19.x, then verifies:

- every public block, part, and item appears exactly once;
- every `guide` or `shared` row names a registered scene;
- every scene names an existing schematic and both localization entries; and
- exclusion reasons are from the fixed list.

The snapshots are derived from the exact minimum AE2 dependency artifacts and
checked in so the test stays deterministic and does not require a game launch
or network access. Updating an AE2 floor must refresh and review the snapshot.

## Upstream Evidence

- [Ponder setup and supported loaders](https://github.com/Creators-of-Create/Ponder/blob/mc1.21.1/dev/README.md)
- [Ponder 1.20.1 API branch](https://github.com/Creators-of-Create/Ponder/tree/mc1.20.1/dev)
- [Ponder 1.21.1 API branch](https://github.com/Creators-of-Create/Ponder/tree/mc1.21.1/dev)
- [Ponder 26.1 development branch](https://github.com/Creators-of-Create/Ponder/tree/mc26.1/dev)
- [AE2 15.x definitions](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/forge/1.20.1/src/main/java/appeng/core/definitions)
- [AE2 19.x definitions](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/1.21.1/src/main/java/appeng/core/definitions)

## Failure Behavior

- Ponder absent: no registration runs and no Ponder class is resolved.
- Missing scene resource or manifest row: tests fail before release.
- Removed or renamed AE2 definition: compilation or the coverage test fails.
- Unsupported 26.1.2 target: no integration is packaged rather than silently
  exposing incomplete guides.

No packet, saved-data, config, or migration path is involved.
