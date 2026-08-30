# No Provider Status Technical Design

## Evidence and ownership

AE2 keeps accepted pattern tasks in `CraftingCpuLogic`. On every dispatch
attempt it calls `CraftingService.getProviders(details)` for the planned pattern.
An empty result is the exact supported signal for `NO PROVIDER`.

The server must own this state. `CraftingStatusEntry` exposes only stored,
active, and pending amounts, so the client cannot distinguish an empty provider
lookup from missing inputs, busy providers, power shortage, or normal queueing.

## Runtime state

Add a Minecraft-free status value for dispatch blockers:

```text
CraftingBlockReason.NO_PROVIDER
CraftingBlockReason.NO_POWER
```

`CraftProfiler` keeps a runtime-only map by CPU identity and network-scoped
output key. Each entry stores the reason and the game tick when it was last
observed. A query returns a reason only while it is no more than 20 ticks old.
Repeated failed attempts refresh the tick. This makes a resolved condition
disappear within the existing one-second request cadence without tracking AE2
provider objects or patterns in the core.

If different remaining patterns for one output report different blockers in
the same window, `NO_PROVIDER` wins over `NO_POWER`. Clear all blocker state on
finish, cancellation, profiler disable, and sample reload.

## AE2 hooks

Extend the existing standard CPU execution mixin at the provider lookup inside
`executeCrafting`. Preserve AE2's return value. When the returned provider
collection is empty, report `NO_PROVIDER` for every positive output of the
current pattern through `ProfilerBridge` using the concrete CPU scope, network
id, and current game tick.

Use the equivalent verified hook in `AdvancedCraftingCpuLogicMixin` where
AdvancedAE is present. Keep it optional with the existing pseudo-mixin and
compile-only dependency. If one supported AdvancedAE artifact lacks the seam,
leave that artifact's behavior unchanged rather than guessing from scheduled
amounts.

The implementation must inspect the supported AE2 and AdvancedAE artifacts
before selecting redirect descriptors. No client inference or provider polling
is allowed.

## Request and packet flow

Add a bounded `blockReasons: map<outputId, reason>` beside `waitingTicks` in the
existing snapshot:

```text
StatsRequestHandler
  -> ProfilerBridge.blockReason(network key, selected CPU, game tick)
  -> StatsPacketCodec.Snapshot.blockReasons
  -> loader packet records
  -> ClientStatsCache
```

Only requested keys may be returned. Decode at most `PacketLimits.MAX_KEYS`
entries and validate every output id. Encode the two known enum values with a
bounded numeric representation and reject unknown ordinals. Because both
planned server blockers share this transport, add both values and their client
handling in the same compatibility-boundary change even if the two status
issues are implemented in separate commits.

Bump every affected wire boundary once:

- 1.20.1 Forge protocol `6` to `7`;
- 1.20.1 Fabric snapshot id `stats_snapshot_v4` to `stats_snapshot_v5`;
- 1.21.1 and 26.1.2 NeoForge registrar `5` to `6`.

No saved-data version changes. Block reasons are runtime-only.

## Client behavior

`ClientStatsCache` replaces block reasons for all requested keys, so omitted
values remove stale state. Opening the screen or switching CPU clears cached
block reasons with waiting state and the request cooldown.

Use a covered pure-Java row-state resolver with this visible priority:

1. `NO SPACE` for a stored-only row when AE2 says it cannot store CPU contents;
2. `NO PROVIDER`;
3. `NO POWER`;
4. `Waiting`;
5. `DELAYED`;
6. TTC or `Collecting data`.

`CraftingStatusTableRendererMixin` uses the result for the visible line and
appends the localized explanation and suggestion to the tooltip. Blocked rows
have no sortable TTC and use warning red rather than the TTC color scale.

## Failure handling

- A missing selected CPU returns no blocker state.
- Provider lookup failures are never inferred from pending amounts.
- A stale server observation expires after 20 ticks and is removed by the next
  snapshot.
- Mixed versions fail the loader compatibility boundary.
- Packet limits and enum validation prevent unbounded or invalid client state.
