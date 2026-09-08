---
navigation:
  title: Job accuracy
  parent: features/index.md
  position: 3
---

# Job accuracy

When AE2 accepts a job, the mod freezes the whole-plan prediction. After a
successful finish it compares predicted TTC with actual wall time and nominal
game-tick time. Coverage tells you how many crafted rows had estimates.

Ctrl-click details in chat show fully covered jobs, mean absolute percentage
error (MAPE), and
the actual-to-TTC ratio. A ratio near `1.0x` means actual time was close to the
prediction; `1.5x` means it took about half again as long. Partial jobs still
show coverage and latest-job context, but they do not affect aggregate error.
Cancelled, restored, or unpredicted jobs are excluded. Accuracy is diagnostic
only and never changes learned throughput automatically.

![Completed job accuracy in chat](images/ttc-job-accuracy.png)

*One real completed job shows full coverage, prediction error, and the actual-to-TTC ratio.*

[Previous: Confidence](confidence.md) | [Features](index.md) |
[Next: Delay diagnostics](delay-diagnostics.md) | [Details and reset](details-and-reset.md)
