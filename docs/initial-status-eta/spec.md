# Initial Crafting Status ETA

Status: ready for implementation. Tracks [issue #350](https://github.com/cTux/ae2-crafting-time/issues/350).

## Expected behavior

On AE2 15.x and 19.x, Crafting Status must not show AE2's native ETA until the
selected job has completed measurable work. The normal **Crafting Status** title
stays visible while progress is zero. AE2 Crafting Time's separate total TTC
badge can still appear when profiler data is available.

After the first item completes, AE2's native ETA, the red can't-store warning,
sorting, tooltips, and status controls keep their existing behavior.

## Acceptance

- A newly submitted job with equal start and remaining counts has no native ETA
  suffix.
- A job with a smaller remaining count keeps AE2's native ETA suffix.
- The red can't-store warning is preserved when the initial ETA is hidden.
- The behavior is shared by the 1.20.1 Forge, 1.20.1 Fabric, and 1.21.1
  NeoForge targets. AE2 26.1.2 remains unchanged.
- Pure decision logic has full line and branch coverage, and a real Crafting
  Status smoke capture shows a plausible initial title.

## Out of scope

This does not change AE2's elapsed-time tracker, packets, job progress, addon
CPUs, or the separate AE2 Crafting Time total TTC estimate.
