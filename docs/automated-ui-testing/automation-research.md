# Faster unattended UI smoke testing

Research date: 2026-09-08. Tracking issue: [#347](https://github.com/cTux/ae2-crafting-time/issues/347).
Source baseline: [`7b127be8`](https://github.com/cTux/ae2-crafting-time/tree/7b127be83aa43d89b4385115c8b146177eee4f43).
This is source and documentation research, not a benchmark or a new smoke run.
The proposed changes are not implemented by this document.

## Recommendation

Keep the existing in-game driver. It already advances known scenarios without
an agent deciding what to click. Use its rendered observations and real server
outcomes for actions and assertions, then add deterministic image validation
and automatic evidence archiving. An agent should receive one finished report
or one failure bundle, with no screenshot reasoning between successful steps.

Almost fully automated is realistic for a reviewed set of checkpoints on a
fixed prepared-client environment. Fully automatic approval of arbitrary new
UI, unknown modpacks, or intentional appearance changes is not the goal. Those
need a reviewed expectation before they can join the unattended gate.

## What already exists

The following findings refer to the baseline above. Links to source files are
relative for navigation; use the pinned tree for historical comparisons.

| Component | Verified implementation | Consequence |
| --- | --- | --- |
| [Host matrix runner](../../scripts/run-ui-smoke-matrix.ps1) | Builds packaged artifacts, seals per-graph bundles, dispatches sequentially, records selection and hashes | Reuse the campaign command; no second orchestrator |
| [Selection planner](../../scripts/get-ui-smoke-plan.ps1) and [case catalogue](../../scripts/ui-smoke-groups.json) | Changed/manual/full selection and standard leaf contracts exist; #218 is closed | Do not implement selection or split the standard flow again |
| [Suite runtime](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/TestDriverRuntime.java) | Advances cases in one client, waits for the old integrated server to stop, resets observations, opens a fresh marked world | No agent is needed between cases; preserve world isolation |
| [Standard scenario](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/StandardAe2Scenario.java) | Named stages, actual UI actions, real output checks, frame-based readiness; an eight-observation stability counter | Replace neither with screen-coordinate scripts nor unconditional faster waits |
| [UI snapshot](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/UiSnapshot.java) | Final text, colors, bold state, rows, widgets, rectangles, frame, scale and screen size | Strong semantic evidence, but not proof that the framebuffer pixels are correct |
| [Shared capture](../../shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/CraftPlanScenario.java) | Saves PNG and JSON; plan readiness includes three stable row observations | Keep capture tied to a completed, expected frame |
| [26.1.2 capture](../../versions/26.1.2-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/DriverScreenshots.java) and [scenario](../../versions/26.1.2-neoforge/src/testDriver/java/com/ctux/ae2craftingtime/testdriver/CraftPlanScenario.java) | Screenshot callback completes a future; scenario waits for it and joins failures before passing | Async writing already exists on this target; retain its completion barrier |
| [Host result validator](../../scripts/get-ui-smoke-results.ps1) | Validates identity, English, exact known check sets, required files, snapshot bounds and expected adapters | It does not decode/compare PNG pixels; file presence cannot replace visual review |
| [Evidence policy](../ui-smoke-evidence.md) and [smoke skill](../../.codex/skills/run-ae2-client-smoke/SKILL.md) | Require saved-image inspection, archive identity and timing reports | The missing automation includes workflow policy, not just code |

The three existing planning files still contain historical #218 planning text.
Their new #347 sections identify this baseline explicitly; old plans and old
campaign results are not evidence that work is either absent or passing today.

## Where time can be removed

No phase durations were measured here. Rank by avoidable work first, then use
the measurement protocol below to establish the actual bottleneck.

1. **Remove agent round trips from known runs.** Invoke one campaign and let
   the driver perform every action. Progress reporting must not gate execution.
   Screenshots remain evidence, not instructions for the next action.
2. **Automate successful evidence review.** Validate every required checkpoint
   against an approved visual contract; only exceptions need interpretation.
   This requires new code and a policy change. Today every image still needs review.
3. **Use existing selection and single-launch suites.** Focused smoke must keep
   all affected coverage. Full/release requests keep full scope and separate
   required newest-adapter graphs. Do not select fewer cases just to win a benchmark.
4. **Reuse build/dependency caches.** Keep Gradle incremental work, downloaded
   dependencies and prepared native installations. Verify exact artifact hashes
   when staging; never reuse last run's PASS or mutable fixture state. Measure
   before adding another cache layer; the runner already has a bundle cache.
5. **Tighten readiness only with evidence.** The standard flow's counter uses
   stage/sort identity, while the craft-plan path observes row IDs. Neither
   means every relevant pixel is stable. First bind readiness to the checkpoint's
   actual expected screen, rows, text, geometry and server response. Only then
   test reducing a minimum frame count. Keep real dispatch and delay thresholds.
6. **Optimize host polling last.** The matrix runner sleeps two seconds while
   checking guest status. This occurs outside the in-game action loop. Poll
   detection adds roughly zero to two seconds per completed graph, excluding
   I/O and scheduling; this is an analytical bound, not a measured latency.
   Inspect status before sleeping, then use 250 ms bounded polling if timing
   shows the shared-folder overhead is acceptable. Do not add a daemon for it.

Minecraft startup, mod loading, world creation, network responses, completed
render frames and real crafting still take time. In particular, the production
delay condition has a ten-second minimum and a learned-duration condition
([architecture](../architecture.md)). Do not bypass them in a smoke run that
claims to verify the real delayed behavior.

## Alternatives and primary sources

Sources accessed 2026-09-08. These support principles or alternatives, not an
unverified compatibility claim for all our targets.

| Approach | Evidence | Decision |
| --- | --- | --- |
| Existing driver with condition-based waits | [Playwright actionability](https://playwright.dev/docs/actionability) waits for observable readiness and retries assertions until a deadline | Apply that principle in Java; Minecraft's UI is not a browser DOM |
| Cropped visual baselines | [Playwright visual comparisons](https://playwright.dev/docs/test-snapshots) describes stable captures, baseline updates and environment-dependent rendering | Use reviewed crops and a fixed render environment; no Playwright dependency |
| Loader-native game tests | [NeoForge Game Tests](https://docs.neoforged.net/docs/misc/gametest/) describes game-world test infrastructure | Useful for behavior fixtures; it does not alone verify our actual rendered screens across four loaders/versions |
| Fabric client game tests | [Fabric automated testing](https://docs.fabricmc.net/develop/automatic-testing) documents client tests, screenshots and production client runs with a virtual framebuffer | A credible alternative for newer Fabric projects; the fetched page targets 26.2, so it does not establish availability on our Fabric 1.20.1 target or justify replacing our multi-target driver |
| Agent/VNC image interpretation | Our existing evidence policy already requires it for visual sign-off | Keep for exceptions and authoring; using it between every known action adds the exact latency we want to remove |
| Full-screen pixel equality or OCR-only approval | Full frames contain moving worlds/items; text recognition cannot prove input targeting or server state | Reject as the universal oracle; assert semantics and compare narrowly defined visual regions |
| New GitHub self-hosted VM runner | Current CI runs builds, tests and PR review; smoke policy excludes a VM scheduler | Defer. One unattended local command meets this request without a new execution service |

## Automatic visual approval without hiding defects

Start with stable menu checkpoints: plan/status TTC badges, totals, sort controls
and tooltip regions. Preserve full PNGs. Compare reviewed crops with fixed
dimensions and exact RGB equality initially; permit no global mismatch budget.
An environment that cannot repeat those crops does not qualify for automatic
approval yet. Keep it in review instead of silently loosening the threshold.

Mask only an explicitly named dynamic region such as the digits of a changing
elapsed time, with its format/value checked semantically. Never mask the whole
TTC row, status label, tooltip or geometry under test. Store the reason, rectangle
and independent assertion for each mask. Moving world highlights need their own
camera/scene contract; they remain review-required until independently qualified.

Baseline identity includes target, exact dependency/adapter graph, fixture and
checkpoint revision, language, framebuffer, effective GUI scale, resources/font,
and renderer environment. Record tested source and JAR hashes separately: source
changes must be compared to the approved baseline, not create a new baseline key
for every commit. An unknown environment or absent baseline requires review.

Keep semantic result, visual result, archive result and overall gate separate.
An unchanged approved image cannot rescue a failed assertion, corrupt file,
missing case, fatal log or wrong adapter. A passing semantic result with unknown
visuals is `REVIEW_REQUIRED`, never a full PASS. Preserve existing driver result
schemas and add this gate at campaign level. The exact contract is in the
[technical design](technical-design.md#unattended-evidence-gate-design).

## Measure before claiming faster

Instrument host phases with a monotonic stopwatch and UTC timestamps. Instrument
driver stages with a monotonic clock and completed-frame IDs. Never subtract a
guest clock from a host clock; report local durations and correlate by run ID.
Count external controller decisions after launch, required checkpoints, cases,
graphs, launches, bytes copied and warm-cache hits. Zero controller decisions
is testable independently of total duration.

Use the same commit/artifacts, VM allocation, framebuffer, fixtures, graph and
coverage for before/after comparisons. Record concurrent host load. Define cold
as empty task-owned build/dependency caches plus a stopped client, and warm as
the same prepared installation and populated caches with a new client process.
Do not erase shared caches or change production clocks to manufacture a result.

1. Record one cold baseline and one cold candidate campaign for setup cost.
2. Record at least five warm runs before and five after on one representative
   compatible graph. Publish each duration, median and range; five runs are not
   enough for a credible tail-latency guarantee.
3. Validate the completed solution on all four compatible targets and required
   newest-adapter graphs. Keep any failing campaign separate from reruns.
4. Include scenario transitions, capture/encode, host validation, archive and
   human/agent review in the report, alongside build, loading and cleanup.
   Mark absent measurements `not measured` and identify overlapping phases.
5. Gate automation on zero controller decisions for qualified passing cases,
   complete evidence, and no lost coverage. Claim a speedup only when total
   observed time decreases under the matched comparison; report raw numbers
   even if startup dominates or the visual checker costs more CPU time.

[#344](https://github.com/cTux/ae2-crafting-time/issues/344) currently reports a
craft-lifecycle timeout for a named-pack AE2 graph. Resolve that affected path
before using it as acceptance evidence. It is not proof that every prepared
graph is broken, and this research did not reproduce it.

## Delivery boundary

This research PR adds the design and implementation contract only. It does not
install a framework, launch Minecraft, create baselines, change smoke policy or
claim any speedup. Implement the ordered slices in the
[implementation plan](implementation-plan.md#unattended-evidence-gate-implementation),
then enable automatic approval only for checkpoints that pass qualification.
