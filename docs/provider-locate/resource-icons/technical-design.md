# Delayed resource icon design

Tracks [issue #376](https://github.com/cTux/ae2-crafting-time/issues/376).
This is a proposed implementation backed by source inspection, not runtime proof.

## Evidence and root cause

Current-code review: `f25d042c2298eee5d77eeeaba8fa725e61e94be1`.
All paths below are repository-relative.

| Source | Finding |
| --- | --- |
| `shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProviderHighlightShapes.java`, `resolveItem`, `renderFacePlatesAndIcons` | Looks only in `BuiltInRegistries.ITEM`; empty stacks draw red plates without icons. The comment explicitly names fluids. |
| `shared/src/mc2612/java/com/ctux/ae2craftingtime/mc1201/ProviderHighlightShapes.java`, `resolveItem` | Same item-only lookup under the newer registry API. |
| `versions/1.20.1-forge` and `versions/1.21.1-neoforge`, `ProviderHighlightRender`; `versions/1.20.1-fabric`, `Ae2CraftingTimeClient` | All resolve the plain output ID through the older shared helper. |
| `versions/26.1.2-neoforge`, `ProviderHighlightRender` | Red plates render separately; `onSubmitGeometry` skips empty item stacks and empty item render states. |
| Both `ProfilerBridge` copies, `key`; shared `ProviderHighlightCodec` and `ProviderHighlightClient.Plate` | Output IDs come from `AEKey.getId().toString()`; highlight state carries no typed resource key. |
| `ProviderStartTracker`, `ProviderLocateRecords.StoredStart`, both `PersistedProviderTag` copies | Live patterns are retained, but saved provider starts contain ID/name/positions, not the typed display key. |
| `ProviderHighlightClient.renderPlates`, `RenderPlate`, and pure `ProviderDisplaySelection.firstByPosition` | One selected plate supplies each dimension/provider position. The render projection currently retains only the position and output ID. |
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
Do not scan client item/fluid registries to guess the type. Distinguish missing
live information from an observed ambiguous match: only missing information may
use a saved fallback. An ambiguous live result replaces an older display key
with no icon, while retaining the provider state.

Carry the same display key through automatic delayed sends in both
`ProfilerBridge` copies, `ProviderLocateRecords.StoredStart`, provider persistence,
login resync, the shared highlight codec and every loader S2C wrapper, and
`ProviderHighlightClient.Plate`. Live pattern data wins over saved fallback data.
Clear and edge-only packets need no display key and retain their existing meaning.
Follow all constructor/copy paths so trimming positions does not discard the key.

`ProviderDispatchObserver` feeds retained patterns through `observeProviders`.
Automatic sends originate in `DelayedNotificationServer.notify`,
`pushAutoHighlight`, and `defaultHighlightSender`; include these in the change,
not just the bridge. Keep display data in `ProviderStartInfo`, `StoredStart`,
snapshot/restore, fallback replacement, resync filtering, packet encode/decode,
`showPlate`, and `trimPositions`. Project the selected plate's key into
`RenderPlate` after `firstByPosition`; do not resolve another candidate by ID.
The generic winner-selection helper needs no new resource-specific policy.

Audit every loader's command registration and `ProviderLocateC2S` callback,
shared `ProviderLocateServer`, `ProviderLocateCommand`, and bridge clear
sends when packet constructors change. These manual/clear paths carry no display
key and never create or overwrite red plates. `BlockReasonNotifier` remains
chat/edge-only. All four loader login hooks retain server-approved resync.

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
At the current-code baseline these are Forge protocol `20`, both NeoForge
registrars `19`, and Fabric `provider_highlight_v4`; the corresponding next
versions are `21`, `20`, and `provider_highlight_v5`. Recheck before editing.

## Rendering decision and API verification

Reuse AE2's native resource rendering support rather than implementing fluid or
Mekanism registries, textures and tint rules independently. Cached source/JAR
inspection established these API boundaries; none is a visual qualification:

| Boundary | Observed API |
| --- | --- |
| AE2 15.0.10 Forge/Fabric | `appeng.api.client.AEKeyRendering.drawOnBlockFace(PoseStack, MultiBufferSource, AEKey, float, int, Level)`; `AEKey.toTagGeneric()` / `fromTagGeneric(CompoundTag)` |
| AE2 19.2.17 NeoForge | Same world-face renderer; `AEKey.CODEC` and `toTagGeneric(HolderLookup.Provider)` / `fromTagGeneric(HolderLookup.Provider, CompoundTag)` |
| AE2 26.1.10-beta NeoForge | `appeng.client.api.AEKeyRenderState.extract(AEKey, Level, int)` and `submit(PoseStack, SubmitNodeCollector, int)`; `AEKey.CODEC`, with tag methods using `ValueInput` / `ValueOutput` |
| Cached AppMek artifacts `9n9p68Qq` and `TpUCzFaW` | `AMChemicalStackRenderer` implements AE2's `AEKeyRenderHandler<MekanismKey>` with `drawOnBlockFace` |

Keep serialization conversions behind target adapters: 1.21.1 needs registry
context unlike 1.20.1 despite sharing other mc1201 code. Use the 26.1.2 codec
with its registry-aware serialization context. Do not put client render classes
in server packet or persistence initialization. Check the minimum 19.0.24
artifact and each exact runtime graph before finalizing these adapters.

The first implementation slice must verify the pinned dependencies' world-render
API and a real fluid/chemical icon before committing to the rendering adapter.
Use AE2's resource renderer for non-items with the existing face transform;
retain the normal item renderer for item keys. If the pinned AE2 world-render API
cannot render a registered type, stop that implementation slice and record the
exact limitation before revising this design. Do not silently ship a bucket icon.
This API qualification is an explicit technical gate, not a completed experiment.

Compile defaults remain AE2 15.0.10 (both 1.20.1 loaders), 19.0.24 (1.21.1),
and 26.1.10-beta. The compatible graphs at this baseline use AE2 15.4.10,
15.1.0, 19.2.17, and 26.1.10-beta respectively; AppMek uses 1.4.3 on Forge
and 1.6.3 on NeoForge 1.21.1. Qualify the exact 1.6.3 artifact rather than
treating the inspected cached AppMek artifact as proof for that runtime version.
Read current pins from `scripts/run-client-versions.json` before running.

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

## Production acceptance on the existing fixtures

[#484](https://github.com/cTux/ae2-crafting-time/pull/484) merged the #482
fixture prerequisite at the current-code baseline. `ResourceFixtureClient`,
`ResourceFixtureServer` and their bounded control protocol already run the two
resource leaves, including connected reconnect and shared-provider promotion.
Their explicit `-ResourceFixtureOnly` mode still requires
`productionIconAcceptance: NOT_RUN`; those results cannot qualify this fix.

Use the same leaves without `-ResourceFixtureOnly` for production acceptance.
Start the resource driver from the scenario name in both modes, while retaining
the flag as the fixture-only result boundary. Update the existing wrappers,
launch preparation, driver dispatch, connected/prewarm guards and result readers
together; ordinary unrelated scenarios must still reject the fixture-only flag.
Keep fixture evidence schema 1 and its NOT_RUN field intact in both modes.
Production runs additionally write `resource-icon-evidence.json` schema 1 with
typed server, retained-plate and selected-render-plate observations, checkpoint
capture names/hashes, SHA and graph identity, `semanticResult: PASS|FAIL` and
`visualAcceptance: REVIEW_REQUIRED|PASS|FAIL`. Missing or mismatched required
evidence fails the run. Semantic assertions alone leave visual acceptance
REVIEW_REQUIRED; only recorded review of every required world capture can
establish visual PASS. Bind that review to the same hashes and retain the raw
automatic gate result; manual review never rewrites an automatic REVIEW_REQUIRED
gate as an automatic PASS. Fixture-only runs never emit production PASS.

Reuse current jobs and lifecycle checkpoints. Add bounded actions only for the
remaining acceptance: resource reload with a surviving plate, provider removal,
and unload/reload under the existing provider lifetime rules. Bind any added
server action to the existing fixture/revision/sequence acknowledgement protocol;
keep client resource reload local and await completion before capture. Retain
normal production sends and resync as the only source of client plate state.
The driver must not draw icons, populate highlights or force a semantic pass.

No new provisioner, runner, command transport or cold/cache-hit qualification
campaign is needed. Reuse #482's sealed sources and prewarm after validating
their identities for the new artifacts. Extend the nearest driver and script
checks for both execution modes, missing evidence and failure cleanup.

## Alternatives rejected

- Item-then-fluid registry lookup loses type and fails on collisions and chemicals.
- Bucket/tank substitutes are not resource icons and exclude bucketless fluids.
- Rewriting global profile identity is broader than this display bug. Ambiguity
  suppresses the icon until the separate identity problem is addressed.

See the [spec](spec.md) and [implementation plan](implementation-plan.md) for
acceptance and the runtime gate. The bug stays open after this docs PR merges.
