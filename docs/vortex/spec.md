# Vortex Spec

Issue: [#141](https://github.com/cTux/ae2-crafting-time/issues/141)

## Goal

Let one player monitor every current craft on all of their connected ME
networks from one owner-only block, see an immediate visual warning when any
craft has an error, and locate a known problem machine.

## Vortex Hub

- The Vortex Hub is a placeable block and item.
- Placement binds it to the placing player's UUID. Placement without a player
  does not create a usable Hub.
- It joins an adjacent ME network as a normal active AE2 device, including the
  network's channel and power rules.
- It has no inventory or menu. Using it does nothing.
- While its chunk is loaded and its grid node is active, it makes that grid's
  current crafting jobs available to Vortex blocks owned by the same player.
- Several Hubs on one grid expose that grid once. A Hub never loads its chunk
  or keeps its grid running.

## Vortex

- The Vortex is a placeable block and item. Placement binds it to the placing
  player's UUID.
- Only its owner can open it. Other players receive private feedback and no
  craft data.
- Its window combines every busy crafting CPU from the owner's active, loaded
  Hubs in every server dimension. It includes a job regardless of who started
  that job.
- Each crafting CPU job is one list item containing only:
  - the final crafting output icon;
  - the output name and amount still to be crafted;
  - the existing TTC label showing its current status or remaining time.
- Duplicate Hubs do not duplicate jobs. Rows remain stable between refreshes
  and refresh at most once per second while the window is open.
- No connected current crafts produces an empty-state message, not a stale
  list.

## Status And Lights

- The normal Vortex light texture is purple.
- If any visible current craft has `DELAYED`, `NO PROVIDER`, `NO POWER`, or
  `NO SPACE`, every loaded Vortex owned by that player changes its light texture
  to red within one second.
- `Waiting` and `No data yet` are not errors and leave the lights purple.
- The red state returns to purple within one second after the last error clears.
- The colored lens texture uses ordinary fixed Minecraft block luminance;
  colored dynamic lighting and a custom emissive renderer are not required.
- A row uses the existing TTC status priority and formatting. If several
  constituent outputs are blocked, the highest-priority visible error is the
  job status.

## Locating A Problem

- Clicking an error row asks the logical server for the currently verified
  problem target.
- When an exact machine is known, the window closes, private feedback names its
  dimension and coordinates, and the client highlights it for 30 seconds when
  it is in the player's current dimension and loaded.
- When the target is in another dimension, the coordinates remain visible but
  no through-dimension outline is rendered.
- When no exact machine exists or can be verified, the window stays open and
  explains that there is no locatable target. `NO PROVIDER` is the standard
  example because no provider machine exists.
- The server never guesses a target from the nearest machine, cable, storage,
  or Hub.

## Tim Craftsman

- Each placed Vortex owns exactly one vanilla villager presentation named
  `Tim Craftsman`, positioned one block in front of the Vortex.
- Tim has no AI, is silent, persistent, invulnerable, cannot be moved, and has
  no interaction result when clicked.
- The Vortex restores its Tim if he is missing while the block is loaded and
  removes him when the block is removed.
- Multiple Vortex blocks create one Tim each; they do not share an NPC.

## Ownership And Persistence

- Hub and Vortex block entities persist the owner's UUID. The owner is not
  copied to the dropped item, so replacing either block binds it to the new
  placing player.
- A Vortex persists the UUID of its linked Tim. Tim stores the Vortex dimension
  and position so duplicates and orphaned entities can be rejected.
- Vanilla block-breaking permissions remain authoritative. Ownership restricts
  opening and data access, not who may mine a block.
- The logical server owns Hub discovery, job aggregation, ownership checks,
  error state, problem targets, and NPC lifecycle. The client only renders
  approved snapshots and temporary highlights.

## Compatibility

- Support every row in `scripts/release-matrix.json`: 1.20.1 Forge, 1.20.1
  Fabric, 1.21.1 NeoForge, and 26.1.2 NeoForge.
- Preserve the current server-owned profiler and `networkId + outputId`
  identity.
- Include standard AE2 CPUs and already-supported addon CPUs when they belong
  to a crafting service exposed by a connected Hub; addon absence remains safe.
- Use English and Ukrainian block names, messages, statuses, and empty states.
  The proper name `Tim Craftsman` remains unchanged in every locale.
- Bound snapshot rows, identifiers, and strings at every packet boundary.

## Not Included

- Chunk loading, offline Hub data, or keeping an unloaded ME network active.
- Starting, cancelling, reprioritizing, or repairing crafts from the Vortex.
- Opening the Vortex Hub or exposing network inventory contents.
- Guessing a machine for errors that do not identify one.
- A new entity type, model, renderer, trade, dialogue, profession, or Tim AI.
- Colored-light-engine integration.
- Survival recipes or balance decisions; the registered block items are
  available through the mod's creative-tab placement until recipes are planned
  separately.

## Acceptance Criteria

- A player can place owner-bound Vortex Hub and Vortex blocks on all supported
  targets; the Hub joins an ME grid and has no menu.
- Only the Vortex owner can open it or receive its snapshots.
- One row appears for every busy CPU on every distinct loaded active grid
  exposed by the owner's Hubs, including jobs started by other players and
  supported addon CPUs registered with that grid's crafting service.
- Each row shows the final output icon, name, remaining amount, and existing TTC
  status or time; completed, unloaded, disconnected, and duplicate-grid jobs do
  not remain.
- Any defined error makes the owner's loaded Vortex lights red within one
  second, and clearing the final error restores purple within one second.
- Clicking a locatable error closes the window, reports the verified dimension
  and coordinates, and highlights the loaded same-dimension machine for 30
  seconds. An unlocatable error explains why without inventing a target.
- Each Vortex maintains exactly one stationary, invulnerable, non-interactive
  Tim Craftsman and removes him with the block.
- Ownership, stale-row, forged-packet, oversized-packet, unload/reload,
  cross-dimension, and duplicate-Hub boundaries are covered.
- New executable branches have full line and branch coverage, CI passes for all
  release rows, and the end-to-end behavior is verified in-game.
