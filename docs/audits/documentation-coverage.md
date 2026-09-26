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
