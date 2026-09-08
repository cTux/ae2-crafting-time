# Delayed resource icon implementation plan

Tracks [issue #376](https://github.com/cTux/ae2-crafting-time/issues/376).
Documentation is ready for the API qualification below; no fix has been applied.

1. **Qualify the rendering API.** Inspect pinned AE2 and Applied Mekanistics
   sources/artifacts for all four release rows. Confirm typed key serialization
   and world rendering, especially the 26.1.2 geometry phase. Record exact
   signatures and dependency versions in the design. Prepare real delayed
   fluid/gas scenarios using existing fixtures; run them after the hook-created
   implementation PR exists. Resolve the API gate before finalizing the fix.
2. **Preserve typed display data.** Own `ProviderStartTracker`, both
   `ProfilerBridge` copies, `ProviderLocateRecords.StoredStart`, both
   `PersistedProviderTag` copies and their saved-data callers. Select one distinct
   output key from retained patterns; preserve it for reconnect. Cover multiple
   outputs, ambiguous matches, legacy starts, absent integration, and live data
   replacing fallback. Keep current profiling IDs and cleanup rules.
3. **Transport and retain it.** Own `ProviderHighlightCodec`, all four
   `ProviderHighlightS2C` wrappers, registrations, and `ProviderHighlightClient`.
   Add bounded optional display data, update compatibility versions together,
   and retain it through copies/position trimming. Cover item/fluid/chemical
   round trips, unknown key types, oversized/malformed data, clear packets and
   edge-only packets. Pure core remains free of AE2 dependencies.
4. **Draw the resource.** Own both `ProviderHighlightShapes` copies, Forge and
   NeoForge `ProviderHighlightRender` hooks, and Fabric `Ae2CraftingTimeClient`.
   Apply the qualified AE2 resource renderer within existing face transforms;
   keep item appearance and render-buffer flush boundaries. Exercise resource
   reload, multiple visible faces and simultaneous plates/edges.
5. **Verify after PR creation.** Follow the development and prepared-client smoke
   skills for tests, coverage, all-target builds and warning review. Extend
   existing provider tests. Run the scenarios below in real clients and retain
   reviewed captures with exact target/mod versions. Test dedicated-server
   sends/resync too, with no client class loading on the server.
6. **Finish implementation.** Reconcile the parent provider-locate docs and
   `docs/server-client-stats.md` with actual payload/protocol choices. Review
   the final diff, tests, captures and current-head CI. Only the implementation
   PR may close #376; the documentation PR uses a non-closing reference.

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
| Provider removal, unload, resource reload | All four | Existing removal/unload rules hold; surviving icons use reloaded assets. |

Pair runtime evidence with deterministic tests for resource-type collisions,
invalid payloads and legacy saved entries. Never infer all-target success from
one loader's screenshot. A missing fixture or rendering API is a named blocker
for implementation acceptance, not an omitted test or a claim of support.
