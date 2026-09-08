# Delayed resource icon design

Tracks [issue #376](https://github.com/cTux/ae2-crafting-time/issues/376).
This is a proposed implementation backed by source inspection, not runtime proof.

## Evidence and root cause

Research baseline: `ef604b26653f6a183ae86b9ce5cafc165a5cb65e`.
All paths below are repository-relative.

| Source | Finding |
| --- | --- |
| `shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProviderHighlightShapes.java`, `resolveItem`, `renderFacePlatesAndIcons` | Looks only in `BuiltInRegistries.ITEM`; empty stacks draw red plates without icons. The comment explicitly names fluids. |
| `shared/src/mc2612/java/com/ctux/ae2craftingtime/mc1201/ProviderHighlightShapes.java`, `resolveItem` | Same item-only lookup under the newer registry API. |
| `versions/1.20.1-forge` and `versions/1.21.1-neoforge`, `ProviderHighlightRender`; `versions/1.20.1-fabric`, `Ae2CraftingTimeClient` | All resolve the plain output ID through the older shared helper. |
| `versions/26.1.2-neoforge`, `ProviderHighlightRender` | Red plates render separately; `onSubmitGeometry` skips empty item stacks and empty item render states. |
| Both `ProfilerBridge` copies, `keyOf`; shared `ProviderHighlightCodec` and `ProviderHighlightClient.Plate` | Output IDs come from `AEKey.getId().toString()`; highlight state carries no typed resource key. |
| `ProviderStartTracker`, `ProviderLocateRecords.StoredStart`, both `PersistedProviderTag` copies | Live patterns are retained, but saved provider starts contain ID/name/positions, not the typed display key. |
| Forge and 1.21.1 NeoForge `AppliedMekanisticsFixture` | Oxygen is represented by `MekanismKey` over `GasStack` and `ChemicalStack`, respectively. Fixture setup does not prove world-icon rendering. |
| `scripts/release-matrix.json` | Applied Mekanistics is optional on Forge 1.20.1 and NeoForge 1.21.1, not listed on the other two targets. |

The fluid report matches the render path. The same non-item limitation applies
to gas/chemical outputs by source inspection. A valid same-ID item could instead
produce a misleading icon: a plain registry ID does not identify the key type.
The server's delay detection and red plate creation already work in the report.

## Data flow and ownership

Keep `ProfileKey` and existing plate lifetime keys unchanged. Add optional typed
**display key** data alongside them, never substitute it for profiling identity.
Resolve that key from the retained pattern outputs in `ProviderStartTracker`
within the existing CPU scope. Match the output ID, retain the complete AE key,
and accept only one distinct matching key. Conflicting candidates yield no icon.
Do not scan client item/fluid registries to guess the type.

Carry the same display key through automatic delayed sends in both
`ProfilerBridge` copies, `ProviderLocateRecords.StoredStart`, provider persistence,
login resync, the shared highlight codec and every loader S2C wrapper, and
`ProviderHighlightClient.Plate`. Live pattern data wins over saved fallback data.
Clear and edge-only packets need no display key and retain their existing meaning.
Follow all constructor/copy paths so trimming positions does not discard the key.

Use AE2's own typed key serialization at the Minecraft API boundary; keep AE2
classes out of the Minecraft-free core. Store an optional `displayKey` field in
provider-start persistence using the target's AE2 codec. Old entries lacking it
load with no icon until a live key can be recovered. No samples-format migration.
Unknown removed addon key types discard only display data, not provider state.

Bound the separately encoded display payload to 16 KiB before decoding, enforce
the underlying NBT/codec depth limits, and reject oversized network payloads.
Malformed display data must not produce a partial state update. Missing optional
key types may yield no icon. Do not log raw key data. Bump the affected highlight
channel/registrar compatibility versions together when implementation changes the
wire layout; select the next versions from the then-current registrations.

## Rendering decision and API verification

Reuse AE2's generic resource rendering support rather than implementing fluid or
Mekanism registries, textures and tint rules independently. AE2 documents
[GenericStack wrapping and serialization](https://appliedenergistics.org/javadoc/appeng/api/stacks/GenericStack.html).
That establishes a typed resource/display representation, but does not prove that
its wrapped ItemStack renders correctly in this mod's FIXED world context.

The first implementation slice must verify the pinned dependencies' world-render
API and a real fluid/chemical icon before committing to the rendering adapter.
Use AE2's resource renderer for non-items with the existing face transform;
retain the normal item renderer for item keys. If the pinned AE2 world-render API
cannot render a registered type, stop that implementation slice and record the
exact limitation before revising this design. Do not silently ship a bucket icon.
This API qualification is an explicit technical gate, not a completed experiment.

Keep two version-specific rendering boundaries: mc1201 for Forge/Fabric 1.20.1
and NeoForge 1.21.1; mc2612 plus its submit-geometry hook for 26.1.2. Preserve
camera-facing culling, scale, depth offset, lighting, and plate pulse. On the
older path, flush each filled plate before resource rendering as today: its
comments record prior invalid-buffer and triangle-strip failures. On 26.1.2,
keep submission in the appropriate geometry phase. Resource reload refreshes
cached models/textures, and disconnect clears display caches.

Chemical rendering delegates through registered AE2 key support. Common runtime
classes must not import Mekanism classes. Version-specific fixtures already have
the optional dependencies needed for chemical checks. No translations change.

## Alternatives rejected

- Item-then-fluid registry lookup loses type and fails on collisions and chemicals.
- Bucket/tank substitutes are not resource icons and exclude bucketless fluids.
- Rewriting global profile identity is broader than this display bug. Ambiguity
  suppresses the icon until the separate identity problem is addressed.

See the [spec](spec.md) and [implementation plan](implementation-plan.md) for
acceptance and the runtime gate. The bug stays open after this docs PR merges.
