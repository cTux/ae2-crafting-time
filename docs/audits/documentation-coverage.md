# Audit documentation coverage

Tracking: [#375](https://github.com/cTux/ae2-crafting-time/issues/375).

The recurring audit asks whether shipped behavior changed without updating all
its maintained documentation. The issue-coverage inventory below is narrower:
it records missing repository coverage for open issues, not a completed audit of
all shipped behavior.

## Recurring shipped-behavior audit

1. Record the current source revision and merged implementation/fix range since
   the last completed audit. On the first run, audit the current shipped state.
2. Compare behavior with player/contributor docs, README files, GuideME,
   feature/dependency references, current specs/designs, repository skills,
   templates, testing/release guidance, and automation instructions.
3. Update every affected maintained surface. An issue, PR, code comment, or
   implementation plan does not replace missing player or contributor guidance.
4. Reconcile stale or contradictory prose using implementation evidence,
   authoritative requirements, tests, and conventions. Fix code that contradicts
   requirements instead of documenting the incorrect behavior as intended.
5. Follow applicable documentation, link, skill, and change-specific checks and
   review, with the repository's PR-before-local-tests ordering. Do not create
   a change when no mismatch exists.

After fixes merge, comment on #375 with Yes or No to its audit question,
the revision/range and surfaces examined, every stale/missing item and update,
and links to PRs, checks, CI, and evidence. For no-change runs, explain the
evidence supporting that outcome. Use `Refs #375`, never a closing keyword;
remove only the current run's claim and leave this recurring issue open.

## Shipped-state review, 2026-10-18

This first shipped-state review uses source
[`72797df5`](https://github.com/cTux/ae2-crafting-time/commit/72797df504d86de7ad1688acad807c98cdd51249)
and wiki revision `c93cb0c92b75997bd3076512028796f9d0037d7c`. The earlier inventory
was setup, not a completed audit. The answer to #375 is **Yes**: the following
maintained surfaces retained descriptions from before merged changes.

This is a documentation correction, not a new feature or runtime campaign.
The table defines the correction scope, its source evidence, and the checks
needed before this run is complete. Original incomplete feature acceptance gates
remain incomplete; a merged PR does not supply missing visual evidence.

| ID | Finding and affected surfaces | Source evidence and verification boundary |
| --- | --- | --- |
| D1 | README issue link filtered a retired label; bug form lacked the fourth target. | `AGENTS.md` label policy and `scripts/release-matrix.json`; inspect link and all four environment choices. |
| D2 | Feature map omitted later shipped surfaces and called ms/Instant planned. | Guide resources, feature specs, #694 and published [1.2.17](https://github.com/cTux/ae2-crafting-time/releases/tag/release-1.2.17); verify navigation without duplicating feature statuses. |
| D3 | Player spec/design described obsolete Status title, fixed initial sort and absent config UI. | Both screen mixin families, `OptionsScreen`, `ClientOptionsRuntime`, `ServerOptionsRuntime`, `FeatureOptions`; compare current title, draft/save and default behavior. |
| D4 | TTC reference described whole-window learning and second-only formatting. | `CraftProfiler.flushCompletedSamples`, `TimeEstimate`, `TtcEstimate`, `TtcText`; preserve precise calculation versus display rounding. |
| D5 | Color reference named removed seconds API and denied the color option. | `TtcColor`, `ClientConfig`, `RowTextColor`, both table-renderer adapters; distinguish configurable colors, backgrounds and shadows. |
| D6 | Sorting references ignored configured initial modes and ms precision. | Both `CraftConfirmScreenMixin` and `CraftingCPUScreenMixin` families plus `TtcSort`; retain Plan missing-first groups and original #318 evidence. |
| D7 | Profiling design called idle-only windows current and used a 30-second delay. | `CraftProfiler`, `ServerConfig`, `StatsRequestHandler`, #287 and #661; record live intervals, configurable 10-second/2.0 defaults and requester-only inventory scans. |
| D8 | EN/UK GuideME and wiki learning, confidence, reset, saved history, delay, sorting and estimated-status pages retained old semantics. Wiki time pages omitted or called ms/Instant upcoming. | Same source boundaries as D3-D7, remembered statuses in [persistence](../world-save-persistence.md#remembered-statuses), and scale-1 renderer layout; review bilingual parity and run `checkGuideResources`. Publish and read back matching wiki pages separately. |
| D9 | Dependency reference repeated incompatible suite counts; coverage ledger called an old campaign the latest. | Declared catalogue and coverage selection versus dated #211 and [#694 evidence](../ttc-milliseconds-instant/verification.md); preserve historical failures and incomplete NeoForge addon qualification. |
| D10 | Configuration scopes still said unimplemented/in review; reset and default prose was stale. | Merged #510, #539, #541, #543, #571, #595 and #620 plus current model/writers; record implementation without claiming missing original runtime or wiki gates passed. |
| D11 | Provider-locate and linked status specs called plates/beams planned. | Merged #626/#630, current provider lifecycle and bilingual guides; mark older delayed-only rules historical and preserve resource-icon qualification separately. |
| D12 | Symbol baseline still said ready-to-implement and presented original palette/color rules as current. | #573/#576, `TtcSymbols`, `RowTextColor`, and completed #658 evidence; preserve historical glyph research and current native color exceptions. |
| D13 | Millisecond scope said PR open/unreleased; hover draft, finite-value references and startup research retained old API/config/adapter claims. | #694 and release 1.2.17, #679, #477, #211 and current source; distinguish historical regression reports from current guidance. |

Other inspected surfaces included architecture, repository layout, contributor
instructions, server-owned stats, persistence, build/release matrices and loader
metadata, release and Discord guidance, development/release/writing/test-driver/
smoke/PR skills, tracker/triage instructions, hooks, Build/Tests/OpenCode/Discord
workflows, and the automated UI testing Java-receipt/cache updates. No further
behavioral correction was established in those comparisons. Historical reports,
screenshots and research keep their dated evidence; planned Vortex, CPU-bound
stats and Ponder scopes are not declared shipped by this audit.

### Delivery and completion checks

1. Review the complete prose/resource/form diff, relative links and anchors,
   EN/UK meaning, current source claims, and unchanged original acceptance gates.
2. Let the conventional documentation commit create the PR through the hook.
   Use `Refs #375`; the issue remains recurrent and open.
3. After PR creation, run `checkGuideResources` for bilingual guide navigation,
   linked images/pages and target boundaries. Check whitespace, documentation
   links and the issue-form environment choices. No runtime, image refresh,
   production code, new test infrastructure or dependency change is needed.
4. Complete current-head CI and the review-comment gate. Publish the reviewed
   matching wiki corrections and read their exact revision back. Local wiki
   drafts alone do not complete the maintained-surface correction.
5. Record the actual merged PR, wiki revision and check results in a new #375
   audit comment. Only that verified handoff completes this run; this page does
   not claim those pending delivery actions or a new Minecraft pass.

## Open-issue documentation inventory, 2026-09-26

Baseline: `cc453f7e`, with 36 open GitHub issues. Thirteen had no issue reference
in repository docs; #536 and #507 had only a neighboring-scope or evidence mention,
and #439–#442 were named without documenting their requested scope.
The following pages fill those 19 gaps one issue at a time. Plans and procedures
do not establish implementation or runtime completion.

| Issue | Added repository coverage |
| --- | --- |
| [#534](https://github.com/cTux/ae2-crafting-time/issues/534) | [Applied Journey UI investigation](../ae2-addon-integration/applied-journey.md) |
| [#533](https://github.com/cTux/ae2-crafting-time/issues/533) | [Native row color requirements](../configuration-screen/native-row-color.md) |
| [#518](https://github.com/cTux/ae2-crafting-time/issues/518) | [Data Energistics qualification](../ae2-addon-integration/data-energistics.md) |
| [#513](https://github.com/cTux/ae2-crafting-time/issues/513) | [Two-player warning mute](../configuration-screen/warning-mute-verification.md) |
| [#512](https://github.com/cTux/ae2-crafting-time/issues/512) | [Server options authority](../configuration-screen/server-options-verification.md) |
| [#511](https://github.com/cTux/ae2-crafting-time/issues/511) | [Every options switch](../configuration-screen/switch-verification.md) |
| [#471](https://github.com/cTux/ae2-crafting-time/issues/471) | [Chance-output research](../chance-output/spec.md) |
| [#450](https://github.com/cTux/ae2-crafting-time/issues/450) | [TTC symbol proposal](../ttc-symbols/spec.md) |
| [#426](https://github.com/cTux/ae2-crafting-time/issues/426) | [Scheduled interactive token](../test-driver/scheduled-interactive-token.md) |
| [#536](https://github.com/cTux/ae2-crafting-time/issues/536) | [Missing server config after edit](../configuration-screen/server-config-edit-investigation.md) |
| [#507](https://github.com/cTux/ae2-crafting-time/issues/507) | [CrazyAE2Addons fixture mismatch](../test-driver/crazyae2addons-dispatch-investigation.md) |
| [#378](https://github.com/cTux/ae2-crafting-time/issues/378) | [Test coverage audit](test-coverage.md) |
| [#374](https://github.com/cTux/ae2-crafting-time/issues/374) | [Build/warning audit](build-warnings.md) |
| [#372](https://github.com/cTux/ae2-crafting-time/issues/372) | [Implementation conformance audit](implementation-conformance.md) |
| [#439](https://github.com/cTux/ae2-crafting-time/issues/439) | [JEI++ qualification](../ae2-addon-integration/jei-plus.md) |
| [#440](https://github.com/cTux/ae2-crafting-time/issues/440) | [JECT discovery relation](../ae2-addon-integration/ject-release-relation.md) |
| [#441](https://github.com/cTux/ae2-crafting-time/issues/441) | [Crafting Optimizer coexistence](../ae2-addon-integration/crafting-optimizer.md) |
| [#442](https://github.com/cTux/ae2-crafting-time/issues/442) | [Crafting Priority coexistence](../ae2-addon-integration/crafting-priority.md) |
| [#375](https://github.com/cTux/ae2-crafting-time/issues/375) | This recurring documentation audit procedure |

The remaining 17 issues already had repository coverage:
#537 in the [optional-connection spec](../optional-connection/spec.md);
#488 in [provider locate](../provider-locate/spec.md);
#420 in [AppliedE compatibility](../appliede-planner-compatibility/spec.md);
#351 in [automated UI testing](../automated-ui-testing/spec.md);
#141 in [Vortex](../vortex/spec.md);
and #468, #467, #466, #463, #462, #461, #460, #459, #458, #457, #455,
#454 in the
[dependency release audit](../dependency-release-audit-2026-09-20.md).
Existing coverage was reused rather than duplicated. Its presence does not
certify that those scopes are complete or all older status prose is current.
