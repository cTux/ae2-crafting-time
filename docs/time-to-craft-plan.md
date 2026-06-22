# Time To Craft Line Research

Date: 2026-06-21

## Requirement

Show an approximate time to finish the whole planned amount for each item, liquid, gas, or other AE key in the craft plan.

Tooltip shape:

```text
Available: N
To Craft: M
Time To Craft: ~000:00:00
```

This is not "time for one craft operation". It is estimated time for the full `To Craft` amount shown for that row or node.

## Existing AE2 UI

AE2 `CraftConfirmTableRenderer` already builds row text and tooltip text from `CraftingPlanSummaryEntry`:

- `getStoredAmount()` -> `GuiText.FromStorage`
- `getMissingAmount()` -> `GuiText.Missing`
- `getCraftAmount()` -> `GuiText.ToCraft`
- `getEntryTooltip(...)` appends the full-amount `To Craft` tooltip line last when `craftAmount > 0`

`CraftingPlanSummaryEntry` carries `what`, `missingAmount`, `storedAmount`, and `craftAmount`.

The cheapest AE2-native integration is a client-only mixin into `CraftConfirmTableRenderer#getEntryTooltip` that appends the new line immediately after AE2 adds `To Craft`.

## Crafting Tree

AE2: Crafting Tree has its own tooltip in `CraftingTreeWidget#draw`. Middle nodes already show `Crafting: <amount>` from `node.amountHelper.craftAmount`.

First pass should target AE2's craft-plan table because the user asked for the crafting plan and AE2 already has the exact `To Craft` line. Crafting Tree can reuse the same estimate later.

## Data Source

Use only server-calculated stats:

- client requests visible/hovered plan keys
- server replies with aggregate snapshots
- client renders from `ClientStatsCache`

Do not calculate timing on the client.

The current server snapshot already carries `amountPerSecond`, so no new packet type is required.

## Estimate Formula

For each craft-plan entry:

```text
normalizedAmount = normalize(entry.what, entry.craftAmount)
seconds = normalizedAmount / stats.amountPerSecond
```

Only show the line when:

- `craftAmount > 0`
- server stats exist for `entry.what`
- `amountPerSecond > 0`

No stats: show nothing and request stats for that output key.

For the whole ordered craft, use the visible plan entries and calculate each
known row with the same formula. Display the total as the maximum known row ETA,
not the sum. AE2 can craft independent rows in parallel across CPUs and
co-processors, so summing every row overstates wall-clock time. If no row has
known stats, show no total line and request missing stats.

## Amount Normalization

The estimate must use the same units as profiling throughput.

Current implementation:

- items: raw amount
- bucket-style keys, including AE2 fluids and optional Applied Mekanistics chemicals:
  mB, converted from AE amount with `AEKey#getAmountPerUnit`

Requirement includes gas/liquid. Lazy correct rule for later key types:

```text
if amountPerUnit > 1:
  normalized = amount * 1000 / amountPerUnit
else:
  normalized = amount
```

Applied Mekanistics support stays optional: the code does not import AppMek classes,
and Forge/NeoForge metadata only orders after `appmek` when it is installed.

## Time Format

Format as fixed `HHH:MM:SS` with `~` prefix:

```text
Time To Craft: ~000:00:01
Time To Craft: ~000:02:15
Time To Craft: ~031:04:09
```

Rules:

- round up to nearest second so nonzero work is never shown as `~000:00:00`
- hours are at least 3 digits
- minutes and seconds are 2 digits
- if estimate exceeds `999:59:59`, allow wider hours instead of clamping

## UI Placement

AE2 craft confirm table tooltip:

```text
Available: N
Missing: X
To Craft: M
Time To Craft: ~000:02:15
```

Actual AE2 localization may say `From Storage` instead of `Available`. Do not rewrite AE2's existing lines in the first pass. Just append `Time To Craft` after the existing `To Craft` line.

Whole ordered craft estimate:

```text
Crafting CPU: Automatic
Storage: N/A : Co Processors: N/A
Total TTC: ~000:02:15
```

Place `Total TTC` in the bottom status area immediately after the `Crafting CPU`
details. Center it on the screen with the same alignment as the CPU status text.
Do not put it in the title; the title already
contains AE2's storage-byte summary. Do not put it in the item grid; the grid
contains per-row estimates.

Crafting Tree tooltip, if added later:

```text
Crafting: M
Time To Craft: ~000:02:15
```

## Tests

Small tests that matter:

- `TimeEstimate` formats `0`, `1`, `75`, and large hour values
- ETA rounds up fractional seconds
- no ETA when throughput is `0`
- total craft ETA uses the maximum known row ETA, not the sum
- packet snapshot roundtrip still carries `amountPerSecond`
- mixin config keeps AE2/Crafting Tree UI mixins client-only

## Implementation Steps

1. Add shared `TimeEstimate` helper.
2. Add tests for `TimeEstimate`.
3. Add a version-specific normalizer for `AEKey + amount`.
4. Add client-only mixin for `CraftConfirmTableRenderer#getEntryTooltip`.
5. In that mixin, append `Time To Craft: ~...` when cached server stats exist, otherwise request stats.
6. In the craft-confirm screen mixin, compute `Total TTC` from the rendered plan entries and draw it centered below the `Crafting CPU` details.
7. Build all versions.

## Sources

- AE2 local source inspected: `CraftConfirmTableRenderer`, `CraftingPlanSummaryEntry`, `CraftingCPUScreen`
- AE2: Crafting Tree local source inspected: `CraftingTreeWidget`, `CraftingTreeHelper.Node`
- Forge networking docs: https://docs.minecraftforge.net/en/latest/networking/simpleimpl/
- AE2 player guide notes autocrafting supports fluids and addon material types: https://guide.appliedenergistics.org/development/ae2-mechanics/autocrafting
