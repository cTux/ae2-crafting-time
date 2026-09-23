# Compact amounts implementation plan

Lifecycle and acceptance criteria: [specification](spec.md).

This reviewed plan settles the format and source-level approach. Implement the
prototype and finish runtime research before treating #438 as delivered. The
documentation PR must use a non-closing issue reference.

## 1. Shared composition and regression checks

Own `TtcText`, `CraftingStatusTableRendererMixin`, and their existing tests in
`shared/src/mc1201Test/java/com/ctux/ae2craftingtime/mc1201/`.

- Implement Q1's eight cases with the key's SLOT formatting and no summation.
  Cover negative-as-absent input, a fractional fluid, abbreviated large values,
  and independent values up to `Long.MAX_VALUE` (Q1, Q2).
- Replace only recognized native quantity components. Exercise unrelated lines
  before, between and after them; missing, duplicate and modified native lines;
  compact amounts disabled; and all-zero rows. Preserve original native lines on a
  failed recognition check (Q6).
- Append TTC once, copy only its RGB, and verify collecting, waiting, delayed,
  blocking, normal gradient, colors-disabled and no-status fallback cases.
  Assert that no extra stats request path was added (Q3).
- Keep native full-value tooltip components and controls unchanged, add the
  legend for available-only and detailed-tooltips-disabled rows, and replace the
  net-growth diagnostic observation with successful-composition evidence (Q5).

Use the existing test suite; do not add a testing framework. Check each new
branch through formatter and composition tests rather than screenshots alone.

## 2. Client option, badge rendering and bilingual resources

Register the client Displays switch in `OptionFeature` and reuse the existing
`FeatureOptions`, `ClientConfigFile`, `OptionsScreen` and `OptionsSession` paths.
Extend the existing option/config/session tests for a missing key defaulting on,
off/on round trips, Done, Cancel, Reset section and Reset all. Test all four
compact/TTC switch combinations and server profiling disabled: only the compact
switch controls quantity replacement and its legend (Q6). Verify that no sibling
option or server configuration changes. No new packet or custom toggle is needed.

Own `CraftingRowState`, its existing core test, both
`shared/src/{mc1201,mc2612}/java/com/ctux/ae2craftingtime/mc1201/mixin/AbstractTableRendererMixin.java`
adapters, and `shared/src/main/resources/assets/ae2craftingtime/lang/{en_us,uk_ua}.json`.

- Register the summary key and reuse the existing width-limited badge path.
  Test widths 0, 89, 90, 91 and a very wide string. Preserve the recurrent badge
  behavior, right anchor, matrix restoration, and legacy return value (Q3, Q4).
- Add the bilingual option label, identical `%s` wrapper placeholders and the two legend translations from
  the design. Validate resource keys, format placeholders and symbol order in
  both locales without adding a Ukrainian Minecraft smoke run (Q5, Q7).
- Update bilingual guide text describing crafting-status quantities, using the
  existing status/estimates pages; do not advertise the feature before it ships.

## 3. Cross-version validation

Follow repository instructions: create the implementation commit and let the
hook create the PR before local checks. Run targeted existing tests, the normal
Gradle test suite, and `scripts/build-all-versions.ps1`. Report GitHub CI separately.
Use [prepared-client smoke](../../.codex/skills/run-ae2-client-smoke/SKILL.md) and
the existing [test driver](../test-driver/spec.md). Extend its current status
scenario/fixtures for deterministic quantity combinations rather than adding a
second UI driver. Build on the host; run prepared clients sequentially in CodexVM.

| Target | Java | Required evidence |
| --- | --- | --- |
| 1.20.1 Forge | 17 | Composition, badge, tooltip and interaction checks |
| 1.20.1 Fabric | 17 | Same checks, including loader-specific fluid units |
| 1.21.1 NeoForge | 21 | Same checks on the prepared compatible AE2 version |
| 26.1.2 NeoForge | 25 | Same checks through the newer rendering API |

For every target, capture and review English screenshots for all eight quantity
combinations (empty entry may be a deterministic fixture), item/fluid large
amounts, fractional fluids, normal TTC, collecting, waiting, delayed and blocked
rows. Exercise transitions between categories, completion, scroll, all sort
modes, CPU selection, hover, details/reset and provider-locate. Include default
and a wider resource-pack font, effective GUI scales 1, 2 and Auto, with sidecars
recording actual dimensions and scale. Check supported addon keys in the
prepared compatible graph; record absence rather than inventing support evidence.
Use non-smoke font/translation checks for Ukrainian strings and placeholders.

On every target, toggle compact amounts off/on through Client > Displays, apply
with Done, reopen crafting status and verify native/compact quantities while TTC
stays enabled. Also check compact on with TTC off, both off, Cancel, both reset
actions, and the saved value after relaunch. Server profiling off must leave the
compact option effective; its label must remain usable by a non-operator (Q6).

For Q4 inspect the actual badge bounds, adjacent cell and icon, not only string
assertions. For Q5 compare native full amounts before/after and verify category
labels. For Q6 inspect mixin logs and foreign-line fixtures; a successful vanilla
launch alone does not establish compatibility with other mixins.

## Completion gates

| Criteria | Gate |
| --- | --- |
| Q1, Q2 | Exact formatter/composition tests plus four-target item/fluid captures |
| Q3 | Component RGB assertions, config changes and reviewed warning/normal captures |
| Q4 | Width boundary tests and reviewed scale/font screenshots on all targets |
| Q5 | Full tooltip comparison, bilingual resource checks and interaction smoke |
| Q6 | Option/config/session and independent-switch tests, fallback/foreign-component tests, options/relaunch and compatible-graph smoke |
| Q7 | Passing current-head CI, all builds, exact-revision four-target evidence |

Record tested commit, artifact identity, AE2/loader versions, scenario result,
screenshots and visual review in a research follow-up. Update the canonical
scope status with evidence. Do not close #438 or mark the scope finished while
its prototype, readability, or tooltip checks remain unverified.
