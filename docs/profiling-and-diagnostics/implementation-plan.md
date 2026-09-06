# Normalized sample details: implementation plan

Issue: [#114](https://github.com/cTux/ae2-crafting-time/issues/114).
Status: documentation only; the steps below have not been implemented or run.
Read the [specification](spec.md#planned-normalized-sample-details-114) and
[technical design](technical-design.md#planned-design-for-114-per-unit-sample-presentation)
first. This plan extends the existing profiling feature directory.

## Fixed decisions

- `9 items / 90 ticks` becomes `1 item / 10 ticks`, still one observation.
- Preserve raw collection, persistence, weighting, filtering, confidence, TTC,
  and stall calculations. Do not create samples per inserted item or execution.
- Use ticks with up to three decimal places and the documented small-value rule.
- Normalize compact average/latest details too; distinguish their arithmetic
  average from the existing weighted rate.
- Implement through existing core and shared text seams on all four targets.

## 1. Add derived values without changing collected evidence

Ownership:

- `shared/src/main/java/com/ctux/ae2craftingtime/core/ProfileStats.java`:
  implement the three OptionalDouble methods in the design.
- `shared/src/main/java/com/ctux/ae2craftingtime/core/TimeEstimate.java`:
  add the numeric sample-tick formatter with the stated validation and rounding.
- `shared/src/test/java/com/ctux/ae2craftingtime/core/`: add focused tests for
  the derived methods and extend `TimeEstimateTest.java` for formatting.

Cover A1–A10 and A12 with deterministic raw pairs. Include unequal arrays,
invalid index, zero/negative amounts or durations, absent arrays, NaN/infinite
formatter input, 0.001 exactly, 1/16 -> 0.063, and long-range positive inputs.
Verify source lists and raw stats remain unchanged after reading derived values.

Use existing `CraftProfilerTest` fixtures for serial and parallel windows,
retention, preview, and saved-history round trips. Add only an uncovered assertion
needed to show that the normalized view preserves sample count and original
`amountPerTick`, `amountPerSecond`, confidence, and `TimeEstimate.seconds`.
Do not replace their expected raw windows with one-item records.

Exit: pure calculations match all table values; no collector or DTO field changes.

## 2. Update both actual details paths and translations

Ownership under `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/`:

- `TtcText.java`: normalized sample lines, explanatory label, compact summaries,
  singular item units, and missing-detail behavior.
- `StatsChatServer.java`: server-side normalized compact details assembled from
  translatable components. Preserve request handling and broadcast/reset rules.
- `shared/src/main/resources/assets/ae2craftingtime/lang/en_us.json` and
  `uk_ua.json`: per-unit labels, singular item, explanation, rate-only fallback,
  and matching arguments.
- `shared/src/mc1201Test/java/com/ctux/ae2craftingtime/mc1201/TtcTextTest.java`:
  assert localized complete values, sample counts, explanation, and fallback.
  Add equivalent server component assertions in this existing test source set;
  expose `details` package-private if needed rather than booting a server for a
  formatting assertion.

Assert client and server values agree for A2, A4, A5, A9, A10, and A12. Tests must
load the actual translation resources: checking only a translation key cannot
detect a broken placeholder or `1 items`. Verify unit labels in both languages
and unchanged low-confidence/used-samples suffixes. Server classes must not
acquire client-only imports. Keep sample details bounded to the retained list.

Exit: tooltips and real Ctrl-click server messages agree; rates and TTC remain
unchanged. No new sample UI is added to integrations that currently show TTC only.

## 3. Commit, then run targeted checks and build the target matrix

Follow the repository workflow: one implementation branch, setup-git once per
clone, one conventional commit, and the hook-created PR before local tests.
The commit includes slices 1 and 2; they are work order, not separate commits.
Follow the development skill's current required verification at execution time.

Minimum focused checks after the PR exists:

```powershell
.\gradlew.bat :shared:test
.\gradlew.bat :mc_1_20_1_forge:test --tests '*TtcTextTest' --tests '*StatsChatServerTest'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
git diff --check
```

Use `StatsChatServerTest` as the new test class name for slice 2. Verify the
translation JSON key sets and format argument counts. Check that packaged
resources match across these exact `scripts/release-matrix.json` rows:

| Target | Required proof |
| --- | --- |
| 1.20.1 Forge | Shared tests, standard details, server chat, real parallel-machine reproduction |
| 1.20.1 Fabric | Build and standard details/chat; no Forge-only machine dependency |
| 1.21.1 NeoForge | Build and standard details/chat; supported optional CPU details |
| 26.1.2 NeoForge | Build and standard details/chat; no pre-26 UI adapter introduced |

No save or packet layout change is expected. Reuse existing round-trip checks;
old raw samples must survive load/save unchanged. Report GitHub CI separately
from local tests and do not call pending CI passed.

## 4. Observe the feature in prepared clients

Use the prepared-client smoke skill and its existing test driver. Add only
missing observation/assertion coverage through the driver skill. Record the
actual mod versions, raw profiler pairs, displayed text, sample count, rate,
TTC, and reviewed screenshots. Do not insert synthetic profiler history as a
substitute for the furnace/factory reproduction.

1. On 1.20.1 Forge, reset history and request one-item-output processing in a
   regular furnace, then a larger order with queued input. Compare each retained
   `(amount, ticks)` with its visible quotient. Do not require 100 samples from
   100 requested items: a continuous busy window remains one observation.
2. Reproduce the reported bulk return with a supported installed Mekanism
   factory. Record its actual slot configuration. For an Unobtainium furnace,
   verify its installed mod/version and actual factory mode before citing its
   capacity; use it as an additional reproduction when available. Tests A2/A3
   cover both eight and nine without assuming either machine configuration.
3. Inspect standard plan/status tooltips and Ctrl-click chat. Capture English
   and Ukrainian at supported GUI scales. Use isolated test clients/worlds for
   the public chat action. Check clipping with the maximum retained list.
4. Exercise partial output before window closure, reset, save/reload, and
   cancellation. Verify the existing low-confidence preview and no synthetic
   retained observations. Compare post-reload raw evidence and visible values.
5. On the other three targets, prove standard tooltips and actual server details
   through the prepared clients. Exercise an already supported optional CPU
   and Crafting Tree details where available. Confirm TTC-only optional screens
   and missing optional mods retain their existing behavior.

Exact real machine times depend on power, transport, and server ticks. Runtime
assertions compare displayed values to the captured raw pairs, while unit tests
prove the fixed numerical examples. A missing parallel-machine reproduction is
an explicit incomplete gate, not a reason to claim full coverage from screenshots
of a regular furnace.

## Traceability and completion

| Requirements | Implementation | Evidence |
| --- | --- | --- |
| N1, N2, N6 | Derived ratios, formatter, units | A1–A10, rounding boundaries, localized UI |
| N3, N4 | Raw data and estimator unchanged | A2, A8, A10, A11; profiler/rate/TTC comparison |
| N5 | Client and server details | Both component tests and actual Ctrl-click chat |
| N7 | Shared sources and resources | Four builds, four clients, old-save round trip |
| N8 | Optional derived values and omission | A12, fallback text, no NaN/infinity |

Implementation is complete only when the numerical, localization, persistence,
matrix, and runtime gates above have evidence, the final diff contains no
unplanned collector/protocol change, and CI status is reported accurately.
Update this status and the linked issue only after implementation is verified.
This planning PR does not close the bug or claim those gates have passed.
