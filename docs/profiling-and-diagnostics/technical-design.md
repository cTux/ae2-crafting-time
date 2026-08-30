# Profiling And Diagnostics Technical Design

## Ownership

`CraftProfiler`, `TtcAccuracyTracker`, and their immutable values live in
`shared/src/main/java`. They have no Minecraft dependency, so every target uses
the same calculations and tests.

The logical server owns all mutable profiling state. AE2-facing mixins report
events through the version-specific `ProfilerBridge`; clients receive only
bounded aggregate snapshots.

## Production Windows

`CraftingCpuLogicMixin` observes two AE2 execution points:

```text
expected output dispatch -> ProfilerBridge.start(...)
accepted returned output -> ProfilerBridge.complete(...)
```

Pending queues use the concrete crafting CPU object as an identity scope. A
`BusyWindow` uses `ProfileKey(networkId, outputId)` and spans every CPU scope
that is currently producing that output. Completion consumes only the matching
CPU queue. The window closes and creates one sample after no scope has matching
pending work.

This split keeps cancellation safe while still measuring real network-wide
parallel throughput. `finishJob(...)` clears the CPU scope and rebuilds any
affected network window from work that other CPUs still own.

Before the first window closes, a stats request may preview its completed amount
over its elapsed ticks. This lets a running status row leave `Collecting`
without waiting for the next order. The preview is always low confidence and is
never retained or persisted; the normal completed window remains the only
throughput sample.

## Throughput Calculation

Each retained sample stores normalized amount, unit, and duration ticks. The
profiler:

1. keeps the newest `maxSamples` values in an `ArrayDeque`;
2. calculates average and latest duration from the full retained window;
3. after five samples, filters duration-per-unit outliers around the median;
4. assigns usable samples weights `1..n` from oldest to newest; and
5. divides weighted amount by weighted duration for `amountPerTick`, then
   multiplies by 20 for `amountPerSecond`.

`ProfileStats` carries both total and used sample counts. The client can explain
low confidence without receiving mutable profiler state.

## Prediction Accuracy

When `trySubmitJob(...)` succeeds, `ProfilerBridge.startJob(...)` estimates the
accepted plan using the same normalized row formula as the craft-confirm UI.
`TtcAccuracyTracker` stores one pending job by CPU identity. `finishJob(true)`
converts it into a bounded sample containing predicted seconds, tick time, wall
time, and row coverage. Any other finish removes the pending job without adding
a sample.

The aggregate exposes:

- fully covered jobs over recorded jobs;
- mean absolute percentage error;
- mean actual-to-predicted ratio;
- mean signed seconds error;
- average row coverage; and
- latest prediction, tick time, wall time, and coverage.

Only samples whose known-row count equals total-row count enter the three error
metrics.

## Delayed Output Diagnostics

`CraftProfiler` keeps last-progress ticks per CPU/output and a recent
`CapacityState` per CPU. The status request resolves the player's selected CPU,
then asks for a `StallDiagnostic` alongside its network/output stats.

The delay threshold is:

```text
max(600 ticks, ceil(averageDurationTicks * 2))
```

Capacity expires after 20 ticks. `StallDiagnostic.hints(...)` selects at most
one parallelism hint from scheduled work and recent capacity, then adds the
machine-speed hint.

## Persistence And Requests

Completed throughput samples are exported as `PersistedOutputSamples` and saved
under `ae2-crafting-time`. Accuracy, pending work, busy windows, progress ticks,
and capacity are deliberately absent from NBT.

`StatsRequestHandler` resolves the active grid and selected standard or
AdvancedAE CPU on the server. The response contains aggregate `StatsEntry`
values and current network amounts. Packet collection and string limits are
defined by `PacketLimits` and checked while decoding.

## Version Adapters

- `shared/src/mcCommon`: common AE2 execution hooks, request handling, codecs,
  text, and table integrations.
- `shared/src/mc1201` and `shared/src/mc2612`: API-specific key conversion,
  bridge, persistence tag, input, and screen code.
- `shared/src/neoforge`: optional AdvancedAE execution hooks shared by both
  NeoForge targets.
- `versions/<target>`: entrypoints, networking registration, and `SavedData`
  glue.

The adapters should remain conversion and delegation only. New calculations or
branches belong in the covered pure-Java core.
