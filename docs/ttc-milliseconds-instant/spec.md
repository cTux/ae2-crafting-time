# Millisecond TTC and Instant status

Status: draft

Scope: New display requirements; implementation is on hold (`backlog`).

Issue: [#692](https://github.com/cTux/ae2-crafting-time/issues/692)

Planning: [Technical design](technical-design.md) and [implementation plan](implementation-plan.md).

The user confirmed that low confidence takes precedence over Instant. This
draft records the requirements; the remaining presentation decisions below
must be settled before implementation is marked ready.

## Display rules

1. Display numeric durations below one second in milliseconds, with the `ms`
   suffix. The requested minimum is exactly `5ms`, not `~5ms` or `0ms`.
2. Replace player-visible tick durations with millisecond durations. Convert
   simulation ticks at 50ms per tick; this does not measure wall-clock latency.
3. Clamp positive numeric duration displays to a minimum of `5ms`. Do not clamp
   retained observations or intermediate estimate calculations.
4. Keep the existing confidence decision. A low-confidence estimate at or below
   5ms displays `5ms?`, never Instant.
5. A confident TTC estimate of 5ms or less displays a new custom green status,
   `🥇Instant`. Use the [user's medal reference](https://discord.com/assets/08b871e132a77f8b.svg).
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
| MS-4 | Confident TTC values of 1ms and 5ms show green 🥇Instant; 6ms does not. |
| MS-5 | Low-confidence TTC values of 1ms and 5ms show 5ms?; a 50ms low-confidence estimate preserves its question mark. |
| MS-6 | No-data and higher-priority statuses never turn into Instant. Sorting and totals use underlying durations, not labels or clamped row values. |
| MS-7 | Instant Off restores numeric TTC; Show emoji Off hides only the medal. Both locales and both guides explain these controls. |
| MS-8 | Native and supported addon surfaces agree across all four release targets; the medal renders legibly without clipping or a missing glyph. |

## Decisions still needed before implementation

- Choose rounding for fractional ms and the 999.xms boundary, while preserving
  the strict 5ms Instant threshold and the minimum display.
- Select the Instant option's default and its configuration label.
- Verify the medal's rendering path and settle whether the linked artwork or a
  font glyph is required. The existing single-code-point policy is not proof
  that this supplementary-plane medal renders correctly on all targets.

## Compatibility and non-goals

Cover Forge/Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. Do not change
profiling cadence, confidence thresholds, sample retention, job scheduling,
network visibility, or rate/count formatting. This is not a claim of 5ms server
tick resolution or guaranteed instantaneous crafting.

The future implementation supersedes tick-unit presentation examples in
[profiling and diagnostics](../profiling-and-diagnostics/spec.md), the tick-text
preservation boundary in [compact hover numbers](../player-controls-and-integrations/compact-hover-numbers/spec.md),
and extends the [symbol palette](../ttc-symbols/spec.md). Those documents still
describe the existing baseline until this feature is implemented.
