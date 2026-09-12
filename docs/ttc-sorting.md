# TTC Sorting

The current requirements and implementation guidance live in:

- [Specification](ttc-sorting/spec.md)
- [Technical design](ttc-sorting/technical-design.md)
- [Implementation plan](ttc-sorting/implementation-plan.md)

Tracked by [issue #318](https://github.com/cTux/ae2-crafting-time/issues/318).

## Active crafting orders

The [issue #387](https://github.com/cTux/ae2-crafting-time/issues/387) feature
extends the status window's existing TTC setting to both item rows and the full
CPU list. Busy known jobs sort numerically, unknown busy jobs follow, idle CPUs
remain last, and AE2 order restores the raw list. Visible/selected CPUs refresh
first while a bounded round-robin collector covers off-screen jobs:

- [CPU sorting specification](ttc-sorting/cpu-list/spec.md)
- [CPU sorting technical design and research](ttc-sorting/cpu-list/technical-design.md)
- [CPU sorting implementation plan](ttc-sorting/cpu-list/implementation-plan.md)
