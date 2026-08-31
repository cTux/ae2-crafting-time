# Vortex Technical Design

## Existing Evidence

The logical server already records TTC, waiting state, and delayed diagnostics
by concrete crafting CPU scope in `ProfilerBridge` and `CraftProfiler`.
`CraftingCpuLogicMixin` passes the owning `CraftingCPUCluster` into those paths,
and `IGrid.getCraftingService().getCpus()` is the existing AE2 service boundary
for finding current CPUs. The Vortex must reuse that state rather than create a
second profiler.

The existing status screen request depends on `StatsRequestContext.current`, so
it cannot aggregate unrelated grids. Vortex needs its own owner-validated menu
and bounded snapshot packets. The repository currently registers no blocks,
block entities, menus, or entities; loader entrypoints are therefore the
registration seams.

## Content And Ownership

Register two blocks, their block items, two block-entity types, and one Vortex
menu. Do not register a new NPC entity: use a tagged vanilla villager for Tim.

Both block entities store `owner: UUID`. A placement callback requires a
non-null placing player and writes that UUID; placement paths with no player do
not create a usable bound block. Item stacks carry no ownership. The Vortex
block entity also stores `tim: UUID` and its horizontal facing.

The Hub owns an AE2 managed grid node created through the supported
`GridHelper`/managed-node API for its source layer. It exposes no inventory or
menu. Save and restore the grid node using AE2's normal block-entity lifecycle,
and destroy the node on removal. The node uses normal power and channel rules;
only an active node contributes a grid.

## Runtime Hub Registry

Add one server-runtime `VortexHubRegistry`, keyed by owner UUID and
dimension/position. A Hub registers on load and unregisters on unload/removal.
Each query revalidates the block entity, owner, active managed node, and grid,
then deduplicates grids by object identity. This avoids world scans, persistent
indexes, stale cleanup, and chunk loading.

The registry contains no saved state. Loaded block entities and their NBT are
the source of truth, so server stop clears the registry and chunk load rebuilds
it.

## Craft Collection

Add one server-side `VortexCraftCollector` that receives the authenticated
owner UUID and current tick:

```text
owner UUID
  -> VortexHubRegistry.activeGrids(owner)
  -> each grid crafting service and busy CPU
  -> current job final output and remaining output amount
  -> existing per-CPU TTC/status state
  -> bounded VortexCraftRow list
```

Use the public crafting-service/CPU contract where it exposes the job status.
Where AE2 versions do not expose the constituent active, scheduled, or stored
amounts required by the existing TTC resolver, add the smallest accessor in
`mc1201` and `mc2612`; do not duplicate job logic in loader modules.
Already-supported addon CPUs use the same collector when their implementation
is returned by the grid's crafting service. If one needs extra state, extend its
existing optional integration seam instead of adding a second discovery path;
the addon must remain absent safely.

The collector reuses the existing TTC estimator and row-state priority. It
reduces constituent output states to one job state, using the established
priority `NO SPACE`, `NO PROVIDER`, `NO POWER`, `Waiting`, `DELAYED`, then TTC
or `Collecting data`. `NO SPACE`, `NO PROVIDER`, `NO POWER`, and `DELAYED` are
errors; the spec's error set remains authoritative if status work lands in a
different order. Total job TTC is the sum of usable remaining-output estimates,
matching the existing crafting-status total rather than estimating only the
final output.

A row identity is valid only within one open Vortex menu. The menu keeps an
identity map from each live CPU object to an opaque, increasing positive
integer, retaining existing ids and dropping completed CPUs on refresh. Render
rows by that id so unchanged jobs do not shuffle; do not sort server-side by
localized display text.

## Menu, Packets, And Validation

Opening the Vortex creates a server menu only after checking block position,
distance, owner UUID, and that the block is still a Vortex. Non-owners receive
private system feedback and no menu.

Add these packets to every loader transport:

- `VortexSnapshotRequestC2S(containerId)`;
- `VortexSnapshotS2C(containerId, rows)`;
- `LocateVortexProblemC2S(containerId, rowId)`;
- `VortexProblemTargetS2C(containerId, rowId, result, dimensionId?, blockPos?)`,
  where the bounded result is `TARGET`, `NO_TARGET`, or `STALE`.

Reuse the existing per-player request rate limiter and one-second request
cadence. Limit a snapshot to `PacketLimits.MAX_KEYS` rows, validate collection
sizes before allocation, use AE2's key stack codec for the final output, reject
nonpositive amounts and unknown status values, and bound dimension and message
identifiers. A response applies only to the matching open container id; closing
the menu clears its rows.

The server menu keeps the most recent `rowId -> ProblemTarget` map for no more
than two refresh intervals. A locate request must match the player's currently
open owned Vortex menu, a live error row, and that map. The client never sends
coordinates and cannot use the packet to probe arbitrary positions.

Because the packet set is new on every target, bump the current Forge protocol
from `6` to `7`, give the four Fabric packets new `vortex_*_v1` payload IDs, and
bump both current NeoForge registrar versions from `5` to `6` in the same
compatibility-boundary commit. If another approved packet change lands first,
rebase and increment the then-current boundary once instead of reusing a
published value.

## Problem Targets

Extend the existing CPU dispatch observation with an optional verified
`ProblemTarget(dimensionId, blockPos)` for an output and CPU scope. Record only
the block entity that AE2 actually selected or invoked for that pattern. Clear
it on progress, job finish/cancel, profiler disable, runtime reload, provider
unload, or replacement by newer verified evidence.

`DELAYED` can expose its last verified Pattern Provider or addon-owned provider
block while that block still exists. Do not call it the downstream processing
machine unless the dispatch hook identifies that machine directly. Other error
states expose a target only when their own server observation identifies one.
`NO PROVIDER` always has none. The collector never uses the Hub, CPU, nearest
provider, or storage as a substitute.

On a successful locate response, close the client screen, show dimension and
coordinates in private system text, and start a client-local 30-second expiry
when the response arrives. Render an outline only when the current dimension
matches and the target chunk is loaded. Dimension change, expiry, disconnect,
or target replacement clears the outline.

## Vortex Lights

Give the Vortex block a boolean `ERROR` block-state property. Its block entity
queries the shared owner snapshot cache once per second and updates `ERROR`
only when the value changes. The cache is populated once per owner/tick, so
multiple Vortex blocks do not repeat grid enumeration.

Block models select purple or red lens textures from `ERROR`. Use one ordinary
fixed luminance value for both states; vanilla does not provide colored light,
and no custom emissive renderer is needed. The server-owned property makes the
appearance consistent even when no UI is open.

## Tim Lifecycle

Spawn a vanilla villager one block in front of the Vortex center and tag it
with the Vortex owner, dimension, and position in persistent entity data. Set
its untranslated custom name to `Tim Craftsman`, make the name visible, and
enable no-AI, no-gravity, silent, persistent, and invulnerable flags.

Once per second while loaded, the Vortex validates its saved Tim UUID. Missing
Tim is recreated and duplicates pointing to the same Vortex are discarded. A
lightweight server tick enables no-physics and resets the survivor to the anchor
position with zero velocity so an obstructed anchor, pistons, water, and entity
collisions cannot move or harm him. Block removal discards the linked Tim. On
entity load, a Tim whose linked Vortex no longer exists discards itself.

Cancel interaction with only tagged Tim villagers through Fabric's entity-use
callback and the Forge/NeoForge entity-interact events. Return no GUI, trade,
sound, or dialogue. Ordinary villagers remain untouched.

## Source Ownership

- `shared/src/main/java`: immutable Vortex row/status/target values, bounded
  aggregation helpers, row ordering, and pure tests.
- `shared/src/mcCommon/java`: registry, collector, menu behavior, block/entity
  lifecycle shared across Minecraft-facing targets, client snapshot/highlight,
  and common resources.
- `shared/src/mc1201/java` and `shared/src/mc2612/java`: AE2/Minecraft API
  adapters and accessors where signatures differ.
- `versions/<target>`: loader registrations, interaction hooks, packet records,
  payload registration, and loader-specific tests only.

## Failure And Compatibility Boundaries

- An unloaded/disconnected Hub contributes nothing and never loads a chunk.
- A removed or rebound block invalidates its open menu and any locate map.
- A disappeared CPU or completed job is omitted on the next refresh.
- Malformed, stale, cross-container, non-owner, or rate-limited requests return
  no data and perform no locate action.
- A missing machine produces the bounded no-target response and clears stale
  client highlighting.
- Owner UUIDs and Tim linkage are block-entity data only; no profiler saved-data
  or profile-key migration is required.
- Cover all four release-matrix rows and update English and Ukrainian together.
