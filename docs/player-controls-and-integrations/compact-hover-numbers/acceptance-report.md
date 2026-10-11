# Compact hover acceptance, 2026-10-11

[Issue #677](https://github.com/cTux/ae2-crafting-time/issues/677) and
[PR #679](https://github.com/cTux/ae2-crafting-time/pull/679).
This report completes the H1-H6 evidence in the [implementation plan](implementation-plan.md).

## Revision and evidence

The implementation reviewed was `cbeca3fa244f987128e2f1429bfb647de4292aec`.
The earlier `958ed3cbe8e1d72877ac01937a825cee65da83ac` Forge/Fabric and
1.21.1 production artifacts remain applicable: the later commit changes only
the 1.21.1 test-driver manifest. The 26.1.2 campaign uses the later revision.
The completion commit changes documentation only.

Retained local evidence is under `build/recovered-reports` (October 9 automated
campaigns) and `build/pr679-acceptance` (October 11 manual campaigns). Each manual
target has `manual-acceptance.json`, three launch receipts, config snapshots and
reviewed screenshots. Dedicated evidence is in `build/pr679-acceptance/dedicated`.
These ignored archives contain environment details and are not release artifacts.
Some primary scenario folders were subsequently overwritten by focused CPU,
Status or compact-hover runs; retained full-suite results and isolated results
are reported separately, rather than presenting an isolated pass as a full suite.

## Acceptance results

| Gate | Evidence and outcome |
| --- | --- |
| H1: throughput formatting | Shared formatter and component tests cover rounding, suffix promotion, scientific fallback, invalid values and item/fluid/chemical/mana units. Reviewed native Plan/Status captures cover ordinary, fractional, thousand, petascale and exascale item fixtures on all four targets. PASS. |
| H2: full chat | Native Ctrl-click preserves full decimal rates and full output amounts. Dedicated Plan Stone x32 broadcasts `0.004545454545454545 items/t` and `0.09090909090909091 items/s` to both clients; Status Stone x27 broadcasts `0.004957507082152974 items/t` and `0.09915014164305949 items/s`. PASS. |
| H3: scope and cleanup | Native AE2 amounts and unrelated tooltip text remain intact. Automated lifecycle checks cover restoration on success, failure, cancellation and repeated close, including the separate 26.1.2 adapter. PASS. |
| H4: persistent option | On each target, Done saves OFF, normal process exit/relaunch retains OFF, Cancel discards a draft, reset restores ON independently of compact amounts, and another normal exit/relaunch retains ON. Original config restored, including the originally absent 1.21.1 config. PASS. |
| H5: visible row identity | On every target, Plan and Status have scrollbars and initially offscreen outputs. Both sort directions, scrolling, two distinct outputs and native Ctrl-click match the visible output ID, amount and full rates. Stone, Oak Planks and Diamond have distinct retained histories. PASS. |
| H6: connected behavior | Two native Fabric clients on a dedicated server use ordinary furnace Stone processing. Both receive identical full-rate broadcasts. A repeat click inside cooldown adds only one message. Chat OFF suppresses detail and reset notices while reset still clears history. Distinct grids retain separate histories; resetting A leaves B learned. The peer's compact option OFF changes its hover to `0.00 items/t, 0.08 items/s` but its broadcast retains `0.004166666666666667 items/t, 0.08333333333333333 items/s`. PASS. |

The dedicated production SHA-256 is
`EEBE5F2C26580790856B1A19C5B5F3D51BCA3AF4E44760C96A35B4FAE0536D88`;
driver SHA-256 is
`0F4EBB7AAFFC70A42D8BD985229BC42D90C8909854E4F67681F0D2627C995D34`.
The sealed server uses Minecraft 1.20.1, Fabric 0.19.5, AE2 15.1.0 and Java 17.
The peer adds Mod Menu 7.2.2 only to access the native settings UI.

The dedicated wrapper failed at screenshot collection because the reviewed host
captures were copied into `client-session/evidence`, rather than `client/evidence`.
This is not reported as a passing automated connected cell. Manual acceptance
was reconciled from the retained full captures, both client logs, command
receipts, sealed artifact inventories and independently passing
`verify-connection-observation.ps1` checks for both clients and server epochs.
The failure and raw transcript are retained. The server stopped cleanly through
RCON; both clients exited normally.

## Process provenance

All times below are UTC on October 11. Each row lists three distinct client PIDs
for initial configuration, OFF relaunch and ON relaunch. All three exits were 0.

| Target | PIDs | First start to final normal exit |
| --- | --- | --- |
| 1.20.1 Forge | 5092, 4764, 8976 | 13:55:57–15:03:51 |
| 1.20.1 Fabric | 4340, 10404, 2740 | 15:04–15:43:44 |
| 1.21.1 NeoForge | 3032, 9620, 4644 | 15:44:16–16:26:05 |
| 26.1.2 NeoForge | 8280, 1812, 7100 | 16:27:07–17:09:36 |
| Dedicated Fabric | server 10744; clients 7612, 8084 | wrapper 17:21:25–18:00:51; primary start 17:23:49, peer start 17:25:09, peer exit 17:59:38 |

The first dedicated setup attempt used a noninteractive client with unusable
LLVMPipe graphics. It was stopped by exact process identity and the server
cleanly shut down. The retry used the existing interactive Windows session.
The primary's first login timed out during initial loading and it rejoined
normally; the peer later rejoined to refresh its operator permission snapshot.
These setup attempts are not acceptance passes.

## Checks and limits

Four target smoke-plan, coverage, run-contract and visual-contract script checks
passed. Fourteen retained October 9 automated status receipts have phase `passed`
and exit code 0. Current implementation CI passed:
[all distributions](https://github.com/cTux/ae2-crafting-time/actions/runs/37916493839)
and [Gradle tests](https://github.com/cTux/ae2-crafting-time/actions/runs/37916493951).
Final documentation-commit CI is checked separately at the merge gate.
English and Ukrainian configuration/details/learning wiki pages explain compact
hover text and unabbreviated Ctrl-click chat.

Non-item units, invalid rates and scientific extremes use component/boundary
tests; the dedicated server establishes ordinary item behavior only. Constructed
extreme-rate fixtures establish rendering, not extreme-value profiler arithmetic
or OmniSequence compatibility. Existing unrelated tooltip clipping and profiler
overflow work remain outside this approved scope. On 26.1.2 the second Plan
identity check uses a missing-input plan while the first job supplies pending
Status rows. The disposable fixture's power and extra CPU were repaired before
acceptance; no production changes were needed.

## Timing

| Part of the smoke UI testing task | Time | Why it took that long |
| --- | ---: | --- |
| October 9 automated compatible suites | Forge 228.0s; Fabric 483.3s; NeoForge 1.21.1 193.9s; NeoForge 26.1.2 49.6s | Retained suite receipts; subsequent focused runs are separate. |
| October 9 focused checks | Forge 219.4s/542.7s/675.1s; Fabric 507.3s/528.3s; NeoForge 1.21.1 414.0s/511.4s; NeoForge 26.1.2 137.9s/104.4s | Compact/CPU/Status isolation receipts; not a fresh October 11 full-suite campaign. |
| Host build and initial staging | not measured | Existing sealed artifacts and dated receipts reused after revision comparison. |
| October 11 Forge manual campaign | 67m54s | Three launches, saved options, Plan/Status sort/scroll identity and review. |
| October 11 Fabric manual campaign | exact duration not measured | Three launches from 15:04 through 15:43:44 and acceptance; includes a 7.5s VM soft suspend/resume after VNC connection leakage. |
| October 11 NeoForge 1.21.1 manual campaign | 41m49s | Three launches, saved options and native row identity. |
| October 11 NeoForge 26.1.2 manual campaign | 42m29s | Three launches and disposable fixture repairs before acceptance. |
| First dedicated setup attempt | exact duration not measured | Recorded between 17:12 and 17:18; noninteractive graphics setup failed, followed by normal server cleanup. |
| Dedicated retry and acceptance | 39m26s | Initial loading, ordinary 1/32/1 Stone jobs, two clients, two grids, settings and collection failure. Server world loading itself reported 13.753s after unpacking/startup. |
| Evidence review and cleanup | overlaps campaigns; separate duration not measured | Captures reviewed between actions; config restoration and normal exits recorded. |
| Total | not measured as one continuous timer | October 9 retained campaigns and October 11 manual work are distinct; overlapping phases are not summed. |
