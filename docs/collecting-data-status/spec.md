# Collecting Data Status Spec

## Why this exists

[Discussion #64](https://github.com/cTux/ae2-crafting-time/discussions/64)
asks the crafting windows to show `TTC: Collecting data` before an estimate is
available. Right now a new player sees no TTC line at all, so the mod can look
inactive while it is learning that craft.

The discussion has no comments or follow-up decisions as of August 30, 2026.
This spec uses the current code as the source of truth where the short proposal
doesn't define an edge case.

## What the code does today

- `CraftProfiler.stats(...)` returns stats after the first completed production
  sample.
- Three clean samples make an estimate reliable. One or two samples still show
  an estimate, but `TimeEstimate.format(...)` adds `?` to mark low confidence.
- The Crafting Plan and Crafting Status table mixins request stats when the
  client cache has no entry, then append no TTC line.
- The server omits unknown outputs from its response. `ClientStatsCache`
  removes the requested key before applying that response, so an output with no
  samples stays in the same cache-miss state.
- Only rows with work that AE2 plans to craft are eligible. Stored-only and
  missing-only rows do not get a TTC line.

The three-sample rule therefore must not gate this feature. The real missing
state is zero usable samples. Once the first valid sample arrives, the existing
low-confidence estimate is more useful than the placeholder.

## Player-facing behavior

For each eligible row in the standard AE2 Crafting Plan and Crafting Status
windows:

| State | Visible line |
| --- | --- |
| No usable stats are cached | `TTC: Collecting data` |
| A usable but low-confidence estimate exists | Existing estimate with `?`, such as `TTC: ~12s?` |
| A reliable estimate exists | Existing estimate, such as `TTC: ~12s` |
| The running craft is delayed | Existing `TTC: DELAYED` warning |

`Collecting data` describes the absence of usable historical data. It is not a
network loading spinner and does not promise that the current craft will create
a sample. The line can remain visible for an output that never completes a
profiled production window.

## Requirements

1. Show the placeholder on the first rendered frame for every eligible row,
   without waiting for a server response.
2. Keep requesting stats through the existing one-second request cooldown.
3. Replace the placeholder automatically when cached stats produce an estimate.
4. Keep the existing low-confidence `?`, three-sample reliability rule, outlier
   handling, colors, sorting, totals, and delayed warning unchanged.
5. Return to the placeholder after the player clears that output's retained
   stats.
6. Use translatable text and add matching English and Ukrainian keys.
7. Keep the placeholder neutral. It must not receive a fast-to-slow TTC color.
8. Keep the existing TTC badge background and bold row treatment.
9. Fit the text inside AE2's fixed-width table cell without covering the next
   cell or the item icon at every supported UI scale. Keep translations short
   enough for the same space.
10. Work on all four supported rows: 1.20.1 Forge, 1.20.1 Fabric, 1.21.1
    NeoForge, and 26.1.2 NeoForge.

## Not included

- Changing how many samples make an estimate reliable.
- Hiding the useful one- and two-sample estimate behind the placeholder.
- Adding packet fields, request states, timers, animation, or persistence.
- Changing total TTC behavior. Totals continue to include known row estimates
  and omit unknown rows.
- Changing AE2: Crafting Tree or ME Requester. They are separate optional UI
  surfaces, not the standard AE2 crafting windows named by the discussion.
- Treating the placeholder as proof that server-side profiling is enabled.

## Acceptance checks

- A fresh output shows `TTC: Collecting data` in both standard crafting tables.
- A stored-only or missing-only plan row shows no TTC line.
- A status row with no active or pending work shows no TTC line.
- One and two valid samples show the existing estimate with `?`.
- Three valid, unfiltered samples show the estimate without `?`.
- A delayed running output shows `TTC: DELAYED`, not the placeholder.
- Clearing retained stats changes the row back to the placeholder.
- English and Ukrainian contain the same translation keys.
- The label does not overlap the item icon or a neighboring cell.

See [technical-design.md](technical-design.md) for the code path and
[implementation-plan.md](implementation-plan.md) for the work order.
