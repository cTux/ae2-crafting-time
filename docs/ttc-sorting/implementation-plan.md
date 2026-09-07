# TTC Sorting Implementation Plan

Implement [#318](https://github.com/cTux/ae2-crafting-time/issues/318) from the
[specification](spec.md) and [technical design](technical-design.md).

## 1. Add prioritized shared sorting

- Extend `shared/src/main/java/com/ctux/ae2craftingtime/core/TtcSort.java` with
  one copy-sorting entry point that accepts a priority predicate and an active
  TTC mode without changing the current Crafting Status call.
- Compare priority before TTC. Preserve stable incoming order when TTC sorting
  is inactive and preserve current known, unknown, equal, ascending, and
  descending behavior when it is active.
- Extend `TtcSortTest` with mixed-group cases for AE2 order, shortest-first,
  longest-first, unknown TTC, equal TTC, and statistics arriving after the
  initial render.

Gate: the shared tests prove that no non-priority row can pass a priority row in
any mode and that existing ungrouped sorting is unchanged.

## 2. Apply missing priority to Crafting Plan

- Update the `shared/src/mc1201` and `shared/src/mc2612`
  `CraftConfirmScreenMixin` classes to classify
  `CraftingPlanSummaryEntry#getMissingAmount() > 0` as the priority group.
- Run the prioritized sorter for all three modes. Keep mode `2` as the default
  and leave the button cycle unchanged.
- Keep rendering and clicked-row lookup routed through the same sorting method.
  Do not change Crafting Status, total TTC, server state, or packets.

Gate: both version seams compile and their render, scroll, hover, details, and
reset paths resolve the same sorted entry.

## 3. Extend observable UI coverage

- Update the prepared-client Crafting Plan fixture to include at least one
  missing row and multiple non-missing rows with known TTC values.
- Record and assert the initial longest-first order, AE2 order, and
  shortest-first order. In every observation, assert that all missing rows form
  the leading group.
- Keep the existing tooltip, total TTC, layout, details, reset, and sort-cycle
  checks enabled so the ordering change cannot hide a row-targeting regression.

Gate: retained smoke evidence shows the missing group first through the complete
sort cycle.

## 4. Verify every supported target

- After the hook-created implementation PR exists, run the development skill's
  shared tests and all four release-matrix build checks.
- Run prepared-client `craft-plan` smoke on Minecraft 1.20.1 Forge, Minecraft
  1.20.1 Fabric, Minecraft 1.21.1 NeoForge, and Minecraft 26.1.2 NeoForge.
- Run documentation/link checks and `git diff --check`; verify GitHub CI
  separately.

Completion gate: every acceptance criterion has passing automated or retained
UI evidence on all four targets, all required checks pass, and no
repository-owned warning remains.
