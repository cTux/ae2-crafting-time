# TTC symbols implementation plan

Lifecycle and acceptance: [spec](spec.md). Architecture: [technical design](technical-design.md).

1. Add the default-On `showEmoji` client Appearance feature. Add matching English
   and Ukrainian config labels. Reuse the existing config reader/writer and screen.
   Cover defaults, old files without the key, false/true round trips, invalid values,
   copied options and reset behavior with `OptionsModelTest`/`ClientConfigFileTest`.
2. Add the covered pure-Java palette and exact translation/argument policy, then
   the shared component adapter. Cover every status in the spec, unknown keys,
   toggle Off, multiple durations, uncertainty, missing data and idempotence.
   Decorate client `TtcText` paths and raw Crafting Tree badges. Preserve numeric
   row classification and native/explicit color behavior with `RowColorPolicyTest`
   and `RowTextColorTest`. Preserve semantic translation roots used by callers.
3. Add thin client chat wrapping adapters at the verified version-specific seams
   in the design. Preserve raw log/history content, nested translation arguments,
   fallback text, click/hover actions and siblings. Refresh visible chat on option
   save. Add component tests beside `TtcTextTest`, `StatsChatServerTest` and
   `ProviderLocateTest` covering server-shaped messages through the decorator,
   all time argument positions, reason-specific warnings and two local settings.
4. Audit every `TimeEstimate.format*` display caller and the complete status-key
   list against the acceptance table. Keep rates, counts, identifiers and item names
   plain. Check decorated widths at row, total, CPU-card and optional-addon sites.
   Preserve badge and text-shadow options and existing status visibility switches.
5. Update both GuideME locales' `features/configuration.md`,
   `features/time-estimates.md` and `statuses/index.md` with the full palette,
   default-On toggle and text-only fallback. Mirror the reviewed text into existing
   English/Ukrainian GitHub wiki configuration/status pages, preserving wiki edits;
   verify published page content and links. Do not replace old screenshots or
   describe them as symbol verification. Review locale keys and placeholder parity.
6. Review the complete diff, then create the implementation PR through the normal
   conventional commit hook. Run no local tests before that PR exists. Afterward
   run `./gradlew test jacocoTestReport checkGuideResources` and the existing
   all-target compile/build checks needed for mixin registrations and API changes.
   Require 100% line/branch coverage for changed pure logic, retain component
   boundary tests, and report GitHub CI separately for the exact reviewed head.
   Do not run Minecraft smoke or claim runtime glyph/layout verification.
7. Reconcile spec status with merged implementation, exact test/CI SHA and wiki
   readback. Completion requires code, default-On config, both guide locales,
   published wiki, review and current-head required CI; visual qualification is
   explicitly outside this user-authorized run.

## Acceptance mapping

| Criterion | Change and evidence |
| --- | --- |
| A1: every duration once | Steps 2-4; tooltip, sample, accuracy, chat and addon component tests; caller audit |
| A2: every status | Step 2; full palette table and each reason/status key tested |
| A3: default-On local toggle | Steps 1/3; config persistence/reset and local chat/UI Off tests |
| A4: preserve behavior/styles | Steps 2-4; click/hover, nested components, row policy, raw-log boundary and layout source review |
| A5: supported targets/docs | Steps 3/5/6; registered adapters, all-target build, locales, GuideME resources and wiki readback |
| A6: verification limits | Steps 6/7; no smoke; explicit unverified glyph/visual appearance statement |

## Time glyph compatibility (#658)

Lifecycle and acceptance: [follow-up scope](spec.md#time-glyph-compatibility-658).
Design: [glyph evidence and boundaries](technical-design.md#time-glyph-compatibility-658).

1. Replace only `TtcSymbols.Symbol.TIME` with U+231A WATCH. Trace existing
   `TtcComponents` callers and audit literal U+23F1 assertions, driver patterns
   and current documentation examples. Preserve historical evidence. Keep all
   other symbols, component composition, option behavior and colors unchanged.
2. Update the nearest existing symbol/component regression expectations for one
   prefix, uncertainty, Off and style preservation. Update current English and
   Ukrainian GuideME and wiki examples where they show the time symbol. No new
   option, dependency or packaged font is required.
3. Prepare focused cases in the existing native scenario only where its current
   captures cannot prove G3. Use the prepared `26.1.2-neoforge` compatible profile
   with `standard-plan-controls`, `standard-status-controls` and
   `cpu-list-total-ttc`; capture rows, footer/total, tooltips and CPU cards under
   default and native uniform fonts at effective GUI scales 2 and 4. Reuse
   disposable fixtures and restore modified font/scale/options. Do not introduce
   a runner or expand into the full addon matrix.
4. Review and commit through the normal hook-created PR before running local
   checks. Run the existing shared/component tests, coverage and guide-resource
   validation, plus all-target builds for Forge/Fabric 1.20.1, NeoForge 1.21.1
   and NeoForge 26.1.2. Inspect `-Changed` smoke selection and use the focused
   leaves above; run a representative prepared `1.20.1-forge` compatible native
   plan/CPU capture for the older renderer. If a required capture is missing,
   complete that focused case rather than treating a string assertion as proof.
5. Inspect retained screenshots for recognizable glyphs, baseline, clipping and
   overlap; compare sidecars for original values and record font, effective scale,
   run timestamp and duration. Confirm Show emoji Off with existing component
   checks and a focused native toggle capture. Report current-head GitHub checks
   separately. Publish and read back changed wiki examples through the normal
   documentation workflow when applicable.
6. Mark only the #658 scope finished after merged implementation, current-head
   verification and required visual evidence exist. Keep the original #450
   verification history intact and record any third-party font-pack limits.

| Criteria | Changes and checks |
| --- | --- |
| G1–G2 | Steps 1–2/4–5: shared constant, caller/literal audit, existing symbol/component tests and Off capture |
| G3 | Steps 3/5: NeoForge 26.1.2 native screenshots across the specified surfaces, fonts and scales |
| G4 | Steps 4–6: Forge representative smoke, shared/component coverage, four-target builds, exact-head CI and merge evidence |
| G5 | Steps 1–2/5: current examples, paired GuideME locales, applicable wiki publication/readback |
