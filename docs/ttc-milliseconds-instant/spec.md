# Millisecond TTC and Instant status

Status: in-progress

Scope: New display requirements; implementation authorized by the user.

Issue: [#692](https://github.com/cTux/ae2-crafting-time/issues/692)

Planning: [Technical design](technical-design.md) and [implementation plan](implementation-plan.md).

Implementation: [PR #694](https://github.com/cTux/ae2-crafting-time/pull/694).

Verification: [Implementation checks, native evidence and timings](verification.md).

Remaining qualification: full NeoForge 1.21.1 addon evidence is blocked by the
known NeoEco renderer crash (#627). The later #695 scale-1 correction has its own
[verification scope](../gui-scale-one-readability/spec.md). The delivery status stays in-progress until the broader
MS-8 gate is satisfied. These limits remain explicit in the verification report.

The user confirmed that low confidence takes precedence over Instant and
approved the defaults below. Implementation and review are complete; [PR #694](https://github.com/cTux/ae2-crafting-time/pull/694)
merged on 2026-10-12.

## Display rules

Follow-up requirement: player-visible throughput must not use `/t` (or `/т`).
Where sample or average/latest times already show ms, omit the per-tick rate
and retain the per-second rate. Rate-only displays use amounts/ms and amounts/s;
convert the stored per-tick rate by dividing by 50. Keep profiling and packet
fields in their existing units. Tests cover valid, absent and invalid samples,
both locales, resource units, compact/regular hovers and full chat precision.

1. Display numeric durations below one second in milliseconds, with the `ms`
   suffix. The requested minimum is exactly `5ms`, not `~5ms` or `0ms`.
2. Replace player-visible tick durations with millisecond durations. Convert
   simulation ticks at 50ms per tick; this does not measure wall-clock latency.
3. Clamp positive numeric duration displays to a minimum of `5ms`. Do not clamp
   retained observations or intermediate estimate calculations.
4. Keep the existing confidence decision. A low-confidence estimate at or below
   5ms displays `5ms?`, never Instant.
5. A confident TTC estimate of 5ms or less displays a new custom green status,
   `🥇 Instant`. The medal is the plain 🥇 emoji (U+1F947), rendered through
   the existing text/symbol path. No custom artwork or special rendering is needed.
   Evaluate eligibility before display rounding; a value above 5ms must not
   become Instant just because its displayed value rounds down.
6. Unknown, missing, invalid, blocked, waiting, and completed states retain
   their existing meaning and precedence. Lack of data is not Instant.

Apply numeric formatting consistently to rows, totals, CPU entries, tooltips,
chat details, samples, typical durations, and supported addon displays. Instant
describes a TTC estimate, not a raw sample, signed accuracy error, or elapsed
waiting timer. Preserve zero and sign where they carry diagnostic meaning.
Existing second/minute/hour presentation remains the baseline at or above 1s;
tick-labelled sample fields specifically change to ms, including larger values.

For totals and CPU estimates, confidence is high only when every remaining row
has usable, reliable stats and the aggregate dependency calculation is complete.
Any unreliable or unknown remaining row, ambiguous dependency choice, or incomplete graph traversal makes
an available aggregate low confidence. This includes rows outside the longest
path. Preserve existing partial numeric estimates, but append `?` and never show
Instant for them. With no usable estimate, preserve the unknown state.
Remaining waiting, blocked or delayed rows also exclude Instant in CPU and
Status totals, preserving the higher-priority row state and numeric duration.

Chat recipients apply their own Instant and Show emoji settings to the same
semantic estimate. One recipient's settings must not affect another's display.

## Configuration and documentation

Provide an on/off client option for the new Instant status under the repository's
feature policy. Off uses numeric TTC with the same minimum and confidence rules.
Respect Show emoji: Off hides the medal but retains the green Instant label.
Update English and Ukrainian text, GuideME, and the GitHub wiki when implemented.

## Acceptance criteria

| ID | Required result |
| --- | --- |
| MS-1 | A 50ms or 500ms estimate uses ms rather than rounded whole seconds; 1s retains the existing seconds presentation. |
| MS-2 | A sample of 1 tick shows 50ms; 10 ticks shows 500ms; 20 ticks shows 1000ms in a formerly tick-labelled field. |
| MS-3 | Positive sample durations below 5ms display 5ms without changing stored values or calculated throughput. |
| MS-4 | Confident TTC values of 1ms and 5ms show green 🥇 Instant; 6ms does not. |
| MS-5 | Low-confidence TTC values of 1ms and 5ms show 5ms?; a 50ms low-confidence estimate preserves its question mark. |
| MS-6 | No-data and higher-priority statuses never turn into Instant. Sorting and totals use underlying durations, not labels or clamped row values. |
| MS-7 | Instant Off restores numeric TTC; Show emoji Off hides only the medal. Both locales and both guides explain these controls. |
| MS-8 | Native and supported addon surfaces agree across all four release targets; the medal renders legibly without clipping or a missing glyph. |
| MS-9 | A total or CPU estimate containing an unreliable or unknown remaining row never shows Instant, even when its known duration is at most 5ms. Fully covered reliable totals can show Instant. |
| MS-10 | Two chat recipients with opposite Instant settings see Instant and numeric time respectively; changing settings refreshes from the original semantic message without losing confidence. |
| MS-11 | Total/CPU payload round trips preserve milliseconds and confidence; incompatible payload versions cannot interpret milliseconds as seconds. |
| MS-12 | Visible throughput has no per-tick rate. Hover and chat omit that rate beside ms sample times; rate-only fallback uses correctly converted amounts/ms and amounts/s. Both locales and resource units agree. |

## Approved display defaults

- Round numeric milliseconds up to a whole ms; 999.1ms displays 1000ms.
  Determine the ms/seconds format from the unrounded value. Preserve the strict
  5ms Instant threshold and minimum display.
- Add `Instant status` to Displays, enabled by default.

## Compatibility and non-goals

Cover Forge/Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. Do not change
profiling cadence, confidence thresholds, sample retention, job scheduling,
network visibility, or count formatting. This is not a claim of 5ms server
tick resolution or guaranteed instantaneous crafting.

The implementation supersedes tick-unit presentation examples in
[profiling and diagnostics](../profiling-and-diagnostics/spec.md), the tick-text
preservation boundary in [compact hover numbers](../player-controls-and-integrations/compact-hover-numbers/spec.md),
and extends the [symbol palette](../ttc-symbols/spec.md). Those documents and both
player guides have been updated; release [1.2.17](https://github.com/cTux/ae2-crafting-time/releases/tag/release-1.2.17)
followed in [PR #707](https://github.com/cTux/ae2-crafting-time/pull/707).
The later [scale-1 correction](../gui-scale-one-readability/spec.md) and
[CPU fixture correction](../test-driver/spec.md#optional-badge-backgrounds-696)
have their own evidence; they do not close the broader MS-8 addon gate.
