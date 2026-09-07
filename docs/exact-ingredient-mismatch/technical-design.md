# Exact ingredient mismatch diagnostics: technical design

Status: planned. Implements the [specification](spec.md).

## Evidence

The source report shows a simulated plan missing a Mekanism energy cell. The
reported fix was to craft the lower tier and re-encode each following pattern
with the produced item; a second player reported the same symptom with tanks.
The discussion suggests saved item data, but does not prove which field differs.
The feature therefore reports only the near-match that the server can verify.

AE2's `CraftingPlanSummary.fromJob` builds exact-key rows, then simulates an
extraction of each key from the active grid storage to calculate `storedAmount`
and `missingAmount`. `AEItemKey` equality includes NBT on AE2 15 and the item
stack component patch on newer AE2 versions. `AEKey.getPrimaryKey()` identifies
the registered item independently of secondary data.

Inspected upstream source anchors:

- [AE2 15.0.10 summary construction](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/menu/me/crafting/CraftingPlanSummary.java)
- [AE2 15.0.10 item-key identity](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/api/stacks/AEItemKey.java)
- [AE2 19.0.24 item-key identity](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/api/stacks/AEItemKey.java)
- [AE2 26.1.10-beta summary construction](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/menu/me/crafting/CraftingPlanSummary.java)
- [AE2 26.1.10-beta item-key identity](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/api/stacks/AEItemKey.java)

These are source checks, not runtime reproduction evidence. Implementation must
verify descriptors against both 1.20.1 artifacts and both NeoForge targets.

## Server classification

Add a shared Minecraft-facing `PlanStoredVariantDetector` called from a thin
`CraftConfirmMenu` mixin after AE2 creates the native `CraftingPlanSummary` and
before the menu discards its completed job.

Snapshot `grid.getStorageService().getInventory().getAvailableStacks()` once.
Build a set of primary item identities for positive `AEItemKey` entries. For
each native summary row, set its flag only when:

```text
row.missingAmount > 0
row.what is AEItemKey
stored candidate amount > 0
candidate is AEItemKey
candidate.getPrimaryKey() == row.what.getPrimaryKey()
candidate != row.what by exact AEKey equality
```

Do not compare display names, serialized strings, tooltip text, profiling IDs,
damage alone, or mod-specific fields. Do not call storage extraction again or
re-run AE2's substitution logic. A set is sufficient because the UI does not
show a candidate amount or enumerate variants. Work is linear in the available
storage keys plus summary rows and is discarded with the menu plan.

The detector returns original summary row indices, before any client sorting.
An empty result sends no diagnostic payload. It never mutates the summary, job,
storage, pattern, or profiler state.

## Transport and lifecycle

Add a mod-owned `PlanStoredVariantsS2C` payload to the existing `StatsNetwork`.
Do not change AE2's native plan packet. The `CraftConfirmMenu` mixin owns a
server summary revision, incremented for every native summary packet. A client
menu mixin owns the matching revision, incremented whenever AE2 installs a new
summary, and clears all stored-variant flags before applying later data.

Send diagnostic chunks immediately after AE2's native plan packet from the same
server broadcast. Each chunk contains `containerId`, positive `summaryRevision`,
`entryCount`, `offset`, `rowCount` from 1 through 256, and a fixed 32-byte bit
mask. Row indices address AE2's original summary order. Send only chunks with a
set bit.

The client handler runs on its normal client executor and requires the active
`CraftConfirmMenu`, matching container, revision, and installed entry count.
Require nonnegative `offset`, `offset % 256 == 0`, and
`rowCount == min(256, entryCount - offset)` using overflow-safe checks. Reject
bits outside `rowCount`. Never allocate from packet-controlled `entryCount`;
validate it against the installed native summary. Apply flags idempotently only
to positive missing `AEItemKey` rows. Invalid, early, late, or mismatched chunks
do nothing.

Packets go only to the player who already received the server-authorized plan.
There is no C2S request, selected-network input, cache keyed by display name, or
world persistence. A closed menu, reconnect, or different container discards
late data. Connection order must be verified on every loader.

Forge appends the message and increments the current channel protocol.
NeoForge appends a clientbound payload and increments each current registrar
version. Fabric registers `plan_stored_variants_v1` and checks receiver support
before sending. Existing packet layouts stay unchanged.

If #320 lands first, reuse its already-implemented summary revision, validation,
and row-flag carrier rather than adding duplicates; the packet and flag remain
independent. This is reuse of shipped code, not a dependency on #320.

## Client rendering

Add a transient stored-variant flag carrier to `CraftingPlanSummaryEntry`. In
the shared `CraftConfirmTableRendererMixin`, append the gold normal-weight
`Stored variant` component only when the flag is set, `missingAmount > 0`, the
key is an item, and the mod is enabled. Append both explanation lines to the
tooltip before the current `craftAmount <= 0` TTC guard so missing-only rows
receive them.

`TtcText` supplies the three components from matching keys in
`shared/src/main/resources/assets/ae2craftingtime/lang/{en_us,uk_ua}.json`.
Do not replace AE2's Missing/Recurrent component, classify the warning as a TTC
badge, or alter sorting. The flag follows the summary entry object when the
screen sorts a copied list.

## Source sets and failure handling

Keep detection, menu/entry carriers, renderer behavior, and codecs in mcCommon
when signatures match. Use mc1201 or mc2612 adapters only for actual AE2 or
Minecraft API differences. Register server hooks on both physical sides and
renderer hooks client-side through every effective mixin list.

A missing required hook is a compatibility failure during verification. A
malformed or unsupported diagnostic packet may lose the extra warning but must
leave AE2's plan usable. No new dependency, optional-mod integration, saved-data
version, configuration, or permission boundary is introduced.

## Acceptance mapping

| Spec | Design path and required evidence |
| --- | --- |
| V1-V2 | Exact-key inequality plus shared primary item and positive availability; real NBT/component fixtures with exact-key controls. |
| V3-V4 | Positive missing/item predicates and set output; ordinary, fluid, successful, zero-count, multi-variant, and unrelated-row tests. |
| V5 | Container/revision/count validation, clear-on-summary, original row indices, and per-player packets; stale, sorted, reconnect, and two-network tests. |
| V6 | Shared rendering and locale keys on all targets; coexistence test with #320 when present. |
| V7 | Read-only snapshot and separate payload; plan/result, storage, profiler, persistence, and optional-screen regression checks. |
