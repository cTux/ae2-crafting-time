# AELIS TTC implementation verification

October 24, 2026. [PR #755](https://github.com/cTux/ae2-crafting-time/pull/755),
[#756](https://github.com/cTux/ae2-crafting-time/issues/756).

## Result

The focused enabled-AELIS scenario passed all 12 assertions on executable source
`8a9287db488f9354dbda5140efd0d89e45c56f6c`. All nine full captures were manually
reviewed. Later repository changes only record verification and documentation.
This qualifies the custom material-table display on the tested beta, not the
whole addon graph or cyclic planner execution.

## Runtime and behavior

Minecraft 1.20.1, Forge 47.4.23, AE2 15.4.10, GuideME 20.1.15, Applied Enhancements
1.1.4-forge beta (CurseForge 1665696/9301183). Beta SHA-512 and initial native
coexistence evidence are in the [earlier baseline](verification.md).
The isolated CodexVM reused Java 17.0.20.101 and a disposable offline world.
Production and matching development-driver JARs were built on the host.
The runner retained exact JAR hashes, loaded-mod receipts, selection, PID and exit
status. The selected optional adapter was `aelis-plan-114`.

Automatic planning was enabled for this scenario and restored afterward.
English, effective GUI scale 4, framebuffer 2558x1093, logical viewport 640x274.
The wide AELIS panel is 430x262 at x105/y6. Narrow fallback explicitly calls the
screen resize boundary with logical width 381; it is not a physical-window test.
Both switches off restore the upstream 360-wide table. Plan totals stay enabled
on the narrow fallback. The tooltip includes TTC for a crafted item.

A real enabled-AELIS plan uses seeded server-owned stone/smooth-stone history and
shows about 5s + 2s, with about 7s total. Stored cobblestone shows an em dash.
The display-boundary fixture then adds an unknown-history dirt row, an exact
quantity above Long.MAX_VALUE, a missing/cyclic glass row and stored filler rows.
These are synthetic client display rows; they do not prove cyclic planner
execution. Unknown history stays neutral, unsupported quantities show unknown,
and partial totals remain stable through the Cyclic filter. Scrolling, tree view,
option-off, native AE2 view and fixture restoration all passed. The expanded
summary and TTC cells have readable foregrounds; names and amounts remain bounded.
The unchanged upstream tutorial toast overlaps part of the screen title in some
captures, outside the TTC table and total.

Remapped startup reports three row-return hooks, one summary hook, three tooltip
hooks, one layout fit hook and one columns hook. Every optional presentation
injection has require=0; both screen and layout hook guards must pass before
activation. Contract fixtures reject missing members, absent addons, wrong targets
and dedicated servers. No dependency requirement, packet or save format changed.

## Automated checks

`./gradlew.bat test jacocoTestReport` passed on all targets in 1m39s:

| Module | Tests | Skipped | Failures |
| --- | ---: | ---: | ---: |
| Shared | 462 | 0 | 0 |
| Forge 1.20.1 | 391 | 5 | 0 |
| Fabric 1.20.1 | 220 | 5 | 0 |
| NeoForge 1.21.1 | 225 | 5 | 0 |
| NeoForge 26.1.2 | 218 | 7 | 0 |

The configured shared JaCoCo metric has 3032 covered lines and 2236 covered
branches, zero missed (100%). Minecraft-facing adapters remain outside that
metric; contract/hook boundary tests and the native UI scenario cover their
integration. This is not a claim of 100% runtime adapter coverage.
GitHub Gradle tests and Build all mod JARs passed on executable source 8a9287db.

PowerShell checks passed: test-prepare-ui-smoke-adapters, test-ui-smoke-plan,
test-ui-smoke-coverage, validate-optional-integrations, test-run-ui-smoke-wrapper,
and test-run-ui-smoke-codexvm. The unchanged suite-preparation test still expects
nine standard AE2 leaves while the inventory has eleven; that separate test defect
is tracked in [#757](https://github.com/cTux/ae2-crafting-time/issues/757).
The change-impact plan was inspected. Its broad shared-file routing selects other
addon graphs; those were not run. Verification here is explicitly focused.

## Evidence and retries

Paths are relative to this worktree. Retain the ignored evidence before removing
it. AELIS final campaign: `build/ui-smoke/campaigns/20261024T141843593Z/compatible`.
Archive: `build/applied-enhancements/implementation-archive/20261024T142229273Z-87fd0674`.
Manual receipt: `build/applied-enhancements/aelis-implementation-visual-review.json`.
Client PID 5320 exited with code 0 before the next client began.

The automatic visual gate remains REVIEW_REQUIRED because this environment has
no qualified baseline. Semantic, archive and cleanup results passed; manual image
review passed. No gate was rewritten to manufacture an automatic visual pass.

Retained attempts:

- 135118045Z: guard expected one draw return; published beta has three. Corrected.
- 135643275Z: tooltip wrapper was applied after the hook guard. Replaced with a
  standard argument injection; exact client stopped and archived.
- 140000609Z: tooltip observation missed the custom screen and hovered a stored
  row. Corrected the development driver; exact client stopped and archived.
- 140524160Z: all 12 checks passed. Manual review identified light foregrounds
  needing improved contrast on the addon background.
- 141130870Z: the contrast helper flattened the total translation key and the
  semantic driver waited. Preserved component semantics; exact client stopped
  and archived. This was not a production crash.
- 141843593Z: final executable source passed all 12 checks and all nine captures.

The original CI failure in the shared test fixture used version-specific resize
and player APIs. The existing DriverPlatform seam now owns each version's resize
call; root tests and both CI jobs passed afterward. Logs retain offline profile,
audio-device, optional missing-class, stale fixture-recipe and slow-tick warnings.

## Limits

No full modpack/addon-graph campaign, real cyclic craft execution, planner failure
or replan transition, reconnect scenario, every search/filter combination, or
custom AELIS fluid-row runtime was tested. Fluid/large-quantity arithmetic,
identity and unsupported-API boundaries have deterministic tests. Separate row-only
and total-only configurations and every badge/color combination lack dedicated
AELIS captures. Craft Tree TTC, new sort/details/reset controls and extra rate
columns were intentionally outside this feature. No merge or release occurred.

## Addon-absent regression

Final-source base-only standard-plan-controls passed all nine assertions, including
sorting, missing-first, tooltip, details, reset, total TTC, layout and item resolution.
All seven captures were manually reviewed: the missing ingredient stays first,
tooltips fit, and resetting stone history reduces the total from about 7s to 2s.
The native panel remains 238x206 at x201/y34, scale 4, logical viewport 640x274.
Client PID 5340 exited with code 0 before completion.
Campaign: build/ui-smoke/campaigns/20261024T142237764Z/compatible.
Archive: build/applied-enhancements/implementation-archive/20261024T142632478Z-ed52c3a4.
Manual receipt: build/applied-enhancements/native-absent-visual-review.json.
The automatic visual gate is REVIEW_REQUIRED; semantic, archive and cleanup pass.

## Timing

The implementation smoke interval starts at 13:51:18.098 UTC and ends with the
final archive at 14:26:32.478 UTC. The earlier beta coexistence campaign has its
own separate timing report. Campaign costs below already include host preparation,
guest staging, Minecraft loading, world generation, assertions, capture and cleanup.
No phase is counted twice. VM reuse and image review were not timed separately.

| Part of the smoke UI testing task | Time | Why it took that long |
| --- | ---: | --- |
| First guard failure | 3m47.05s | Beta has three draw return points |
| Tooltip injection diagnostic | 2m56.47s | Late wrapper prevented guard observation; stopped exact client |
| Tooltip observer diagnostic | 5m02.35s | Driver watched the wrong screen/row; stopped exact client |
| First complete AELIS pass | 3m35.62s | Native loading, fixture and all 12 assertions |
| Contrast/semantic diagnostic | 5m02.75s | Flattened translation key stalled driver; stopped exact client |
| Final AELIS pass | 3m45.12s | Host/staging plus native loading and nine captures |
| Final addon-absent native pass | 234.08s | Native loading, disposable fixture and seven captures |
| Corrections, builds, review and dispatch gaps | included in total | Work between campaign receipts; overlapping review is not additive |
| Host all-target tests | 1m39s, inside interval | Shared and four target test/coverage tasks |
| Isolated VM setup, staging and loading subphases | not measured | Combined within campaign boundaries above |
| Manual review | not measured | All 16 final images reviewed; overlaps campaign and documentation work |
| Total measured implementation smoke interval | 35m14.38s | Includes all failed attempts and both final successful campaigns |
