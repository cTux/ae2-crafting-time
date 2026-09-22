# TTC sorting for active crafting orders

Status: in-progress

Scope: CPU ordering baseline and Crazy AE2 Addons correction.

Implementation: [PR #395](https://github.com/cTux/ae2-crafting-time/pull/395), [PR #451](https://github.com/cTux/ae2-crafting-time/pull/451).
Verification: [PR #451](https://github.com/cTux/ae2-crafting-time/pull/451) records exact-pack, reconnect, fresh-process and
Crazy-absent control evidence with passing current-head CI. This establishes
the Forge correction, not the full C7 gate: integrated and connected UI evidence
for all four targets still needs to be reconciled and linked.

Tracking issues: [#387](https://github.com/cTux/ae2-crafting-time/issues/387)
for the shipped baseline and
[#421](https://github.com/cTux/ae2-crafting-time/issues/421) for Crazy AE2
Addons compatibility. The baseline merged in
[#395](https://github.com/cTux/ae2-crafting-time/pull/395); the Crazy ordering
correction and exact-pack runtime acceptance are recorded in PR #451.

## Goal and scope

Make the Crafting Status window's existing TTC control sort both the selected
order's items and the list of crafting orders (CPUs). Players can find the
shortest or longest remaining job without selecting or scrolling through every
CPU first. TTC means the server's remaining total for that job, including its
existing partial-data rules; it is not elapsed time or the final item's TTC.

This extends the existing [TTC sorting feature](../../ttc-sorting.md). The
[Crafting Plan rules](../spec.md) remain unchanged. The earlier
[CPU badge specification](../../cpu-list-total-ttc/spec.md) remains authoritative
for badge appearance and estimate semantics. This extension supersedes its
no-CPU-sorting boundary and, in TTC modes only, its visible-only refresh and
fixed three-second expiry rules.

## Player behavior

- The window opens in **TTC: longest first**, as it does today. The existing
  button cycles through **AE2 order**, **TTC: shortest first**, and longest
  first. One screen-local setting controls both lists and resets on reopen.
- In either TTC mode, busy CPUs come first. Busy CPUs with a positive known
  total precede busy CPUs without an estimate; idle CPUs come last.
- Known totals sort numerically in the chosen direction. Equal totals, unknown
  busy CPUs, and idle CPUs each retain their relative order in the latest AE2
  list. Do not use formatted text, CPU names, Crazy priority, or the selected
  CPU as TTC keys.
- **AE2 order** shows the complete effective native list, including idle CPUs.
  Without another UI-order integration this is the latest AE2 list in its
  original order. It restores the existing visible-plus-selected refresh
  behavior. The selected order's item sorting continues to behave as today.
- Crazy AE2 Addons may apply its priority, name, and serial order in **AE2
  order**. In either TTC mode, TTC grouping and the chosen numeric direction
  are final: Crazy priority or CPU names cannot reorder those rows. Equal known
  totals, unknown busy CPUs, and idle CPUs retain the latest AE2 order. Crazy's
  priority order remains available by switching back to **AE2 order**.
- The complete active list participates, including jobs outside the viewport
  and lists larger than one request batch. Estimates arrive progressively;
  unknown rows move into the known group when their data arrives. No CPU is
  hidden or silently excluded because of its row index or list size.
- Visible and selected CPUs receive refresh priority. Background jobs are
  visited fairly even if the user keeps scrolling. Large lists take longer to
  collect and refresh; sorting compares the latest valid received totals, not
  a simultaneous measurement of every job.
- Freshness is bounded. With `B` busy CPUs, TTC-mode values expire after
  `max(3, ceil(B / 25) + 2)` seconds without a fresh accepted value. Thus 10,
  100, and 1,000 busy CPUs have bounds of 3, 6, and 42 seconds. This permits
  bounded background refresh without constantly expiring the previous page.
  AE2-order mode retains a three-second bound. Growing the list cannot extend
  an already received value's deadline. Expired/unknown/zero values have no
  badge and do not acquire an invented duration.
- Selection follows CPU identity when the list moves. Sorting never sends a
  selection or cancellation action. A card click, its tooltip, and its badge
  refer to the displayed CPU; cancellation still acts on the selected CPU.
- Keep the current scroll offset, clamped to the available rows, on mode
  changes and refreshes. Do not jump to the selected CPU or reset to the top.
  The selected CPU may move out of view. Selection after removal follows AE2.
- Recompute order at the next frame after a mode, list, or valid estimate
  change, including expiry. Input uses the last displayed row order. A removed
  or replaced row cannot be clicked using a stale frame's identity.
- Invalidate a value when the client observes removal, idle state, a changed
  job output/amount, or decreased elapsed time. Clear all data on close,
  reconnect, or another menu session. Preserve the existing limitation that
  an unobserved same-output replacement cannot be detected before new data.

## Compatibility and non-goals

Support Forge 1.20.1, Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2,
with the same behavior on integrated and matching dedicated servers. Native
addon CPUs shown in AE2's list participate through the existing estimate path;
unknown scopes remain unknown. Separate addon screens are outside this scope.

Crazy AE2 Addons compatibility is limited to verified `2.6.2` behavior on
Minecraft 1.20.1 Forge. The correction uses the shared AE2 render/input seam,
not an addon-private hook contract. It must leave Fabric, NeoForge,
Crazy-absent Forge, and AE2-order/channel-unavailable behavior on their normal
paths. Changed Crazy hook shapes remain unverified and must not be reported as
compatible without new runtime evidence.

If the CPU-total channel is unavailable, the CPU list keeps AE2 order, while
item sorting and the existing title fallback continue normally. Do not infer
CPU totals from item statistics on the client.

No new control, sort mode, saved preference, configuration, translation key,
estimator, server crafting priority, persistence, dependency upgrade, or release
is included. Reuse English/Ukrainian button text and badge layout. The Crazy
correction is client-only and does not change packets, estimates, server CPU
priority, or the menu's stored CPU list.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| C1 | The initial longest-first view and every button mode apply to both lists; reopening restores the default. |
| C2 | Known busy totals sort in both directions, followed by unknown busy and idle groups; ties preserve the latest AE2 order. Without another UI-order integration, AE2 mode exactly restores the latest original list; installed native UI ordering remains effective in AE2 mode. |
| C3 | With more than 32 busy CPUs, every stable job is requested without scrolling; priority rows refresh and background rows are not starved. No request or server budget is increased. |
| C4 | Selection, card click, tooltip, cancellation, badge/title agreement, and scroll clamping remain correct after reorder and removal. |
| C5 | Completion, same-output replacement, expiry, late/duplicate responses, reopen, network change, and reconnect cannot reuse invalid totals. Freshness follows the rule above. |
| C6 | Channel-unavailable behavior and existing item/plan sorting remain intact. Native addon rows with unknown totals remain usable. |
| C7 | All four targets pass the affected unit/boundary checks and actual UI verification on integrated and connected dedicated servers, with rendered-row evidence. |
| C8 | With Crazy AE2 Addons 2.6.2 on Forge 1.20.1, TTC modes remain authoritative before the six-row slice, while AE2 order retains Crazy's priority/name/serial behavior, including live priority changes. |
| C9 | With Crazy installed, rendering, badge, tooltip, click, selection, cancellation, wheel-before-draw, and stale-hit suppression use one displayed CPU identity for the frame. |
| C10 | The current Project Infinity CurseForge main release at execution time promotes every known off-screen job in the initial longest-first check. The current exact target is Project Infinity 0.1 `0.0.51.4-hotfix-2`, file `8895030`. A Crazy-absent control keeps the existing behavior, and all four minimum AE2 target bytecodes retain the shared render/input seam without changing other targets. |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
