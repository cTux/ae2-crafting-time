# Server-Owned Stats Design

Date: 2026-06-21

## Requirement

All craft timing and throughput calculations happen on the Minecraft server. The client only renders performance stats that the server sends.

This matters for dedicated servers:

- AE2 craft execution runs on the server.
- The server sees the real tick clock, pattern pushes, and completed outputs.
- The client may not have the same mod state, world state, or timing data.
- Client-side calculation would be wrong or empty in multiplayer.

Singleplayer must also work. An integrated singleplayer world still has a logical server, so the mod should use the same server-owned profiler and packet snapshot flow instead of special-casing local client reads.

## Current Architecture

The implementation uses the server-owned path for both dedicated servers and
singleplayer:

- server-side AE2 mixins record `start` and `complete` events through
  `ProfilerBridge`
- client UI mixins request visible output ids through `StatsRequestC2S`
- the server looks up retained stats for the active AE2 network and output ids
- `StatsSnapshotS2C` updates `ClientStatsCache`
- UI code renders only from the client display cache

## Target Architecture

```text
AE2 server craft hooks
  -> server RAM CraftProfiler
  -> StatsRequestC2S visible output keys
  -> server snapshots matching those keys
  -> StatsSnapshotS2C
  -> client RAM display cache
  -> AE2 / optional integration render paths
```

## Data Ownership

Server owns:

- `CraftProfiler`
- pending operation matching
- rolling sample buffers
- concurrent production-window aggregation
- average duration and throughput calculation
- config value that affects collection: `enabled`

Client owns:

- last received display cache
- UI formatting
- visible output lookup for the currently open screen
- config value that affects local display only: `showInTree`

Client must not:

- calculate timings
- mutate server profiler state
- infer missing stats from local tick time
- receive raw pending operations

## Packet Shape

Each loader module owns its packet glue. Forge uses `SimpleChannel`, Fabric uses
Fabric networking, and NeoForge uses the NeoForge payload registrar.

### `StatsRequestC2S`

Sent when a supported UI opens or its visible output set changes.

Fields:

```text
keys: list<string> output ids
reset: boolean
```

Rules:

- Client requests output ids visible in its current AE2 or optional integration UI.
- Server treats keys as hints, not trusted facts.
- Server replies with known stats for the player's active AE2 network and silently omits unknown keys.
- If `reset` is true, server clears retained samples for those output keys and replies with no stats for them.
- Rate limit if needed later; not needed for first pass.

### `StatsSnapshotS2C`

Sent from server to only the requesting player.

Fields:

```text
entries:
  requestedKeys: list<string>
  key: string
  unit: item | millibucket
  sampleCount: int
  averageDurationTicks: double
  amountPerTick: double
  amountPerSecond: double
  lastDurationTicks: long
  sampleDurationTicks: list<long>
  sampleAmounts: list<long>
  stall: optional {
    idleTicks: long
    typicalDurationTicks: double
    activeBatches: int
    usedParallelSlots: int
    totalParallelSlots: int
  }
```

Rules:

- Snapshot is immutable display data.
- A sample describes one continuous production window for an output across all
  crafting CPUs on the AE2 network, not one individual pattern push. This makes
  its amount-per-time rate include parallel batches.
- Pending pattern outputs are still matched per crafting CPU. Finishing or
  cancelling a CPU job discards its unmatched pending outputs so they cannot
  inflate a future sample.
- A stall diagnostic is included only for the selected crafting CPU after its
  no-progress threshold is reached. It is runtime-only and never persisted.
- Client drops cache entries for requested keys before applying returned stats.
- Missing entries therefore mean "no known stats for this output in the current network-scoped context".

## UI Flow

1. An AE2 or optional integration UI renders or rebuilds.
2. Client collects output ids from currently visible nodes.
3. Client sends `StatsRequestC2S`.
4. Server reads stats from server `CraftProfiler`.
5. Server sends `StatsSnapshotS2C` to that player.
6. Client stores snapshot in `ClientStatsCache`.
7. UI mixins render stats from `ClientStatsCache`.

Optional UI mod not installed:

- Client sends no requests.
- Server still may collect stats when `enabled = true`, but no UI exists.
- No fallback screen.

Singleplayer:

- Integrated server collects samples.
- Local client sends `StatsRequestC2S` to the integrated server.
- Integrated server replies with `StatsSnapshotS2C`.
- UI renders exactly as it does on a dedicated server.

## Config

Server config:

```text
enabled = true
showChatMessages = true
```

Client config:

```text
showInTree = true
```

`showChatMessages` controls the public Ctrl-click TTC detail and reset player chat messages. Reset still works when this is false.

Config storage is loader-specific, but ownership stays the same: `enabled` and
`showChatMessages` affect server behavior, while `showInTree` affects local
display only.

## Version Layout

Shared module:

- `CraftProfiler`
- `ProfileKey`
- `ProfileStats`
- packet DTOs if they can stay Minecraft-free
- client display cache if Minecraft-free

`versions/<minecraft>-<loader>`:

- loader packet registration and send helpers
- packet encode/decode/handlers or payload codecs
- AE2 mixins
- optional UI mixins
- AE2 key conversion: `AEKey` to output id

## Security / Trust

This data is observational and low risk, but still do the boring safe thing:

- client requests are hints only
- server computes all stats
- server sends only aggregate stats, not pending tasks or machine internals
- packets should be handled on the correct server/client thread

## Sources

- Forge SimpleImpl networking docs: https://docs.minecraftforge.net/en/latest/networking/simpleimpl/
- Forge community packet targeting notes: https://forge.gemwire.uk/wiki/Sending_Packets
- AE2 local source inspected: `CraftingCpuLogic`, `ExecutingCraftingJob`, `ICraftingPlan`, `ICraftingCPU`
