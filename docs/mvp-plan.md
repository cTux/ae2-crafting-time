# MVP Plan

## Decision

Build a small 1.20.1 Forge addon that depends on:

- Applied Energistics 2

Optionally integrate with:

- AE2: Crafting Tree

Do not build a standalone tree UI. Use Crafting Tree as the screen and node source when installed.

## Scope

MVP shows per-node rolling averages in the existing Crafting Tree screen:

- average duration in ticks and seconds
- throughput in items/s or mB/s
- sample count

MVP does not try to identify exact machine blocks, exact provider names, or every parallel operation perfectly.

Version-specific code must live under a version directory such as `1.20.1`. Shared profiling logic must stay outside version directories so later Minecraft/AE2 ports reuse it instead of copying it.

In multiplayer, all timing and throughput calculations must be server-owned. The client only requests and renders server-provided aggregate snapshots. See `docs/server-client-stats.md`.

## Implementation Steps

1. Scaffold minimal Forge 1.20.1 mod.
2. Add dependencies for AE2 and AE2: Crafting Tree.
3. Add server-side profiler store:
   - `Map<ProfileKey, ArrayDeque<Sample>>`
   - max samples: 20
   - RAM only; no world save, no file save
4. Add AE2 mixins:
   - inject after successful `provider.pushPattern(...)` in `CraftingCpuLogic.executeCrafting`
   - inject after successful `CraftingCpuLogic.insert(...)` output handling
5. Match completed outputs to pending operations by output key and FIFO.
6. Add a tiny packet: server sends aggregated stats for keys visible in the current Crafting Tree.
7. Patch Crafting Tree UI by optional mixin:
   - render one compact stat line beside each node
   - add tooltip details
   - do nothing when Crafting Tree is not installed
8. Test with one slow processing pattern and one fast molecular assembler pattern.

## First Check

Before coding the whole thing, prove this one path:

```text
start craft -> pushPattern records pending -> machine returns output -> insert records sample -> log avg duration
```

If that works, UI work is straightforward. If it does not, stop and inspect AE2's provider/output path before adding packets.

## Minimal Config

```text
enabled = true
samples = 20
showInTree = true
```

No custom units, no themes, no history window yet.

## Decisions

- Stats are session-only: RAM data, cleared on restart.
- AE2: Crafting Tree is optional.
- No fallback UI when AE2: Crafting Tree is absent.
- `enabled = true` gates collection and UI.
- Profile keys use output identity: every recipe that makes the same output shares one average.
- Version-specific hooks/UI adapters live under directories like `1.20.1`; shared logic stays version-neutral.
