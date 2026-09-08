---
navigation:
  title: Configuration
  parent: features/index.md
  position: 8
---

# Configuration

Every loader uses `ae2craftingtime-common.toml`.

- `enabled` turns profiling and server-owned stats on or off.
- `showInTree` controls Crafting Tree badges, tooltips, spacing, and clicks on
  supported pre-26 targets.
- `showChatMessages` controls Ctrl-click details and reset notices. It does not
  block the reset itself.
- `maxSamples` keeps 1–100 recent throughput and runtime accuracy samples per
  output; the default is 10.
- `outlierMultiplier` accepts 1.0–1000.0 and defaults to 4.0.

For example, set `maxSamples = 20` to retain a longer recent history. Restart
the game or server after changing the sample window or outlier boundary because
they are applied when the profiler is created. Invalid Fabric values keep the
current/default value, and out-of-range numbers are clamped; Forge and NeoForge
use their native config validation.

![Crafting Tree with TTC display enabled](images/crafting-tree-tooltip.png)

*The seeded tooltip shows the Crafting Tree display controlled by `showInTree`.*

[Previous: Saved history](saved-history.md) | [Features](index.md) |
[Next: AE2: Crafting Tree](crafting-tree.md) | [Confidence](confidence.md)
