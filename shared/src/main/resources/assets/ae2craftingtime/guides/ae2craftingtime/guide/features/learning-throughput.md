---
navigation:
  title: Learning throughput
  parent: index.md
  position: 1
---

# Learning throughput

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
