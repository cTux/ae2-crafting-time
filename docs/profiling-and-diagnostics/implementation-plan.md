# Live throughput learning and normalized sample details

Issue: [#114](https://github.com/cTux/ae2-crafting-time/issues/114).
Status: documentation only; none of these implementation steps has run.
Read the [specification](spec.md#planned-normalized-sample-details-114) and
[technical design](technical-design.md#planned-design-for-114-per-unit-sample-presentation).
This replaces the earlier presentation-only implementation plan.

## Fixed decisions

- Learn from completed intervals during a running order, even with old history.
- Combine same-network/output returns within one tick across callbacks and CPUs.
- `9 items / 90 ticks` displays `1 item / 10 ticks`, still one observation.
- Keep weighted-rate/outlier/reliability formulas; change when evidence arrives.
  Three clean observations may remove `?` before completion; ten is only a limit.
- Final completion adds no duplicate whole-order sample. Save/reset/cancel cannot
  duplicate or resurrect output. Existing saved raw pairs remain valid history.
- Keep existing one-second UI polling, packet layouts, and optional-mod boundaries.

## 1. Implement and exercise the pure collection state machine

Own `shared/src/main/java/com/ctux/ae2craftingtime/core/CraftProfiler.java` and
`shared/src/test/java/com/ctux/ae2craftingtime/core/CraftProfilerTest.java`.
Replace whole-window retention with the design's cursor/bucket/flush path and
same-tick tail amendment. Update all callers/tests of `complete`'s result. Remove
obsolete cumulative preview/rebuild behavior; preserve pending CPU attribution,
last progress, waiting, remaining-work bookkeeping, and accuracy boundaries.

Write deterministic event traces L1–L10 before changing old window assertions.
Tests must call the real collector API with dispatches, completions, flushes,
reset/cancel/save boundaries, and tick advances. Assert raw arrays and rate,
not only formatted text. Existing tests that assert full-window retention must
be replaced by the new requirement's interval assertions, not deleted wholesale.

Required extra traces:

- Split vs unsplit same-tick return gives identical rate/count/duration.
- Multiple CPUs with staggered dispatches/returns count network elapsed time once.
- Cancel one CPU while another survives; cursor is not rewound to original start.
- Empty flushes, stalled time, no GUI, repeated reads, and successful finish
  cannot grow the count. Progress after a silent gap includes the whole gap.
- Final return plus immediate finish/cancel; save before end tick; save followed
  by another return and repeat save in the same tick; tail amendment after an
  idle same-tick redispatch; next-tick reference pruning; reset clears the tail.
- Invalid/unmatched/simulated outputs, unit boundaries, clock regression,
  checked amount overflow, and retention eviction do not create corrupt samples.
- A slower/faster new interval changes learned rate with old history present.
  The third clean sample removes `?`; an excluded outlier can restore it. Limits
  one/two remain unreliable. Frozen accuracy prediction does not change.
- Delay tests use raw batch interval: `(9,900)` implies 1,800-tick learned delay
  threshold, not a normalized 200-tick threshold. Keep partial-progress reset.

Exit: all collection, confidence, and lifecycle traces pass without Minecraft.

## 2. Wire server ticking, saved history, and existing UI refresh

Own both `shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProfilerBridge.java`
and the matching `mc2612` copy. Stop per-completion full snapshots; add a shared
flush entry point and keep both adapters behaviorally identical.

For every release-matrix target, own `versions/<target>/src/main/java/com/ctux/
ae2craftingtime/mc1201/Ae2CraftingTime.java` and `Ae2CraftingTimeSavedData.java`:
register end-server-tick and stopping callbacks; flush before save field reads.
Verify exact loader callback types/ordering against each pinned runtime before
coding. Core decisions above are fixed; adapters only supply lifecycle calls.
Guard against recursive save and duplicate registration. Never use a per-CPU
end tick as the network-wide flush boundary.

Snapshot once per changed tick, not per returned item or CPU. No sample dirtying
on silent ticks. Profile a burst of many returns to verify one global snapshot
per normal tick; repeated explicit saves are allowed their own snapshots.
Measure snapshot cost against retained outputs, not order size, and report it.
Preserve status/provider data in all four SavedData paths. Reuse existing version
and raw pair codecs; old records coexist with new intervals until eviction.

Verify `StatsRequestHandler`, `ClientStatsCache`, and all four
`ClientStatsRequests.java` wrappers return/apply new count, rate, reliability,
and remaining-job total even with cached history. Existing visible render
requests should suffice; fix only a proven stale-cache path. No new push packet.
Test dedicated-server load to catch client-only imports in server paths.

Exit: history changes with screens closed, saves retain finalized intervals,
and open status rows refresh without hover/reopen. Every optional CPU event
arrives before flush or uses the covered late same-tick amendment path.

## 3. Add normalized presentation on both actual details paths

Own core `ProfileStats.java` and `TimeEstimate.java` for the design's derived
methods/formatter; cover A1–A12 plus invalid index, 0.001 exactly, 1/16 -> 0.063,
nonfinite values, and long-range positive inputs in existing core test sources.
Normalization must not change raw lists or rate for an identical history.

Own shared `mcCommon/.../TtcText.java`, `StatsChatServer.java`, and shared language
`en_us.json`/`uk_ua.json`. Update sample list and average/latest text together.
Keep server components translatable; no client I18n calls. Test actual localized
values, singular item, sample count, fallback, and confidence suffixes through
`shared/src/mc1201Test/.../TtcTextTest.java` and a new `StatsChatServerTest.java`.
Make `details` package-private if needed for direct component assertions.

Exit: client/server agree numerically; tooltip text explains effective throughput
and live learning. Rate-only integrations do not gain an unsolicited sample list.

## 4. Reconcile docs, commit, and run the target checks

During implementation update the baseline sections of this directory,
`docs/architecture.md`, `docs/collecting-data-status/`, and the collection advice
in `docs/cpu-bound-stats/collection.md` to replace full-window/preview assumptions.
Keep CPU-bound collection explicitly a separate planned feature. Do not label
runtime code implemented in this documentation-only PR.

Follow repository workflow: one branch, setup-git once per clone, one conventional
implementation commit and hook-created PR before local tests. Slices 1–3 are
work order, not separate commits. Use the development skill's required checks.
Minimum focused commands after the PR exists:

```powershell
.\gradlew.bat :shared:test
.\gradlew.bat :mc_1_20_1_forge:test --tests '*TtcTextTest' --tests '*StatsChatServerTest'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
git diff --check
```

Run added lifecycle/bridge tests for each target, not only the text filter above.
Check shared locale keys/placeholders and old-save round trips. No schema or
packet change is planned. Report GitHub CI independently of local checks.

| Target | Required proof |
| --- | --- |
| 1.20.1 Forge | Server tick/save hooks, standard UI/chat, real furnace and parallel machine |
| 1.20.1 Fabric | Server tick/save hooks and live standard UI/chat, no Forge dependency |
| 1.21.1 NeoForge | Server tick/save hooks, live UI/chat, supported optional CPU |
| 26.1.2 NeoForge | Server tick/save hooks and live UI/chat, no pre-26 UI adapters |

## 5. Observe one long order learning before completion

Use the prepared-client smoke and test-driver skills. Extend the driver only
where real observations/assertions are missing. Keep captures and machine/loader
versions with tick, raw interval pairs, sample count, used count, confidence,
remaining amount, rate, and displayed TTC. Screenshots alone cannot prove server
collection. Synthetic profiler history cannot replace the real reproduction.

1. Fresh output: request 1,000 furnace outputs, keep the status screen open and
   cursor off its rows. Record one, two, and three completed intervals while
   positive work remains. Capture `?` present at two and absent at three clean
   samples within the next normal refresh, without reopening or hovering.
2. Existing history: learn two clean observations through real crafts, then run
   another large order. Prove that the new interval updates cached count/rate
   and removes `?` mid-order. Also show that three clean observations begin
   reliable; do not claim that 3/10 itself requires a question mark.
3. Parallel machine: reproduce multiple returns in a supported installed
   Mekanism factory. Record its actual slot configuration. Assert one interval
   per distinct return tick regardless of callback count. An Unobtainium furnace
   is additional evidence when available; inspect its actual mode/capacity first.
4. Change real machine speed during a long order. Verify newly completed
   intervals adapt TTC with old history present. Preserve outlier behavior and
   do not demand that TTC decrease monotonically or that `?` never return.
5. Close every GUI during production, reopen while work remains, and show that
   sample collection continued. Exercise save/reload mid-order, partial-return
   cancellation, reset, delayed output, and concurrent CPUs. No duplicate final
   sample or reset resurrection is allowed.
6. Run the standard live-count/refresh/chat path on all four targets. Inspect
   English/Ukrainian at supported GUI scales, optional CPU event ordering, and
   existing optional details where available. Keep public chat in test worlds.

For repeatable latency assertions use a local non-overloaded server/client and
allow the one-second polling interval plus measured packet scheduling. Lag does
not constitute a new sample. Missing parallel-machine or any target live-refresh
proof remains an incomplete implementation gate.

## Traceability and completion

| Requirements | Slices | Evidence |
| --- | --- | --- |
| N1, N2, N5, N6, N8 | 1, 3 | A1–A12; complete localized client/server values |
| N3, N4, N9 | 1, 2, 5 | L1–L6, L8, L10; real mid-order count/rate/confidence |
| N7, N11 | 1, 2, 4, 5 | L7–L9; old saves, reset/cancel, same-tick save/amend |
| N10 | 2, 5 | Closed-GUI learning and open cached-row refresh on four targets |
| N12 | 1, 5 | Raw batch-duration stall threshold and progress reset |

Complete only when all deterministic, integration, persistence, localization,
and runtime gates have evidence; report actual CI status and remaining limits.
The final diff must preserve raw pair/wire layouts and contain no unplanned
machine-specific collector. Update the issue after implementation is verified;
this planning PR keeps it open and does not claim implementation tests passed.
