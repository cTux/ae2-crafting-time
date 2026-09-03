# Screenshot gallery

These are real Minecraft 1.20.1 Forge screenshots, captured at native pixel size.
Each image focuses on a window, recipe row, or tooltip. There are no full-screen
captures or duplicate light/dark versions.

## Crafting plan

Before any timing history exists:

![Crafting plan with no timing data](crafting-plan-no-data.png)

After a completed craft has supplied timing samples:

![Two-stage crafting plan with recipe and total estimates](crafting-plan-estimate.png)

When only part of the plan has history, the total covers the known work:

![Crafting plan with one known and one unknown recipe](crafting-plan-partial-estimate.png)

## Live crafting status

Active recipes, scheduled work, and the remaining-time estimate:

![Running crafting job](crafting-status-running.png)

A recipe that has not dispatched yet waits behind a stalled ingredient:

![Delayed ingredient and waiting output](crafting-status-waiting.png)

The delayed state after a processing machine stops producing output:

![Delayed crafting job](crafting-status-delayed.png)

Hovering the delayed recipe shows its timing, recent activity, and advice:

![Delayed recipe diagnostics](crafting-status-ttc-bottleneck-diagnostics.png)

When the CPU can't return its stored items to the ME network, the row explains
the problem and suggests freeing or adding storage:

![NO SPACE warning and storage advice](crafting-status-no-space.png)

When scheduled work loses its connected Pattern Provider or encoded pattern,
the row shows NO PROVIDER and explains how to resume the job:

![NO PROVIDER warning and provider recovery advice](crafting-status-no-provider.png)

## Timing details

The first sample is marked as low confidence:

![Low-confidence timing tooltip](ttc-low-confidence.png)

The sample-history tooltip also shows throughput:

![Production samples and throughput](ttc-production-sample-details.png)

Ctrl-click expands timing details in chat. Ctrl-Alt-click clears that item's
history and confirms the reset:

![Expanded TTC details and reset feedback](ttc-details-chat.png)

A completed job with only partial timing coverage is reported separately:

![Completed-job accuracy with partial coverage](ttc-job-accuracy-partial.png)

After a fully covered job completes, the tooltip includes prediction error and
the actual-to-estimated duration ratio:

![Completed-job accuracy with a fully covered result](ttc-job-accuracy.png)

## Sorting

The sort button cycles through these three modes:

![AE2 order](ttc-sort-ae2.png)

![Longest TTC first](ttc-sort-longest.png)

![Shortest TTC first](ttc-sort-shortest.png)

## Other windows

The crafting tree has its own estimate and tooltip:

![Crafting tree estimate](crafting-tree-estimate.png)

![Crafting tree tooltip](crafting-tree-tooltip.png)

ME Requester with a diamond request and TTC below the amount fields:

![ME Requester diamond request with row and total TTC](me-requester-estimate.png)

## Capture notes

The stone and smooth-stone examples use two real furnaces in a disposable test
world. Removing fuel produces the delayed state; restoring it lets the job
finish. The partial-accuracy example includes that deliberate pause, so its
long duration is not a normal smelting benchmark.

The furnace-item, crafting-tree, sorting, and ME Requester examples use the
repository's UI test fixtures. Their seeded sample values demonstrate the UI;
they are not machine-performance measurements. ME Requester uses a diamond
request with a seeded two-second estimate.

The NO SPACE example uses a full item cell and seeded retained CPU contents.
Adding writable storage clears the warning without reopening the screen.

The NO PROVIDER example uses a real processing job with one active output and
63 scheduled outputs. Removing its encoded pattern triggers the warning;
restoring it clears the warning in the same open screen. The crop shows the
tooltip at its original pixel size.

To refresh a crop, open the target UI and run `scripts/capture-ui-region.ps1`
inside the same interactive Windows session. Supply the output PNG path and
the region's physical-pixel `X`, `Y`, `Width`, and `Height`. Inspect the saved
image before keeping it; live labels can change between inspection and capture.
