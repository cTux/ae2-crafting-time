# Finite Outlier Validation Design

Lifecycle: see the [scope status and evidence](spec.md).

This implements the [specification](spec.md) for
[#469](https://github.com/cTux/ae2-crafting-time/issues/469).
Source evidence was inspected at
[`5e2472d`](https://github.com/cTux/ae2-crafting-time/tree/5e2472dcfe0b31eb02ad75d6947ee0e7d8aa9fda).

## Current path and cause

1. Fabric `Ae2CraftingTime.onInitialize` loads `ae2craftingtime-common.toml`.
   `versions/1.20.1-fabric/src/main/java/com/ctux/ae2craftingtime/mc1201/
   Ae2CraftingTimeConfig.java` passes each multiplier string and the current
   value to `parseDouble`. `Double.parseDouble` accepts NaN and infinity;
   `Math.min`/`Math.max` preserve NaN and clamp infinities to the config bounds.
2. Both `shared/src/mc1201` and `shared/src/mc2612` `ProfilerBridge` copies
   construct their static profiler with that config value. In
   `shared/src/main/java/com/ctux/ae2craftingtime/core/CraftProfiler.java`, the
   constructor's `< 1.0` check admits NaN and positive infinity.
3. `stats` is the only caller of `filteredSamples`. With fewer than five samples
   filtering is bypassed. At five, a NaN multiplier makes every sample fail the
   range predicate. Weighted amount and duration become zero, producing NaN
   throughput and an unreliable estimate.
4. `ProfileStats`, `StatsPacketCodec`, and `ClientStatsCache` preserve that rate.
   `TimeEstimate.seconds` only rejects nonpositive rates. NaN passes, and casting
   the calculated result to `long` yields zero. `format` adds `?`. Positive
   infinite throughput independently yields zero through division.

These are source-established consequences, not a recorded Minecraft run.

## Smallest shared correction

- Move the bounded double parsing decision into a small Minecraft-free
  `ConfigNumbers.parseDouble(String value, double fallback, double min,
  double max)` helper under `shared/src/main/java/com/ctux/ae2craftingtime/core`.
  Parse once, return the supplied valid fallback for a nonfinite result or
  malformed text, and otherwise use the existing clamp. Fabric delegates to it.
  Its caller supplies finite fallback and bounds; no generic config framework
  or new integer/boolean parser is needed.
- In `CraftProfiler`, require a finite multiplier as well as `>= 1.0` before
  retaining it. Keep the constructor's exception contract and sample-limit rule.
- In `TimeEstimate.seconds`, reject nonfinite `amountPerSecond` before dividing,
  alongside the existing nonpositive guards. `format` already delegates to it.

The constructor protects both production bridge callers. Valid positive samples
and a finite multiplier of at least one retain the median sample, so this defect
does not need a separate empty-filter fallback. Do not alter sampling or rate
arithmetic to conceal invalid configuration.

## Consumers and compatibility

`TimeEstimate.seconds` also serves both versions of `ProfilerBridge` (accepted
plan coverage and remaining-job TTC), `AbstractTableRendererMixin` (plan/status
ordering and color), `CraftConfirmScreenMixin` (plan totals), and
`CraftingCPUScreenMixin` (status ordering), plus pre-26 `CraftingTreeTtc` and
`MERequesterScreenMixin`. `format` serves shared status rows, `TtcText`,
`StatsChatServer`, both wireless-terminal mixins, and ME Requester.
All already handle empty optionals; keep those existing display choices.

All four modules include the pure shared source. Forge and NeoForge retain
`defineInRange` config declarations; their malformed-file correction is not
being changed or claimed as tested here. Packet doubles and saved positive raw
samples keep their layouts. Restarting with valid config can use existing
history without resetting it. No loader-specific copy of the core guards is
needed.

## Test seams

Pure helper tests own every parsing branch, constructor tests own the invariant,
and `TimeEstimateTest` owns unknown versus rounded finite estimates. A Fabric
`Ae2CraftingTimeConfigTest` loads temporary files through the real `load(Path)`
API, restores the static multiplier through valid config, and proves delegation
and repeated-key fallback. Four/five-sample tests use public profiler operations
and `flushCompletedSamples`, then assert rate, used count, confidence, and TTC.
No reflection, Minecraft launch, test-driver extension, or new dependency is
needed. Loader adapter coverage remains subject to the existing exclusion;
the new parsing decisions stay in fully covered shared code.
