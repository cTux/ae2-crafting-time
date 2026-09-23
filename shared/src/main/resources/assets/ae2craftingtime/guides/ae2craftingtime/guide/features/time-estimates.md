---
navigation:
  title: Time estimates
  parent: features/index.md
  position: 0
---

# Time estimates

Crafting Plan shows TTC beside every row that has learned timing data. The row
time covers the full **To Craft** amount, not one pattern operation. The total
below the CPU details adds the known plan rows; if some rows have no data, it is
only the known part of the job.

Crafting Status uses the same row estimates while a job runs. Its total follows
the longest remaining dependency path, so parallel branches are not added as if
they ran one after another. For example, two independent ten-second branches
can still finish in about ten seconds, while two dependent steps take about
twenty. Estimates are not deadlines: machine speed, other work, and server load
can change the result.

Crafting Status also puts available, crafting, and scheduled amounts on one line,
in that order: `4/10/200`. A missing amount is `-`; when only one amount is
present, its `A`, `C`, or `S` prefix identifies it. Hover for AE2's full labeled
amounts. Turn **Compact crafting-status amounts** off under **Options > Client >
Displays** to restore AE2's separate lines. This setting works independently of
row TTC.

![Crafting Plan with row and total TTC](images/crafting-plan-estimate.png)

*Seeded test values show two learned recipe estimates and their known total.*

[Features](index.md) | [Next: Learning throughput](learning-throughput.md) |
[Make your first estimate](../getting-started.md)
