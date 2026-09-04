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
expiry notice, and otherwise sends the highlight packet to the clicker only.

One shared `FriendlyByteBuf` codec carries `dimension id, positions,
duration seconds`; each loader wraps it in its own S2C packet following the
existing snapshot pattern:

```text
locate click (command, runs as the clicker, silent)
  -> record lookup (owner must match clicker)
  -> ProviderHighlightS2C(dimension, positions, 15s) to the clicker only
```

Adding the packet changes the wire registry. Bump every affected
compatibility boundary in the same commit:

- 1.20.1 Forge channel protocol: `9` to `10`;
- 1.20.1 Fabric: new `provider_highlight_v1` channel (existing channels
  keep their versions because their layouts do not change);
- 1.21.1 and 26.1.2 NeoForge registrar version: `8` to `9`.

## Client behavior

A small client store keeps the latest highlight (dimension, positions,
expiry timestamp) and prunes it on access. Each loader draws red outline
boxes while the highlight is live and the player is in the same dimension:

- 1.20.1 Forge: game-bus subscriber on the translucent-particles render
  stage;
- 1.20.1 Fabric: `WorldRenderEvents.AFTER_TRANSLUCENT`;
- 1.21.1 and 26.1.2 NeoForge: game-bus subscriber on the render-level
  stage event.

Plain `LevelRenderer.renderLineBox` boxes are enough for at most 16
positions over 15 seconds; no extra render library is needed. Every loader
drives the box opacity from one shared one-second pulse so the highlight
blinks instead of sitting static.

The warning message splits the status word out of the sentence so it can be
styled without breaking translation order:

```text
chat.delayed: "%s %s: no output for %s (typically %s)"
chat.delayed.word: "is delayed" / "затримується" (red)
```

The name is underlined with a hover hint while clickable. Placeholder counts
stay matched between English and Ukrainian.

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
  -> highlight packet -> red boxes for 15 seconds

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
