# Delayed resource icon implementation plan

Tracks [issue #376](https://github.com/cTux/ae2-crafting-time/issues/376).
No fix has been applied. API and fixture qualification below are required before
accepting implementation; the existing smoke cases do not yet cover this bug.

1. **Qualify the rendering API.** Inspect pinned AE2 and Applied Mekanistics
   sources/artifacts for all four release rows. Confirm typed key serialization
   and world rendering against the design's observed signatures, including
   minimum AE2 19.0.24, the pinned AppMek 1.6.3 artifact, and 26.1.2 geometry
   submission. Establish the bounded fixture prerequisites below. Run them
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
   skills for tests, coverage, all-target builds and warning review. Extend
   existing provider tests. Run the scenarios below in real clients and retain
   reviewed captures with exact target/mod versions. Run cheap checks first,
   then one representative resource scenario, then the selected target matrix,
   and connected dedicated sends/resync last. Verify no client class loading
   on the dedicated server.
6. **Finish implementation.** Reconcile the parent provider-locate docs and
   `docs/server-client-stats.md` with actual payload/protocol choices. Review
   the final diff, tests, captures and current-head CI. Only the implementation
   PR may close #376; the documentation PR uses a non-closing reference.

## Fixture prerequisites and preflight

Reuse the existing driver, native loaders, disposable fixture reset, capture
writer and result model. `delayed-status` currently holds item outputs;
`appmek-cpu` seeds oxygen storage and a CPU but does not delay a chemical output.
Neither is sufficient evidence for this change.

- Add a bounded `delayed-resource-icons` case beside `StandardAe2Scenario` and
  the version-specific 26.1.2 fixture. Encode real provider processing patterns
  for water and lava, hold their output until DELAYED, then return it through
  the normal crafting path. Reuse the item control and lifecycle actions.
- Add `appmek-resource-icons` on the two integration targets, reusing their
  `MekanismKey` construction for oxygen and hydrogen. Submit and hold real
  chemical-output jobs; storage insertion alone does not qualify this case.
- Name a bucketless fluid from the pinned fixture graph before implementation
  acceptance. If none exists, register one test-driver-only fluid with a known
  texture/tint and no bucket on one target and use the same processing fixture.
  Record its ID, registration, target and expected appearance; production must
  gain no fluid or dependency for the test.
- Extend the existing connected dedicated seam for the resource case only:
  server fixture commands create/hold/release the job, normal packets supply
  client plates, and the same client reconnects to the still-running disposable
  server. The runner currently permits only `cpu-list-total-ttc` and
  `recurrent-plan`; its scenario validation and driver dispatch need an explicit
  extension. Preserve loopback-only access, schema-2 source/disposable markers,
  dependency/artifact hashes, bounded acknowledgements and exact-process cleanup.
  Do not build a new runner or general multiplayer automation. If this needs
  an independently large lifecycle/provisioning change, deliver that documented
  prerequisite separately before implementing the dependent resource checks.

Update `docs/test-driver/spec.md`, `docs/test-driver/technical-design.md`, and
`docs/dependencies.md` before changing those fixture flows or coverage. Add only
the necessary scenario registration/selection and result-contract entries.
The scenario names above are planned, not currently runnable commands.

Preflight the host Java 17/21/25 installations and use Java 17 or 21 for Gradle
8.12. On CodexVM verify matching guest Java, each prepared native `launch.json`,
disposable world markers and the dedicated source marker before launching.
Inspection found the VMX but no running VM in the tool context; guest manifests,
SSH access and dedicated source identities remain unverified. Use the existing
prepared-client staging/provisioning path; missing profiles are setup blockers.
Never substitute a modpack, guest Gradle build or unmarked server.

Use the four compatible target profiles for the matrix below and explicit
base-only profiles for absent-integration controls. After the PR exists, review
`scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`
selection and archive it. Add the explicit resource
cases to the selected coverage; the current selector cannot prove new cases.
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
| 6: verified evidence | All-target builds and warning review; shared pure logic at 100% line/branch coverage; relevant packet/NBT/provider tests; reviewed, current-head captures and separate GitHub CI evidence. |

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
| Fluid without bucket | Target with an available fixture fluid | Resource icon renders without bucket lookup; record fixture and target. |
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
