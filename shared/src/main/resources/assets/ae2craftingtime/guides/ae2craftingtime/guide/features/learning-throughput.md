---
navigation:
  title: Learning throughput
  parent: features/index.md
  position: 1
---

# Learning throughput

Crafting Time shortens large hover rates with k, M, B, T, P and E for powers
of 1,000. For example, `~1.23M items/s` means about 1.23 million items per
second. The `~` marks a rounded value; rates beyond E use scientific notation.
Small positive rates below the display precision show `<0.01` instead of zero.
This changes only throughput text; sample durations and native AE2 quantities
keep their existing format.

The mod learns from outputs that your AE2 network actually accepts. It measures
a production window for an output and keeps recent completed windows for that
network. Parallel batches are combined, so running two machines at once teaches
their real combined speed instead of adding both durations.

For example, if a network accepts 64 processors over eight seconds, that window
teaches roughly eight processors per second. A different ME network learns its
own history. Items use item counts; fluids and supported chemicals use
millibuckets. A new output can show **No data yet** until a usable window has
completed, and changing machines affects only later samples.

![Running crafting job producing learned output](images/crafting-status-running.png)

*A live job produces the outputs that become timing history after a completed window.*

[Previous: Time estimates](time-estimates.md) | [Features](index.md) |
[Next: Confidence](confidence.md) | [Saved history](saved-history.md)
