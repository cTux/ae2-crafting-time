# AE2 Ponder Guides Spec

Issue: https://github.com/cTux/ae2-crafting-time/issues/145

## Goal

Provide optional Ponder scenes that teach every mechanics-bearing AE2 block,
cable part, and usable item through an in-game visual example.

## Player Behavior

- When Ponder is installed, its normal entry points show a guide for every
  functional AE2 block, part, and item available on that Minecraft version.
- A guide demonstrates what the content does, the minimum setup it needs, the
  important configuration or interaction, the successful result, and a common
  failure state when one is useful.
- Variants with the same behavior, such as cable colors or storage tiers, may
  share one scene, but every variant remains an entry point to that scene.
- Guides are grouped by resource acquisition, networking and channels, energy,
  storage, terminals, transport, autocrafting, spatial systems, quantum links,
  wireless access, and tools.
- Scene titles, captions, and shared text are available in English and
  Ukrainian.
- Without Ponder, AE2 Crafting Time starts and behaves exactly as it does now.

## Coverage Rule

The implementation must inventory the public definitions in AE2's `AEBlocks`,
`AEParts`, and `AEItems` for each covered AE2 line. Every definition is recorded
in a checked coverage manifest as one of:

- `guide`: it has distinct player-visible behavior and owns a scene;
- `shared`: it is linked to another definition's behavior-equivalent scene; or
- `excluded`: it is outside this feature, with a reason from the list below.

Functional content includes blocks, cable-mounted parts, tools, cards, storage
cells, portable or wireless devices, and other items whose use changes a world,
network, device, or player interaction. The inventory must cover at least these
families:

- certus growth and processing, the Charger, Inscriber, and Matter Condenser;
- network power, controllers, cables, channels, Quartz Fiber, Toggle Buses, and
  cable anchors;
- drives, chests, storage cells, Cell Workbench, IO Port, storage buses, and
  storage monitors;
- terminals, pattern terminals, pattern access, interfaces, import/export
  buses, planes, level emitters, and conversion monitors;
- Pattern Providers, Molecular Assemblers, crafting CPU multiblocks, monitors,
  co-processors, and crafting cards;
- wireless access and terminals, quantum bridges, spatial IO, pylons, and
  spatial anchors;
- P2P tunnel types and their attunement behavior; and
- functional tools and devices such as the Network Tool, Memory Card, Color
  Applicator, Entropy Manipulator, Matter Cannon, Meteorite Compass, and Tiny
  TNT.

## Compatibility

| Target | Ponder support | Planned result |
| --- | --- | --- |
| 1.20.1 Forge | Official Ponder artifact available | Guides included |
| 1.20.1 Fabric | Official Ponder artifact available | Guides included |
| 1.21.1 NeoForge | Official Ponder artifact available | Guides included |
| 26.1.2 NeoForge | No official Ponder Maven artifact published | Integration omitted |

Ponder stays an optional client dependency. The compile and development-client
pins are `1.0.92` for 1.20.1 Forge/Fabric and `1.0.69` for 1.21.1 NeoForge,
matching the official Maven releases verified when this plan was written.

## Non-Goals

- Decorative blocks, raw materials, crafting intermediates, developer/debug
  content, and hidden implementation items do not need guides.
- Behavior-equivalent colors, tiers, and shapes do not get duplicate scenes.
- This does not replace AE2's GuideME guide or reproduce recipe pages.
- AE2 addon content is not included.
- The 26.1.2 target is not supported until an official compatible Ponder
  artifact is published.
- No server logic, packets, persistence, configuration, or AE2 mechanics change.

## Acceptance Criteria

- The checked manifest accounts for every public `AEBlocks`, `AEParts`, and
  `AEItems` definition in both the AE2 15.x and 19.x lines with `guide`,
  `shared`, or an allowed exclusion reason.
- Every `guide` and `shared` definition opens a relevant scene through Ponder's
  standard item/block entry points.
- Each distinct functional family has a visual scene that demonstrates its
  minimum working setup and result; setup-sensitive families also show one
  useful failure or misconfiguration.
- All scene text has matching `en_us` and `uk_ua` keys and placeholders.
- Resource and coverage checks fail for a missing schematic, translation,
  registration, or unclassified AE2 definition.
- All three covered clients start with Ponder present and can open representative
  scenes from every guide category.
- All four supported clients start without Ponder, and the 26.1.2 JAR contains
  no Ponder classes or dependency declaration.
