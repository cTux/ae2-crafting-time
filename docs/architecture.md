# Architecture

AE2 Crafting Time watches real autocrafting throughput on the logical server so
it can explain slow or stalled jobs. The client only displays what the server
sends: TTC estimates, delay warnings, prediction accuracy, and bottleneck clues
supported by the available data, plus whether scheduled outputs are waiting
for their first dispatch. Singleplayer uses the same path because an integrated
world still has a logical server.

## Supported Targets

| Minecraft | Loader | Module |
| --- | --- | --- |
| 1.20.1 | Forge | `:mc_1_20_1_forge` |
| 1.20.1 | Fabric | `:fabric_1_20_1` |
| 1.21.1 | NeoForge | `:mc_1_21_1_neoforge` |
| 26.1.2 | NeoForge | `:mc_26_1_2_neoforge` |

## Code Boundaries

- `shared/src/main/java`: Minecraft-free profiling, estimates, DTOs, cache, and
  helpers.
- `shared/src/mcCommon/java`: AE2/Minecraft-facing code shared by every target.
- `shared/src/mc1201/java`: Minecraft 1.20.1/1.21.1 API boundary.
- `shared/src/mc2612/java`: Minecraft 26.1.2/AE2 26 API boundary.
- `shared/src/neoforge/java`: code shared by both NeoForge targets.
- `versions/<minecraft>-<loader>`: loader entrypoints, packet glue, config,
  saved-data glue, metadata, and loader tests.

## How It Runs

```text
AE2 CraftingCpuLogic mixins on the server
  -> ProfilerBridge
  -> CraftProfiler retained samples
  -> per-CPU first-dispatch waiting timers
  -> frozen job TTC versus successful completion accuracy
  -> delayed-output diagnostics and recent parallel-dispatch capacity
  -> Ae2CraftingTimeSavedData world save snapshot
  -> StatsRequestC2S for visible output ids
  -> server looks up stats for the active AE2 network
  -> StatsSnapshotS2C aggregate entries
  -> ClientStatsCache
  -> AE2 / optional integration UI text
```

The server owns profiling, retained samples, persistence, resets, and aggregate
stats. The client owns the display cache, request cooldowns, formatting, sort
state, and click handling.

Job-accuracy samples are a bounded runtime diagnostic. The prediction is frozen
only after AE2 accepts a plan, and completion is recorded only when
`finishJob(true)` runs. They are not persisted and never alter throughput
samples or displayed TTC calculations.

Stall diagnostics are also runtime-only. For the selected crafting CPU, the
server tracks the last accepted output and AE2's rolling pattern-dispatch use.
An output is delayed after at least 30 seconds without progress and at least
twice its learned average production-window duration. Partial output resets the
timer. The client only renders the server snapshot alongside AE2's active and
scheduled amounts.

First-dispatch waiting state is runtime-only too. The server registers every
crafted output when AE2 accepts a job, removes each output after its first
pattern dispatch, and sends elapsed waiting ticks for requested rows. This lets
the client distinguish work that has never started from a later gap between
batches.

The client never reads profiler state directly. That rule matters in
singleplayer too: local UI still requests snapshots from the integrated server
instead of using a separate client-side profiler.

## Profile Keys

Runtime and persisted stats are scoped by AE2 network plus output:

```text
networkId + outputId
```

For controller-backed networks, `networkId` is derived from the connected
controller anchor position.

## UI Surfaces

The core AE2 screens are always available when AE2 is present:

- craft-confirm plan row TTC lines, color hints, total TTC, sort button, and TTC
  details/reset clicks
- crafting status waiting and TTC lines, total TTC, sort button, and TTC
  details/reset clicks

Optional integrations add UI only when the target mod is installed:

- AE2: Crafting Tree: node TTC lines and details/reset clicks
- ME Requester: row labels and total TTC hints
- Applied Mekanistics: chemical key normalization and display

There is no fallback profiling screen. If an optional UI mod is missing, its
integration simply stays out of the way.

## Persistence

Retained samples are saved through Minecraft `SavedData` as:

```text
<world>/data/ae2-crafting-time.dat
```

The saved payload stores `version`, `networkId`, `key`, `unit`, and retained
`samples`. Pending crafts and first-dispatch waiting state are runtime-only and
are not persisted.
