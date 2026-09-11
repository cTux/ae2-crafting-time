# Active-order TTC sorting implementation plan

For [#387](https://github.com/cTux/ae2-crafting-time/issues/387), using the
[specification](spec.md) and [technical design](technical-design.md).
Status: ready for implementation; no step below is claimed as executed by the
documentation PR. Implement as one feature, not separate partial releases.

## 1. Extend the bounded cache and prove the ordering rules

Ownership: `shared/src/main/java/com/ctux/ae2craftingtime/core/CpuTtcCache.java`,
`TtcSort.java` only if necessary, and their tests under `shared/src/test/java`.

- Separate complete observed CPU membership from each 32-entry request.
  Implement priority-plus-round-robin selection, one outstanding request,
  timeout, per-entry merge/expiry, job generations, and AE2-mode pruning exactly
  as designed. Keep the codec limit and server budgets unchanged.
- Reuse the existing prioritized stable sorter with busy rows first and raw
  AE2 input order. Add no second comparator implementation.
- Cover modes 0/1/2; busy known/equal/unknown/zero/idle rows; source-list
  immutability; all-unknown and empty inputs; arrival and expiry changing order.
- Cover 0, 1, 25, 26, 32, 33, 100, and 1,000 busy rows; duplicates in priorities;
  selected off-page or idle; continuous scrolling; queue wrap, membership
  additions/removals, direction changes, and a response delayed past a second.
- Verify every stable busy serial appears within `max(1, ceil(B/25))` successful
  request opportunities. No packet exceeds 32 serials or one send per second.
  A three-second timeout permits later work without a burst or deadlock.
- Cover exact reply-set validation, negative/duplicate/unsolicited entries,
  consumed/old/timed-out sequences, per-job invalidation in flight, explicit
  unknown clearing, unrelated-entry retention, deadline shrink/no extension,
  and mode switch/reopen/container reuse. Maintain 100% shared line/branch
  coverage; do not move decisions into excluded adapters.

Gate: focused cache/sort tests exercise C2, C3, and C5, with deterministic fake
time and a non-starvation assertion independent of queue implementation.

## 2. Bind one display list to the native widget

Ownership: `shared/src/mcCommon/java/.../mc1201/CpuTtcClient.java`, new
`mixin/CPUSelectionListOrderMixin.java`, the existing `mc1201` and `mc2612`
`CraftingCPUScreenMixin` / `CPUSelectionListMixin`, and each target's client
mixin registration. Java package prefix is `com/ctux/ae2craftingtime`.

- Publish the existing screen mode on initialization and every button cycle,
  including when selected status data is absent. Keep one authoritative mode.
- Prepare the full-list identity/seconds snapshot at screen-update HEAD before
  title calculation. Use those frozen values for the title, CPU sort, and badges.
- Move widget open/refresh coordination out of the two badge mixins into the
  common ordering mixin. Observe the full raw list, publish a frame-stable
  sorted copy, and derive priorities from the resulting visible slice.
- Wrap all `cpus()` reads in native drawing, hit testing, and range updates;
  require the expected injection matches on every supported AE2 version.
  Keep the raw menu list untouched. Preserve offset and selection serial.
- Suppress stale-frame hits for removed/replaced jobs. Keep tooltip, click,
  native cancellation, badge, title, and item-row behavior intact.
  Hit testing uses the last drawn scroll offset, including a wheel event and
  click before the next render; there are no hits before the first draw.
- Keep channel-unavailable CPU order native. Check `ClientStats` title fallback
  remains unchanged and no new loader/network version is introduced.

Gate: minimum-dependency builds cover all four targets; prepared-client Mixin
application and real input prove C1, C2, C4, and C6. A compile pass alone does
not establish the widget injection contract.

## 3. Extend the existing CPU-list scenario and observations

Ownership: shared `testDriver1201`'s `CpuListTtcScenario`, `StandardCraftFixture`,
`UiObservationStore`, `UiSnapshot`, and
`mixin/CPUSelectionListObservationMixin`; corresponding native 26.1 driver
adapters where present. Reuse `CpuListTtcControl` for the connected server and
`CpuTtcPacketControl` for hold/drop delivery. Extend existing test homes,
including `versions/1.20.1-forge/src/test/.../TestDriverCoreTest.java`.

- Keep the `cpu-list-total-ttc` scenario name and its lifecycle/relaunch checks.
  Add sorting checkpoints rather than another runner or duplicated fixture.
- Replace raw-list reconstruction with actual per-card render observations:
  serial, job identity, displayed TTC, position, selected state, frame id, and
  scroll offset. Retain the raw order separately for the AE2-mode oracle.
- Use at least eight rows with distinct totals, a tie, unknown busy and idle
  entries, plus an off-screen job whose TTC should move it to the first row.
  Capture the initial mode and full button cycle; assert both CPU and item
  orders from the rendered observations.
- Hover and click a card after reorder and scroll; compare the tooltip and
  server-selected serial with the displayed card. Cancel the chosen job and
  verify the server changed that CPU only. Remove a row between drawing and
  input to prove stale-hit suppression and clamped scrolling.
- Add a focused 33-busy-CPU phase with server-known totals. Request observations
  must prove off-screen coverage without selecting CPUs or scrolling first.
  Do not inflate the lifecycle/relaunch fixture to 1,000 CPUs; the large-count
  fairness and freshness limits are pure tests from step 1.
- Cover equal/unknown ordering, changed estimates, same-output replacement,
  expiry in both modes, background results arriving after mode switch, and
  the absent-channel fallback. Keep original selected-title, long-name,
  scale, reconnect, second-grid, and relaunch evidence.
- Update the test-driver spec/design, existing check catalogues, and
  `scripts/ui-smoke-impact.json` so the new common ordering seam selects this
  CPU-list scenario on every target. Retain affected status/plan-control checks
  for shared sorting/button changes. Update `docs/dependencies.md` if its
  coverage descriptions change; add no support claim for an untested addon.

Gate: C1-C6 have independent checks and screenshot/JSON mappings. Observations
record what the renderer used, not what the sorter was expected to return.

## 4. Verify, reconcile docs, and finish

Follow `AGENTS.md`: inspect/statically review, make one conventional feature
commit, let the post-commit hook create the PR, then run local tests and smoke.
Do not execute these runtime steps for the planning-only PR.

- Run `./gradlew :shared:test :shared:jacocoTestReport` for changed pure logic.
- Run `./gradlew test jacocoTestReport` for the full test/boundary gate, including
  `CpuTtcPacketTest`, `CpuTtcRequestHandlerTest`, and limiter regressions.
- Run `scripts/build-all-versions.ps1` for all four release-matrix artifacts.
  No dependency minimum or wire format is changed.
- Use `run-ae2-client-smoke` and its `-Changed` selection after PR creation.
  Confirm selection includes the expanded CPU-list case and affected
  status/plan controls. Complete the CPU-list two-phase acceptance run at the
  committed head, with integrated and real connected-dedicated client evidence
  on each target below. Resume-only or headless server checks do not replace it.

| Target | Prepared graph | Required CPU-list evidence |
| --- | --- | --- |
| 1.20.1 Forge | compatible / AE2 15.4.10 at research time | integrated + connected dedicated |
| 1.20.1 Fabric | compatible / AE2 15.1.0 at research time | integrated + connected dedicated |
| 1.21.1 NeoForge | compatible / AE2 19.2.17 at research time | integrated + connected dedicated |
| 26.1.2 NeoForge | compatible / AE2 26.1.10-beta at research time | integrated + connected dedicated |

Run clients in CodexVM under the established smoke policy. Preserve `en_us`
runtime evidence and static English/Ukrainian resource/layout checks. Record
the selected optional-adapter identity; retain unknown-scope behavior and use
the existing focused fixture if the prepared graph misses an affected adapter.
Do not claim minimum-version runtime compatibility from prepared-version smoke.

Update `docs/architecture.md` and the CPU badge documents when implementing to
describe the full-list cache and adaptive TTC-mode expiry. Keep their appearance
and lifecycle guarantees. Update the TTC sorting index and player guidance to
describe both lists only when the behavior is shipped. Check relative links,
document consistency, and `git diff --check`; report GitHub CI separately.

| Criteria | Completion evidence |
| --- | --- |
| C1-C2 | sorter tests plus full button cycle showing both lists and reopen default |
| C3 | 33-CPU UI/request capture plus 100/1,000-CPU deterministic fairness/budget tests |
| C4 | rendered serials matched to hover, click, cancel, selection, title, and scroll results |
| C5 | cache boundary tests and retained lifecycle/delivery/expiry UI checkpoints |
| C6 | no-channel boundary, existing plan/status checks, native addon known/unknown cases |
| C7 | four-target build/test results and immutable-head integrated/dedicated evidence |

Implementation is complete only when every criterion has this evidence, shared
coverage remains 100%, required CI is green at the current PR head, and the
documents match the implemented behavior. Merging the planning PR completes
the research deliverable only; keep #387 open for implementation tracking.
