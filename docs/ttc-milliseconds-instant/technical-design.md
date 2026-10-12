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

## Estimate representation and compatibility

Use one shared estimate value containing a finite, nonnegative `double`
`durationMs` and a `boolean reliable`, wrapped in `Optional` for unknown values.
Keep fractional milliseconds through sums and critical-path calculations; apply
rounding and the 5ms floor only for display. Reject non-finite/negative inputs;
an overflowing aggregate is unknown, never Instant. Zero remaining work keeps
the existing completed-state presentation rather than becoming an estimate.

Replace whole-second TTC carriers end to end: `CraftingJobEstimate.remainingSeconds`,
`ProfilerBridge` total calculations, request/response DTOs, `ClientStatsCache.totalTtcSeconds`,
CPU cache/resolver entries, total rendering, sorting/color callbacks, and the row
snapshot and CPU packet codecs and all four loader adapters. Rename fields and
methods to state their millisecond unit. Sort and color using `durationMs`, never
clamped strings. Keep unrelated elapsed-tick fields and saved profiler samples
in their existing units. Convert explicitly at any legacy seconds-only diagnostic
boundary; do not silently change the unit of an existing seconds-named API.

Encode estimates as presence, then an IEEE-754 double in ms and a reliability
boolean. Validate decoded values and preserve packet entry/count/context limits.
This is a wire-breaking change: increment Forge's channel protocol and both
NeoForge payload registrar versions from their then-current versions (currently
28 and 27 respectively). On Fabric, use new versioned IDs for both the row
request/snapshot pair and CPU request/reply pair, and gate requests and replies
on peer support for those new IDs. Never send new data on old IDs or fall back
to decoding the old seconds layout as ms. Preserve optional-mod behavior;
unsupported peers get unavailable TTC, not an invented zero/Instant value.
No persisted sample schema changes are required. These are future implementation
requirements; this documentation commit changes no packet or runtime code.

### Accuracy boundary

Accuracy remains a seconds-based diagnostic, with fractional seconds retained.
At job acceptance pass `durationMs / 1000.0` to `TtcAccuracyTracker.start` as a
`double`; require a finite strictly positive prediction. Change `predictedSeconds`
in that method, `PendingJob`, and `AccuracySample`, plus
`TtcAccuracyStats.lastPredictedSeconds`, from `long` to `double`. Keep existing
actual wall/tick seconds and signed-error seconds as doubles. Do not use integer
division, ceiling, the 5ms display floor, or a formatted string in this path.
The existing ratio/error formulas continue to operate entirely in seconds.

Update every accuracy snapshot codec and loader adapter to encode/decode the
prediction as a finite positive double under the same new protocol/ID versions;
retain the existing absence/no-sample representation. Preserve the accepted-job
freeze point, successful-completion-only recording and coverage semantics.
Accuracy is runtime-only, so no saved-history migration is needed.

At tooltip/chat accuracy presentation boundaries convert predicted/actual seconds
to ms with `* 1000.0` before duration formatting, so a 0.05s prediction displays
50ms. Signed errors use ms when their absolute magnitude is below 1s, preserve
zero/sign and do not acquire Instant or the positive-duration floor. Latest-job
diagnostic durations remain numeric; Instant applies only to live TTC estimates.
Test a frozen 50ms prediction and 100ms successful completion: the sample remains,
the actual/predicted ratio is 2, and signed error is +50ms. Cover packet round
trips, display, zero/invalid predictions, cancelled jobs and existing coverage
filtering alongside tracker tests.

## Aggregate confidence

Evaluate remaining rows once using their existing `ProfileStats.reliableEstimate()`
decision. Aggregate reliability is the conjunction of reliable, usable stats for
all positive remaining rows and complete dependency traversal. Missing rows,
cycles or depth truncation make an otherwise available partial estimate unreliable.
`CraftingJobEstimate.dependencies` currently omits an edge when an input has
multiple crafted candidates. Return and retain a graph-completeness flag alongside
the dependency map: any such ambiguous choice marks it incomplete. Propagate that
flag from plan capture through remaining-job estimation and aggregate reliability;
reliable row samples alone cannot restore confidence in an ambiguous graph.
Retain the existing numeric sum versus critical-path semantics. Carry reliability
with the duration through snapshots, CPU replies and caches; do not infer it
from a selected row or from the duration. No known duration means absent estimate.

## Client-local chat presentation

`StatsChatServer.summary` must not choose Instant on the server. Replace its
preformatted estimate argument with a nested semantic translatable component:
separate reliable/unreliable TTC keys, with argument 0 containing the numeric
fallback text and argument 1 containing the unrounded `durationMs` encoded with
`Double.toString` as a locale-independent string. Numeric Java arguments do not
survive vanilla 1.20.1 component serialization as numeric types, so use the string
transport deliberately on every target.
Use `%1$s` as each key's translation/fallback so a client without the decorator
still sees readable numeric text; confidence is also present in the fallback.
Unknown estimates retain the current unknown component, not a numeric marker.

At the receiving client's `TtcComponents` boundary, recognize only these exact
keys and validate argument count and a string metadata argument of at most 32
characters. Parse that metadata with `Double.parseDouble`, catching malformed
values, and require a finite nonnegative result. Parse only this designated
machine-readable argument, never the localized fallback or other display text.
Use the key's reliability and raw ms to choose the local Instant/numeric display;
malformed components keep their fallback. Never parse translated display text.
Preserve original semantic components for settings refresh, as with existing
emoji refresh, plus sibling text, click/hover actions and broadcast scope.
Do not consult server or sender client preferences. Numeric sample details can
remain server-formatted ms; they never become Instant.

Raw sample durations remain numeric ms, even when very short. Signed diagnostic
errors and elapsed zero values retain their meaning. Keep rate quantities,
output normalization, compact number behavior, and confidence thresholds intact.

Use the plain 🥇 emoji (U+1F947) through the existing text/symbol path. The user
confirmed that no custom artwork or special rendering is required. Do not add
an SVG asset, image download, custom font, or separate rendering path.

## Verification boundaries

Extend `TimeEstimateTest` for units, floor, confidence, invalid input and threshold
boundaries. Extend `TtcTextTest`, `TtcComponentsTest`, and `StatsChatServerTest`
for translated units, status styles, symbols and chat/hover agreement. Exercise
native renderer tests and existing total/CPU tests for precedence and precision.
These tests cannot establish glyph appearance, clipping, or addon compatibility;
focused client evidence must cover those on each affected release target.

Add aggregate tests for mixed confidence, missing rows, off-critical-path low
confidence, ambiguous dependency choices, cycles/depth limits and overflow. Add codec round-trip and malformed
value tests, plus Fabric capability/version mismatch checks. Test one broadcast
rendered by clients with opposite settings and re-rendered after toggling them.
Round-trip the semantic component through each target's actual vanilla component
serializer/deserializer before rendering, particularly both 1.20.1 loaders;
direct in-memory component tests alone do not establish chat transport behavior.

No optional integration should gain a required dependency. If implementation
changes an optional presentation mixin, retain `require = 0` and verify its
remapped production target plus client startup under repository policy.

## Throughput presentation follow-up

Retain per-tick stats and payloads internally. ProfileStats converts rates with
amountPerTick / 50 and detects valid normalized sample times. Shared hover text
uses only per-second rates when ms samples are displayed, otherwise per-ms and
per-second rates. Both detail builders omit the redundant rate from timed chat
and use per-ms in the rate-only fallback. Both locales keep matching placeholders.

The changed detail shapes use `.ms` translation keys with truthful English
fallbacks for older/vanilla recipients. Legacy detail keys retain their original
tick contract for messages from older servers; these compatibility translations
are not used by current UI builders. Fabric's chat capability is `stats_chat_v3`.
No-space CPU titles and partially missing Plan rows use the numeric estimate
even when it is otherwise eligible for Instant.
