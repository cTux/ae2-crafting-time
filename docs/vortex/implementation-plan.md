# Vortex Implementation Plan

Implement this as one feature commit. Let the commit hook create the PR before
running the required verification.

## Phase 1: Add Owner-Bound Blocks

1. Register Vortex Hub and Vortex blocks, block items, block entities, the
   Vortex menu, creative-tab entries, loot tables, block states, models,
   textures, and English/Ukrainian names on all four targets.
2. Persist owner UUIDs from placement with a non-null player; keep dropped
   items unbound and leave non-player placement unusable.
3. Give the Hub an AE2 managed grid node with normal power/channel behavior and
   no use interaction or inventory.
4. Add loader and shared tests for registration, placement ownership, NBT
   round trips, rebinding, node destruction, no-menu Hub behavior, owner-only
   Vortex opening, and ordinary block breaking.

## Phase 2: Discover Hubs And Current Jobs

1. Add the runtime Hub registry and register/unregister it from block-entity
   load, unload, and removal.
2. Deduplicate active grids by identity and cache one owner snapshot per server
   tick without loading chunks.
3. Add the Vortex collector over each grid's crafting service and busy CPUs,
   using `mc1201`/`mc2612` accessors only where public AE2 APIs are insufficient.
4. Reuse existing TTC estimation and status priority to produce one row per CPU
   job with final output, remaining amount, aggregate status/time, and opaque
   menu-local row id.
5. Add pure tests for multiple owners, grids, Hubs and dimensions; duplicate
   grids; busy/idle/completed CPUs; other-player jobs; row stability; TTC
   aggregation; status priority; unloads; and the row limit.

## Phase 3: Send And Render The Window

1. Add the owner-validated Vortex menu and the four bounded request/snapshot/
   locate packet types.
2. Bump Forge `6` to `7`, add Fabric `vortex_*_v1` payload IDs, and bump both
   NeoForge registrars `5` to `6` (use the next unused value if already taken);
   extend packet round-trip,
   size, identifier, enum, amount, stale-container, rate-limit, and forged-owner
   tests.
3. Build the scrolling Vortex screen with the requested icon, output name and
   remaining amount, and existing TTC label only; add a localized empty state.
4. Refresh once per second, replace the whole matching-container snapshot, and
   clear it on close or invalidation.
5. Add UI structural tests and automated driver coverage for empty, normal,
   mixed, overflow, stale-removal, and denied-owner screens.

## Phase 4: Add Error Lights And Location

1. Add the Vortex `ERROR` state and purple/red lens model variants with one
   fixed vanilla luminance value and no custom renderer.
2. Drive it from the cached owner snapshot once per second and test normal,
   each error status, multiple Vortex blocks, and last-error recovery.
3. Capture only verified dispatch-machine positions in existing CPU-scoped
   profiler state; clear them on every documented lifecycle boundary.
4. Validate locate requests against the current menu map and send coordinates
   only for a still-live error target.
5. Add the 30-second same-dimension client outline and private coordinate or
   no-target messages, with tests for expiry, unload, dimension mismatch,
   missing targets, `NO PROVIDER`, and forged row ids.

## Phase 5: Add Tim Craftsman

1. Spawn and tag one vanilla villager at each Vortex anchor with the required
   name and vanilla flags; do not add an entity type or renderer.
2. Reconcile the linked UUID once per second; enforce no-physics and re-anchor
   position/velocity every server tick; remove duplicates and orphans; discard
   Tim with the block.
3. Cancel interaction with tagged Tim only through each loader's native event;
   leave ordinary villagers unchanged.
4. Test placement, chunk reload, missing/duplicate/orphan recovery, block
   removal, movement attempts, invulnerability, interaction cancellation, and
   multiple Vortex blocks.

## Phase 6: Verify

After the hook-created PR exists:

1. Let required GitHub CI run all release-matrix Gradle rows and coverage.
2. Run the documentation/link checks, resource parity checks, packet checks,
   and full warning/error sweep; fix repository-owned warnings.
3. Smoke one owner with two distinct loaded networks in different dimensions,
   duplicate Hubs on one network, jobs started by both owner and another
   player, and a denied second player.
4. Verify empty, TTC, Waiting, No data yet, and every available error state;
   confirm red/purple recovery with the screen closed.
5. Verify a locatable delayed machine, an unloaded/cross-dimension target, and
   unlocatable `NO PROVIDER` without guessed coordinates.
6. Verify Tim placement, noninteraction, immobility, reload recovery, and
   removal on every supported client.

Done means all four CI rows are green, every new executable branch is covered,
packet and ownership boundaries reject invalid input, English/Ukrainian assets
match, and every acceptance criterion in `spec.md` passes in-game.
