# CPU-list total TTC implementation plan

Status: planned. [Issue #324](https://github.com/cTux/ae2-crafting-time/issues/324)
tracks implementation; merging these documents must leave it open.

Read the [specification](spec.md) and [technical design](technical-design.md)
together. Follow `AGENTS.md` and the implementation/development, UI-driver,
and prepared-client smoke skills when executing this plan.

## 1. Establish the version seams

- Confirm the four rows in `scripts/release-matrix.json` and current protocols.
  Inspect pinned AE2 `CPUSelectionList.drawBackgroundLayer` bytecode for each
  target before choosing exact mixin descriptors.
- Confirm `cpuSerialMap`, live grid membership, and actual profiler scope for
  native CPU entries, including already supported addon CPUs.
- Own the menu accessor under `shared/src/mcCommon/java/.../mc1201/mixin`;
  widget hooks belong to the `mc1201` and `mc2612` source roots.
- Deliverable: precise hooks consistent with the design, without selecting
  CPUs, replacing AE2's renderer, or changing estimation.

## 2. Add bounded per-CPU snapshots

- Add common `CpuTtcRequestHandler` and `net/CpuTtcPacketCodec`, using
  `ProfilerBridge.remainingJobSeconds` and the validated serial map.
- Add `CpuTtcRequestC2S`/`CpuTtcSnapshotS2C` wrappers and registrations in all
  four modules. Apply the design's protocol and Fabric capability rules.
- Implement the 32-entry cap, four-packet/128-serial per-second limits,
  wrong-menu/grid rejection, immutable replies, and disconnect cleanup.
- Add focused codec/handler tests in existing test source sets: empty/max,
  excessive counts, duplicates, negatives, truncation, foreign serials/grids,
  idle/unknown/disabled values, and two distinct CPU estimates.
- Deliverable: A1/A3/A6 paths with runnable checks ready for post-PR verification.

## 3. Bind the display cache to the screen and request

- Add `core/CpuTtcCache` and common client `CpuTtcRequests` coordination.
  Wire loader send/receive and connection cleanup through existing hooks.
- Capture visible serials plus selected serial, request once per second, expire
  after three seconds, and reject superseded/session-mismatched replies.
- Clear on observed idle/removal/job change or elapsed-time reset, screen close,
  and disconnect. Preserve other CPUs' entries on selection changes.
- Route `ClientStats.totalTtcSeconds()` to the list cache for capable Crafting
  Status menus. Retain the old path for standalone CPU screens and Fabric peers
  without the new capability.
- Test same-name/same-output CPUs, out-of-order replies, reused container ids,
  reopen, network switch, empty replacement, expiry, and title/card equality.
  Use a controllable monotonic clock rather than sleeps.
- Deliverable: A2/A3/A5 with one value source for the title and selected card.

## 4. Render the badge in both API variants

- Add client-only `CPUSelectionListMixin` variants and all four registrations.
  Reuse `TimeEstimate`, `TtcText`, and `TtcBadge`; share style constants only
  where title and list would otherwise duplicate them.
- Reserve the top-right interior, ellipsize only drawn names, preserve full
  tooltip names/click behavior, and scale long TTC values as specified.
- Check layout arithmetic for absent, normal, maximum long values, narrow
  width, and no remaining name space. Verify actual rendering in step 6.
- Deliverable: A4 on GuiGraphics and GuiGraphicsExtractor boundaries.

## 5. Document, commit, and verify

- Update `docs/architecture.md`, `docs/server-client-stats.md`, and
  `docs/feature-coverage.md` with shipped behavior, bounds, and wire versions.
  Keep translation wording unchanged unless new text is introduced; if so,
  update English and Ukrainian together.
- Review A1-A7 against the diff. Follow the one conventional feature commit
  workflow and let the post-commit hook push/create the PR. Do not run local
  tests before that PR exists.
- After PR creation, run focused shared/cache/codec/loader tests and required
  builds/checks through the development skill, plus `git diff --check`.
  Record GitHub tests, coverage, builds, and review separately from local checks.

## 6. Exercise real screens

Extend the existing driver with one multi-CPU scenario under
`shared/src/testDriver1201` and the 26.1.2 test-driver boundary. Run through the
prepared-client smoke skill; no named modpack is needed for native QA.

- Start three jobs with distinct learned totals, one idle CPU, and one unknown
  estimate. Include partial and stalled estimates. Assert badges before
  selecting CPUs, then compare selected card/title from the same snapshot.
- Create more than six CPUs, scroll both ways, rename/reorder entries, remove
  a CPU, finish/cancel jobs, and replace a job with the same output on the same
  CPU. Confirm tooltip names and row selection through the badge.
- Open another grid with overlapping names/serials, close/reopen, reconnect,
  and delay/drop responses to test expiry. Confirm no stale totals.
- Capture selected/unselected cards with long names/times, English/Ukrainian,
  and the smallest/largest GUI scales where the screen fits. Verify name,
  icon, amount, progress bar, and scrollbar remain unobstructed.
- Run integrated and dedicated-server cases on all four targets. Include
  supported addon scopes when installed; unknown scopes stay blank. Record
  target/dependency versions, assertions, screenshots, and logs. Builds alone
  do not demonstrate runtime support.

## Completion gate

Implementation is complete when A1-A7 have test or UI evidence, all four targets
pass required checks, and the feature PR has no unresolved blocking feedback.
Link that PR and evidence to #324. This planning PR instead requires consistent
linked documents, documentation/link checks, and green required GitHub checks;
no game code or release artifacts change here.
