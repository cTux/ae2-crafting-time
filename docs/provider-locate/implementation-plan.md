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
3. Add the shared Brigadier locate command (permission 0, owner-must-match
   plus active-job and valid-target checks) and register it on each loader's
   command event.
4. Add the shared highlight codec (`networkId, dimensionId, positions,
   outputId, durationSeconds, plateOnly`) plus one S2C wrapper, registration
   line, and send helper per loader. `plateOnly=true` is red-plate-only
   (automatic); `false` is rainbow-edge-only (manual); empty positions with
   zero duration clears one plate and keeps rainbow.
5. Set Forge channel protocol `14`, Fabric `provider_highlight_v4` plus
   `provider_locate_v1`, NeoForge registrars `13` on 1.21.1 and 26.1.2.
   Keep `networkId` and stored `dimension` additive with tolerant reads.
6. Add the client highlight store with independent lifetimes: plates persist
   until server clear, provider break, or session end; edges expire after 15
   seconds. Both blink. Never consult the UI cache; never silently evict.
   One render hook per loader draws rainbow edges and red plates in the
   matching dimension, with shared broken-target trim.

Changing the wire registry affects every supported loader, so complete these
steps in the same commit.

## Phase 3: Persist links and re-warn after reload

1. Add the per-key provider-start section (network, output, owner, dimension,
   positions, display name) to all four `Ae2CraftingTimeSavedData` files with
   tolerant reads; do not bump the samples version. Never persist rainbow.
2. Snapshot the section whenever records change, a job owner is set, or a
   load happens; restore it in `ProfilerBridge.load`.
3. Fall back to the persisted owner and positions when live dispatch data is
   absent, so resumed crafts warn again with a working link after reload.
   Login resync re-sends plates for still-delayed crafts (chat never re-sent)
   with server-side broken-target filtering; unloaded stays unknown and kept.
4. Answer clicks on missing, foreign, finished, cancelled, or broken records
   with the private expiry notice and forget broken records.
5. Double-click means "any active crafting item" (server validates scope,
   ownership, and resolvability); `notifyOnDelayed` means "chat only" and
   never gates plates, clears, or resync. Document both explicitly.

## Phase 4: Blocked warnings (NO SPACE, NO POWER)

1. Add a tiny pure-core episode tracker (one instance per reason) with tests
   for once-per-episode, re-arm, scope independence, and null safety.
2. Add the server-side `NO SPACE` probe reading the AE2-mirrored status
   methods through reflection, with graceful empty results.
3. Notify per reason from every CPU tick path (power everywhere, space
   everywhere) plus a power-only backup on status requests, sharing chat
   records only. Blocked warnings never create or clear red plates and never
   update the provider fallback.
4. Add the shared three-placeholder blocked sentence with per-reason red
   status words in both languages; reuse the clickable name for edge-only
   manual locates.
5. Extend the message, placeholder, and language tests.
6. Update the spec, design, and protocol doc sections.

## Phase 5: Tests, docs, and verification

1. Add codec round-trip tests (network, positions, dimension, duration,
   plateOnly, oversize rejection, legacy defaults) and NBT round-trip tests
   (records, owners, stored dimension, legacy saves).
2. Add message-component tests: red status word, underlined clickable name
   with hover text, plain fallback, and placeholder parity.
3. Add lifecycle tests: auto plate without edge, manual edge without plate,
   independent clear, provider-break trim, session-end clear, no silent
   eviction, network-scoped independence, blocked never touches red.
4. Update `docs/server-client-stats.md` (packet, command, persistence) and
   index the feature in `docs/feature-coverage.md` and `README.md`.
5. After the hook-created PR exists:
   1. Let required CI run every supported Gradle row and the coverage gate.
   2. Check the full warning/error sweep and fix repository-owned warnings.
   3. In a development client, stall a craft and verify red plates appear
      with no click; click the item name or double-click any active item and
      verify rainbow edges blink for 15 seconds without changing red.
   4. Recover one output and verify red clears while rainbow continues;
      finish or cancel and verify the same. Break one provider and verify
      only its plate and outline drop.
   5. Re-enter the world and verify red returns for still-delayed crafts,
      rainbow never returns, and active chat links still work.
