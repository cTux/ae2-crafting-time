# AE2 Craft Performance Debugger Research

Date: 2026-06-21

## Goal

Build a Minecraft 1.20.1 Forge addon that shows AE2 autocrafting performance hints beside each node in the AE2: Crafting Tree view:

- average craft duration over the latest N observations
- average throughput as items/tick, items/second, mB/tick, mB/second
- slow or under-parallelized steps

## Existing Mods

AE2 itself exposes autocrafting, crafting CPUs, crafting plans, and coarse job status, but not per-pattern profiling.

AE2: Crafting Tree already exists for 1.20.1 Forge and adds a tree view to the AE2 craft confirmation screen. CurseForge lists version `ae2ct-1.20.1-1.1.1.jar`, updated 2025-11-03, with Forge/NeoForge support and MIT license metadata. Its source is `nimeng1299/AE2CraftingTree`, branch `1.20.1-forge`.

Conclusion: do not build a tree UI first. Reuse or depend on AE2: Crafting Tree for the visualization, then add timing/throughput annotations.

## AE2 1.20.1 Integration Points

Checked against AE2 `forge/v15.3.3`, matching a common Minecraft 1.20.1 Forge version.

Public API:

- `ICraftingService.beginCraftingCalculation(...)` returns an `ICraftingPlan`.
- `ICraftingService.submitJob(...)` assigns the plan to a crafting CPU.
- `ICraftingPlan.patternTimes()` maps `IPatternDetails` to operation counts.
- `ICraftingCPU.getJobStatus()` returns only final-output progress and elapsed time.
- `CraftingJobStatus` is coarse: final stack, total items, progress, elapsed nanos.

Internal but useful hooks:

- `CraftingCpuLogic.trySubmitJob(...)` starts a job and creates `ExecutingCraftingJob`.
- `CraftingCpuLogic.executeCrafting(...)` pushes individual pattern operations into providers.
- `CraftingCpuLogic.insert(...)` accepts outputs back into the CPU and decrements waiting items.
- `ExecutingCraftingJob.tasks` stores remaining pattern operations.
- `ExecutingCraftingJob.waitingFor` stores expected outputs still pending.

Per-pattern profiling needs internal hooks or mixins. The AE2 public API is enough for coarse CPU-level timing, not enough for "Pattern Z is slow".

## Crafting Tree Integration Points

AE2: Crafting Tree already uses mixins:

- `AE2CraftingPlanSummary` injects into AE2 `CraftingPlanSummary.fromJob`, `write`, and `read`.
- It serializes a custom `RecipeHelper` derived from AE2 internal `CraftingPlan`.
- `RecipeHelper` records recipe inputs, outputs, and operation count from `CraftingPlan.patternTimes()`.
- `CraftingTreeScreen` and `CraftingTreeWidget` render tree nodes.

This is the cheapest display path: add profiling values to the tree node model, then render a short text line or tooltip beside each node.

## Data Model

Use pattern output key as the MVP identity:

```text
profileKey = output AEKey id
```

This intentionally merges every recipe that makes the same output into one average. For example, all recipes that make `iron_plate` share one `iron_plate` average. This matches Crafting Tree's current cache, which also keys recipes by first output key.

The alternative is a full pattern signature:

```text
profileKey = sorted inputs + sorted outputs
```

Example: if one recipe makes `iron_plate` from a hammer and another makes `iron_plate` from a press, output-key identity merges their timings into one average. Full signature would keep those averages separate, but that is out of scope for the first release.

For each key, keep a RAM-only ring buffer of the latest N observations:

```text
durationTicks
inputAmount
outputAmount
fluidAmount
operations
completedAtTick
```

Derived stats:

```text
avgDurationTicks = average(durationTicks)
itemsPerTick = average(outputAmount / durationTicks)
itemsPerSecond = itemsPerTick * 20
mbPerTick = average(fluidAmount / durationTicks)
mbPerSecond = mbPerTick * 20
```

Default N: 20. The data is session-only for the first release: restart the server/client and the samples are gone.

## Measuring Strategy

MVP:

1. On `executeCrafting`, when `provider.pushPattern(details, craftingContainer)` returns true, record a pending operation:
   - pattern identity
   - expected outputs
   - start server tick
2. On `insert`, when an expected output arrives, match it to the oldest pending operation for that output key.
3. When all expected output amount for that pending operation is observed, record duration.
4. Send aggregated stats to the client when Crafting Tree opens or refreshes.

This measures real external processing delay for machines, not only AE2 scheduling overhead.

Known ceiling: if several providers run the same pattern and output simultaneously, output matching is FIFO approximation. Good enough for a debugger; exact tracking would need provider-level correlation that AE2 does not expose cleanly.

## UI

Beside each tree node:

```text
avg 43t | 2.3/s
```

Tooltip:

```text
Avg: 43 ticks / 2.15 s
Throughput: 2.3 items/s
Samples: 20
Last: 51 ticks
```

For fluids:

```text
avg 80t | 250 mB/s
```

Only show stats in AE2: Crafting Tree when that mod is present. If Crafting Tree is absent, collect nothing visible and add no fallback screen.

Skip charts, overlays, and a separate profiling screen until the basic values are useful.

## Risks

- AE2 internals are not stable API. Mixins into `CraftingCpuLogic` and `CraftingPlanSummary` may break across AE2 versions.
- Version-specific mixins and UI adapters should be isolated under directories like `1.20.1`; putting shared profiling code there would make future ports copy/paste-heavy.
- Pattern identity by output can merge multiple recipes for the same output.
- FIFO output matching can be wrong under parallel machines with identical outputs.
- Client display needs a small server-to-client packet or piggybacking on Crafting Tree's existing summary serialization.
- Crafting Tree is MIT on CurseForge, but verify repository license before copying code. Depending on it is cleaner than copying.

## Sources

- AE2 GitHub README/API: https://github.com/AppliedEnergistics/Applied-Energistics-2
- AE2 API notes: https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/API.md
- AE2 1.20.1 source inspected locally from tag `forge/v15.3.3`
- AE2: Crafting Tree CurseForge: https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-tree
- AE2: Crafting Tree source: https://github.com/nimeng1299/AE2CraftingTree/tree/1.20.1-forge
