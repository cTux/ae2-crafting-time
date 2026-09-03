# Automated UI Testing Technical Design

## Change-based selection research and design

Planned extension for CS-01 through CS-09 in the [specification](spec.md).
Repository baseline: `728e279edbfd33925a16469e0531ea6afabe8461`, inspected
2026-09-03. The following findings describe source inspection, not a new smoke run.

### What can be reused

| Evidence | Finding and consequence |
| --- | --- |
| [`run-ui-smoke-matrix.ps1`](../../scripts/run-ui-smoke-matrix.ps1) | Already builds immutable bundles on the host and dispatches sequentially; target selection currently uses only `-Target`, with one scenario string shared across selected targets. Add planning before this loop. |
| [`StandardAe2Scenario`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/StandardAe2Scenario.java) | One phase machine owns 18 check names; DELAYED is phase 15, after plan/status sorting and resets. Merely exposing phase 15 would depend on earlier mutations. |
| [`StandardCraftFixture`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/StandardCraftFixture.java) | Reusable real AE2 grid and two furnaces. `prepare`, `seed`, `pump`, `cpu`, and fresh-sample observations already cover setup and real completion. |
| [`SuitePlan`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/SuitePlan.java), [`TestDriverRuntime`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/TestDriverRuntime.java), [`prepare-ui-smoke-suite.ps1`](../../scripts/prepare-ui-smoke-suite.ps1) | Existing flat ordered cases, unique worlds, per-case results and world transitions provide the needed isolation. Both plan validators cap the list at 32. |
| [`DriverResult`](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/DriverResult.java), [`run-ui-smoke.ps1`](../../scripts/run-ui-smoke.ps1) | Driver and host each validate exact required checks; the host also requires specific screenshots. Update both, not just scenario registration. |
| [`get-ui-smoke-coverage.ps1`](../../scripts/get-ui-smoke-coverage.ps1) | Coverage entries refer to a single scenario. Group coverage must aggregate all required leaves, while focused campaigns leave unrelated cases unrun. |
| [`26.1.2 build`](../../versions/26.1.2-neoforge/build.gradle), [`test-driver.gradle`](../../scripts/test-driver.gradle) | Even 26.1.2 consumes selected `testDriver1201` classes, including the standard flow. Folder names alone cannot infer target ownership; exclusions and target replacements matter. |

The checked-in full lists contain 29/11/25/14 cases for Forge 1.20.1,
Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. Replacing one standard
entry with six leaves yields 34/16/30/19 before any adapter-policy graph
partitioning. Raise the validated flat-plan limit to 64 in the Java and
PowerShell validators together; do not split a compatible graph into extra
client launches solely to work around 32.

External research supports two narrow decisions:

- [Git diff documentation](https://git-scm.com/docs/git-diff) defines merge-base
  comparison, name/status output, rename handling and NUL-delimited paths. Use
  these facilities instead of parsing human-readable diff headings.
- [Playwright isolation guidance](https://playwright.dev/docs/best-practices#make-tests-as-isolated-as-possible)
  explains why tests need their own state and should run independently. Apply
  that principle to fresh Minecraft worlds using our existing suite runtime;
  do not introduce Playwright or a new test framework.

### Public command contract

Extend the existing host `run-ui-smoke.ps1` and matrix entry point:

```powershell
# Existing full run stays unchanged.
.\scripts\run-ui-smoke.ps1
# Plan and execute changes relative to the branch point, including local edits.
.\scripts\run-ui-smoke.ps1 -Changed -BaseRef origin/master
# Inspect selection without builds, downloads, VM access, or client launch.
.\scripts\run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly
# Existing manual selector; standard-ae2 now expands to six leaves.
.\scripts\run-ui-smoke.ps1 -Target 1.20.1-forge -Scenario delayed-status
```

Add `-Changed`, `-BaseRef` (default `origin/master`), and `-PlanOnly`.
`-PlanOnly` also supports existing explicit full/manual scopes. In changed mode,
reject `-Target`, `-Scenario`, `-ProjectId`, `-Interactive`, and `-Latest` rather
than silently intersecting away required coverage. Existing explicit commands
retain these options and their current restrictions. No new public arbitrary
case-list override is needed: the planner hands its validated flat list to the
runner internally. Interactive mode stays one leaf; reject a group.

The planner is `scripts/get-ui-smoke-plan.ps1`, returning an object; the matrix
runner writes `selection.json` before build/dispatch. No new background service,
post-commit Minecraft launch, or GitHub-to-VM execution is part of this change.
Update development/smoke skill instructions to invoke `-Changed` for authorized
change-focused smoke. Full requests and release gates keep full mode.

### Diff and freshness rules

1. Resolve `BaseRef` and HEAD to commits; use `git merge-base` and compare that
   commit to HEAD. Fail on missing/ambiguous merge bases or Git errors. Never
   fetch implicitly or silently compare against an empty tree.
2. Read `git diff --name-status -z -M <merge-base> HEAD --`, then union staged,
   unstaged and non-ignored untracked paths. Parse NUL records from redirected
   native stdout, preserving spaces, Unicode and newlines. For rename/copy
   records inspect old and new paths; deleted paths still retain ownership.
   Normalize separators to `/`; use ordinal case-sensitive repository matching.
3. Detect unmerged entries and stop. Use `--` to end revision options, resolve
   refs before use, and never execute content found in a changed file.
4. Store schema 1, mode, base ref/SHA, merge-base SHA, HEAD SHA, sorted changes,
   rule-file hashes and a SHA-256 fingerprint of relevant working-tree content
   (including untracked files and deletion markers). Recheck before host build
   and after bundle creation. If changed, fail with `STALE_PLAN`; require a new
   invocation. Once sealed, evidence identifies the immutable bundle hashes.
5. `-PlanOnly` emits the same plan but never builds or launches. An empty or
   fully ignored change set has overall `NOT_REQUIRED` and explicit reasons.
   Execution failures cannot be converted into `NOT_REQUIRED`.

### Conservative impact rules

Add one reviewed `scripts/ui-smoke-impact.json` with schema 1, source ownership
rules, behavior rules and explicit no-runtime paths. It contains paths, target
IDs, scenario/group IDs and reasons, never dependency versions. Rules are data;
reject invalid patterns/IDs, duplicate rule IDs and contradictory no-runtime
classifications. Union matching runtime behavior rules; a broad rule dominates
a narrow one. Ownership defaults are used only when no narrower ownership rule
applies, and ambiguity unions targets rather than choosing one.

| Path/category | Target ownership | Behavior selection |
| --- | --- | --- |
| `versions/<target>/src/main/**`, target fixture or driver | Exact target | Reviewed behavior mapping, otherwise full target |
| `shared/src/main/java/**`, `shared/src/mcCommon/**`, common resources | All four | Specific rule below or full |
| `shared/src/mc1201/**` | Forge/Fabric 1.20.1 and NeoForge 1.21.1 | Specific rule or full |
| `shared/src/mc2612/**` | NeoForge 26.1.2 | Specific rule or full |
| `shared/src/neoforge/**` | Both NeoForge targets | Specific rule or full |
| `shared/src/testDriver1201/**` | All actual consumers, conservatively all four unless an exclusion is explicitly validated | A dedicated leaf/fixture selects its cases; shared orchestration selects full |
| `shared/src/testDriverAddons/**` | Forge 1.20.1 and NeoForge 1.21.1 where included | Integration case intersected with declared support; shared code selects full consumers |
| Target `build.gradle` or loader metadata | Target | Full target |
| Root/shared build logic, wrapper, release/client/coverage/impact/catalogue data, smoke launch and validation scripts | All four | Full, including newest-adapter focused obligations |
| `StallDiagnostic.java` | Its consuming targets | `delayed-status` including tooltip assertions |
| A dedicated `NoSpaceScenario.java`, `NoProviderScenario.java`, or `NoPowerScenario.java` driver file | Actual consumers of that shared or target replacement file | Corresponding `no-space-status`, `no-provider-status`, or `no-power-status` case |
| `CraftingStatusTableRendererMixin.java`, `CraftingRowState.java` | Their consuming targets | `standard-status-controls`, `waiting-status`, `running-status`, `delayed-status`, `no-space-status`, `no-provider-status`, `no-power-status`, `craft-lifecycle` |
| `CraftProfiler.java`, common caches, packets, general text/layout/sort helpers | Their consuming targets | Full; their effects are wider than one status |
| English language JSON | Resource consumers | Semantic key diff: `text.ae2craftingtime.ttc_delayed` -> `delayed-status`; any other changed key -> full |
| Ukrainian language JSON only | No runtime under SP-03 | Static key/placeholder validation still required |
| `docs/**`, `images/**`, root prose, `.codex/skills/**`, `AGENTS.md`, pure test source trees and `scripts/test-*.ps1` | No runtime | Explicit `NOT_REQUIRED`; normal relevant checks continue |
| Any unclassified path | All four if ownership is unknown | Full with a fallback reason |

For JSON key narrowing, parse old and new documents, union added/deleted/changed
keys, and compare values structurally. Formatting-only changes need no UI run;
malformed JSON fails preflight. Duplicate keys are invalid, not silently last-win.
File add/delete selects the full resource scope. Any unrecognized changed key
widens coverage. Staged and unstaged versions cannot hide an intermediate
change; their union may conservatively run more cases.

Initial scope is file ownership plus this bounded resource-key comparison.
Do not add an AST parser, hunk keyword classifier, production marker comments,
or a second call graph. In particular, changing only `delayedTtcLine()` inside
the shared renderer still selects all status cases. Maintainers may add a
narrow path rule only with caller evidence and selector regression cases.
Unit tests remain responsible for branches no UI fixture can distinguish.

Validate ownership against the four actual Gradle source-set declarations and
driver exclusions during implementation and whenever those declarations change.
Unknown/new source layouts deliberately broaden; the planner must not guess
that every directory named `1201` excludes 26.1.2.

### Groups and independent standard flow

Add `scripts/ui-smoke-groups.json` with one `standard-ae2` entry listing the six
leaf IDs in the spec's order. Full target JSON lists retain `standard-ae2` and
their other existing entries. Expand groups on the host, stable-deduplicate
leaves, validate target support, then prepare the existing flat `SuitePlan`.
Only one group level is allowed; reject unknown members, nested groups and
duplicates inside a group. This is a list, not a dependency graph of tests.

Keep the common standard fixture and one bounded standard-flow implementation.
Introduce named flow stages and a leaf identifier instead of jumping into the
old numeric phase machine. Factor only the repeated operations: prepare,
open plan, submit through UI, open status, wait for stable observations, pump
real output, and verify completion. Each leaf starts at preparation and owns
the minimal route needed for its assertions. Existing DriverPlatform and
ServerDriverPlatform handle target API differences, including 26.1.2.

| Leaf | Fixture and assertion route; old checks retained |
| --- | --- |
| `standard-plan-controls` | Prepare seeded distinct estimates; open plan; run plan controls. Retains `plan`, `plan-sort`, `plan-tooltip`, `plan-details`, `plan-reset`; verify total and plan badge bounds. |
| `standard-status-controls` | Prepare, submit, open status; assert controls while real job remains busy. Retains `status`, `status-sort`, `status-tooltip`, `status-details`, `status-reset`, `header`, `layout`. Restore samples locally when reset would prevent a later assertion. |
| `waiting-status` | Withhold input completion for smooth stone after stone is dispatched; assert waiting row, then fuel/import stone and observe first dispatch clearing WAITING. Retains `waiting`; no sort/reset prerequisite. |
| `running-status` | Prepare and submit with seeded durations; observe active stone TTC and pending smooth stone, then fuel/import progress. Retains `running` and checks status/header bounds. |
| `delayed-status` | Prepare and submit; withhold fuel, wait for production stall detection, assert the exact active row label/color/bold/tooltip/bounds. Restore fuel, import real output and observe label clearance on recovery or job completion. Retains `delayed`; no forced stall flags or synthetic elapsed-time shortcut. |
| `craft-lifecycle` | Prepare and traverse actual terminal/amount/plan/Start/status UI; pump actual furnace output; compare sample counts, require exactly one smooth stone and idle CPU, reopen empty status. Retains `submitted`, `completed`, `output` and both original completion-view screenshots. |

Declare exact required checks and screenshots per leaf in the driver and host
validator, with parity tests. Reuse existing names within each leaf directory;
add `waiting-recovered.png`, `running-progress.png`, `delayed-tooltip.png`, and
`delayed-recovered.png` for new distinct checkpoints. Setup observations are
not results for an unselected case. Preserve modifier release, focus handling,
server-thread mutation, snapshot reset, and fresh per-world profiler identity.

The exact new leaf contracts are below. Each check must be true; screenshots
also require their existing semantic snapshot sidecars. A `sort-1/2/3` entry
means three separately required files, not one literal slash-containing name.

| Leaf | Required check keys | Required screenshots |
| --- | --- | --- |
| `standard-plan-controls` | `plan`, `plan-sort`, `plan-tooltip`, `plan-details`, `plan-reset`, `total-ttc`, `layout` | `plan-default.png`, `plan-sort-1/2/3.png`, `plan-tooltip.png`, `plan-details.png`, `plan-reset.png` |
| `standard-status-controls` | `submitted`, `status`, `status-sort`, `status-tooltip`, `status-details`, `status-reset`, `header`, `layout` | `status-default.png`, `status-sort-1/2/3.png`, `status-tooltip.png`, `status-details.png`, `status-reset.png`, `status-progress.png` |
| `waiting-status` | `submitted`, `waiting`, `first-dispatch`, `recovered`, `layout` | `status-waiting-running.png`, `waiting-recovered.png` |
| `running-status` | `submitted`, `running`, `progress`, `header`, `layout` | `status-waiting-running.png`, `running-progress.png` |
| `delayed-status` | `submitted`, `delayed`, `row`, `style`, `tooltip`, `layout`, `recovered` | `status-delayed.png`, `delayed-tooltip.png`, `delayed-recovered.png` |
| `craft-lifecycle` | `plan`, `submitted`, `status`, `profile-sample`, `completed`, `output` | `plan-default.png`, `status-default.png`, `status-finished-job.png`, `status-completed.png` |

`first-dispatch` checks the actual selected CPU/output dispatch observation;
`progress` checks actual returned output plus the synchronized UI;
`profile-sample` requires both furnace outputs to gain a real sample beyond the
setup baseline. `recovered` checks the same output's label clearance after real
progress, or the empty status after proven completion. Bounds checks use the
existing LayoutValidator. Delayed style is bold red (`0xFF5555`); tooltip checks
the rendered stall diagnostics and their actual numeric inputs, not just that
some tooltip exists. Setup seeds only retained throughput estimates.

`CraftPlanScenario`, `AddonCpuFixture.supports/create`, `DriverOptions` and
`DriverResult.requiredChecks` must agree on the six leaf names. Reject unknown
names before loading a world. The raw JVM API accepts leaves or `suite`;
`standard-ae2` is a host-expanded group. Document this raw-property migration;
the supported host CLI keeps `-Scenario standard-ae2` working.

### Execution and evidence integration

The plan records target entries with mode (`full` or `focused`), ordered leaf
IDs, requested group IDs, per-path reasons and fallback flags. Expand the
existing full lists for full entries. Carry resolved case lists through matrix,
VM dispatcher, guest runner and fixture preparation; use one leaf directly or
`suite` for multiple leaves. Keep target/profile locks and fail-before-next-client
behavior. Existing 8-minute single-case and 40-minute suite limits remain;
do not silently extend them. Runtime verification must show the expanded graph
fits, or report the measured failure for a separately justified limit change.

Keep the full compatible dependency graph for focused core scenarios; automatic
selection saves cases/targets without silently changing installed mods. Explicit
`-ProjectId` remains a separate manual graph option. Apply the existing newest-
adapter policy after behavior selection, deriving identities from the integration
catalogue rather than this impact map. A focused newest-adapter fixture is a
separate graph/run where required; unavailable required coverage fails preflight.

Write `selection.json` beside the campaign result and copy the expanded plan
with each run. Reports record individual leaves and a `groups` aggregation;
`standard-ae2` is PASS only when all six required leaves passed. Leave unrelated
project/group coverage `NOT_RUN`; never reuse the current single-scenario result
as evidence for several selected cases. Earlier leaves keep their results on
failure; remaining leaves are `NOT_RUN`. Preserve schema-1 leaf and flat-suite
data; add group/selection metadata at campaign level. Historical paths/checks
are not rewritten. Update evidence documentation and dependent archive readers
for the new layout rather than manufacturing an old-shaped aggregate result.

Named Prism packs can use the same group expander and flat suite preparation
against their inspected graph, but changed mode only dispatches prepared clients.
All fixture/process/adapter/language/artifact safeguards below remain binding.

## Planned smoke-policy enforcement

Implement [SP-01 through SP-04](spec.md#smoke-policy) without changing product
translation support. Resolve the newest implemented adapter per dependency and
target before scheduling direct cases, then assert the runtime-selected ID
against it. The integration selector's immutable catalogue/snapshot owns this
identity; do not infer it by sorting dependency versions or duplicate its order
in a runner. Until that selector exists, explicitly record the adapter/contract
established by artifact inspection in the campaign evidence.

Preserve compatible/latest graph identities. Where a compatible pin selects
an older variant, omit its direct adapter case and schedule a focused fixture
for the newest variant. Record the omitted case as an older-adapter policy skip,
not PASS, failure, or removed support. Latest-profile diagnostics do not waive
the required newest-adapter case. A requested named pack is never upgraded to
meet this policy.

Set `en_us` before the first observed frame and verify it for each scenario.
Remove Ukrainian reload/capture states from the shared and 26.1.2 driver
counterparts and update checkpoint/result validators together. Keep distinct
English behavior checkpoints and both-language static resource checks. Historical
campaigns retain their original languages, filenames, and results.

## Execution path

The public unattended command is `scripts/run-ui-smoke.ps1` with no target.
It delegates to `run-ui-smoke-matrix.ps1`, which selects release-matrix targets
in order. `-Latest` selects a separate diagnostic campaign. A focused run uses
`run-ui-smoke-matrix.ps1 -Target <id> -Scenario <scenario>`.

```text
release-matrix.json + run-client-versions.json + ui-smoke-coverage.json
  -> host run-client.ps1 -ResolveOnly -Packaged
  -> immutable production / driver / dependency bundle
  -> invoke-ui-smoke-codexvm.ps1
  -> guest-local disposable worlds and native loader
  -> driver observations, screenshots, logs and campaign result
```

JAR builds and dependency resolution stay on the host. The guest uses its
installed loader's `launch.json`; target, Java major and resolved loader must
match. `prepare-ui-smoke-launch.ps1` preserves that installation's native
classpath and assets, replaces its game directory and test properties, and
uses an 8 GB heap. It copies the bundle's exact manifest into the owned runtime
and verifies every copied JAR hash. Bundles are immutable for each campaign;
a later build cannot replace files while a guest reads them.

## Ownership

| Component | Responsibility |
| --- | --- |
| `run-ui-smoke-matrix.ps1` | Target order, host preparation, VM dispatch, campaign evidence and exit status |
| `get-ui-smoke-coverage.ps1` | Matrix parity, explicit project dispositions, exclusions and required scenarios |
| `run-ui-smoke-codexvm.ps1` | Interactive guest task, worktree staging and recorded-process stop |
| `run-ui-smoke.ps1` | Disposable fixtures, runtime lock, native launch, result validation and cleanup |
| `prepare-ui-smoke-suite.ps1` | One fresh marked world for each scenario in a single client session |
| Driver Java | Bounded scenarios, final UI observations, server outcome assertions and atomic results |
| Target adapters | Loader activation, Minecraft input APIs, fixture APIs and framebuffer capture |

All four targets have separate development-only driver artifacts under
`build/test-driver`. Their version must match the installed production mod.
Existing artifact checks reject driver classes and mod IDs in production JARs;
release discovery accepts only production filenames. Explicit test properties
and a valid disposable-world marker are required for activation.

## Standard scenario

`StandardAe2Scenario` creates a native AE2 grid in the copied world. Two actual
vanilla furnaces process cobblestone into stone, then smooth stone. Known
retained samples give the two rows distinct estimates for sorting assertions.
Fixture code supplies fuel and imports only actual furnace output through the
AE2 storage API. Completion requires exactly one smooth stone in storage and
an idle crafting CPU.

The client opens the terminal face, selects the craftable output, clicks the
amount confirmation and Crafting Plan buttons, then opens Crafting Status.
It exercises all TTC sort modes, hovers the visible item, sends modifier-click
input and checks returned chat plus server reset state. Fuel is withheld to
observe waiting, running and delayed states, then restored to complete the job.

Driver observations inspect final screen rows, rendered translation components,
widget bounds, item cells and tooltips. Assertions use those observations and
the server's actual job/output state. Each input phase waits for stable frames;
phase timestamps and the last fixture checkpoint make timeouts attributable.
Modifier keys are released on both success and failure.
The Windows VM receives native key events through Minecraft's existing JNA
library; input does not depend on AWT's headless setting. Before pressing keys,
the driver verifies native foreground ownership and transfers focus from the
previous desktop window when necessary. It waits for Minecraft to observe the
modifiers before clicking the row.

After completion, the driver saves the last job view, returns to the terminal
and reopens Crafting Status through its buttons. The fresh view must be empty.
Older AE2 builds can retain a final incremental row in the already-open view;
`status-finished-job.png` preserves that observation instead of hiding it.
Each screenshot also has a JSON snapshot with transformed screen-space bounds.

## Coverage and results

`ui-smoke-coverage.json` contains project IDs, dispositions and required
scenario IDs, without dependency versions. Its keys must exactly match the
client matrix. Compatible exclusions require a reason; replacements remain
explicit. A missing standard or direct scenario fails before launch. Startup
alone never promotes coexistence to direct integration coverage.

The driver atomically writes `result.json` with schema, driver, target, profile,
scenario, `complete`, result, required checks, screenshots and optional failure.
The runner independently validates these fields, screenshot existence, process
exit and fatal-log absence. Each suite case has its own evidence directory;
shared logs and the resolved-mod manifest belong to the containing run.

Campaigns are stored under
`build/ui-smoke/campaigns/<UTC-run-id>/<profile>/<target>/`. They preserve the
host bundle, coverage ledger and copied guest run. Compatible failures produce
a nonzero exit after all selected targets have been attempted. Latest failures
are retained as `DIAGNOSTIC_FAILURE` and do not change compatible results.
A focused campaign leaves unrelated coverage `NOT_RUN`.
Completed case outcomes remain in the ledger even when another part of the run
fails. An unconfirmed client exit stops the campaign before another launch.

## Fixture and process safety

The runner hashes the marked source fixture before copying and verifies it
again during cleanup. Only uniquely named copies inside the owned runtime may
be opened or removed. Each suite case receives a separate copy. Runtime locks
prevent concurrent use of the same target/profile directory.

Fabric keeps the marked Forge block layout but copies `level.dat` from the
tracked Fabric world. This removes the Forge metadata dependency on Blood Magic
dimensions without changing either checked-in world. Both source layout and
target metadata hashes are recorded before and after the run.

The native Java PID, process creation time and argument-file path identify the
launched client. Normal driver completion requests shutdown. Timeout cleanup
can terminate only that recorded process tree; explicit stop also verifies its
creation time and command line to reject a reused PID. No broad Java process
selection is used. Failed runs retain available evidence before later attempts.

Interactive diagnosis is single-target only. It forwards explicit interactive
mode and the existing per-run token environment to the driver. The bounded
loopback endpoint and its existing allowlist remain separate from unattended
release-facing runs.

Screenshots require visual review, not pixel equality. The permanent archive
and timing contract is documented in [UI smoke evidence](../ui-smoke-evidence.md).
