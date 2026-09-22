# Delayed resource icon implementation plan

Lifecycle: see the [scope status and evidence](spec.md).

Tracks [issue #376](https://github.com/cTux/ae2-crafting-time/issues/376).
No production fix has been applied. #482's fixture prerequisite merged in
[#484](https://github.com/cTux/ae2-crafting-time/pull/484) at
`f25d042c2298eee5d77eeeaba8fa725e61e94be1`. Reuse it for the API and production
acceptance gates below; fixture-only PASS does not qualify visible icons.

1. **Qualify the rendering API.** Inspect pinned AE2 and Applied Mekanistics
   sources/artifacts for all four release rows. Confirm typed key serialization
   and world rendering against the design's observed signatures, including
   minimum AE2 19.0.24, the pinned AppMek 1.6.3 artifact, and 26.1.2 geometry
   submission. Validate the existing bounded fixtures below. Run them
   only after the hook-created implementation PR exists. Resolve the API gate
   before finalizing the fix.
2. **Preserve typed display data.** Own `ProviderStartTracker`, both
   `ProfilerBridge` copies, `ProviderLocateRecords.StoredStart`, both
   `PersistedProviderTag` copies and their saved-data callers. Select one distinct
   output key from retained patterns; preserve it for reconnect. Cover multiple
   outputs, ambiguous matches, legacy starts, absent integration, and live data
   replacing fallback, including ambiguity clearing a previously valid key.
   Include `ProviderStartInfo`, snapshot/restore and resync filtering. Keep
   current profiling IDs and cleanup rules.
3. **Transport and retain it.** Own `ProviderHighlightCodec`, all four
   `ProviderHighlightS2C` wrappers, registrations, and `ProviderHighlightClient`.
   Add bounded optional display data, update compatibility versions together,
   and retain it through copies/position trimming and the selected `RenderPlate`
   projection. Update `DelayedNotificationServer` and every sender/constructor
   listed in the design. Cover item/fluid/chemical round trips, unknown key
   types, oversized/malformed data, clear packets and
   edge-only packets. Pure core remains free of AE2 dependencies.
4. **Draw the resource.** Own both `ProviderHighlightShapes` copies, Forge and
   NeoForge `ProviderHighlightRender` hooks, and Fabric `Ae2CraftingTimeClient`.
   Apply the qualified AE2 resource renderer within existing face transforms;
   keep item appearance and render-buffer flush boundaries. Exercise resource
   reload, multiple visible faces and simultaneous plates/edges.
5. **Verify after PR creation.** Follow the development and prepared-client smoke
   skills for tests, coverage, all-target builds and warning review. Add the
   production acceptance mode described below to the existing resource driver
   and runners, preserving fixture-only results. Extend
   existing provider tests. Run the scenarios below in real clients and retain
   reviewed captures with exact target/mod versions. Run cheap checks first,
   then one representative resource scenario, then the selected target matrix,
   and connected dedicated sends/resync last. Verify no client class loading
   on the dedicated server.
6. **Finish implementation.** Reconcile the parent provider-locate docs and
   `docs/server-client-stats.md` with actual payload/protocol choices. Review
   the final diff, tests, captures and current-head CI. Only the implementation
   PR may close #376; the documentation PR uses a non-closing reference.

## Existing fixtures and production acceptance

Reuse `ResourceFixtureClient`, `ResourceFixtureServer`, their control protocol,
native loaders, disposable resets and capture writer from #484. The leaves
`delayed-resource-icons` (all four targets) and `appmek-resource-icons` (the two
AppMek targets) already create, hold, release and cancel real processing jobs,
exercise shared-provider promotion and reconnect to the same dedicated server.
`appmek-cpu` alone remains insufficient evidence for this change.

Use Forge's existing driver-only
`ae2craftingtime_test_driver:resource_fixture_fluid` for the bucketless control.
`ResourceFixtureFluid` registers it without a bucket, using water textures and
cyan tint `0xff00ffff`. Record these facts with the capture. Add no production
resource or optional dependency.

Current launchers and result readers admit these leaves only with
`-ResourceFixtureOnly`. Implement ordinary execution of the same leaves as
production acceptance, following the
[mode and evidence contract](technical-design.md#production-acceptance-on-the-existing-fixtures).
Keep explicit fixture-only runs unchanged. Update `DriverOptions`, both
`CraftPlanScenario` dispatchers, `ResourceFixtureClient`/`ResourceFixtureServer`,
suite validation, launch preparation, host/VM wrappers, matrix selection and
connected/prewarm guards together. Add no new runner or fixture transport.

Retain fixture evidence schema 1 with production acceptance NOT_RUN; ordinary
runs additionally require `resource-icon-evidence.json` schema 1. Assert typed
server/retained/selected key agreement and preserve full world captures for
texture/tint review. Add bounded resource reload and provider removal/unload
checkpoints, then verify surviving or restored state under existing rules.
Extend nearest Java/PowerShell checks for mode routing, required evidence,
malformed results and failure cleanup. Review images before marking production
acceptance PASS; semantic state alone remains REVIEW_REQUIRED.

Preserve loopback-only access, schema-2 source/disposable markers, artifact and
dependency hashes, bounded acknowledgements and exact-process cleanup. Reuse
the sealed-source provisioner and prewarm from #482; do not repeat its entire
cold/cache-hit qualification matrix unless these mechanisms change.

## Preflight

The read-only preflight at the current-code baseline resolved host Java 17/21/25,
confirmed the existing CodexVM was running through the actual host context, and
connected by key-based SSH. Prepared manifests exist for Forge 47.4.23, Fabric
0.19.5, NeoForge 21.1.251 and NeoForge 26.1.2.109. This proves availability,
not the contents or identity of the next runtime.

Before launching, verify guest Java 17/21/25, each prepared `launch.json` and
its dependencies, disposable world markers and sealed dedicated-source identity.
Use Java 17 or 21 for Gradle 8.12 and build only on the host. Use the existing
staging/provisioning path; a missing or mismatched profile is a setup blocker.
Never substitute a modpack, guest Gradle build or unmarked server.

Use the four compatible target profiles for the matrix below and explicit
base-only profiles for absent-integration controls. After the PR exists, review
`scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`
selection and archive it. Add the explicit resource
cases to the selected coverage; the current selector cannot prove production
acceptance. Run these leaves without `-ResourceFixtureOnly` only after the new
mode and its boundary checks exist.
Keep clients sequential, preserve captures, and record process launches and a
wall-time budget before connected checks. No test or client ran for this plan.

## Acceptance mapping

| Spec criterion | Change and required evidence |
| --- | --- |
| 1: fluids, tint, bucketless | Typed rendering adapters; real water/lava captures on all four targets and the named bucketless fixture on one target. |
| 2: chemicals | Exact AppMek API qualification; oxygen/hydrogen delayed-provider captures on both integration targets. |
| 3: items and identity | Distinct-key selection tests for duplicates, variants, multiple outputs and cross-type same-ID collisions; item captures; winner promotion retains the promoted key. |
| 4: automatic/lifecycle/resync | Sender, persistence and client changes; tests for chat-disabled sends, trim/copy preservation, reconnect, recovery, finish/cancel and independent rainbow expiry; integrated and connected captures/state evidence. |
| 5: safe boundaries and saves | Codec/NBT tests for legacy missing keys, removed/unknown types, malformed/deep/oversized data and no partial update; ambiguous live keys clear stale fallback; base profiles load without AppMek. |
| 6: verified evidence | Both-mode driver/script boundary checks; all-target builds and warning review; shared pure logic at 100% line/branch coverage; relevant packet/NBT/provider tests; schema-1 production evidence with reviewed current-head captures and separate GitHub CI evidence. |

Extend `ProviderLocateTest`, `ProviderHighlightTriggerTest`, `ProviderPlatesTest`,
`ProviderResyncTest` and loader SavedData tests at their existing boundaries;
put new selection/bounds decisions in pure Java with focused tests. Retain
overlapping-provider coverage in `delayed-status` and test selection through
`renderPlates`, not just the generic helper. Follow the development skill's
coverage and hook-before-tests rules without weakening exclusions or assertions.

## Runtime acceptance matrix

| Scenario | Targets | Observable pass condition |
| --- | --- | --- |
| Item delayed control | All four | Existing item icon stays centered and visible. |
| Water and a second differently tinted fluid | All four | Correct resource texture/tint on visible plates. |
| Fluid without bucket | Forge 1.20.1 driver fixture | Cyan water-texture icon renders without bucket lookup; record fixture identity. |
| Oxygen and a second chemical | Forge 1.20.1; NeoForge 1.21.1 with Applied Mekanistics | Matching gas/chemical icon visibly renders, not just a row or plate. |
| No optional integration | Base targets | Items/fluids work; no optional-class loading failure. |
| Chat disabled, terminal closed | Fluid and chemical cases | Automatic red plate and icon appear. |
| Manual locate then recover/finish/cancel | Item, fluid and chemical cases | Red/icon clear; rainbow retains independent expiry. |
| Reconnect while delayed | Fluid and chemical cases | Server restores plate and icon; no rainbow restoration. |
| Connected dedicated send and reconnect | Fluids on all four; chemicals on both AppMek targets | Normal server packets restore the correct plate/icon; no rainbow restoration or client-only class loading on the server. |
| Shared provider, selected output recovers | Item/fluid and chemical cases | Exactly one plate per provider; next selected output supplies its own icon. |
| Provider removal, unload, resource reload | All four | Existing removal/unload rules hold; surviving icons use reloaded assets. |

Pair runtime evidence with deterministic tests for resource-type collisions,
invalid payloads and legacy saved entries. Never infer all-target success from
one loader's screenshot. A missing fixture or rendering API is a named blocker
for implementation acceptance, not an omitted test or a claim of support.
