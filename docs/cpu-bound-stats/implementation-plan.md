# CPU-Bound Stats: Implementation Plan

Part of `cpu-bound-stats/`. Ordered tasks. Each task is small and testable on its
own. Build all versions after the core and the version-specific SavedData changes
land.

## Task 1 — Extend `ProfileKey`

File: `shared/src/main/java/com/ctux/ae2craftingtime/core/ProfileKey.java`

- Add `cpuId` as the middle field.
- Keep `ProfileKey(String networkId, String outputId)` and
  `ProfileKey(String outputId)` delegating with `cpuId = ""`.
- Normalize `null` `cpuId` to `""`.
- Add `isCpuSpecific()`.

Tests: `ProfileKeyTest` (new) — blank/empty cpuId normalizes to `""`; two-arg and
one-arg constructors produce `cpuId = ""`; `isCpuSpecific` reflects state.

## Task 2 — `cpuId` derivation helper

File: `shared/src/.../mc1201/ProfilerBridge.java` (and a tiny shared helper if the
anchor extraction needs MC types).

- Add `cpuId(IGrid grid, Object cpu)` returning `"<x>,<y>,<z>#<index>"`: the CPU's
  world block anchor plus its 0-based index in `grid.getCraftingService().getCpus()`
  (`collection.md`). Return `""` for null / unrecognized shape / missing grid.
- `cpuAnchorPos` handles `CraftingCPUCluster` and `AdvCraftingCPU` (reflection for
  the Advanced AE shape, mirroring `StatsRequestContext.optionalAdvancedCpu`).
- `cpuListIndex` walks `getCpus()` to find the match position, or `-1`.

Tests: unit test with fake CPU objects — known anchor + index returns
`"12,64,10#2"`; null returns `""`; unknown type returns `""`.

## Task 3 — Pass cpuId through profiling

Files: `ProfilerBridge.java`, `CraftingCpuLogicMixin.java`,
`AdvancedCraftingCpuLogicMixin.java`

- Add `key(networkId, cpuId, what)` overload.
- In `start`, `complete`, and `startJob`, build the cpu-aware key from
  `cpuId(grid, scope)`, where `grid` is `cluster.getGrid()` / `cpu.getGrid()`.
- In `startJob`, pass `cpuId(grid, scope)` into `ACCURACY.start(...)`.

Tests: `CraftProfilerTest` — `start`/`complete` with a cpuId scope stores the
sample under the cpu-specific key; `stats` for `cpuId = ""` is separate; the
IdentityHashMap scope behavior is unchanged.

## Task 4 — Lookup fallback

File: `shared/src/.../mc1201/ProfilerBridge.java`

- Add `stats(networkId, cpuId, what)` that prefers the cpu-specific key when it has
  `>= 3` samples, else falls back to `cpuId = ""`.
- Mirror the fallback in the accuracy lookup.

Tests: with two samples on a CPU-specific key and ten on the network key, the
fallback returns the network stats; with four on the CPU key, it returns the CPU
stats.

## Task 5 — Persistence: `version: 2` + `cpuId` + accuracy

Files: each `Ae2CraftingTimeSavedData` (`versions/*/...`), shared DTOs.

- Bump top-level save `version` to `2`.
- Encode `cpuId` per output (absent/empty for old).
- Decode `version: 1` with `cpuId = ""`; decode `version: 2` reading `cpuId`.
- Persist accuracy too: add a top-level `accuracy` list of
  `PersistedAccuracySamples` keyed by the same cpu-aware `ProfileKey` (read
  optionally so `version: 2` saves without it still load).
- `ProfilerBridge.load` re-snapshots and rewrites both samples and accuracy; the
  first save migrates the file to `version: 2` (`ProfilerBridge.java:167`).
- On `ACCURACY.finish(...)`, `ProfilerBridge` marks the saved data dirty and writes
  both snapshots (`ProfilerBridge.java:51-54, 138-141`).

Tests: existing `Ae2CraftingTimeSavedDataTest` extended — roundtrip of a
`cpuId`-bearing sample entry **and** a `cpuId`-bearing accuracy entry; loading a
synthetic `version: 1` blob yields `cpuId = ""` and equivalent stats; storage id
stays exactly `ae2-crafting-time`; accuracy reloads into the correct
`(networkId, cpuId, outputId)` bucket.

## Task 6 — Packet codec

File: `shared/src/mcCommon/.../net/StatsPacketCodec.java`

- Add a leading `boolean cpuAware` flag to `writeSnapshot` / `readSnapshot`.
- When `cpuAware`, write/read `networkId`, `cpuId`, `outputId`; else write/read the
  legacy two-field key.
- Construct `new ProfileKey(networkId, cpuId, outputId)`.
- Add a `cpuSummaries` section after `entries`: for each CPU a `(cpuId, name,
  coProcessors, outputs)` compact block, where each output carries the CPU-specific
  `ProfileStats` **aggregate without raw sample lists**. Gate it behind `cpuAware`
  (`data-model.md`).

Tests: `StatsPacketTest` — snapshot with cpuId roundtrips; `cpuSummaries` roundtrips
without exceeding the raw-sample size; legacy-shaped buffer (without cpuId)
decodes with `cpuId = ""` when the flag is `false`.

## Task 7 — Server enumerates grid CPUs into `cpuSummaries`

File: `shared/src/mcCommon/.../StatsRequestHandler.java`

- In `collect`, read `context.grid().getCraftingService().getCpus()`
  (`collection.md`).
- For each CPU and each requested output, build the CPU-specific aggregate
  `ProfileStats` via `ProfilerBridge.stats(networkId, cpuId, outputId)` (raw samples
  stripped for the wire).
- Attach the `cpuSummaries` list to the response/snapshot.

Tests: `StatsRequestHandlerTest` — response includes one summary per grid CPU;
missing CPU data is omitted per output; network-level `entries` still cover the
displayed CPU.

## Task 8 — Pinned-CPU request path

Files: `shared/src/mcCommon/.../StatsRequestContext.java`, a new
`CraftConfirmMenu` accessor, `ProfilerBridge.java`

- `StatsRequestContext.current` must also read the selected CPU from
  `CraftConfirmMenu` (the `cpuCycler` selection), not only `CraftingCPUMenu`.
- When that CPU is present, `ProfilerBridge.entry(...)` keys by
  `(networkId, cpuId, outputId)` and returns cpu-specific `entries` (with raw
  samples) so the detail/chat views honor rule 1.
- The lookup helper returns `CpuStatsResult` so the client knows `cpuSpecific`.

Tests: `StatsRequestContextTest` — opening a CraftConfirmMenu with a selected CPU
yields that CPU in the context; `CpuStatsResult.cpuSpecific` is `false` on fallback.

## Task 9 — Crafting Plan window: min headline, breakdown, `*` marker

File: `shared/src/mc1201/.../mixin/CraftConfirmScreenMixin.java`, `TtcText.java`

- **Unchosen:** compute each CPU's Total TTC from `cpuSummaries` + plan row amounts;
  headline = minimum; render `Total TTC: ~...*` plus the per-CPU breakdown
  `CPU Alpha ~..* · CPU Beta ~..*`. Show the "depends on CPU" legend.
- **Pinned:** switch every stat to the selected CPU via the fallback lookup. No `*`
  when that CPU has its own data for all rows; `*` + legend when any row fell back.
  Hide the breakdown list while pinned.
- Add `TtcText.totalTtcCpuDependent(...)`, `TtcText.cpuDependentLegend()`, and the
  matching translation keys.

Tests: shared tests cover `CpuStatsResult` fallback and the `TtcText` `*`/legend
formatting; client UI verified in a Prism/VM world per `working-with-project.md`.

## Task 10 — Accuracy and stall keyed by CPU, accuracy persisted

File: `TtcAccuracyTracker.java`, `ProfilerBridge.java`, `Ae2CraftingTimeSavedData`
(per version)

- Record `cpuId` into the accuracy key from `startJob` (`collection.md`).
- Scope stall diagnostics to the CPU when known (stall stays runtime-only; it
  describes the in-flight delayed output, not a learned value).
- Expose per-CPU accuracy with network-level fallback; keep accuracy diagnostic-only
  (no feedback into throughput).
- Add `TtcAccuracyTracker.snapshotAccuracy()` / `loadAccuracy(...)` and persist the
  result in the `accuracy` list so per-CPU accuracy survives restarts
  (`data-model.md`, Persisting accuracy). `ProfilerBridge.load` hydrates accuracy
  alongside samples.

Tests: `TtcAccuracyTrackerTest` — accuracy stored under cpu-specific key; fallback
returns network accuracy when CPU has none; stall diagnostic keys by CPU;
`Ae2CraftingTimeSavedData` roundtrips a cpu-specific accuracy entry and reloads it
into the same `(networkId, cpuId, outputId)` key.

## Build and verify

- Run `scripts/setup-git.ps1` once if not already done (AGENTS.md).
- Build all versions, then exercise in a Prism/VM world with two CPUs of different
  co-processor counts to confirm: the unchosen headline shows the minimum per-CPU
  TTC with a `*` and the per-CPU breakdown; selecting a CPU pins every stat to it
  (no `*` when it has its own data, `*` when it falls back); and old saves load
  without data loss.
- No local test run required before the PR; the post-commit hook opens the PR and
  CI runs checks (AGENTS.md).

## Rollout notes

- This is a data-format change (save `version: 2`, packet `cpuAware` flag +
  `cpuSummaries`). Keep `version: 1` decode forever so downgrades and partial saves
  stay safe.
- Changelog under `IMPROVED`: Crafting Plan now shows the fastest-CPU Time To Craft
  with a per-CPU breakdown; selecting a CPU shows that CPU's own times, marked with
  `*` ("depends on CPU") when it lacks measured data. No raw commit logs.
