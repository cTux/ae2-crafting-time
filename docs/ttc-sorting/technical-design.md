# TTC Sorting Technical Design

This design implements the [specification](spec.md) for
[#318](https://github.com/cTux/ae2-crafting-time/issues/318).

## Current behavior and ownership

`CraftConfirmScreenMixin` intercepts the list passed to AE2's
`CraftConfirmTableRenderer`. It stores sort mode `2` as the per-screen default,
uses `TtcSort.copySorted` for TTC modes, and reuses the same sorted list for TTC
details and reset hit testing.

The Minecraft 1.20.1/1.21.1 seam lives in `shared/src/mc1201`; Minecraft 26.1.2
uses the matching class in `shared/src/mc2612`. Both paths read
`CraftingPlanSummaryEntry#getMissingAmount()`, so no server or packet change is
needed.

The Crafting Status screen has a separate client interception in
`CraftingCPUScreenMixin`. It continues to call the existing `TtcSort.copySorted`
path, including its longest-first default and known-before-unknown behavior.

TTC remains separate from AE2's terminal `Sort By` setting. The craft-confirm
screen renders `CraftingPlanSummary` entries directly, so patching AE2's closed
terminal sort enum would affect unrelated screens and would not address this
list. The sort stays on a display copy at the existing client boundary.

## Ordering pipeline

Add a small shared `TtcSort` entry point that accepts a priority predicate and
whether TTC sorting is active. It sorts a copy with these keys, in order:

1. rows selected by the priority predicate first;
2. known TTC before unknown TTC when TTC sorting is active;
3. TTC ascending or descending according to the active mode;
4. AE2's natural row order for equal known TTC values.

When TTC sorting is inactive, the sort is stable after the priority comparison,
so AE2 order is preserved inside both groups. The Crafting Plan mixins pass
`entry.getMissingAmount() > 0` as the priority predicate. Crafting Status keeps
using the existing `copySorted` entry point and does not change.

The helper always returns a copy. AE2's plan summary and server job order remain
untouched. Java's stable list sort preserves the incoming order for comparisons
that are equal, including unknown TTC rows and AE2-order mode.

## Screen integration

Replace the current early return for sort mode `0` in both Crafting Plan mixins
with the shared prioritized sort. Pass TTC sorting as active only for modes `1`
and `2`; mode `2` remains descending.

Continue calling the same mixin sorting method from rendering and clicked-row
resolution. This keeps the visible row, scrollbar index, hover lookup, details
click, and reset click on one ordering path. Stats requests and total TTC still
iterate the original plan because neither depends on display order.

TTC seconds continue to use the visible craft amount normalized through
`AeKeyAmounts`, then `TimeEstimate.seconds` with the current `ClientStats` value.
The change does not alter estimation, TTC colors, or stats requests. Color range
calculation receives the displayed entries through the existing renderer path.

## State and failure behavior

- Missing status comes only from AE2's current positive missing amount; no local
  missing state is cached.
- Missing priority applies even when TTC data is absent or arrives later.
- Unknown TTC remains unknown and is never replaced with a sentinel duration.
- A null plan follows the existing no-render and no-click paths.
- Sort mode remains local to the screen and resets to longest-first when a new
  Crafting Plan opens.

## Compatibility

The shared helper is Minecraft-free. Only the two existing client mixins need
version-specific calls. No compatibility version, packet codec, persisted data,
language key, optional integration, or dedicated-server class path changes.

## Verification map

| Acceptance area | Proof |
| --- | --- |
| Missing rows always first | `TtcSortTest` covers every mode with mixed missing and non-missing rows |
| TTC precedence inside groups | unit checks cover ascending, descending, unknown, equal, and arriving-stat cases |
| AE2 order | unit check proves stable relative order in both groups when TTC sorting is off |
| Screen targeting | existing Crafting Plan click path is exercised after the same prioritized sort |
| Cross-version UI | prepared-client `standard-plan-controls` smoke observes missing-first order and the full sort cycle on all four targets |
| Client-only boundary | existing mixin placement and release-matrix builds remain green |

## Rejected alternatives

- Sorting the server plan would change job data for a display-only rule.
- Assigning missing rows an artificial TTC would mix two independent meanings
  and break shortest-first behavior.
- Duplicating partition logic in both version mixins would leave two ordering
  implementations to keep aligned.
