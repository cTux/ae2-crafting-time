# No Power Status Technical Design

## Evidence and ownership

Inside `CraftingCpuLogic.executeCrafting`, AE2 calculates the pattern dispatch
cost and calls `IEnergyService.extractAEPower(..., Actionable.SIMULATE, ...)`.
It stops the dispatch when the returned amount is below the required amount.
That comparison is the exact signal for `NO POWER`.

The earlier `cluster.isActive()` guard is not sufficient evidence. A CPU may be
inactive for topology or multiblock reasons, so this feature must not infer a
power failure from that guard or from a stationary status row.

## Shared runtime and transport

Reuse the `CraftingBlockReason` runtime map, 20-tick freshness window, bounded
snapshot map, client cache, compatibility bumps, and row-state resolver defined
by `docs/no-provider-status/technical-design.md`. If this status is implemented
first, introduce that shared two-value foundation once; do not create a second
power-specific packet field.

Record `NO_POWER` by concrete CPU identity and every positive output of the
pattern whose simulated extraction failed. Repeated failures refresh the tick.
Do not clear another pattern's `NO_PROVIDER` observation for the same combined
output. The shared resolver gives `NO_PROVIDER` priority.

## AE2 hooks

Extend the existing execution mixin at the simulated energy-extraction call,
not the later modulating extraction after a successful push. Preserve AE2's
return value exactly. When it is below AE2's own required-power threshold,
report the pattern outputs through `ProfilerBridge` with the network id, CPU
scope, and current game tick.

Use the equivalent verified seam in the optional AdvancedAE pseudo-mixin.
Choose redirect descriptors only after checking each supported artifact. If an
artifact does not expose the same comparison, leave it unchanged instead of
substituting a heuristic.

## Client behavior

`StatsRequestHandler` returns `NO_POWER` only for requested outputs and the
selected CPU while the observation is fresh. `ClientStatsCache` replaces
requested values, so stale power warnings disappear.

`CraftingStatusTableRendererMixin` renders the localized warning and appends
the two tooltip lines. The warning replaces TTC for that refresh, has no TTC
color, and is treated as unknown by TTC sorting.

## Failure handling

- A successful dispatch does not need an explicit clear; an unrefreshed
  observation expires after 20 ticks and disappears on the next snapshot.
- A null grid or CPU returns no blocker.
- Unknown packet values are rejected at the shared compatibility boundary.
- No runtime blocker is persisted.
- AE2's energy extraction and dispatch behavior remains untouched.
