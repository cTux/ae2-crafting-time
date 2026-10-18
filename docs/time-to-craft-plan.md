# Time To Craft Lines

Current formatting: [millisecond TTC and Instant status](ttc-milliseconds-instant/spec.md).
The original implementation steps below are historical; current behavior follows
this reference and the linked feature scopes.

Date: 2026-06-21

## Goal

Show roughly how long the full planned amount will take for each item, liquid,
gas, or other AE key in the craft plan.

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

## Where It Fits In AE2

AE2 `CraftConfirmTableRenderer` already builds row text and tooltip text from `CraftingPlanSummaryEntry`:

- `getStoredAmount()` -> `GuiText.FromStorage`
- `getMissingAmount()` -> `GuiText.Missing`
- `getCraftAmount()` -> `GuiText.ToCraft`
- `getEntryTooltip(...)` appends the full-amount `To Craft` tooltip line last when `craftAmount > 0`

`CraftingPlanSummaryEntry` carries `what`, `missingAmount`, `storedAmount`, and `craftAmount`.

The smallest AE2-native integration is a client-only mixin into
`CraftConfirmTableRenderer#getEntryTooltip`. It adds the new line right after
AE2 adds `To Craft`.

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

`amountPerSecond` comes from accepted completion intervals. Returns for the
same network/output in one tick form one observation, including concurrent CPUs.
The server tick flush retains samples while work remains; learning does not wait
for the whole order to finish.

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

No stats: request the output key and show the applicable no-data/collecting
state instead of inventing a numeric estimate.

For the whole ordered craft, use the plan entries and calculate each known row
with the same formula. Display the total as the sum of known row ETAs. If no row
has known stats, show no total line and request missing stats.

The running crafting-status screen is different. When AE2 accepts the job, the
server retains the selected patterns as a dependency graph and tracks the
remaining amount of each crafted output. Its total is the longest remaining
dependency path:

```text
finish(node) = rowTtc(node) + max(finish(dependency))
totalTtc = max(finish(finalOutput), longestKnownRow)
```

A dependency ladder therefore adds each sequential step, while independent
branches contribute only the slowest branch. Shared dependencies are counted
once per path. Ambiguous substitution inputs are omitted from the graph so a
partial estimate remains an honest lower bound, and the longest known row keeps
the header from showing a value below a visible row. The total is scoped to the
selected crafting CPU and cleared when the job or selection changes.

## Prediction Accuracy

When AE2 accepts a job, the server freezes the running job's critical-path TTC.
It also
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
with `DELAYED`. Its tooltip keeps the estimate and shows no-progress time,
the learned typical duration, AE2 active/scheduled amounts, recent parallel-slot
use, and up to two possible improvements.

An active output is delayed only when both conditions hold:

- no accepted output for the configured minimum (10 seconds by default)
- no accepted output for the configured multiple of its learned average
  completion interval (twice by default)

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

Gas and liquid support uses the same normalization rule. For later key types,
keep the rule simple:

```text
if amountPerUnit > 1:
  normalized = amount * 1000 / amountPerUnit
else:
  normalized = amount
```

Applied Mekanistics support stays optional: the code does not import AppMek classes,
and Forge/NeoForge metadata only orders after `appmek` when it is installed.

## Time Format

`TimeEstimate.milliseconds` retains precise duration and confidence. Positive
durations below one second display whole milliseconds, rounded up with a 5ms
minimum. At or above one second, round up to seconds and use `~12s`, `~2:15`,
or `~31:04:09`. Low confidence appends `?`; unknown remains unknown.

Reliable TTC at or below 5ms can show green Instant when enabled. Partial or
unreliable totals never show Instant. The display clamp does not alter estimates,
sorting or retained samples. See the linked millisecond specification for
status precedence, per-recipient chat and the Show emoji option.

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
TTC: ~000:02:15
```

Place the total `TTC` in the bottom status area immediately after the `Crafting CPU`
details. Center it on the screen with the same alignment as the CPU status text.
Do not put it in the title; the title already
contains AE2's storage-byte summary. Do not put it in the item grid; the grid
contains per-row estimates.

Crafting Tree tooltip:

```text
Crafting: M
Time To Craft: ~000:02:15
```

When enabled, Crafting Tree TTC badges use a compact dark background with two pixels
of padding and four-pixel rounded corners, centered below the node with Minecraft text shadow. The tree reserves
extra vertical spacing for the badge so it does not cover the node amount or
connector lines.

ME Requester follows the same optional badge-background and text-shadow settings. Row TTC badges
sit below the amount fields, clear of the item icon, and are colored by relative duration.
The status bar keeps its right edge and gives the badge room on its left. Row badges
use one pixel of vertical padding and fit long labels within that space;
the total badge stays inside the requester header instead of extending beyond
the screen. Its row estimate uses only the current network shortfall:
`normalize(max(0, wanted amount - network amount)) / amountPerSecond`. A row
whose wanted amount is already available does not show a TTC badge.

## Tests

Small tests that matter:

- `TimeEstimate` formats `0`, `1`, `75`, and large hour values
- subsecond ETA uses ms; longer durations round up fractional seconds
- no ETA when throughput is `0`
- total craft ETA sums known row ETAs
- packet snapshot roundtrip still carries `amountPerSecond`
- mixin config keeps AE2/Crafting Tree UI mixins client-only

## Original implementation steps (historical)

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
