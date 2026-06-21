# Server-Owned Stats Design

Date: 2026-06-21

## Requirement

All craft timing and throughput calculations happen on the Minecraft server. The client only renders performance stats that the server sends.

This matters for dedicated servers:

- AE2 craft execution runs on the server.
- The server sees the real tick clock, pattern pushes, and completed outputs.
- The client may not have the same mod state, world state, or timing data.
- Client-side calculation would be wrong or empty in multiplayer.

## Current Gap

The current implementation has the right collection hook location, but the wrong display data source:

- Server-side AE2 mixin records `start` and `complete` events through `ProfilerBridge`.
- Crafting Tree client mixin currently calls `ProfilerBridge.stats(...)` directly.

That works only by accident in a single integrated client/server. On a dedicated server, the client-side `ProfilerBridge` is separate RAM and will not contain server samples.

## Target Architecture

```text
AE2 server craft hooks
  -> server RAM CraftProfiler
  -> StatsRequestC2S visible output keys
  -> server snapshots matching those keys
  -> StatsSnapshotS2C
  -> client RAM display cache
  -> Crafting Tree render/tooltip
```

## Data Ownership

Server owns:

- `CraftProfiler`
- pending operation matching
- rolling sample buffers
- average duration and throughput calculation
- config values that affect collection: `enabled`, `samples`

Client owns:

- last received display cache
- UI formatting
- Crafting Tree node lookup
- config value that affects local display only: `showInTree`

Client must not:

- calculate timings
- mutate server profiler state
- infer missing stats from local tick time
- receive raw pending operations

## Packet Shape

Use Forge `SimpleChannel` for 1.20.1 Forge.

Forge docs describe `SimpleChannel` as the straightforward custom packet system for sending custom data between client and server. The docs also show creating a static channel and registering messages. For server-to-client targeting, Forge/Gemwire notes `PacketDistributor.PLAYER` sends to a specified `ServerPlayer`.

### `StatsRequestC2S`

Sent when Crafting Tree opens or its visible node set changes.

Fields:

```text
requestId: int
keys: list<string> output ids
```

Rules:

- Client may request only output ids visible in its current Crafting Tree.
- Server treats keys as hints, not trusted facts.
- Server replies with known stats for matching keys and silently omits unknown keys.
- Rate limit if needed later; not needed for first pass.

### `StatsSnapshotS2C`

Sent from server to only the requesting player.

Fields:

```text
requestId: int
entries:
  key: string
  unit: item | millibucket
  sampleCount: int
  averageDurationTicks: double
  amountPerTick: double
  amountPerSecond: double
  lastDurationTicks: long
```

Rules:

- Snapshot is immutable display data.
- Client replaces cache entries for returned keys.
- Client may keep old entries briefly, but simplest first pass is "replace on response".

## UI Flow

1. Crafting Tree renders or rebuilds.
2. Client collects output ids from currently visible nodes.
3. Client sends `StatsRequestC2S`.
4. Server reads stats from server `CraftProfiler`.
5. Server sends `StatsSnapshotS2C` to that player.
6. Client stores snapshot in `ClientStatsCache`.
7. Crafting Tree mixin renders stats from `ClientStatsCache`.

No Crafting Tree installed:

- Client sends no requests.
- Server still may collect stats when `enabled = true`, but no UI exists.
- No fallback screen.

## Config

Server config:

```text
enabled = true
samples = 20
```

Client config:

```text
showInTree = true
```

For the current Forge common config file, we can keep all three values in one config initially. Semantically, `showInTree` is client display only; later it can move to client config without changing protocol.

## Version Layout

Shared module:

- `CraftProfiler`
- `ProfileKey`
- `ProfileStats`
- packet DTOs if they can stay Minecraft-free
- client display cache if Minecraft-free

`versions/1.20.1`:

- Forge `SimpleChannel`
- packet encode/decode/handlers
- AE2 mixins
- Crafting Tree UI mixin
- AE2 key conversion: `AEKey` to output id

## Implementation Steps

1. Add `ClientStatsCache` in shared or version code.
2. Add `StatsSnapshot` data model.
3. Add Forge `SimpleChannel` in `versions/1.20.1`.
4. Add `StatsRequestC2S` and `StatsSnapshotS2C`.
5. Change Crafting Tree mixin:
   - collect visible output ids
   - send request
   - render from `ClientStatsCache`
   - stop calling `ProfilerBridge.stats(...)` on the client
6. Keep AE2 collection hooks server-side only.
7. Add one small test for cache replacement and missing keys.

## Security / Trust

This data is observational and low risk, but still do the boring safe thing:

- client requests are hints only
- server computes all stats
- server sends only aggregate stats, not pending tasks or machine internals
- packets should be handled on the correct thread using Forge packet context enqueueing

## Sources

- Forge SimpleImpl networking docs: https://docs.minecraftforge.net/en/latest/networking/simpleimpl/
- Forge community packet targeting notes: https://forge.gemwire.uk/wiki/Sending_Packets
- AE2 local source inspected: `CraftingCpuLogic`, `ExecutingCraftingJob`, `ICraftingPlan`, `ICraftingCPU`
