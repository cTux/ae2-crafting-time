# NO CHANNEL research and technical design

Implements the proposed [specification](spec.md), tracked in
[#405](https://github.com/cTux/ae2-crafting-time/issues/405).

## Verified evidence

Repository baseline: [415248db685390f5fb93736d8b874010a869f8e1](https://github.com/cTux/ae2-crafting-time/tree/415248db685390f5fb93736d8b874010a869f8e1),
equal to fetched `origin/master` on 2026-09-13. This is source research, not
compiled-hook or in-game proof. The following pinned AE2 sources were fetched
and inspected for all four dependency defaults in `versions/*/build.gradle`.

| Target / AE2 | Pattern-provider source | Node API | Channel implementation |
| --- | --- | --- | --- |
| 1.20.1 Forge / 15.0.10 | [PatternProviderLogic](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/bcdb7c040bc3badf24e3354381d6fe0fe6490592/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java#L269-L302) | [IGridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/bcdb7c040bc3badf24e3354381d6fe0fe6490592/src/main/java/appeng/api/networking/IGridNode.java#L117-L149) | [GridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/bcdb7c040bc3badf24e3354381d6fe0fe6490592/src/main/java/appeng/me/GridNode.java#L446-L448) |
| 1.20.1 Fabric / 15.0.10 | [PatternProviderLogic](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java#L267-L300) | [IGridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/api/networking/IGridNode.java#L117-L149) | [GridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/me/GridNode.java#L446-L448) |
| 1.21.1 NeoForge / 19.0.24 | [PatternProviderLogic](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java) | [IGridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/api/networking/IGridNode.java#L117-L149) | [GridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/me/GridNode.java#L482-L484) |
| 26.1.2 NeoForge / 26.1.10-beta | [PatternProviderLogic](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java#L280-L313) | [IGridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/api/networking/IGridNode.java) | [GridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/me/GridNode.java#L505-L507) |

All four share the relevant behavior:

- `getAvailablePatterns()` returns the patterns without checking activity.
- `pushPattern` first rejects a nonempty send queue, an inactive `mainNode`,
  or a pattern it no longer owns, before the lock/target/input checks.
- `isBusy()` checks the send queue, not channel availability.
- `IGridNode.isActive()` combines power, finished boot, and channel requirements.
  `meetsChannelRequirements()` is true for a node not requiring a channel or
  when its allocated channel count is positive. `hasGridBooted()` excludes
  pathing recalculation. Therefore `!isActive()` is not a channel diagnosis.

The [official channels guide](https://guide.appliedenergistics.org/1.21.1/ae2-mechanics/channels)
also explains that routing can leave devices without channels even when spare
capacity exists elsewhere. The tooltip deliberately avoids naming an exact
cable or assuming a fixed capacity; use the node API, including channel modes.

Current project behavior is confirmed in `shared/src/mcCommon/java/com/ctux/
ae2craftingtime/mc1201/{ProviderDispatchObserver,ProviderDispatchContext}.java`
and `mixin/PatternProviderLogicMixin.java`: the first observed provider check
is the lock lookup, so inactivity exits with an unchecked frame and UNKNOWN.
Both `ProfilerBridge` variants test provider-list presence for NO PROVIDER,
not activity. A first-dispatch waiting timer consequently remains Waiting.

## Observe the actual guard

Extend the existing `PatternProviderLogicMixin` in `mcCommon`; no new mixin
registration, provider registry, dependency, or CPU hook is needed. Wrap the
existing `IManagedGridNode.isActive()Z` invocation inside `pushPattern` with
MixinExtras `WrapOperation`. Call the original once and return its unchanged
boolean. Read that receiver's nullable `getNode()` and convert its presence,
power, boot, and channel results to facts for the current dispatch frame.
Observe only the matched provider frame in `ProviderDispatchContext`.

Put the classification predicate in the Minecraft-free `ProviderDispatchTracker`:
an observed failed activity check plus a present, powered, booted node with
unmet channel requirements proves `AttemptResult.NO_CHANNEL`. The adapter
only extracts facts and delegates. No inference from a generic false push,
client cache, block position, `getUsedChannels()` heuristic, or `isOnline()`.
An absent node is unknown. If the queue short-circuits the activity check,
the new fact is never observed. Power loss and reboot remain unknown here;
they do not create NO POWER, which retains its dispatch-energy meaning.

In `ProviderDispatchContext.Frame.finish`, preserve SUCCESS first, then return
NO CHANNEL for the proven activity rejection before the existing unchecked
fallback. Existing lock/target/input handling stays intact. Calls outside a
CPU dispatch frame add no state. Nested frames retain existing scope behavior.
An addon overriding the method participates only if it actually executes this
guard and returns a failed dispatch through the observed path; success wins.

Extend `ProviderDispatchTracker.AttemptResult` and append `NO_CHANNEL` to
`CraftingBlockReason`. Replace the current final "otherwise LOCKED" mapping
with an explicit mapping that covers the new result. Preserve `Evaluation`'s
unanimous-reason rule: complete iteration, at least one candidate, no success,
no unknown/busy alternative, and agreement among all observed failures.
Do not rescan `CraftingService.getProviders()`; the existing native and
AdvancedAE observers see the iterable selected after addon wrappers.

## State, recovery, and ordering

Reuse `ProviderDispatchTracker`'s per-CPU/per-pattern failures and positive
output keys. Add NO CHANNEL to its accepted reasons and rank it above LOCKED.
Its existing 20-tick expiry, backwards-tick invalidation, replacement, and
cleanup implement NC-05. Do not retain provider nodes or allocate a second
tracker. An old observation can remain until expiry if dispatch stops; that
bounded observation is not a claim that a fresh channel check occurred.

`CraftProfiler.blockReasons` already overlays dispatch reasons beneath
NO PROVIDER/NO POWER using `putAll`. Preserve that ordering and the
selected-scope remembered-status rules. `rememberBlockReason` explicitly
whitelists only NO PROVIDER and NO POWER; retain that whitelist. Do not append
to `StatusKind` or modify `PersistedStatusTag`/SavedData. Save/reopen and runtime
reload must not alias NO CHANNEL to another remembered status.

No polling, insertion simulation, forced dispatch, channel-mode changes, or
inventory mutation is added for diagnostics. Successful dispatch learning,
total TTC, `ProviderStartTracker`, notifications, locate, and plates stay intact.

## Transport, rendering, and target boundaries

Reuse `blockReasons` in the existing snapshot, `StatsPacketCodec`, and
`ClientStatsCache`. Preserve MAX_KEYS, enum validation, requested-key filtering,
authorized menu/CPU context, request rate limits, and replacement of omitted
requested keys even without samples. New enum values are a wire boundary:

| Loader | Inspected boundary | Planned boundary |
| --- | --- | --- |
| Forge 1.20.1 | `PROTOCOL = "17"` | `18` |
| Fabric 1.20.1 | `stats_snapshot_v9` | `stats_snapshot_v10` |
| NeoForge 1.21.1 / 26.1.2 | registrar `16` | `17` on both |

If the implementation base advances, increment that current boundary once;
never reuse an incompatible identifier or roll it back. Preserve Fabric's
capability check and existing mismatched-peer policy. No saved-data migration.

Add badge recognition in `CraftingRowState` and include NO CHANNEL in
`TtcText.isDispatchReason` for the mixed-row qualifier. Generic
`TtcText.blockReason` already supplies the bold red style and translation key.
The shared `CraftingStatusTableRendererMixin` and both
`CraftingCPUScreenMixin` variants must retain pending-only rendering and
unknown-time sorting. Add the exact three locale keys from the spec in
English/Ukrainian; document the new status in both GuideME locale trees.
Update `docs/dependencies.md` to describe the verified integration scope,
without raising minimums or claiming arbitrary provider-addon support.

Common logic goes in `shared/src/main`; Minecraft adapters go in `mcCommon`.
Review both `mc1201` and `mc2612` bridges/codecs/screens and all four loader
wrappers. The existing `neoforge` AdvancedAE CPU adapter is also compiled for
Forge 1.20.1; it must exercise the same observer on its three applicable targets.
No new bespoke adapters for unrelated CPU/provider addons. Existing retained
variants still need contract/packaging coverage; live tests use the newest
implemented adapter. See the [implementation plan](implementation-plan.md).

## Review decisions

- Generic inactivity was rejected: it would mislabel power loss and reboot.
- Merely finding one starved provider was rejected: another provider may work.
- New persistent state was rejected: current dispatch tracking already owns
  exact-pattern aggregation and bounded recovery.
- This source-backed design still needs compiled descriptor checks and actual
  saturated-channel fixtures; no runtime success is claimed by this plan.
