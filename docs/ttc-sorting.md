# Craft Plan TTC Sorting Research

Date: 2026-06-21

## Request

Add sorting inside the AE2 crafting plan by the visible `TTC` value.

Sorting helps players bring the slowest predicted steps together when looking
for crafting bottlenecks; it does not diagnose the cause by itself.

## Feasibility

This is possible, but not by adding `TTC` to AE2's existing terminal `Sort By`
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

## Recommended Design

Keep TTC sorting client-only and sort a copy immediately before rendering the
craft plan table.

1. Add a small client-only sort mode state for `CraftConfirmScreen`.
2. Intercept the `entries` argument passed to `CraftConfirmTableRenderer.render`.
3. If TTC sorting is off, return the original list.
4. If TTC sorting is on, copy the list and sort the copy by estimated seconds.
5. Use the same calculation as the existing visible `TTC` line:

```text
normalizedAmount = AeKeyAmounts.normalize(entry.what, entry.craftAmount)
seconds = TimeEstimate.seconds(normalizedAmount, stats)
```

6. Render entries with known TTC first.
7. Keep entries with no stats after known entries, preserving their original
relative order.
8. Tie-break equal TTC values with AE2's existing `Comparable` order.

This avoids mutating AE2's `CraftingPlanSummary`, avoids server packets, and
keeps the existing scrollbar behavior.

## UI Scope

Smallest useful UI:

```text
AE2 order -> TTC shortest first -> TTC longest first
```

Use one local button on the craft-confirm screen. Do not extend AE2's
`Settings.SORT_BY`; enum extension is brittle and would affect terminal screens
outside this feature.

If placing a new button in AE2's styled widget container is awkward, the fallback
is a tiny normal Minecraft `Button` added by a `CraftConfirmScreen` mixin. That is
uglier than reusing AE2 styling, but lower risk than patching terminal settings.

## Missing Stats

TTC depends on the client stats cache. When stats are missing:

- request stats using the existing request path
- leave those rows after rows with known TTC
- keep their original relative order
- do not invent a default TTC

After stats arrive, the next render can reorder the copied list.

## Interaction With Colored TTC

The current colored TTC implementation already scans the full list in
`AbstractTableRendererMixin`. Sorting before `CraftConfirmTableRenderer.render`
means that color calculation receives the displayed order. The color range still
uses min/max seconds across the current plan list, so no extra color code is
needed.

## Tests

Smallest useful checks:

- shared comparator/helper test: known TTC rows sort shortest-first
- shared comparator/helper test: unknown TTC rows stay after known rows
- shared comparator/helper test: equal TTC falls back to AE2 natural order
- version test: new craft-confirm screen mixin is in the `client` section of
  `ae2craftingtime.mixins.json`

No server test is needed because this is display-only.

## Recommendation

Implement a local craft-plan sort mode and sort a copied list at render time.
Skip enum patching, server-side sorting, packets, config files, and terminal-wide
settings.
