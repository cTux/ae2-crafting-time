---
navigation:
  title: "Chapter 3: Statuses"
  parent: index.md
  icon: minecraft:comparator
  position: 2
---

# Chapter 3: Statuses

Crafting Status labels explain what the selected CPU is doing or what currently
blocks it. Start at the top and use the first label you see:

1. [NO SPACE](no-space.md)
2. [NO PROVIDER](no-provider.md)
3. [NO POWER](no-power.md)
4. [LOCKED](locked.md)
5. [INPUT BLOCKED](input-blocked.md)
6. [NO TARGET](no-target.md)
7. [Waiting](waiting.md)
8. [DELAYED](delayed.md)
9. [No data yet](no-data-yet.md)
10. [A TTC estimate such as ~12s](estimated.md)

The order matters. `NO SPACE` applies to stored output. The other blocking
labels apply to scheduled batches; a row can still have an active batch that
finishes while its next batch is blocked. The mod reports observed conditions,
not a diagnosis of a particular machine.

[Introduction](../index.md) | [Chapter 1](../getting-started.md) |
[Chapter 2: Features](../features/index.md) | [Delay diagnostics](../features/delay-diagnostics.md)
