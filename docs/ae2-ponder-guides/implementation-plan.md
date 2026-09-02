# AE2 Ponder Guides Implementation Plan

## 1. Add The Optional Integration Seam

1. Add the three official Ponder compile dependencies and optional loader
   metadata ranges from the technical design.
2. Add `shared/src/mc1201/resources` to the 1.21.1 NeoForge resource roots;
   leave the 26.1.2 resource graph unchanged.
3. Add the shared `Ae2CraftingTimePonderPlugin` and one guarded client setup
   call in each covered loader, passing the target's AE2 line.
4. Prove all four targets start without Ponder before adding scenes.

Done when the three covered clients register an empty plugin with Ponder and the
26.1.2 dependency graph and JAR remain unchanged.

## 2. Establish Exhaustive Coverage

1. Extract public IDs from `AEBlocks`, `AEParts`, and `AEItems` in the pinned
   AE2 15.x and 19.x artifacts.
2. Check in deterministic definition snapshots and one coverage manifest with
   line-specific entries that is also the plugin's source of registry IDs.
3. Classify every entry using the spec's inclusion and exclusion rules.
4. Add the manifest test before scene authoring so omissions fail immediately.

Done when every definition is classified exactly once and every included entry
points at a planned scene ID.

## 3. Build Shared Guides By Category

Implement and review categories in this order so later scenes reuse the network
and overlay patterns established earlier:

1. resource acquisition and processing;
2. energy, networking, cables, channels, and controls;
3. storage cells, drives, workbench, IO, and monitors;
4. terminals, interfaces, buses, planes, and emitters;
5. autocrafting and crafting CPU multiblocks;
6. wireless, quantum, spatial, and P2P systems; and
7. functional tools and devices.

For each category:

- create the smallest schematic that proves each distinct behavior;
- link equivalent colors, tiers, and forms to the same storyboard;
- add English and Ukrainian text together; and
- extend resource validation and open every new scene in a development client.

## 4. Complete Compatibility Verification

1. Add Ponder to the compatible and latest client profiles for 1.20.1 Forge,
   1.20.1 Fabric, and 1.21.1 NeoForge.
2. Run unit/resource checks and the required build row for all four targets.
3. Smoke-start every target without Ponder.
4. With Ponder present, open at least one scene from every category on each of
   the three covered targets and verify every manifest entry resolves.
5. Inspect the 26.1.2 JAR and metadata to prove Ponder code and declarations are
   absent.
6. Update `docs/dependencies.md`, feature documentation, and release metadata with
   only the verified optional support.

Complete when the manifest has no unclassified functional content, all linked
scenes and translations validate, the three covered clients expose the guides,
and every absent-Ponder startup remains clean.
