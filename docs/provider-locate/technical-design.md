# Provider Locate Technical Design

## Research findings

Issue #231 asks for three facts the delayed warning does not currently have:
which provider ran the craft, a red "delayed" word, and state that survives a
world reload.

AE2's supported APIs expose the needed seams:

- `CraftingService.getProviders(IPatternDetails)` returns the providers that
  offer a pattern right now.
- `IGrid.getNodes()` plus `IGridNode.getService(ICraftingProvider.class)`
  identifies which grid node hosts a given provider object by identity.
- `InWorldGridNode.getLocation()` turns such a node into a `BlockPos`. This
  is the same recipe AE2 Network Analyser's `wrapGridNode` uses; nodes that
  are not in-world resolve to nothing and are skipped.

The current mod already observes every pattern dispatch with its outputs:

```text
CraftingCpuLogic.executeCrafting  -> ProfilerBridge.observeProviders(scope, pattern, outputs, hasProvider)
AdvancedAE/ECO/TimeWheel          -> the same ProfilerBridge method
```

`observeProviders` builds the pattern-to-output map and then drops patterns
that have a provider. Only the missing-provider failures are retained. The
location work therefore needs no new mixin: retain the successful patterns
per crafting CPU and output, and resolve positions later, at notify time, so
a provider that moved between dispatch and stall never yields a stale box.

Intercepting the exact `pushPattern` acceptor was rejected: it would touch
four different CPU dispatch paths, including addon-owned logics, while
resolving current candidates at notify time is enough for the first version.

## Server state

Add a Minecraft-facing (not pure-core) tracker beside `CraftProfiler`,
keyed by crafting CPU object identity with network-scoped `ProfileKey`s:

```text
dispatchedPatterns: Map<CpuScope, Map<ProfileKey, Set<IPatternDetails>>>
```

`ProfilerBridge.observeProviders` records the pattern for each of its output
keys. Only the standard and AdvancedAE dispatch paths call it today; NeoEco
and TimeWheel jobs keep working warnings with plain names until their
dispatch paths expose patterns too. `ProfilerBridge.startJob` and
`finishJob` clear the scope, alongside the existing pending/owner cleanup,
so a new job starts a fresh link set.

Position resolution runs only on the notify path with the live grid:

```text
for each stored pattern for (scope, key):
  craftingService.getProviders(pattern)
    -> for each provider, scan grid.getNodes()
    -> keep node.getService(ICraftingProvider.class) == provider
    -> keep InWorldGridNode locations only
```

Cap resolved positions at `PacketLimits.MAX_HIGHLIGHT_POSITIONS` (16).
Dimension comes from the grid pivot level. An empty result means "no
locatable target": the name renders as plain text.

Blocked warnings (`NO SPACE`, `NO POWER`) reuse the same records, packets,
and red-word message shape. Their once-per-episode memory lives in a tiny
pure-core tracker (one instance per reason) keyed by crafting CPU identity:

```text
tick: blockReasons(scope, grid, tick) filtered to NO POWER -> poll episode
tick: probe(logic).isCantStoreItems + stored/outstanding per output -> poll episode
status request: NO POWER backup through the same episode memory
```

Power observation runs on the standard and AdvancedAE dispatch paths, so
`NO POWER` warnings cover those CPU types. The `NO SPACE` probe reads the
AE2-mirrored status methods every CPU logic in the mod supports (`isCantStoreItems`, `getAllWaitingFor`, `getStored`,
`getWaitingFor`) through reflection, so addon-owned logics need no direct
type reference; logics without those methods simply never report. A key
counts when stored items exist with nothing still outstanding, mirroring the
client row predicate. Finishing a job or reloading runtime state clears both
episode memories beside the other per-scope cleanup.

Locate records live in a bounded server registry:

```text
recordId (UUID) -> owner UUID, dimension id, positions, output name, created tick
```

Cap the registry at 256 records with eldest eviction. Records are
click-scoped: a locate click only serves a record owned by the clicking
player.

The persisted fallback is per output key, not per record:

```text
ProfileKey -> owner UUID, dimension id, positions, display name
```

Cap at 256 entries. Live dispatch data always wins; the persisted copy only
fills gaps after a reload. Persist it in the existing world `SavedData`
beside `outputs`, read tolerantly so old saves load with empty provider
state and no save-version bump is needed.

## Command and packet flow

The item name carries `ClickEvent(RUN_COMMAND, "/ae2craftingtime locate
<recordId>")`. Raw coordinates never travel through the click, so the packet
cannot be abused to probe arbitrary positions.

One shared Brigadier builder lives in Minecraft-facing code; each loader
registers it on its own command event. The tree is open to every command
source; the handler itself validates the player and record ownership, and
non-player sources get no answer:

- 1.20.1 Forge: `RegisterCommandsEvent` on the Forge event bus;
- 1.20.1 Fabric: `CommandRegistrationCallback`;
- 1.21.1 and 26.1.2 NeoForge: `RegisterCommandsEvent` on the NeoForge event
  bus.

The handler loads the record, rejects foreign or missing records with the
expiry notice, and otherwise sends the highlight packet to the clicker only,
followed by a private "Highlighting <provider> at <coords> in <dimension>"
system message built by `DelayedChatText.highlightingMessage`. The message
names the provider block at the first resolved position
(`Block.getName()`, with a generic `chat.provider` fallback) instead of the
crafted item, and every coordinate is an underlined literal with a
`RUN_COMMAND` `/tp @s x y z` click and a `chat.teleport.hint` hover, joined
with ", ". The builder lives in the per-version `DelayedChatText` copies
because the click/hover constructors differ between the 1.20.1/1.21.1 and
26.1 mappings; the shared `ProviderLocateCommand.providerName` helper
resolves the name from the level. `chat.highlighting` keeps three
placeholders in both languages. The double-click path
(`ProviderLocateServer.locate`, both source sets) sends the same message
after its highlight. All three send points were reworked for
[issue #241](https://github.com/cTux/ae2-crafting-time/issues/241).
No packet layout changes anywhere in that batch, so no compatibility
boundaries moved.

The highlight packet carries `dimension id, positions, output id, duration
seconds`; the output id is the profile key id the client resolves to an
item icon. Each loader wraps it in its own S2C packet following the
existing snapshot pattern (second layout revision; bump every affected
compatibility boundary again in the same commit):

```text
locate click (command, runs as the clicker, silent)
  -> record lookup (owner must match clicker)
  -> ProviderHighlightS2C(dimension, positions, output id, 15s) to the clicker only
  -> client draws edges for 15s and pins plates while the output stays delayed

 double-click a delayed CPU row (ProviderLocateC2S(output id), no record)
   -> resolve the clicker's open CPU scope and grid, require job ownership
   -> live positions resolve -> same highlight packet to the clicker only
   -> nothing resolves -> private expiry notice
 ```

 Clicking the chat link closes the chat via a `ChatScreenMixin` (one copy
 per version group, same fully qualified name) that watches vanilla
 `handleComponentClicked` at return and closes the screen only when it
 handled our own `/ae2craftingtime locate` run-command. A double-click
 locate closes the CPU screen the same way, right after the request is
 sent.

Adding the packet changes the wire registry. Bump every affected
compatibility boundary in the same commit:

- 1.20.1 Forge channel protocol: `9` to `10` for the first layout,
  then `10` to `11` when the output id field lands, then `11` to `12` for
  the locate request;
- 1.20.1 Fabric: new `provider_highlight_v1` channel, then a new
  `provider_highlight_v2` channel for the output id layout, then a new
  `provider_locate_v1` channel for the locate request (existing
  channels keep their versions because their layouts do not change);
- 1.21.1 and 26.1.2 NeoForge registrar version: `8` to `9`, then `9`
  to `10`, then `10` to `11`.

## Client behavior

A small client store keeps the latest highlight (dimension, positions,
output id, expiry timestamp) and prunes it on access, plus one persistent
plate per located output (dimension, positions, output id, capped at 32).
Each loader draws thick (2-3x) rainbow-cycling outline boxes for the live
15-second highlight, while plates render while
`ProviderHighlightClient.shouldShowPlates` allows: a stall present, or no
cache entry at all (unknown outputs show, e.g. with the CPU screen closed
so no snapshot ever arrived). A positive entry without a stall hides the
plate, and `prunePlates` drops that case once a snapshot arrives
(see [issue #239](https://github.com/cTux/ae2-crafting-time/issues/239)
and [issue #240](https://github.com/cTux/ae2-crafting-time/issues/240)):

- 1.20.1 Forge: game-bus subscriber on the translucent-particles render
  stage;
- 1.20.1 Fabric: `WorldRenderEvents.AFTER_TRANSLUCENT`;
- 1.21.1 NeoForge: game-bus subscriber on the render-level stage event;
- 26.1.2 NeoForge: game-bus subscriber on the render-level stage event for
  edges and plates, plus a `SubmitCustomGeometryEvent` subscriber for the
  item icons (the 26.1 submit pipeline requires items to go through its
  collector).

Edge color cycles rainbow hues on a time-based phase (instead of static
red) so the box contrasts with any environment. Vanilla
`RenderType.lines()` width is fixed, so thicker edges use multi-offset
strokes on 1.20.1/1.21.1 and the `ShapeRenderer` line-width path on 26.1.2.
Face plates are thin red filled boxes (`debugFilledBox` on 1.20.1/1.21.1,
`debugFilledBox` on 26.1, where both pipelines are `QUADS`-mode) with the
output item rendered item-frame style (`FIXED` display context) at half
scale; the client resolves the packet's output id through the item registry
and renders plate-only when it is not an item. Only faces pointing toward
the camera render (at most 3 per block). On 1.20.1/1.21.1 each plate is one
thin box from vanilla `LevelRenderer.addChainedFilledBoxVertices`, flushed
with its own `endBatch` per face, after the invisible-plate follow-up in
[issue #241](https://github.com/cTux/ae2-crafting-time/issues/241):
`debugFilledBox` there is `TRIANGLE_STRIP` with culling and has no vanilla
callers, so appending each face (and the item icon writes between faces)
to one shared strip continued the strip out of phase and the culled
pipeline dropped the plates; every face now uploads as its own
self-contained strip before its icon draws. Raw vertex calls differ
between 1.20.1 (`vertex`/`endVertex`) and 1.21.1 (`addVertex`/`setColor`),
so the shared shapes go through the vanilla static, which exists
identically on both (verified against the 1.20.1 sources and the mapped
1.21.1 jar). On 26.1 the same quads keep feeding its `QUADS`-mode
`debugFilledBox` pipeline unchanged (that matches vanilla
`DrawableGizmoPrimitives`, which accumulates quads into one filled-box
buffer). Every loader keeps driving the box
opacity from one shared one-second pulse so the highlight blinks instead of
sitting static.

Render hooks batch one render type per pass and never hold a non-fixed
consumer across other-type writes: on 1.20.1/1.21.1 the line and filled
buffers share one fallback builder, so writing plates or items and then
reusing a cached lines/filled consumer hits an ended builder and crashes
(`BufferBuilder not started`, fixed for issue #237 follow-up). Edges draw
for all positions first, then plates and icons; each plate face is
flushed with its own `endBatch` before its icon draws (see
[issue #241](https://github.com/cTux/ae2-crafting-time/issues/241)).

The dev-client log for that follow-up also showed the chat auto-close
mixin never applying on 1.20.1 (`handleComponentClicked` is declared on
`Screen`, not `ChatScreen`, so the subclass target never resolves).
The injection now targets `Screen.handleComponentClicked` and returns
early for non-chat screens. Because the 1.21.1 toolchain cannot remap
vanilla targets while the 1.20.1 production mappings need the remap, the
mixin ships as twins: `ChatScreenMixin` (`remap = false`, listed only in
the 1.21.1 config, which the 1.20.1 builds still compile harmlessly) and
`ChatScreenMixinSrg` (remapped, listed only in the shared 1.20.1 config
and excluded from the 1.21.1 compile). 26.1 keeps its own private-method
mixin unchanged.

A delayed row's tooltip appends the gray `locate_hint` line right after the
stall breakdown (only when a stall is present, so only locatable rows offer
it), just before the shared Ctrl-Click details hint; other rows are
untouched (see
[issue #241](https://github.com/cTux/ae2-crafting-time/issues/241)).

The warning message splits the status word out of the sentence so it can be
styled without breaking translation order:

```text
chat.delayed: "%s %s: no output for %s (typically %s)"
chat.delayed.word: "is delayed" / "затримується" (red)
```

The name is underlined with a hover hint while clickable. Placeholder counts
stay matched between English and Ukrainian. Blocked warnings use one shared
three-placeholder sentence with a per-reason red status word and the
existing reason explanation as detail.

## State flow

```text
pattern dispatched
  -> retain pattern per (CPU scope, output key)

output becomes DELAYED, owner online
  -> resolve provider positions through the live grid
  -> create locate record, persist per-key fallback
  -> private warning with clickable red-accented message

click (same session)
  -> record lookup, owner must match
  -> highlight packet -> thick rainbow boxes with item-on-red face plates for 15 seconds

click with foreign/missing record
  -> private expiry notice, no highlight

world reload
  -> records registry starts empty; per-key fallback loads from NBT
  -> resumed dispatches rebuild pending state and re-resolve positions
  -> still-delayed craft warns again with a fresh working link
  -> clicking a pre-reload message explains expiry

job finishes / scope cleared / profiler disabled
  -> drop live links, records for that scope, and per-key fallback on next save
```

## Failure handling

- Missing grid, empty pattern set, or no offering providers: plain name, no
  click action, warning still sent.
- Providers without world positions are skipped silently.
- Oversized or malformed highlight packets are rejected before allocation;
  positions beyond the cap are never stored.
- Mixed client/server versions fail the loader's compatibility boundary
  instead of decoding an unknown packet.
- A command from a non-player source or without permission does nothing.
- Command and render registrations are client/server separated so a
  dedicated server never loads client classes.

## Sources checked

- [Issue #231](https://github.com/cTux/ae2-crafting-time/issues/231).
- Repository code: `CraftProfiler`, `ProfilerBridge` (both source sets),
  `DelayedNotificationServer`, `StatsRequestHandler`, `StatsNetwork` and
  packet records on all four loaders, `Ae2CraftingTimeSavedData` on all four
  loaders, `PersistedSamplesTag`, `PacketLimits`, `ClientStats`.
- Local AE2 artifacts: `ICraftingService.getProviders`,
  `IGrid.getNodes`, `IGridNode.getService`, `ICraftingProvider`,
  `IPatternDetails` from AE2 Forge 15.4.10; `InWorldGridNode.getLocation`
  call shape confirmed in the cached AE2 Network Analyser jar
  (`ItemNetworkAnalyzer.wrapGridNode`).
