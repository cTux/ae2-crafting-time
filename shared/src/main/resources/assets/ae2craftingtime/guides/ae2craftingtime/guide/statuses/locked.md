---
navigation:
  title: LOCKED
  parent: statuses/index.md
  position: 3
---

# LOCKED

**Label:** `LOCKED`

This appears on scheduled Crafting Status rows after an attempted dispatch finds
an active Pattern Provider crafting lock. Provider and power failures have
priority. Active batches on the row may still finish.

## What to check

1. Check the provider's redstone and lock mode.
2. Satisfy or disable the active redstone condition.
3. Return the previous result to its provider when that lock mode requires it.

The next successful, unknown, or changed evaluation clears the observation;
otherwise it expires after 20 server ticks and a refresh. Configuring a lock is
not enough: the mod must observe it actively preventing a dispatch. The state is
runtime-only and is not restored after reopening the world.

![LOCKED row and crafting-lock advice](images/crafting-status-locked.jpg)

*An active Pattern Provider lock prevents the next scheduled batch.*

[Previous: NO POWER](no-power.md) | [Statuses](index.md) |
[Next: INPUT BLOCKED](input-blocked.md) | [Delay diagnostics](../features/delay-diagnostics.md)
