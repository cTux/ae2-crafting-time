# Collecting Data Status Spec

## Why this exists

[Discussion #64](https://github.com/cTux/ae2-crafting-time/discussions/64)
introduced a placeholder before an estimate is available, so the mod doesn't
look inactive while it is learning that craft.
[Issue #166](https://github.com/cTux/ae2-crafting-time/issues/166) names that
placeholder `No data yet`, matching the existing absence-of-statistics text.

The discussion's follow-up confirms that delayed rows must stop before adding a
normal estimate and that the placeholder uses neutral gray text. This spec uses
the current code as the source of truth where the short proposal doesn't define
an edge case.

## What the code does today

- `CraftProfiler.stats(...)` returns retained stats after the first completed
  production sample. While the first continuous production window is still
  active, completed output within that window can provide a live low-confidence
  preview without becoming a retained sample.
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
windows, the first matching state wins:

| State | Visible line |
| --- | --- |
| The running craft is waiting for its first dispatch | Existing `Waiting` status |
| The running craft is delayed | Existing `DELAYED` warning |
| No usable stats are cached | `No data yet` |
| A usable but low-confidence estimate exists | Existing estimate with `?`, such as `~12s?` |
| A reliable estimate exists | Existing estimate, such as `~12s` |

`No data yet` describes the absence of usable historical or live production
data. It is not a network loading spinner and does not promise that the current
craft will make progress.

## Requirements

1. Show the placeholder on the first rendered frame for every eligible row,
   without waiting for a server response.
2. Keep requesting stats through the existing one-second request cooldown.
3. Replace the placeholder automatically when cached stats produce an estimate.
   For a first continuous production window, completed output must make a live
   low-confidence estimate available before the whole order becomes idle.
4. Keep the existing low-confidence `?`, three-sample reliability rule, outlier
   handling, colors, sorting, totals, and `Waiting` and `DELAYED` priorities
   unchanged.
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

- A fresh output shows `No data yet` in both standard crafting tables.
- A fresh running output changes from `No data yet` to a low-confidence estimate
  after its first completed output, without waiting for the next order.
- A stored-only or missing-only plan row shows no TTC line.
- A status row with no active or pending work shows no TTC line.
- One and two valid samples show the existing estimate with `?`.
- Three valid, unfiltered samples show the estimate without `?`.
- A pending-only status row with cached waiting state shows `Waiting`, not the
  placeholder.
- A delayed running output shows `DELAYED`, not the placeholder.
- Clearing retained stats changes the row back to the placeholder.
- English and Ukrainian contain the same translation keys.
- The label does not overlap the item icon or a neighboring cell.

See [technical-design.md](technical-design.md) for the code path and
[implementation-plan.md](implementation-plan.md) for the work order.
