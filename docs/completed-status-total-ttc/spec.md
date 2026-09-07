# Completed Craft Total TTC Specification

Issue: [#325](https://github.com/cTux/ae2-crafting-time/issues/325)

## Goal

Remove the total TTC from Crafting Status as soon as the selected crafting job
has no remaining work.

## Player behavior

- Show the header total TTC while at least one status row has a positive active
  or pending amount.
- Hide the header total TTC when every status row has zero remaining work or the
  completed view has no rows.
- Apply the change on the status update that reports the job as finished. The
  player must not need to close or reopen the screen.
- Keep the completed Crafting Status screen, its navigation controls, and AE2's
  own completion state unchanged.

## Compatibility

- Apply the same behavior to all four targets in `scripts/release-matrix.json`.
- Treat an AE2 version that retains zero-amount rows after completion the same
  as one that removes completed rows.
- Keep the decision client-side, based on the Crafting Status data AE2 already
  sends.

## Not included

- Changing row TTC, Crafting Plan totals, estimate calculations, profiling, or
  retained throughput samples.
- Clearing the cached total for an active job or changing packet timing.
- New configuration, localization, persistence, or optional-mod behavior.

## Acceptance criteria

- A running job with positive active or pending work can show its total TTC.
- When the job finishes, no total TTC appears in the Crafting Status title.
- Completion hides the total for both an empty entry list and retained entries
  whose active and pending amounts are all zero.
- Closing and reopening the finished status does not restore the old total.
- The existing `craft-lifecycle` prepared-client scenario proves the transition
  from a visible running total to a completed status without that total on every
  supported target.
