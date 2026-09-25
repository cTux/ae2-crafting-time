# Badge Background Plan

Lifecycle and criteria: [specification](spec.md). Source map: [technical design](technical-design.md).

## 1. Add the client switch and render gate

Add `BADGE_BACKGROUND` / `badgeBackground` to Client / Appearance. Reuse generic
options widgets and client feature persistence. Add English/Ukrainian labels.
Gate both `TtcBadge.fillRoundedRect` implementations at their shared draw seam,
using the client preference independently of profiling. Preserve ARGB composition,
text calls, layout, and every caller listed in the design (A1-A4).

## 2. Add focused checks and player documentation

Extend `OptionsModelTest` and `ClientConfigFileTest` for default/missing/malformed
values, Off roundtrip, copy isolation, and reset. Check custom RGB and opacity at
zero, full, and intermediate alpha survive Off/On. Exercise shared visibility
policy in both states and its independence from shadow/color switches. Cover
Appearance mapping with two feature rows, including the opacity page boundary.
Preserve 100% shared line/branch coverage without changing exclusions (A1-A5).

Update the English and Ukrainian GuideME `features/configuration.md` pages and
the existing GitHub wiki configuration guidance. Explain default On, preserving
color/opacity when toggling, client-only ownership, and native-background scope.
Prepare and review the exact wiki change before its authorized publication;
record the published revision/readback separately from the repository PR (A5).

## 3. Prepare focused visual checks

Before launching a campaign, complete the design's guest prerequisite inventory:
verify SSH/tools, guest Java 17/21/25, exact compatible native-loader manifests
under the prepared client root, source/disposable marker handling, and artifact
copy route. Reuse the configured CodexVM and its existing launchers. If recovery
or missing native installations require substantial new infrastructure, report
the prerequisite and obtain scoped authorization instead of adding it silently.

Extend existing Plan/Status driver checkpoints only as needed to use the native
Appearance switch, save Off and On, and capture both states. Include a tinted
row, scaled badge, visible text, and an unchanged native row/tooltip background.
Use a nondefault badge RGB/opacity so restoration is observable. Check Cancel,
section reset, and full reset through existing Options widget helpers. Retain
the checkpoint contract in `ui-smoke-groups.json` and update test-driver docs
before changing its flow, as its skill requires (A1-A4, A6).

Use compatible profiles `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-neoforge`, and
`26.1.2-neoforge`. Select `standard-plan-controls`, `standard-status-controls`,
and `cpu-list-total-ttc` for the shared native surfaces. Add the existing
`crafting-tree-screen` and `merequester-screen` leaves on compatible targets
where those adapters are present; never invent support on 26.1.2. Each selected
surface must show Off and On. Reuse the existing suite to group leaves into one
launch per target where supported. Plan a second focused launch per target
after saving Off to verify actual relaunch persistence, then restore the test
settings. Budget eight native launches plus any optional-adapter launch the
existing runner requires; record actual launch count and time instead of claiming
unmeasured duration (A2-A4, A6).

## 4. Deliver and verify

Follow branch/hook ordering: review the complete implementation, make one
conventional commit, and let its hook create the PR before running local tests.
After PR creation run `test jacocoTestReport` and the four target builds using
the existing Gradle tasks. Validate locale key/placeholder parity. Record
current-head GitHub CI separately from local results (A5).

Review the changed-scope selection with
`scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`, then run
the approved focused selection through the prepared-client workflow. Keep the
four-target requirement even if automatic selection is narrower; do not turn
this into a full release suite. Bind screenshots, checks, and artifacts to the
immutable implementation head. Review tinted-row readability in English at
the supported small/large GUI scale checkpoints, native backgrounds, retained
text, and restored appearance. Archive each run before reuse, distinguish
diagnostic retries from clean final proof, and shut down only CodexVM after
evidence review with `vmrun list` readback (A6).

## Evidence and completion

| Criteria | Evidence |
| --- | --- |
| A1 | Two-feature Appearance mapping tests and native Options/pagination checks |
| A2 | Complete caller/two-adapter review and both-state surface screenshots |
| A3 | Core preservation/independence tests and custom-appearance visual restoration |
| A4 | Config/model checks plus native save/cancel/reset and process-relaunch evidence |
| A5 | Shared coverage, all-target builds, locale checks, GuideME diff, wiki readback, current-head CI |
| A6 | Clean focused runs on all four targets, reviewed tinted-row captures, timing and cleanup evidence |

Keep the canonical status in progress while any required check or guest
prerequisite is unresolved. Mark finished only after the implementation merge
and the listed evidence; do not inherit the earlier text-shadow task's smoke
waiver or substitute source review for visual results.
