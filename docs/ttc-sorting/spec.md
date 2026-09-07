# TTC Sorting Specification

Issue: [#318](https://github.com/cTux/ae2-crafting-time/issues/318)

## Goal

Keep every missing item at the start of the Crafting Plan so shortages stay
visible, then apply the selected TTC sorting rule within the missing and
non-missing groups. TTC longest first remains the default mode.

## Player behavior

- A row is missing when its displayed missing amount is greater than zero.
- Missing rows always appear before rows with no missing amount.
- In **TTC: longest first**, rows with known TTC sort from longest to shortest
  inside each group.
- In **TTC: shortest first**, rows with known TTC sort from shortest to longest
  inside each group.
- In **AE2 order**, each group preserves AE2's original relative order.
- In either TTC mode, rows without TTC data remain after rows with known TTC
  inside their own group and preserve their relative order.
- Equal TTC values continue to use AE2's natural row order.
- The screen opens in **TTC: longest first**. The mode remains local to the open
  screen and continues to cycle through AE2 order, shortest first, and longest
  first.
- Newly received TTC data may reorder a row inside its group, but never move a
  non-missing row ahead of a missing row.

## Compatibility

- Apply the same Crafting Plan behavior to all four targets in
  `scripts/release-matrix.json`.
- Keep the sort client-only and use the plan data AE2 already sends.
- Keep rendering, scrolling, hover, TTC details, and TTC reset clicks aligned
  with the displayed row order.

## Not included

- Changing Crafting Status row ordering.
- Changing AE2's server-side crafting plan, job execution, or terminal sorting.
- New packets, persisted settings, configuration options, or translations.
- A new sort mode or toolbar control.
- Assigning TTC to rows whose statistics are unavailable.

## Acceptance criteria

- In every sort mode, all rows with a missing amount appear before every row
  without a missing amount.
- The initial view sorts known TTC values from longest to shortest separately
  within the missing and non-missing groups.
- Shortest-first and longest-first apply only inside each group. They keep
  unknown TTC rows after known rows and use AE2 order for equal known values.
- AE2 order preserves the original relative order inside each group.
- Receiving TTC statistics can reorder rows only inside their current group.
- Scrolling, hover, TTC details, and TTC reset target the same row the player
  sees after sorting.
- Automated checks and prepared-client smoke prove the behavior on every
  release-matrix target.
