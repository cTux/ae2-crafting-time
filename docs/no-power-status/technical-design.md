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

Replace the shipped missing-provider set with the approved shared
`CraftingBlockReason` snapshot map, retaining its CPU context and request bounds.
Keep NO PROVIDER exact-pattern revalidation. Power observations use a separate
20-tick lifetime and feed the same map, cache, and row resolver.

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

- A sufficient simulated extraction clears that exact pattern immediately.
  An unrefreshed power observation expires after 20 ticks and disappears on the
  next snapshot.
- A null grid or CPU returns no blocker.
- Unknown packet values are rejected at the shared compatibility boundary.
- No runtime blocker is persisted.
- AE2's energy extraction and dispatch behavior remains untouched.

## Approved implementation update (2026-09-03)

NO PROVIDER has since shipped a bounded missing-output set. Replace that field
with one bounded `outputId -> CraftingBlockReason` map shared by both statuses.
The user approved this additional compatibility change: Forge 8 -> 9, Fabric
stats_snapshot_v6 -> v7, and both NeoForge registrars 7 -> 8. This supersedes
the earlier single-bump assumption; persisted data stays unchanged.

Keep NO PROVIDER's exact-pattern revalidation. Track power failures by CPU and
pattern, and merge fresh positive outputs into the shared map with NO PROVIDER
priority. Clear a pattern on a successful simulated check; otherwise expire its
power observation after 20 ticks. Match AE2's `extracted < required - 0.01`
comparison, verified in every supported AE2 and AdvancedAE artifact. The hook
observes ordinal 0 (SIMULATE), returns its value unchanged, and uses the exact
pattern captured by the existing provider lookup. Never observe MODULATE.
