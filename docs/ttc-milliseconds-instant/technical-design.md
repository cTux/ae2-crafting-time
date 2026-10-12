# Millisecond TTC design

See the [canonical status and requirements](spec.md).

## Current behavior and affected paths

- `shared/src/main/java/com/ctux/ae2craftingtime/core/TimeEstimate.java`
  rounds estimates upward to integer seconds before formatting, adds `?` for
  unreliable estimates, converts tick durations to seconds, and separately
  formats fractional sample ticks. Milliseconds cannot be recovered after the
  current whole-second rounding.
- `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/TtcText.java`
  formats tooltip samples, normalized average/latest timings, estimates, and
  typical durations. `StatsChatServer.java` also formats estimates and samples.
  Change these together so chat and hover units agree.
- `TtcComponents.java` and core `TtcSymbols.java` handle client-local decoration.
  Add a localized status component with its own green style and medal handling;
  do not represent Instant as a numeric time and accidentally prefix a stopwatch.
- Audit all callers of `TimeEstimate`, plus `CraftingJobEstimate`, CPU resolvers,
  totals, `TtcSort`, `TtcColor`, and version-specific adapters for precision loss.
  Keep row/status precedence at the existing render boundaries.
- `OptionFeature`, `ClientConfigFile`, locale resources, configuration screens,
  and GuideME/wiki documentation own the new visibility option and explanation.

## Intended data flow

Keep profiler and persisted durations in their existing units. Derive display
milliseconds from the unrounded estimate (`amount / amountPerSecond * 1000`)
or tick duration (`ticks * 50`). Validate finite, meaningful input first.
Choose existing non-time states before testing confidence and the Instant
threshold. Apply the numeric display floor only at formatting boundaries.

Do not sum 5ms-clamped row strings to obtain totals. Preserve the existing
critical-path versus sum semantics and use sufficient precision through the
total calculation. Audit DTO and packet units before implementation: if a total
path currently transmits only whole seconds, formatting alone cannot satisfy
this requirement. Document and test any necessary precision change explicitly;
no wire or persistence migration is authorized by this planning-only change.

Raw sample durations remain numeric ms, even when very short. Signed diagnostic
errors and elapsed zero values retain their meaning. Keep rate quantities,
output normalization, compact number behavior, and confidence thresholds intact.

The supplied Discord URL is a visual reference, not a runtime dependency.
The medal rendering decision remains open in the spec. Do not fetch remote art
while rendering or silently replace the requested medal with another symbol.

## Verification boundaries

Extend `TimeEstimateTest` for units, floor, confidence, invalid input and threshold
boundaries. Extend `TtcTextTest`, `TtcComponentsTest`, and `StatsChatServerTest`
for translated units, status styles, symbols and chat/hover agreement. Exercise
native renderer tests and existing total/CPU tests for precedence and precision.
These tests cannot establish glyph appearance, clipping, or addon compatibility;
focused client evidence must cover those on each affected release target.

No optional integration should gain a required dependency. If implementation
changes an optional presentation mixin, retain `require = 0` and verify its
remapped production target plus client startup under repository policy.
