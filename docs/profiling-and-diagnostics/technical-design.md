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
over its elapsed ticks. This lets a running status row leave `No data yet`
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

## Planned design for #114: per-unit sample presentation

Status: not implemented. See [spec N1–N8](spec.md#requirements) and the
[implementation plan](implementation-plan.md). Source inspection baseline:
`493d277757489ef4d1f0970509c0cfbb5a8bbdb6`.

### Evidence and decision

Paths below are relative to the repository root.

| Existing component | Verified behavior | Consequence |
| --- | --- | --- |
| `shared/src/main/java/com/ctux/ae2craftingtime/core/CraftProfiler.java`, `start`, `complete` | CPU-scoped pending queues feed a network/output busy window; one sample is retained when all matching pending work ends | A sample does not count provider insertions |
| Same file, `stats`, `filteredSamples` | Outliers already use duration/amount; rate is weighted amount divided by weighted duration | Raw bulk samples are already normalized for TTC math |
| Same file, `inProgressStats`, `stall` | Preview is temporary; stall uses raw average window duration | Do not change these fields to per-item durations |
| `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/TtcText.java`, `windows`, `compactMessages` | Sample list prints raw amount/duration; compact details print raw average/latest window times | Change presentation and its explicitly named derived values |
| Same directory, `StatsChatServer.java`, `details` | Public details are independently assembled on the server | Updating client text alone misses the real Ctrl-click path |
| `shared/src/main/java/com/ctux/ae2craftingtime/core/ProfileStats.java` | Snapshot already has aligned raw duration and amount lists | No new packet or persisted fields are needed |
| `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/net/StatsPacketCodec.java` | Encodes those lists as bounded integer values | Keep wire representation exact and unchanged |
| `shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/PersistedSamplesTag.java` and its `mc2612` counterpart | Persist positive raw `amount` and `durationTicks` | Existing saves already contain the necessary evidence |

The standard `CraftingCpuLogicMixin` records expected outputs and CPU-accepted
returns through `ProfilerBridge`. AdvancedAE and other supported CPU adapters
route observations into the same core. None of these events identifies the
instant a machine slot starts processing. Treating dispatch-to-return latency
as service time would include queued work; duplicating parallel durations would
also distort rates if fed into the current estimator.

The confirmed requirement is therefore a presentation correction with derived
per-unit values. Preserve the collector and estimator. A display-only change
must not claim to improve prediction accuracy numerically.

### Derived values and rounding

Keep all `ProfileStats` record fields and constructor signatures unchanged.
Add these computed methods to that existing pure-Java type:

- `sampleTicksPerUnit(int index): OptionalDouble` returns `(double) duration /
  amount` for a valid pair. Invalid index, unequal list lengths, or nonpositive
  amount/duration returns empty.
- `averageTicksPerUnit(): OptionalDouble` returns the arithmetic mean of all
  retained ratios, including observations excluded from rate estimation. Return
  empty for empty lists or if any pair is invalid; do not silently average a
  different history.
- `latestTicksPerUnit(): OptionalDouble` derives the last pair only. Return empty
  when the lists are empty, unequal, or the last pair is invalid.

These methods neither mutate history nor change `averageDurationTicks` and
`lastDurationTicks`. Those raw fields still serve diagnostics. Cast before
division. Do not sum raw longs to compute the per-unit average.

Add `TimeEstimate.formatSampleTicks(double): Optional<String>` as a numeric
formatter usable on both server and client. Reject nonpositive/nonfinite values;
render positive values below 0.001 as `<0.001`; otherwise use
`BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP)`, strip trailing zeros,
and emit plain decimal text. This is presentation rounding only. Do not reuse
`formatTicks`, which rounds to whole seconds, or feed rounded text into TTC.

For raw pairs `(A_i, D_i)`, retain the existing estimator:

```text
sample detail = D_i / A_i ticks per unit
compact average = sum(D_i / A_i) / retained observation count
compact latest = D_last / A_last
weighted rate = sum(w_i * A_i) / sum(w_i * D_i), over existing usable observations
```

Example A10 intentionally has a different inverse average and weighted rate.
Do not replace the weighted ratio with an average of inverse sample durations.

### Presentation ownership

`TtcText.windows` calls the computed ratio and numeric formatter for each raw
pair, preserving oldest-to-newest order and the existing count suffix. Skip an
invalid pair; omit the sample line if no valid pairs remain. Rename the label to
`Samples (per unit)` and add one short tooltip explanation: `Effective throughput,
not single-item processing time.` Do not repeat the explanation for every sample.

Reuse `text.ae2craftingtime.value.window` with three string arguments: literal
`1`, the unit's per-unit label, and formatted ticks. Add a dedicated singular item
key; do not change the plural item label used by the production-rate field.
Use the existing mB/mana labels for their per-unit forms.

Both `TtcText.compactMessages` and `StatsChatServer.details` use the same numeric
methods. Replace ambiguous average/latest labels with `average per unit` and
`latest per unit`, each containing the complete `1 <unit> / <ticks> ticks` value.
When either derived summary is unavailable, use a new rate-only details
translation with sample count, rate, and unit; never fall back to raw window time
under a per-unit label. Keep used-sample and low-confidence suffixes.

`StatsChatServer` must keep building translatable components on the server.
Do not call client-only `I18n` or `TtcText` from it. Share calculations and numeric
formatting through the pure core, not a new UI dependency.

Update shared `en_us.json` and `uk_ua.json` together. Suggested Ukrainian labels
are `Зразки (на одиницю)`, `середнє на одиницю`, `останнє на одиницю`, and singular
`предмет`. Explanation: `Ефективна пропускна здатність, а не час обробки одного
предмета.` Retain translated tick/mB/mana conventions and match placeholders.

### Compatibility, state, and limits

No new runtime state, event hooks, per-item arrays, packets, schema versions,
configuration, or dependencies. Keep `maxSamples` and packet bounds unchanged;
formatting is O(retained samples), never O(output amount). A billion-item
observation still produces one displayed sample.

Old samples and live previews are normalized from their existing raw evidence.
The preview remains low confidence and unpersisted. Reset, disable, cancellation,
network isolation, selected-CPU diagnostics, saved statuses, accuracy, and
SavedData dirtying retain their current behavior. Server validation, chat
cooldown, visibility settings, and reset authority remain unchanged.

All four release-matrix targets share the affected core, `mcCommon` text, and
resources. Version bridges, saved-data codecs, and loader packet wrappers need
verification, not changes. Optional Crafting Tree details and any shared text
consumers inherit the format where they already expose details; ME Requester
and terminal TTC-only surfaces need no new sample list. AdvancedAE, ECO,
AE2-LT, Applied Mekanistics, and mana support retain their existing registrations
and unit normalization. No new dependency on Mekanism or Iron Furnaces is added.

The separate proposed CPU-bound stats design keeps raw windows too; #114 can
apply to whichever scope supplied a snapshot without adding that feature. Do
not implement its proposed key or persistence changes as part of this work.
