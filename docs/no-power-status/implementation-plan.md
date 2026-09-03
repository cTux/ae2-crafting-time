# No Power Status Implementation Plan

1. Replace the shipped NO PROVIDER snapshot set with a shared
   `CraftingBlockReason` map, using the approved compatibility bump below.
   Preserve its CPU context and exact-pattern revalidation.
2. Extend the standard AE2 execution mixin around the simulated energy check,
   preserving its exact amount and threshold behavior.
3. Add the equivalent optional AdvancedAE hook only where verified.
4. Report `NO_POWER` for each positive output of the blocked pattern with the
   selected CPU scope and current game tick.
5. Add `NO POWER`, its explanation, and its suggestion to English and
   Ukrainian. Update translation-key checks.
6. Render the warning and tooltip through the shared crafting-row resolver and
   exclude it from TTC sorting and color.
7. Cover insufficient, exact, and excess power boundaries; expiration;
   precedence with `NO PROVIDER`; CPU switching; and absent stats.
8. After the hook-created PR exists, run the development skill's required
   shared and four-target checks. Verify GitHub CI separately.
9. In development clients, drain and restore AE2 network energy while keeping
   the CPU active, then separately starve only an external machine to prove it
   does not trigger the status.

Complete when every acceptance criterion in `spec.md` has automated or recorded
cross-version evidence, all required checks pass, and no repository-owned
warning remains.

## Approved implementation update (2026-09-03)

NO PROVIDER has since shipped a bounded missing-output set. Replace that field
with one bounded `outputId -> CraftingBlockReason` map shared by both statuses.
The user approved this additional compatibility change: Forge 8 -> 9, Fabric
stats_snapshot_v6 -> v7, and both NeoForge registrars 7 -> 8. This supersedes
the earlier single-bump assumption; persisted data stays unchanged.

Keep NO PROVIDER's exact-pattern revalidation. Track power failures by CPU and
pattern, and merge fresh positive outputs into the shared map with NO PROVIDER
priority. Clear a pattern on a successful simulated check; otherwise expire its
power observation after 20 ticks. Match AE2's `extracted < required - 0.01`
comparison, verified in every supported AE2 and AdvancedAE artifact. The hook
observes ordinal 0 (SIMULATE), returns its value unchanged, and uses the exact
pattern captured by the existing provider lookup. Never observe MODULATE.
