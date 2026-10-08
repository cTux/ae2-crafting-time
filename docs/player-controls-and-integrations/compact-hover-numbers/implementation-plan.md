# Compact hover numbers: implementation plan

Canonical status and holds: [specification](spec.md).
Implementation contract: [technical design](technical-design.md).

## Before implementation

1. Obtain approval of the final issue wording and resolve the proposed
   throughput-only scope. Mark the scope ready-to-implement only after these
   decisions and consistency review. Keep `backlog` until starting implementation
   is explicitly authorized; this planning PR does not close #677.
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
2. Own `OptionFeature`, option configuration tests and localized option text.
   Verify default on, missing-key on, persistence, Done/Cancel, both reset paths
   and independence from existing switches. Reuse generic option plumbing.
3. Own shared `TtcText`, `StatsChatServer` and their English/Ukrainian chat and
   hint templates. Change only the throughput call sites. Extend `TtcTextTest`
   and `StatsChatServerTest` with compact/on, legacy/off, full chat, both units
   of time, fractional precision and unchanged sample/confidence/accuracy cases.
   Check translation placeholder counts and types in both locales.
4. Extend the nearest existing renderer/configuration tests to demonstrate
   native and foreign tooltip lines remain intact, both native screens use the
   shared path, detailed-tooltip off remains off, and disabled chat/context/
   cooldown behavior is unchanged. No packet fixture or NBT shape should change;
   run existing relevant boundary tests to establish that fact.
5. Update the parent controls/profiling docs and the three guide topics in both
   locales using [player-documentation.md](player-documentation.md). Match wiki
   topics after reading current pages. Update screenshots only from reviewed
   evidence. Keep feature/index status honest about remaining validation.
6. Follow the repository's single conventional commit and post-commit hook PR
   workflow. Do not run local tests before the hook has created the PR. Then
   execute targeted formatter, renderer, configuration and server-chat tests;
   build all four release targets and report GitHub CI separately.

## Runtime acceptance matrix

Use the prepared-client smoke workflow on Forge 1.20.1, Fabric 1.20.1,
NeoForge 1.21.1 and NeoForge 26.1.2. Run real connected server/client chat checks
as well as a representative integrated-server case; report the exact tested
revision and fixture for each result. A constructed profile verifies rendering,
not extreme-value profiler arithmetic or OmniSequence compatibility.

For each target inspect Crafting Plan and Status with ordinary, fractional,
thousand, petascale and exascale rates. Capture tooltip and Ctrl+click chat for
the same output. Use actual item and fluid fixtures; test installed chemical/mana
keys where supported by that target without claiming absent addons were tested.
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
| H1 | Pure formatter edge cases, shared text tests and both native tooltip captures on four targets. |
| H2 | Server/client detail tests and connected chat capture with both full rates and exact long amount. |
| H3 | Renderer preservation tests, unit-specific fixtures and unchanged diagnostic assertions. |
| H4 | Generic option/config tests plus interactive save, Cancel, reset and relaunch checks. |
| H5 | Existing request boundary tests and sorted/scrolled, disabled-chat, reset and cooldown scenarios. |
| H6 | Locale parity, documentation/link review, four builds/CI and reviewed visual evidence. |

## Completion gate

Planning completion requires approved issue text, resolved scope and a consistent
spec/design/plan. Implementation completion additionally requires all H1-H6
evidence, updated GuideME/wiki, green current-head CI, and an authorized merge.
Record limitations and blockers without converting them to passes. Only then
mark this scope finished and close #677. A planning merge alone does neither.
