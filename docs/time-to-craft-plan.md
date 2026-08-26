# Time To Craft Line Research

Date: 2026-06-21

## Requirement

Show an approximate time to finish the whole planned amount for each item, liquid, gas, or other AE key in the craft plan.

TTC is one part of the mod's broader diagnostics: learned throughput powers the
estimate, while running-job views also expose delays, prediction accuracy, and
bottleneck clues.

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

AE2's craft-plan table and AE2: Crafting Tree both reuse the same server stats
and estimate helper.

## Data Source

Use only server-calculated stats:

- client requests visible/hovered plan keys
- server replies with aggregate snapshots
- client renders from `ClientStatsCache`

Do not calculate timing on the client.

The current server snapshot already carries `amountPerSecond`, so no new packet type is required.

`amountPerSecond` is derived from continuous production windows that combine
concurrent batches for the same network output. Individual pattern latency is
not treated as if every parallel batch ran sequentially.

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

For the whole ordered craft, use the plan entries and calculate each known row
with the same formula. Display the total as the sum of known row ETAs. If no row
has known stats, show no total line and request missing stats.

The running crafting-status screen is different: its total uses AE2's elapsed
time and overall completed-work progress. It must not sum status rows because
independent rows and crafting CPUs can run concurrently.

## Prediction Accuracy

When AE2 accepts a job, the server freezes the same total TTC that the craft
plan displays: the sum of estimates for rows with known throughput. It also
records how many crafted-output rows had estimates. On successful completion,
the server compares that frozen prediction with both nominal tick time and
monotonic wall-clock time. Cancelled jobs, jobs restored after a restart, and
jobs with no prediction are excluded.

Accuracy is retained in a rolling runtime window keyed by network and final
output. Aggregate error metrics use only fully covered plans so missing row
statistics cannot make TTC appear systematically optimistic. Average and latest
coverage are still shown for partial predictions. These measurements are
diagnostic only and do not feed back into per-output throughput or TTC math.

The detailed TTC tooltip and Ctrl-click chat diagnostics show:

- fully covered jobs / recorded jobs
- mean absolute percentage error (MAPE)
- mean actual wall time / predicted TTC ratio
- mean signed error (`actual - predicted`)
- average coverage
- latest predicted, wall-clock, nominal tick, and row-coverage values

## Delayed Craft Diagnostics

The running crafting-status screen replaces a delayed row's visible estimate
with `TTC: DELAYED`. Its tooltip keeps the estimate and shows no-progress time,
the learned typical duration, AE2 active/scheduled amounts, recent parallel-slot
use, and up to two possible improvements.

An active output is delayed only when both conditions hold:

- no accepted output for at least 30 seconds
- no accepted output for at least twice its learned average production-window duration

Partial output resets the timer. Diagnostics are scoped to the selected crafting
CPU and are not persisted.

Recommendations are evidence-bounded:

- if scheduled work remains and AE2's recent dispatch budget has room, suggest
  parallel Pattern Providers or machines
- if scheduled work remains and the dispatch budget is saturated, suggest
  Crafting Co-Processors
- always suggest speeding up the active machine once its output is delayed

AE2 co-processors limit pattern pushes across a rolling three-tick window. They
are not occupied for the lifetime of an external machine operation, so the UI
calls this `Parallel slots: X/Y recently used` instead of claiming persistent
processor occupancy.

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

Format with a `~` prefix:

```text
Time To Craft: ~1s
Time To Craft: ~2:15
Time To Craft: ~31:04:09
```

Rules:

- round up to nearest second so nonzero work is never shown as `~000:00:00`
- show seconds-only for values under a minute
- show `M:SS` for values under an hour
- show `H:MM:SS` for hour-scale values

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

Crafting Tree tooltip:

```text
Crafting: M
Time To Craft: ~000:02:15
```

Visible Crafting Tree TTC badges use a compact dark background sized to their
text, centered below the node with Minecraft text shadow. The tree reserves
extra vertical spacing for the badge so it does not cover the node amount or
connector lines.

ME Requester uses the same compact dark badge and text shadow. Row TTC badges
are centered over the request status lines and colored by relative duration;
the total badge stays inside the requester header instead of extending beyond
the screen. Its row estimate uses only the current network shortfall:
`normalize(max(0, wanted amount - network amount)) / amountPerSecond`. A row
whose wanted amount is already available does not show a TTC badge.

## Tests

Small tests that matter:

- `TimeEstimate` formats `0`, `1`, `75`, and large hour values
- ETA rounds up fractional seconds
- no ETA when throughput is `0`
- total craft ETA sums known row ETAs
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
