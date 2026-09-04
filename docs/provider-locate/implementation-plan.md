# Provider Locate Implementation Plan

Implement this as one feature commit. Let the commit hook create the PR, then
use required CI as the first Gradle test run.

## Phase 1: Track dispatched patterns per craft

1. Add a Minecraft-facing tracker keyed by crafting CPU identity holding each
   output's dispatched patterns.
2. Record the pattern for its output keys inside both `ProfilerBridge`
   `observeProviders` paths (standard/AdvancedAE and ECO/TimeWheel funnel
   through the same two methods).
3. Clear one scope in `ProfilerBridge.startJob` and `finishJob` beside the
   existing pending/owner cleanup.
4. Resolve positions only at notify time: providers from the crafting
   service, identity-matched to grid nodes, `InWorldGridNode` locations
   only, capped at the new packet limit.

## Phase 2: Message, command, and highlight packet

1. Split `chat.delayed` so the status word is a separate red translatable;
   keep four matching placeholders in English and Ukrainian.
2. Underline the output name with a hover hint and a run-command click event
   while a record exists; render a plain name otherwise.
3. Add the shared Brigadier locate command (permission 0, owner-must-match)
   and register it on each loader's command event.
4. Add the shared highlight codec plus one S2C wrapper, registration line,
   and send helper per loader.
5. Bump the Forge channel protocol from `9` to `10`, add the Fabric
   `provider_highlight_v1` channel, and bump both NeoForge registrar
   versions from `8` to `9`.
6. Add the client highlight store and one render hook per loader drawing red
   boxes for 15 seconds in the matching dimension.

Changing the wire registry affects every supported loader, so complete these
steps in the same commit.

## Phase 3: Persist links and re-warn after reload

1. Add the per-key provider-start section (owner, dimension, positions,
   display name) to all four `Ae2CraftingTimeSavedData` files with tolerant
   reads; do not bump the samples version.
2. Snapshot the section whenever records change, a job owner is set, or a
   load happens; restore it in `ProfilerBridge.load`.
3. Fall back to the persisted owner and positions when live dispatch data is
   absent, so resumed crafts warn again with a working link after reload.
4. Answer clicks on missing or foreign records with the private expiry
   notice.

## Phase 4: Blocked warnings (NO SPACE, NO POWER)

1. Add a tiny pure-core episode tracker (one instance per reason) with tests
   for once-per-episode, re-arm, scope independence, and null safety.
2. Add the server-side `NO SPACE` probe reading the AE2-mirrored status
   methods through reflection, with graceful empty results.
3. Notify per reason from every CPU tick path (power everywhere, space
   everywhere) plus a power-only backup on status requests, sharing the
   delayed records, highlight packet, and persistence.
4. Add the shared three-placeholder blocked sentence with per-reason red
   status words in both languages; reuse the clickable name.
5. Extend the message, placeholder, and language tests.
6. Update the spec, design, and protocol doc sections.

## Phase 5: Tests, docs, and verification

1. Add codec round-trip tests (positions, dimension, duration, oversize
   rejection) and NBT round-trip tests (records, owners, legacy saves).
2. Add message-component tests: red status word, underlined clickable name
   with hover text, plain fallback, and placeholder parity.
3. Update `docs/server-client-stats.md` (packet, command, persistence) and
   index the feature in `docs/feature-coverage.md` and `README.md`.
4. After the hook-created PR exists:
   1. Let required CI run every supported Gradle row and the coverage gate.
   2. Check the full warning/error sweep and fix repository-owned warnings.
   3. In a development client, stall a craft, click the item name, and verify
      the provider box draws for 15 seconds.
   4. Re-enter the world and verify the link still works and a still-delayed
      craft warns again.
