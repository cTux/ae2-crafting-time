---
navigation:
  title: Delay diagnostics
  parent: index.md
  position: 4
---

# Delay diagnostics

Crafting Status replaces TTC with red **DELAYED** only when pending work has
learned history and no accepted output has arrived for both the fixed idle
threshold and at least twice its normal production-window duration. Any partial
output restarts the timer.

Hover the row to see its estimate, idle time, active and scheduled amounts, and
recent parallel-slot use. Advice follows the evidence: free recent slots can
suggest more Pattern Providers or machines, saturated slots can suggest
Crafting Co-Processors, and the active machine may need attention. When a
clickable provider hint appears, click it to highlight the relevant provider.
A warning points to where work stopped; it does not prove a machine is broken.

![Delayed output diagnostics tooltip](images/crafting-status-ttc-bottleneck-diagnostics.png)

*A deliberately unfuelled furnace demonstrates the delayed row and its bottleneck hints.*

[Previous: Job accuracy](job-accuracy.md) | [Features](index.md) |
[Next: Sorting and colors](sorting-and-colors.md) | [Statuses](../statuses/index.md)
