# CPU-Bound Stats: Collecting The CPU Id

Part of `cpu-bound-stats/`. See `index.md` and `data-model.md`.

## What we have at craft start

The job is submitted to exactly one CPU. The mixins run on that CPU object, so it
is available everywhere we need it:

- Fabric/Forge vanilla path: `CraftingCpuLogicMixin` works on the `cluster`
  field, a `CraftingCPUCluster`
  (`shared/src/mcCommon/.../mixin/CraftingCpuLogicMixin.java:29`).
- Advanced AE path: `AdvancedCraftingCpuLogicMixin` works on `AdvCraftingCPU`
  (`shared/src/neoforge/.../mixin/AdvancedCraftingCpuLogicMixin.java:31`).

Both already forward the CPU as the `scope` to `ProfilerBridge.start(...)`,
`.complete(...)`, `.startJob(...)`, and `.finishJob(...)`
(`CraftingCpuLogicMixin.java:45,60,67,82`). The `scope` is currently used only for
pending-craft disambiguation and capacity/stall tracking. We now also derive a
stable `cpuId` from it.

## Deriving a stable cpuId

The identifier combines the CPU's world coordinates with its index in the grid's
`getCpus()` list:

- `CraftingCPUCluster` / `AdvCraftingCPU` expose their blocks. Take the anchor
  block position (the cluster's main block) and format it as `"x,y,z"`.
- Take the CPU's position in `grid.getCraftingService().getCpus()` iteration order
  as the index (0-based). This index is stable within a session and gives a short,
  network-unique ordinal.
- The full `cpuId` is `"<x>,<y>,<z>#<index>"`, e.g. `"12,64,10#2"`.
- The player-facing name from `ICraftingCPU.getName()` is used only for display in
  the Crafting Plan window, never as the key.

Coordinates make the id meaningful and location-stable; the index makes it unique
and orderable even when coordinates are hard to read. Coordinates alone are already
unique within a `networkId` (two CPUs cannot share a block), so the index is a
secondary discriminator, not the sole one.

Add a small helper in `ProfilerBridge`:

```java
public static String cpuId(IGrid grid, Object cpu) {
    if (cpu == null) {
        return "";
    }
    var pos = cpuAnchorPos(cpu); // dimension-independent "x,y,z" of the CPU block
    if (pos == null) {
        return "";
    }
    var index = cpuListIndex(grid, cpu); // position in getCpus(), or -1
    return index < 0 ? pos : pos + "#" + index;
}
```

`cpuAnchorPos` inspects `CraftingCPUCluster` and `AdvCraftingCPU` (the latter via
the existing reflection pattern used for `optionalAdvancedCpu` in
`StatsRequestContext.java:25`). `cpuListIndex` walks
`grid.getCraftingService().getCpus()` and returns the match position for `cpu`, or
`-1` if the grid is unavailable. If neither shape is recognized or the index is
missing, return the bare coordinates (or `""`) so the sample still lands in a
resolvable key instead of failing.

Stability note: coordinates are restart-stable; the `getCpus()` index may shift if
the CPU set or its iteration order changes between sessions, which would orphan old
per-CPU samples for that CPU. The network-level (`cpuId = ""`) fallback keeps
estimates working after such a shift, so this is a quiet degradation, not a break.
Prefer coordinates as the human-meaningful part; treat the index as a tie-breaker.

## Wiring cpuId into the profiler

In `ProfilerBridge`, the `start` / `complete` / `startJob` paths build a
`ProfileKey` from `(networkId, what)`. Replace those with the cpu-aware key:

```java
// before
PROFILER.start(key(networkId, what), scope, normalizeAmount(what, amount), unit, tick);

// after
PROFILER.start(key(networkId, cpuId(scope), what), scope, normalizeAmount(what, amount), unit, tick);
```

`key(networkId, cpuId, what)` is the new three-arg overload from `data-model.md`.
Do the same for `complete` and `startJob`. The `scope` object is unchanged; it
keeps doing its existing job for pending/capacity/stall. The mixin already has
`cluster.getGrid()` (or `cpu.getGrid()`), so pass that grid into `cpuId(grid, scope)`
to compute the coordinates + `getCpus()` index.

`startJob` currently keys accuracy by the final output only
(`ProfilerBridge.java:86`). Capture the CPU there too so per-CPU accuracy is
possible:

```java
ACCURACY.start(key(networkId, cpuId(grid, scope), plan.finalOutput().what()), scope,
        predictedSeconds, knownRows, totalRows, tick, nanoTime);
```

## Client-side CPU resolution

The Crafting Plan window needs the same `cpuId` for the CPU the player selects.
Add a `CraftConfirmMenu` accessor mirroring `CraftingCPUMenuAccessor` (grid + chosen
`cpu`). Derive `cpuId` from that object with the same `ProfilerBridge.cpuId(grid, cpu)`
helper, then request/look up stats with the cpu-aware key and the network-level
fallback. The server's `cpuSummaries` already carry the matching `cpuId` strings, so
the pinned view reuses the same identifier recorded at craft time.

For the "no CPU chosen yet" case, `cpuId` is `""` and the lookup uses the
network-level rate (see `estimation.md`).

## Server must expose the network's CPUs

The per-CPU breakdown and the min-across-CPUs headline require the client to know
every CPU on the plan's grid. The server already has the grid in
`StatsRequestHandler.collect` (`StatsRequestHandler.java:27`), so enumerate it:

```java
var crafting = context.grid() == null ? null : context.grid().getCraftingService();
var cpuList = crafting == null ? List.<ICraftingCPU>of() : crafting.getCpus();
```

For each CPU build `(cpuId, name, coProcessors)` and, for each requested output,
the CPU-specific aggregate `ProfileStats`. Hand that to the snapshot as the
`cpuSummaries` section (`data-model.md`). Use `ICraftingCPU.getName()` /
`getCoProcessors()` for display, and `ProfilerBridge.cpuId(grid, cpu)` for the key.
This is the same cluster object that `trySubmitJob` runs on, so the derived `cpuId`
matches what is recorded at craft time.

## Pinned CPU flows to the server too

"All stats relative to the chosen CPU" must hold for the Ctrl-click detail and chat
as well, not just the plan screen. Today `StatsRequestContext.current` only reads
the CPU from `CraftingCPUMenu` (`StatsRequestContext.java:14`). Extend it to also
read the selected CPU from `CraftConfirmMenu` (the `cpuCycler`'s current selection)
via a new accessor, so `ProfilerBridge.entry(...)` keys by that CPU when present.
When the context CPU is set, the returned `entries` are cpu-specific (with raw
samples) and the client marks any fallback row with `*`.

## Edge cases

- `scope` is `null` (should not happen on a real submission): `cpuId` returns `""`,
  sample is network-level. No exception.
- CPU shape not recognized (future AE2/Advanced AE change): `cpuId` returns `""`,
  graceful degrade to network-level.
- A CPU that was moved after its samples were recorded: old samples keep their old
  `cpuId` and are simply not matched until enough new samples accumulate; the
  network-level fallback covers the gap.

No behavior change for players who never pick a CPU: every sample becomes
`cpuId = ""` and the system behaves exactly like today.
