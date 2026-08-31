# No Provider Status Implementation Plan

1. Add `CraftingBlockReason` and covered runtime blocker storage/query logic to
   `shared/src/main/java`, including freshness, precedence, replacement, and
   lifecycle clearing tests.
2. Add the covered pure-Java crafting-row status resolver and prove the complete
   priority order, including active-plus-pending combined rows.
3. Extend `ProfilerBridge` in both API source sets and the standard AE2
   execution mixin to report empty provider lookups for the pattern outputs.
4. Add the equivalent optional AdvancedAE hook only for artifacts whose seam is
   verified.
5. Add bounded blocker values to `StatsRequestHandler`, `StatsPacketCodec`, all
   four loader packet records, and `ClientStatsCache`; bump the four wire
   boundaries once for `NO PROVIDER` and `NO POWER` together.
6. Add `NO PROVIDER`, its explanation, and its suggestion to English and
   Ukrainian. Update translation-key checks.
7. Render the status and tooltip in the standard crafting-status table. Exclude
   blocked rows from TTC sorting and color.
8. Cover packet round trips, invalid ordinals, stale replacement, CPU switching,
   and absent stats.
9. After the hook-created PR exists, run the development skill's required
   shared and four-target checks. Verify GitHub CI separately.
10. In development clients, remove and restore the only provider, remove and
    restore the pattern, and confirm a second provider prevents the status.

Complete when every acceptance criterion in `spec.md` is covered by an
automated boundary check or recorded cross-version client check, all required
checks pass, and no repository-owned warning remains.
