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

The identifier is **config-derived**, not location-derived:

- The dominant factor in AE2 crafting speed is the CPU's co-processor count
  (parallel throughput), so the base `cpuId` is that count as a string, e.g.
  `"4"`.
- Optionally extend it with a hash of the CPU's attached crafting machines (pattern
  providers / the machines they drive). This separates same-co-proc CPUs that feed
  genuinely different machines.
- The full `cpuId` is `"<coProcessors>"` or `"<coProcessors>-<machineHash>"`,
  e.g. `"4"` or `"4-a1b2"`.
- The player-facing name from `ICraftingCPU.getName()` is display-only.

Why not coordinates or the `getCpus()` index: the `cpuId` is the key we persist
learned data under, so it must be stable across restarts. Coordinates change when a
CPU is moved and the `getCpus()` index can reorder between sessions, both of which
would orphan saved per-CPU samples and accuracy. Co-processor count is intrinsic to
the CPU and survives restart, move, and rename. It also matches the reason
CPU-bound stats exist: networks with differently-sized CPUs get separated exactly
where throughput differs, and identical dynamically created CPUs (e.g. a quantum
crafter that spawns bit-identical CPUs) collapse to one id and aggregate correctly.

Add a small helper in `ProfilerBridge`:

```java
public static String cpuId(Object cpu) {
    if (cpu == null) {
        return "";
    }
    var coProcessors = cpuCoProcessors(cpu);   // ICraftingCPU.getCoProcessors()
    if (coProcessors < 0) {
        return "";
    }
    var machineHash = cpuMachineHash(cpu);      // attached machines, or "" if none
    return machineHash.isEmpty() ? Integer.toString(coProcessors)
            : coProcessors + "-" + machineHash;
}
```

`cpuCoProcessors` reads `ICraftingCPU.getCoProcessors()` (works for
`CraftingCPUCluster`; for `AdvCraftingCPU` use the reflection pattern from
`StatsRequestContext.optionalAdvancedCpu`). `cpuMachineHash` walks the CPU's
attached pattern providers / driven machines and hashes a stable descriptor; return
`""` when that is impractical so the id degrades to just the co-processor count. If
the CPU shape is unrecognized, return `""` so the sample lands in network-level
storage instead of failing.

Over-merge note: two same-co-proc CPUs feeding different machines will share an id
if the machine hash is omitted, blending rates that differ. That is acceptable
because the measured accuracy for that shared id drops, and the UI surfaces it (see
`estimation.md`): a low-accuracy id gets a tooltip like "your setup has
differently performant machines". Adding `cpuMachineHash` removes the ambiguity
when it matters.

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
keeps doing its existing job for pending/capacity/stall. The CPU object already
exposes co-processor count, so pass it into `cpuId(scope)` to build the
config-derived id.

`startJob` currently keys accuracy by the final output only
(`ProfilerBridge.java:86`). Capture the CPU there too so per-CPU accuracy is
possible:

```java
ACCURACY.start(key(networkId, cpuId(scope), plan.finalOutput().what()), scope,
        predictedSeconds, knownRows, totalRows, tick, nanoTime);
```

## Client-side CPU resolution

The Crafting Plan window needs the same `cpuId` for the CPU the player selects.
Add a `CraftConfirmMenu` accessor mirroring `CraftingCPUMenuAccessor` (grid + chosen
`cpu`). Confirm the grid is reachable from `CraftConfirmMenu` (it extends
`AEBaseMenu` whose target is an `IActionHost`, so `getTarget()` yields the grid).
Derive `cpuId` from that object with the same `ProfilerBridge.cpuId(cpu)`
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
`getCoProcessors()` for display, and `ProfilerBridge.cpuId(cpu)` for the key.
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
