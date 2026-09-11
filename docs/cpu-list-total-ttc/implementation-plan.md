# CPU-list total TTC implementation plan

Status: implementation in progress in [PR #381](https://github.com/cTux/ae2-crafting-time/pull/381).
[Issue #324](https://github.com/cTux/ae2-crafting-time/issues/324) remains the
implementation tracker until the change and its evidence are accepted.

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
- Inspect `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`
  before runtime verification, then execute its required selection. Current
  CS-02/CA-04 policy selects full affected-target suites for shared wire changes
  unless a reviewed mapping proves narrower coverage. The new scenario adds
  feature evidence; running it alone does not satisfy those broader gates.
  Preserve `selection.json` and report required broader coverage explicitly.

## 6. Exercise real screens

Before changing driver code, update `docs/test-driver/spec.md` and
`docs/test-driver/technical-design.md` with the A1-A5 fixture, transitions,
observations, and connected dedicated-server case described here. Extend the
existing driver with the `cpu-list-total-ttc` scenario under
`shared/src/testDriver1201` and the 26.1.2 test-driver boundary, and register it
through the existing scenario/coverage path. This scenario is planned, not an
existing runnable command. Use the prepared-client smoke skill and these exact
target/profile pairs:

| Minecraft / loader | Prepared target | Profile | Feature scenario |
| --- | --- | --- | --- |
| 1.20.1 Forge | `1.20.1-forge` | `compatible` | `cpu-list-total-ttc` |
| 1.20.1 Fabric | `1.20.1-fabric` | `compatible` | `cpu-list-total-ttc` |
| 1.21.1 NeoForge | `1.21.1-neoforge` | `compatible` | `cpu-list-total-ttc` |
| 26.1.2 NeoForge | `26.1.2-neoforge` | `compatible` | `cpu-list-total-ttc` |

Resolve the native launch manifest from the configured `PreparedLaunchRoot`
using the target and resolved loader as documented in
[prepared UI smoke](../dev-client.md#prepared-ui-smoke). Record dependency and
selected adapter identities; keep any required newest-adapter fixture separate
when the compatible graph reaches an older adapter. Run clients sequentially.

- Start three jobs with distinct learned totals, one idle CPU, and one unknown
  estimate. Include partial and stalled estimates. Assert badges before
  selecting CPUs, then compare selected card/title from the same snapshot.
- Create more than six CPUs, scroll both ways, rename/reorder entries, remove
  a CPU, finish/cancel jobs, and replace a job with the same output on the same
  CPU. Confirm tooltip names and row selection through the badge.
- Open another grid with overlapping names/serials, close/reopen, reconnect,
  and delay/drop responses to test expiry. Confirm no stale totals.
- Capture selected/unselected cards with long names/times in English (`en_us`)
  only, at the smallest/largest GUI scales where the screen fits. Verify name,
  icon, amount, progress bar, and scrollbar remain unobstructed. Preserve
  English/Ukrainian static key/placeholder checks and layout cases for long
  Cyrillic CPU names; do not switch runtime language or duplicate screenshots.
- Run integrated and dedicated-server cases on all four targets. Include
  supported addon scopes when installed; unknown scopes stay blank. Record
  target/dependency versions, assertions, screenshots, and logs. Builds alone
  do not demonstrate runtime support.
- For each dedicated case, connect the matching prepared client to a disposable
  dedicated server with matching production/driver artifacts. Prepare the real
  multi-CPU grid on the server and exercise A1-A5 through that client's menu,
  including reconnect and expiry. Reuse the scenario's fixture/observation
  logic and existing launch tools; add only the missing driver connection and
  server-fixture boundary needed for this case. Record the connection, both
  artifact identities, server estimates, and matching client snapshots. The
  existing headless `DedicatedCpuScenario` does not provide this UI evidence.

## Step 10 diagnostic execution contract

- Capture phase 1 once into an immutable resume bundle containing the marked
  disposable world, continuation/evidence, head and artifact hashes. Record the
  base/catalogue mode plus the ordinal managed JAR name/hash catalogue in both
  bundle provenance and the world marker; reject mismatch before launch.
- Use resume-only phase 2 for diagnosis, with current-PID callback liveness,
  an absolute loader/world/fixture startup deadline, and a 60-second checkpoint
  watchdog armed only in the active scenario phase. Record evidence and exactly
  one client launch. Never count it as final approval.
- After the focused fix passes, run the complete two-process scenario at the
  committed head and record phase timings, launch count, PID/start identities,
  and predecessor continuation hash.
- Changed planning selects only `cpu-list-total-ttc` and the primary graph on
  all four targets. Seal one bundle per head/fingerprint and verify its byte
  identity before integrated or connected-dedicated reuse.

## Completion gate

Implementation is complete when A1-A7 have test or UI evidence, all four targets
pass required checks, and the feature PR has no unresolved blocking feedback.
Link the implementation PR and evidence to #324. No release is published as
part of this work.
