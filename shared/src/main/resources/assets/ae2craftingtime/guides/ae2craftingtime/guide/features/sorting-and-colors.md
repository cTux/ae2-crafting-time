---
navigation:
  title: Sorting and colors
  parent: features/index.md
  position: 5
---

# Sorting and colors

Crafting Plan and Crafting Status start in the mode saved in Client Controls;
the default is **TTC: longest first**. The toolbar cycles AE2 order, shortest
first and longest first for that open screen. Plan always puts missing rows
first, then sorts within missing and non-missing groups. In TTC modes, unknown
times follow known times within each group.

Status sorting also orders its CPU list: known busy jobs by time, unknown busy
jobs next, idle CPUs last. AE2 order restores the native CPU list.

**Fast-to-slow colors** is Off by default. Turn it on to compare known times
across the current list, using configurable green/yellow/red defaults. Scrolling
does not change that range. Red in this scale is a relative time, not proof of
a delay; the separate red **DELAYED** label reports that state. Badge background
and text shadow have their own switches.

![Crafting Plan sorted by longest TTC](images/ttc-sort-longest.png)

*Seeded values show longest-first order and relative fast-to-slow colors.*

[Previous: Delay diagnostics](delay-diagnostics.md) | [Features](index.md) |
[Next: Details and reset](details-and-reset.md) | [Time estimates](time-estimates.md)
