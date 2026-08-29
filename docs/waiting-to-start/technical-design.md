# Waiting To Start Technical Design

## Research findings

Discussion #65 asks for one fact that the client does not currently have: an
output is in the accepted plan but has not received its first dispatch.

AE2's supported status models expose:

- `CraftingStatus.getElapsedTime()` for the whole job;
- `CraftingStatusEntry.getActiveAmount()` for dispatched work awaiting output;
- `CraftingStatusEntry.getPendingAmount()` for patterns not yet dispatched.

Those amounts describe the current moment. They cannot prove that a row with no
active work has never dispatched before, so a client-only timer would be wrong
after reopening the screen or between later batches.

The current mod already hooks the needed server events:

```text
CraftingCpuLogic.trySubmitJob      -> ProfilerBridge.startJob
CraftingCpuLogic.executeCrafting   -> ProfilerBridge.start
CraftingCpuLogic.finishJob         -> ProfilerBridge.finishJob
AdvancedAE equivalents            -> the same ProfilerBridge methods
```

The same method and status-field seams were checked in the repository's AE2
15.0.10, 19.0.24, and 26.1.10-beta artifacts. No extra AE2 mixin is needed.

## Server state

Extend `CraftProfiler` with one runtime-only map keyed by crafting CPU object
identity. Each value contains:

```text
acceptedAtTick
outputsStillWaitingForFirstDispatch: Set<ProfileKey>
```

Reuse the crafted-output collection already built in `ProfilerBridge.startJob`.
Register its network-scoped keys after AE2 accepts the job. `CraftProfiler.start`
removes the dispatched key from that CPU's waiting set before recording the
existing pending batch.

Expose a query that returns `max(0, currentTick - acceptedAtTick)` only while the
key remains in the selected CPU's set. `clearPending`, `setEnabled(false)`, and
`loadSamples` clear the matching waiting state with the other runtime-only
state.

One CPU cannot accept a second job while busy. A later accepted job for the same
CPU identity replaces any stale waiting record.

## Request and packet flow

Waiting state must be returned even when no `ProfileStats` exists. Do not create
fake stats or make `StatsEntry.stats` nullable.

Add a bounded `waitingTicks: map<outputId, long>` beside `networkAmounts` in the
existing snapshot:

```text
StatsRequestHandler
  -> ProfilerBridge.waitingTicks(network key, selected CPU, game tick)
  -> StatsPacketCodec.Snapshot.waitingTicks
  -> each loader's StatsSnapshotS2C
  -> ClientStats waiting cache
```

Only requested keys may appear in the map. Decode at most `PacketLimits.MAX_KEYS`
entries, keep output IDs within `MAX_OUTPUT_ID_LENGTH`, and reject negative tick
values before storing them.

The four loader packet records must change together because this changes the
wire layout. Bump every affected compatibility boundary in the same commit:

- 1.20.1 Forge channel protocol: `5` to `6`;
- 1.20.1 Fabric snapshot payload ID: `stats_snapshot_v3` to
  `stats_snapshot_v4`;
- 1.21.1 and 26.1.2 NeoForge registrar version: `4` to `5`.

The Fabric request and chat IDs stay unchanged because their layouts do not
change. No persisted-data version changes because waiting state is never saved.

## Client behavior

`ClientStats` replaces waiting values for all requested keys just as it replaces
network amounts. Omitted values remove stale state.

`CraftingStatusTableRendererMixin` uses this order:

1. If `activeAmount == 0`, `pendingAmount > 0`, and cached waiting ticks exist,
   render the localized waiting line.
2. Otherwise, render the existing delayed state when present.
3. Otherwise, render the existing estimated TTC when stats exist.
4. Otherwise, add no TTC line.

Format waiting duration as completed whole seconds with no `~` prefix because
it is measured time, not an estimate. Add a small `TtcText` method and English
and Ukrainian translation keys. Reuse the existing dark row badge; add the new
translation key to its key check.

The status sort and color calculations return no estimate for a cached waiting
row. Plan rows and other screens keep their current behavior.

## State flow

```text
job accepted
  -> register every crafted output as waiting at the accepted game tick
  -> snapshot can return waiting ticks without retained stats

first pattern dispatch for one output
  -> remove that output from the CPU waiting set
  -> start the existing pending throughput batch
  -> later snapshots use TTC or delayed behavior

job finishes or is cancelled
  -> clear pending batches, capacity, and all remaining waiting outputs
```

## Failure handling

- Invalid or empty plans register no waiting outputs.
- A dispatch for an unregistered key keeps existing profiling behavior.
- A missing selected CPU returns no waiting state.
- A stale client value disappears on the next response because replacement is
  scoped by requested keys.
- Game-tick rollback clamps the displayed duration to zero.
- Mixed client/server versions fail the loader's protocol or payload
  compatibility boundary instead of decoding the changed snapshot layout.

## Sources checked

- [GitHub discussion #65](https://github.com/cTux/ae2-crafting-time/discussions/65),
  opened 2026-08-29; it had no comments when checked on 2026-08-30.
- Repository code: `CraftProfiler`, `ProfilerBridge`, `StatsRequestHandler`,
  `StatsPacketCodec`, `ClientStats`, `CraftingCpuLogicMixin`,
  `AdvancedCraftingCpuLogicMixin`, `CraftingStatusTableRendererMixin`, and both
  supported `CraftingCPUScreenMixin` implementations.
- Local supported AE2 artifacts: `CraftingCpuLogic`, `CraftingStatus`, and
  `CraftingStatusEntry` from AE2 15.0.10, 19.0.24, and 26.1.10-beta.
