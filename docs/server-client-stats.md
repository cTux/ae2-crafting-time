# Why Crafting Stats Stay On The Server

Date: 2026-06-21

## The Rule

The Minecraft server owns all craft timing, throughput, first-dispatch waiting
state, delay, prediction accuracy, and bottleneck diagnostics. The client only
shows the snapshot it receives.

That matters on dedicated servers because:

- AE2 craft execution runs on the server.
- The server sees the real tick clock, pattern pushes, and completed outputs.
- The client may not have the same mod state, world state, or timing data.
- Client-side calculation would be wrong or empty in multiplayer.

Singleplayer follows the same rule. An integrated world still has a logical
server, so there is no separate shortcut that reads profiler data from the
client.

## How It Works Today

The implementation uses the server-owned path for both dedicated servers and
singleplayer:

- server-side AE2 mixins record `start` and `complete` events through
  `ProfilerBridge`
- accepted jobs register their crafted outputs until each output's first
  pattern dispatch
- client UI mixins request visible output ids through `StatsRequestC2S`
- the server looks up retained stats for the active AE2 network and output ids
- `StatsSnapshotS2C` updates `ClientStatsCache`
- UI code renders only from the client display cache

## The Data Flow

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
- per-CPU first-dispatch waiting state
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
```

Rules:

- Client requests output ids visible in its current AE2 or optional integration UI.
- Server treats keys as hints, not trusted facts.
- Requests are capped at 256 output ids and 512 ids per player per second.
- Server replies with known stats for the player's active AE2 network and silently omits unknown keys.

### `StatsChatC2S`

Carries only a bounded output id, amount, and `SHOW` or `RESET` action. The
server resolves the player's current AE2 network, reads or clears authoritative
stats, and formats the translatable message. `SHOW` details are broadcast as
player-attributed chat; the `RESET` confirmation is sent as a private system
message visible only to the player who triggered it. Clients never send
chat text for the server to relay. A reset is accepted only when that output has
retained stats on the player's current network.

### `StatsSnapshotS2C`

Sent from server to only the requesting player.
Fields:

```text
requestedKeys: list<string>
networkAmounts: map<string, long>
waitingTicks: map<string, nonnegative long>
entries: list {
  key: string
  unit: item | millibucket | mana
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
  no-progress threshold is reached. Live data always wins; after a reload the
  last remembered stall fills the row until fresh observations arrive (see
  status persistence below).
- Waiting ticks are included for requested outputs that the selected crafting
  CPU has not dispatched yet, even when those outputs have no retained stats.
  The map is bounded to 256 output ids. Live data always wins; after a reload
  remembered waiting rows return until the craft dispatches or finishes.
- Client drops cache entries for requested keys before applying returned stats.
- Missing stats or waiting values therefore remove old client state instead of
  leaving stale values behind.

### `ProviderHighlightS2C`

Sent from server to only the clicking player after a locate click.

Fields:

```text
dimensionId: string
positions: list<BlockPos>, at most 16
outputId: string, at most 128 chars (profile key id, e.g. an item id)
durationSeconds: nonnegative int (15)
```

Rules:

- Positions resolve server-side through live grid nodes at notify time;
  clients never send positions.
- The locate command (`/ae2craftingtime locate <record>`) only serves
  records owned by the clicking player. Missing or foreign records answer
  with a private expiry notice and highlight nothing.
- The client draws thick (2-3x) rainbow-cycling outline boxes while in the
  same dimension until the duration expires, plus the output item centered
  on a red plate on each camera-facing face (plate-only when the output id
  is not an item)
  (see [issue #237](https://github.com/cTux/ae2-crafting-time/issues/237)).

### Provider-start persistence

Per-output provider links (owner, provider positions, display name) persist
in the world `SavedData` beside throughput samples under a `providers`
section. Old saves without the section load with empty provider state.
After a reload, resumed crafts warn again with a working link because the
owner and positions fall back to the persisted copy when live dispatch data
is absent.

### Status persistence

Per-output statuses (delayed, waiting, no provider, no power) persist in the
world `SavedData` under a `statuses` section beside samples and provider
links:

```text
statuses: [
  { networkId, key, kind: delayed | waiting | no_provider | no_power,
    idleTicks, typicalTicks, acceptedAtTick }
]
```

Bounded to 256 entries, tolerant reads, no save-version bump; old saves
load with no remembered statuses. Live dispatch data always wins: any new
pending craft drops the remembered status for its output, finishing or
cancelling drops it, and a still-remembered row only shows while nothing
live contradicts it. `NO SPACE` stays live-only because the client derives
it from the open CPU screen each frame.

## UI Flow

1. An AE2 or optional integration UI renders or rebuilds.
2. Client collects output ids from currently visible nodes.
3. Client sends `StatsRequestC2S`.
4. Server reads stats and first-dispatch waiting time from server
   `CraftProfiler`.
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
notifyOnDelayed = true
```

Client config:

```text
showInTree = true
```

`showChatMessages` controls the public Ctrl-click TTC details and the private reset confirmation. Reset still works when this is false.
`notifyOnDelayed` controls the private delayed-output warning sent only to the craft owner. The server owns generation; the recipient remains the job initiator.

Config storage is loader-specific, but ownership stays the same: `enabled`,
`showChatMessages`, and `notifyOnDelayed` affect server behavior, while `showInTree` affects local
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

This is observational data and the risk is low, but the boring safety rules
still apply:

- client requests are hints only
- every collection and string is bounded while decoding, before allocation
- clients send structured chat actions, never server-relayed text
- server computes all stats
- server sends only aggregate stats, not pending tasks or machine internals
- packets should be handled on the correct server/client thread

## Sources

- Forge SimpleImpl networking docs: https://docs.minecraftforge.net/en/latest/networking/simpleimpl/
- Forge community packet targeting notes: https://forge.gemwire.uk/wiki/Sending_Packets
- AE2 local source inspected: `CraftingCpuLogic`, `ExecutingCraftingJob`, `ICraftingPlan`, `ICraftingCPU`
