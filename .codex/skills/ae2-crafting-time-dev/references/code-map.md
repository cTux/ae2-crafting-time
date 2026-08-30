# Code Map and Runtime Invariants

Start here before adding code. Find the path that already owns the behavior,
then recheck the current files because supported targets and upstream APIs move.

## Source Layers

| Path | Owns |
| --- | --- |
| `shared/src/main/java` | Minecraft-free profiling, value types, TTC/accuracy/stall calculations, cache, sorting, color, limits, cooldowns, and rate limits. |
| `shared/src/mcCommon/java` | AE2/Minecraft code shared by every target: server hooks, request handling, codecs, chat, client text/cache bridges, and common UI mixins. |
| `shared/src/mc1201/java` | AE2 15/19 and Minecraft 1.20.1/1.21.1 API adapters, UI mixins, input, persistence tags, and profiler bridge. |
| `shared/src/mc2612/java` | AE2 26 and Minecraft 26.1.2 ports of the API-sensitive adapters. |
| `shared/src/neoforge/java` | Config and AdvancedAE integration shared by NeoForge targets. |
| `versions/<target>` | Loader entrypoints, networking registration and packet records, SavedData glue, metadata, target tests, and Gradle packaging. |
| `shared/src/main/resources` | English and Ukrainian translations. |
| `shared/src/mc1201/resources`, `versions/*/src/main/resources` | Mixin registration, pack metadata, and loader dependency metadata. |
| `scripts` | Git hook setup, development-client dependency resolution, multi-target builds, and release automation. |

Current Gradle projects are `:shared`, `:fabric_1_20_1`, `:mc_1_20_1_forge`, `:mc_1_21_1_neoforge`, and `:mc_26_1_2_neoforge`. Java toolchains are 17, 17, 21, and 25 for the four game targets.

## The Pure-Java Core

- `CraftProfiler` owns pending work per CPU identity, network-wide busy windows, retained samples, weighted throughput, outlier filtering, stall timing, capacity freshness, import/export, and reset behavior.
- `TtcAccuracyTracker` freezes accepted-plan estimates and records successful completion accuracy. Cancelled, invalid, and partially covered jobs must not pollute fully covered accuracy.
- `TimeEstimate`, `TtcSort`, and `TtcColor` are the reusable display calculations. Never reimplement their math in a mixin.
- `ClientStatsCache` replaces requested keys before applying a response so omitted entries remove stale client state.
- `PacketLimits`, `PlayerRequestRateLimit`, `PlayerMessageRateLimit`, and `RequestCooldown` enforce trust and traffic bounds.
- `ProfileKey`, `ProfileStats`, `StatsEntry`, persistence records, accuracy records, `StallDiagnostic`, and enums are immutable boundary values; preserve defensive copies.

## Server-To-Client Flow

```text
CraftingCpuLogic or AdvancedAE mixin
  -> ProfilerBridge
  -> CraftProfiler and TtcAccuracyTracker
  -> Ae2CraftingTimeSavedData
  -> StatsRequestC2S
  -> StatsRequestHandler and StatsRequestContext
  -> StatsSnapshotS2C
  -> ClientStatsCache and network amounts
  -> AE2 or optional-mod UI mixins
```

`CraftingCpuLogicMixin` and `AdvancedCraftingCpuLogicMixin` capture expected output, accepted output, job finish, accepted plan, and recent parallel-slot use. `ProfilerBridge` normalizes AE keys, scopes data by network, hydrates SavedData, marks snapshots dirty, and combines profiler, accuracy, and stall data into `StatsEntry`.

For addon CPUs, check each execution method instead of treating
`ICraftingCPU` discovery as profiling coverage. `ICraftingCPU` exposes status
and capacity, but not pattern dispatch, accepted output, grid, or level. AE2's
concrete `CraftingService` also stores `CraftingCPUCluster` objects, so a
service mixin can observe only the CPUs the service actually exposes. Keep the
inherited `CraftingCpuLogic` hooks as the broad path, then add one optional
adapter only for a proven custom or overridden crafting loop.

`StatsRequestContext` resolves the active AE2 grid and selected standard or AdvancedAE CPU. `StatsRequestHandler` treats requested IDs as hints, resolves authoritative network data, and returns only aggregate entries plus current stored amounts. Network adapters must enqueue handling on the correct side/thread.

## Profiling Semantics

- Identity is `networkId + outputId`; controller-backed networks use the lowest controller anchor. Do not merge different networks.
- A throughput sample is one continuous network production window for an output, including concurrent batches from multiple CPUs.
- Pending work stays scoped by CPU identity and is cleared when that CPU finishes or its stats are reset.
- Retain only the configured latest sample window. Throughput is recent-weighted and rejects extreme duration-per-unit outliers using the configured multiplier.
- Item amounts stay in items. Fluid/chemical-style keys use normalized millibuckets through `AeKeyAmounts`; inspect normalization and saved/runtime samples before changing TTC math.
- Same-tick work has a one-tick minimum. Invalid, disabled, unmatched, or nonpositive events do not create samples.
- Stall output requires retained stats and pending work, then at least 200 idle ticks and twice the learned typical duration. Partial output restarts the idle clock; parallel capacity expires after 20 ticks.
- Capacity means recently used pattern-dispatch slots. Do not describe co-processors as machines remaining busy until outputs return.

## TTC and UI Semantics

- Plan-row TTC uses the row's `craftAmount`.
- Status-row TTC uses `activeAmount + pendingAmount`.
- Plan total adds known row estimates; rows without stats are omitted rather than assigned an invented TTC.
- Running-job total uses elapsed time and overall completed-work progress because rows may execute in parallel.
- Sorting copies the AE2 list; known TTC rows precede unknown rows, equal TTC uses AE2 order, and unknown order stays stable.
- Color context is render-scoped across the current plan: green fastest, yellow midpoint, red slowest; equal known values are green.
- Ctrl-click shows server-authoritative details; Ctrl-Alt-click resets retained stats. Chat actions carry structured intent, never arbitrary client text.
- Prefer `CraftConfirmTableRendererMixin`, `CraftingStatusTableRendererMixin`, and existing screen mixins. `TtcText`, `ClientStats`, `ClientStatsRequests`, and `StatsChatMessages` are the shared UI paths.
- AE2: Crafting Tree and ME Requester integrations exist only on pre-26 targets. Applied Mekanistics is handled through key normalization where supported. AdvancedAE profiling is NeoForge-only.

## Wire, Persistence, and Configuration Boundaries

- Requests allow at most 256 output IDs, each at most 128 characters and matching the namespaced ID pattern. Per-player request cost is at most 512 IDs/second; chat detail/reset messages use a two-second player cooldown.
- Snapshot collections and sample lists must be bounded before allocation. Keep packet fields synchronized across Forge, Fabric, NeoForge 1.21.1, and NeoForge 26.1.2.
- SavedData ID is exactly `ae2-crafting-time`, producing `<world>/data/ae2-crafting-time.dat`. NBT version 1 stores retained `(networkId, key, unit, amount, durationTicks)` samples. Pending work, accuracy, and stalls are runtime-only.
- Config semantics are stable across loaders: `enabled`, `showInTree`, `showChatMessages`, `maxSamples`, and `outlierMultiplier`. Server behavior must not depend on a client-only setting.
- Mixin JSON must keep server mixins under `mixins` and renderer/input integrations under `client`. Optional string-target mixins use tolerant injection only where absence or upstream variation is expected.
- Required and optional dependencies must agree across Gradle, loader metadata, `DEPENDENCIES.md`, run-client profiles, and actual code.
