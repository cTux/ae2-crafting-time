# Compact amounts implementation plan

Lifecycle and acceptance criteria: [specification](spec.md).

This reviewed plan settles the format and source-level approach. Implement the
prototype and finish runtime research before treating #438 as delivered. The
documentation PR must use a non-closing issue reference.

## 1. Shared composition and regression checks

Own the pure formatter in core `CraftingRowState` and its existing core test.
Keep `TtcText` as component conversion, and test it and
`CraftingStatusTableRendererMixin` through their existing tests in
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
Keep Minecraft-free decisions in `shared/src/main/java` with 100% line and
branch coverage; keep Minecraft component/API conversion at the tested boundary.

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

### Verification prerequisites

Before approving the runtime campaign, inspect the exact CodexVM prepared
`launch.json` for each target and its compatible loader, Java executable and
libraries. Verify the guest's Java 17/21/25 installations and the disposable
fixture source/marker path. The host JDKs, VMX and launch scripts exist; the
2026-09-23 investigation found the VM stopped, so guest readiness is still
unverified. Missing native installations must be provisioned and checked before
the campaign; the runner does not install or substitute them.

Add these bounded checkpoints to the existing `standard-status-controls` leaf,
`StandardCraftFixture`, and matching 26.1.2 adapters. They are prerequisite
implementation work, not capabilities already supplied by the current driver:

- Q1/Q2: deterministic native item/fluid rows for the eight presence masks,
  large counts and fractional fluids, with per-row raw amounts recorded beside
  the final rendered description and native full tooltip. Use fixture-controlled
  native menu entries for otherwise transient/empty cases, clearly identify
  these synthetic quantity cases, and separately exercise real craft transitions
  through the normal server/menu synchronization. Never seed formatted output,
  replace production callbacks or fabricate observation snapshots.
- Q3/Q5: reuse the existing waiting, running, delayed and blocking leaves and
  their real fixture state. Assert summary RGB against the actual appended
  status component; retain native tooltips and existing interaction checkpoints.
- Q6: use real Options controls for all four compact/TTC combinations, Done,
  Cancel and both resets. Check server profiling off and a non-operator client
  separately. Save compact off, exit cleanly, then relaunch the same disposable
  client config and verify off before restoring on. Keep that config outside
  pristine per-case fixture resets; record its path/hash and both launch results.
- Q4: stage a test-only resource pack in the disposable client that maps the
  default font to Minecraft's built-in uniform font. Await resource reload and
  prove the representative quantity string has a greater measured `Font.width`
  than under the default font; otherwise stop and repair the fixture. Capture
  both fonts at requested GUI scales 1, 2 and Auto, recording effective scale,
  dimensions and final badge/icon bounds. Restore the original font/scale after
  the case. Keep the pack out of production resources and dependency profiles.

Update the existing driver checkpoint/result expectations and
`ui-smoke-groups.json` entries to require these captures. Register any new helper
paths in `ui-smoke-impact.json` so changed-mode selection includes the leaf.
The [driver spec](../test-driver/spec.md#compact-status-amounts-extension) and
[design](../test-driver/technical-design.md#compact-status-amounts-extension)
keep this extension separate from their shipped baseline.

After the PR exists, run plan-only selection and cheap deterministic checks,
then prove the extended leaf on 1.21.1 NeoForge before the four-target campaign.
Budget at least two sequential launches per target for save/relaunch proof;
record actual cold-start cost and a bounded progress timeout before expanding.
Do not turn an unavailable fixture or unreadable capture into a skipped gate.

### Execution

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
