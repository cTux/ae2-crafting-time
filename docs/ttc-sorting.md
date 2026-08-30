# Sorting Crafting Rows By TTC

Date: 2026-06-21

## Goal

Add sorting inside the AE2 Crafting Plan and Crafting Status screens by the
visible `TTC` value.

Sorting helps players bring the slowest predicted steps together when looking
for crafting bottlenecks; it does not diagnose the cause by itself.

## Does It Fit?

Yes, but `TTC` should not be added to AE2's existing terminal `Sort By`
setting.

The screenshot tooltip (`Sort By` / `Number of Items`) is AE2 terminal sorting.
In AE2 1.20.1 build `5565729`, that setting is backed by the closed enum
`appeng.api.config.SortOrder`, which only has:

```text
NAME
AMOUNT
MOD
```

The craft-confirm screen does not use that terminal sort source. Local bytecode
shows `CraftConfirmScreen.drawFG(...)` renders the plan directly:

```text
menu.getPlan().getEntries()
CraftConfirmTableRenderer.render(..., entries, scrollbar.getCurrentScroll())
```

`CraftingPlanSummary.fromJob(...)` builds the entries, calls
`Collections.sort(...)`, and `CraftingPlanSummaryEntry.compareTo(...)` uses AE2's
private comparator. The mod should not replace that server-side summary order.

The Crafting Status screen receives its row list through
`CraftingCPUScreen.postUpdate(...)`. Its client mixin replaces only the list
used to build the displayed `CraftingStatus`; it does not change the server job.

## Approach

Keep TTC sorting client-only and sort a copy immediately before rendering or
storing each screen's row list.

1. Add a small client-only sort mode state to each supported crafting screen.
2. Intercept the Crafting Plan render list and the Crafting Status update list.
3. If TTC sorting is off, return the original list.
4. If TTC sorting is on, copy the list and sort the copy by estimated seconds.
5. Use the same calculations as the existing visible `TTC` lines:

```text
planAmount = entry.craftAmount
statusAmount = entry.activeAmount + entry.pendingAmount
amount = planAmount or statusAmount for the current screen
normalizedAmount = AeKeyAmounts.normalize(entry.what, amount)
seconds = TimeEstimate.seconds(normalizedAmount, stats)
```

6. Render entries with known TTC first.
7. Keep entries with no stats after known entries, preserving their original
relative order.
8. Tie-break equal TTC values with AE2's existing `Comparable` order.

This avoids mutating AE2's `CraftingPlanSummary` or server job, avoids new
packets, and keeps the existing scrollbar behavior.

## UI Scope

The useful minimum is:

```text
AE2 order -> TTC shortest first -> TTC longest first
```

Both the Crafting Plan and Crafting Status screens start in `TTC longest first`
mode. The mode is local to the open screen and is not saved as a config value.

Use one local AE2-styled `TtcSortButton` on each screen. Do not extend AE2's
`Settings.SORT_BY`; enum extension is brittle and would affect terminal screens
outside this feature.

## Missing Stats

TTC depends on the client stats cache. When stats are missing:

- request stats using the existing request path
- leave those rows after rows with known TTC
- keep their original relative order
- do not invent a default TTC

After stats arrive, the next plan render or status update can reorder the copied
list.

## Interaction With Colored TTC

The current colored TTC implementation already scans the full list in
`AbstractTableRendererMixin`. Sorting before each table receives its entries
means that color calculation receives the displayed order. The color range still
uses min/max seconds across the current list, so no extra color code is needed.

## Tests

Smallest useful checks:

- shared comparator/helper test: known TTC rows sort shortest-first
- shared comparator/helper test: unknown TTC rows stay after known rows
- shared comparator/helper test: equal TTC falls back to AE2 natural order
- version test: both screen mixins are in the `client` section of
  `ae2craftingtime.mixins.json`

No server test is needed because this is display-only.

## Chosen Approach

Keep the sort mode local to each crafting screen and sort a copied list at its
existing client boundary. Skip enum patching, server-side sorting, new packets,
config files, and terminal-wide settings.
