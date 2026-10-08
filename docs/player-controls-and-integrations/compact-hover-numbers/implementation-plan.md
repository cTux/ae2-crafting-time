# Compact hover numbers: implementation plan

Canonical status and holds: [specification](spec.md).
Implementation contract: [technical design](technical-design.md).

## Before implementation

1. The user approved the final issue wording and throughput-only scope on
   2026-10-08. Consistency review is complete and the scope is ready-to-implement.
   Keep `backlog` until starting implementation is explicitly authorized;
   this planning PR does not close #677.
2. Recheck the actual head, callers of `statsLines`, `compactMessages`,
   `normalizedDetails` and `StatsChatServer.details`, release matrix and #616.
   If numeric overflow blocks the real fixture, record the blocker before
   claiming extreme-value runtime acceptance.

## One implementation change

1. Own shared core `ThroughputNumbers` and its `shared/src/test` unit tests.
   Implement the spec table, suffix promotion, scientific ceiling and full
   decimal output. Test every tier, just-below/at/above boundaries, 999.995-style
   carry, tiny positive values, negative/zero, NaN, infinities, Double.MIN_VALUE,
   Double.MAX_VALUE and the screenshot-scale values. Compare full output to
   the canonical decimal representation, not an invented exact integer recovered
   from a double. Cover changed pure logic completely by lines and branches.
2. Own `OptionFeature`, the required `ClientConfigFile.DESCRIPTIONS` entry,
   option configuration tests and localized option text. Extend
   `ClientConfigFileTest` to save each toggle state without throwing, assert the
   new TOML description and `compactHoverNumbers` boolean, and reload that state.
   Verify default on, missing-key on, Done/Cancel, both reset paths and
   independence from existing switches. Reuse the existing option plumbing
   and generic client-help tooltip; only the new screen label is localized.
3. Own shared `TtcText`, `StatsChatServer` and their English/Ukrainian chat and
   hint templates. Change only the throughput call sites. Extend `TtcTextTest`
   and `StatsChatServerTest` with compact/on, legacy/off, full chat, both units
   of time, fractional precision and unchanged sample/confidence/accuracy cases.
   Add a server summary/handler assertion using `9_007_199_254_740_993L`, checking
   the unchanged integral component argument and rendered `9007199254740993`;
   the small runtime job is not evidence for long precision. Use parameterized
   `ProfileStats` with ITEM, MILLIBUCKET and MANA units in hover/client/server component
   tests; verify both rate fields and translated units in each mode. Test fluid
   and chemical normalization through the existing `ProfileAmountsTest` and
   `AeKeyAmounts` boundary coverage before display. These non-item unit checks
   are component/boundary evidence, not addon runtime claims.
   Assert invalid rates render `?` with the option both on and off; legacy
   two-decimal output applies only to valid positive rates. Assert `999.995`
   remains `1000.00` and `999995` promotes to `~1M`. Supply constructed
   `ProfileStats` at `1e21` and around the E ceiling to the shared text/component
   tests used by both native renderers. This is renderer-component evidence,
   not a claim that the server seed can produce that rate. Check translation
   placeholder counts and types in both locales.
4. Extend the nearest existing renderer/configuration tests to demonstrate
   native and foreign tooltip lines remain intact, both native screens use the
   shared path, detailed-tooltip off remains off, and disabled chat/context/
   cooldown behavior is unchanged. No packet fixture or NBT shape should change;
   run existing relevant boundary tests to establish that fact.
5. Update `docs/dependencies.md` for the changed standard-suite coverage and
   counts on all four targets. Update the parent controls/profiling docs and the three guide topics in both
   locales using [player-documentation.md](player-documentation.md). Match wiki
   topics after reading current pages. Update screenshots only from reviewed
   evidence. Keep feature/index status honest about remaining validation.
6. Follow the repository's single conventional commit and post-commit hook PR
   workflow. Do not run local tests before the hook has created the PR. Then
   execute targeted formatter, renderer, configuration and server-chat tests;
   build all four release targets and report GitHub CI separately.

## Runtime acceptance matrix

### Fixture implementation owned by this feature

Add a `compact-hover-numbers` leaf to the shared `StandardAe2Scenario` check
registry and dispatch, with a focused `CompactHoverNumbersScenario` helper under
`shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/`. Extend
`TestDriverCoreTest` for leaf registration and mandatory result checks. Add the
leaf to the `standard-ae2` group in `scripts/ui-smoke-groups.json` and its
visual contract in `scripts/ui-smoke-visuals.json`; group expansion supplies it
on all four targets. Define its `cases` catalogue entry in `ui-smoke-groups.json`
with the mandatory checks and screenshot names below, and add the scenario to
the `scripts/run-ui-smoke.ps1` allowlist. Extend `scripts/test-run-ui-smoke.ps1`
and `scripts/test-ui-smoke-plan.ps1` to prove group expansion and standalone
scenario launch accept it with the required result contract. Leave
`scripts/ui-smoke-coverage.json` unchanged: it maps
dependency project IDs to top-level scenarios, not individual leaves. Use the
existing runner and evidence archive, not a new launch script.
Add `scripts/ui-smoke-impact.json` rules selecting the leaf for changes to
`ThroughputNumbers`, `OptionFeature`, `ClientConfigFile`, `TtcText`,
`StatsChatServer`, both native renderer mixins, the new driver helper, and
`shared/src/main/resources/assets/ae2craftingtime/lang/en_us.json`.
Keep `uk_ua.json` under the existing `ukrainian` noRuntime rule and static locale
validation; never match it with a behavior rule. Add a planner test proving a
Ukrainian-only change causes no rule conflict and selects no visual smoke leaf.
Extend `scripts/test-ui-smoke-plan.ps1` with each path and a negative unrelated
path to prove focused `-Changed` selection includes the leaf when required.
Also select `crafting-tree-screen` for shared chat/locale changes on targets
where Tree is supported; retain the existing absent-addon/26.1.2 exclusion.

Extend `CraftingTreeScenario` and its existing `StatsInteraction` call to assert
both full-rate fields and the selected node's exact requested amount in the
actual received chat component. Add a `full-chat-rates` required result check
to the `crafting-tree-screen` contract and a corresponding chat screenshot in
the visual manifest. Run on the supported 1.20.1/1.21.1 targets with Tree installed;
cover both widget mixin layouts with the real `crafting-tree-screen` scenario:
use the Forge 1.20.1 artifact identified by the source URL and SHA-512 in
`shared/src/test/resources/integration-contracts/tree-helper-forge.tsv` for the
helper layout, and the NeoForge 1.21.1 artifact pinned by
`tree-layout-neo.tsv` in that directory for the direct-amount layout. Prepare
separate disposable dependency selections; never install both Tree artifacts
in one client. Before each run, inspect the
resolved widget/data class members and record which mixin applies; a changed
dependency layout blocks that layout's gate rather than silently counting the
other one twice. For each run, record the selected node's output ID and source
amount (`amountHelper.craftAmount` or `amount`), Ctrl+click that node, and assert
the received chat has that output, its normalized amount and both full rates.
Use two sibling nodes with distinct positive amounts and click each to detect a
wrong-node match. The TSV adapter descriptions are not executable click evidence.
No new throughput tooltip is added to Tree. Record Tree
coverage in `docs/dependencies.md` alongside the standard-suite change.

Own a parameterized seed operation beside `StandardCraftFixture.seed` in both
the shared driver and `versions/26.1.2-neoforge/src/testDriver` adapter. Reuse
the real grid, stone processing pattern and held-output job. On the integrated
server thread, clear only the fixture output's retained stats, then call
`ProfilerBridge.start` and `complete` with one positive amount/duration pair,
following the existing seed path. Flush through the normal server tick before
checking `ProfilerBridge.stats`. Do not inject the client cache or fake chat.

Use independent cases `(amount, durationTicks)` of `(1, 20)`, `(1, 1000)`,
`(1000, 1)`, `(1000000000000000, 1)` and `(1000000000000000000, 1)` for ordinary,
tiny fractional, thousand, petascale and exascale rates. One sample per case
avoids weighted accumulation overflow; derive expected `/t` and `/s` from the
server snapshot and compare against independently specified expected strings.
This tests server-owned presentation data, not natural production at those speeds.

Open the native plan for the same stone output, await the normal snapshot and
hover it, then submit the small real job with its output held so the same row
remains in native Status. Never clear or reseed stats after job submission:
`clearStats` also removes pending profiler state. Seed only while the fixture
has no active job. Hold returned output during capture so no new completion
changes the retained rate; if setup changed the expected rate, fail that case,
cancel and clean up the job, then prepare a fresh case. Await the next normal
snapshot before capture. Use
`StatsInteraction` for a real Ctrl+click in each screen and inspect the received
chat component for both full rates and the clicked amount. Toggle compact
formatting and capture both modes. Never infer chat correctness from hover text.

Require checks `seeded-server-rate`, `plan-throughput`, `status-throughput`,
`full-chat-rates`, `legacy-hover` and `fixture-restored` for every case; write
`compact-hover-<case>-<plan|status|chat|legacy>.png` and map each visible check to
its image. Fail on missing snapshots, wrong output or unexpected rate changes.
The helper must capture the original in-memory client option before its first
toggle and restore it explicitly on success, failure and cancellation, before
finalizing the result. Wire this idempotent close operation through the scenario
and `CraftPlanScenario.cleanup` lifecycle; `SuiteFixture.restore` does not restore
client options. Observe the restored value before setting `fixture-restored`.
Extend lifecycle tests for success, failed capture, cancellation and double close.
Use suite cleanup for samples/world state and cancel held jobs; do not rely on
post-result cleanup to prove an already-written check. The automated helper does
not save persistent user settings; saving and restoring files belongs to the
explicit manual relaunch case below.

Connected-server coverage is a manual acceptance case, not a new connected
runner scenario. Use the prepared dedicated server and a disposable test world;
connect the matching client, build a powered native AE2 grid with a crafting CPU,
terminal and a furnace processing pattern, and complete at least one stone job.
Request another stone job, hover the plan, Ctrl+click, then submit it and repeat
on the pending Status row. Record the full chat components and tooltip screenshots
for the same output. Verify the compact values are the defined rounding of the
reported full rates; use two clients to check broadcast and two distinct grids
to check isolation. Test chat off, a repeat click within the cooldown, and reset.
Keep command/state evidence for the server settings and the selected grid.
Do not pass the new leaf to `run-connected-dedicated-ui-smoke.ps1` or route it
through `ResourceFixtureClient`; no connected runner/fixture extension is planned.
This case does not claim seeded extreme-rate dedicated-server coverage.

Sorted/scrolled row identity is a manual native-screen case on each target.
In the disposable world, prepare a composite processing pattern with more
distinct craftable ingredients than the current viewport can display, each
backed by a processing pattern and retained history. Hold their returned outputs
so the accepted job keeps enough pending rows for Status. Record two target
outputs with different IDs, requested amounts and rates. In Plan, cycle both TTC
sort directions, move the scrollbar until a target initially outside the viewport
is visible, hover it and Ctrl+click. Assert the chat output ID and amount match
that visible row and both rates match its full server-owned details. Submit the
job and repeat in Status after sorting and scrolling there. Capture viewport
before/after scrolling, selected tooltip and resulting chat. Record mandatory
manual checks `plan-scrolled-identity` and `status-scrolled-identity`; a fixture
with no scrollbar or fewer pending rows fails setup. Do not count CPU-card scroll
tests or the single-output automated leaf as this evidence.

Relaunch is also a manual acceptance case on each prepared target. Record the
initial client config, set the new option off through Done, capture the option
screen and a legacy-format tooltip, then exit the Minecraft process normally.
Inspect the saved `compactHoverNumbers = false`, relaunch the same instance and
world without deleting or regenerating config, and capture the still-off option
and tooltip. Repeat with on. Test Cancel and reset separately, then restore the
original config. Archive before/after config, process-exit/relaunch evidence and
`compact-hover-relaunch-<on|off>-<options|tooltip>.png`. A screen reopen does not
count as relaunch. No runner continuation file or resumed leaf is added, and
automated leaf completion alone cannot pass H4.

Non-item unit checks use the component/boundary tests named in step 3; existing
resource-icon and addon CPU scenarios do not establish throughput-format coverage
and are not extended by this plan. Update the test-driver spec/design with the
integrated item leaf, mark manual cases separately in coverage evidence, and
keep all fixture changes out of production JARs.

### Execution and evidence

Use the prepared-client smoke workflow on Forge 1.20.1, Fabric 1.20.1,
NeoForge 1.21.1 and NeoForge 26.1.2. Run real connected server/client chat checks
as well as a representative integrated-server case; report the exact tested
revision and fixture for each result. A constructed profile verifies rendering,
not extreme-value profiler arithmetic or OmniSequence compatibility.

For each target inspect Crafting Plan and Status with ordinary, fractional,
thousand, petascale and exascale rates. Capture tooltip and Ctrl+click chat for
the same item output. Fluid, chemical and mana formatting use the parameterized
component and normalization boundary tests above; this gate does not claim live
addon throughput coverage or require new addon fixtures.
Inspect default and wide fonts at supported GUI scales, including near screen
edges. Existing sample-list wrapping is outside scope; record any remaining
width issue separately rather than claiming the entire tooltip now always fits.

Check on/off, Done/Cancel, reset and relaunch, detailed-tooltip off, compact-row
amounts independently on/off, sorted/scrolled click identity, no stats, chat off,
cooldown, reset and unchanged server-chat audience. Verify no new startup mixin
failures. English gets visual evidence; Ukrainian gets translation and component
checks, plus layout inspection if its changed text alters the available width.

| Criterion | Required evidence |
| --- | --- |
| H1 | Formatter/component tests cover all boundaries including scientific notation; native captures cover ordinary through exascale on four targets. |
| H2 | Server/client detail tests and connected chat capture with both full rates and exact long amount. |
| H3 | Renderer preservation, ITEM/MILLIBUCKET/MANA component cases, normalization boundary tests and unchanged diagnostics; only item throughput has runtime captures. |
| H4 | Generic option/config tests plus interactive save, Cancel, reset and the manual process-relaunch procedure above on four targets. |
| H5 | Existing request boundary tests and sorted/scrolled, disabled-chat, reset and cooldown scenarios. |
| H6 | Locale parity, documentation/link review, four builds/CI and reviewed visual evidence. |

## Completion gate

Planning completion requires approved issue text, resolved scope and a consistent
spec/design/plan. Implementation completion additionally requires all H1-H6
evidence, updated GuideME/wiki, green current-head CI, and an authorized merge.
Record limitations and blockers without converting them to passes. Only then
mark this scope finished and close #677. A planning merge alone does neither.
