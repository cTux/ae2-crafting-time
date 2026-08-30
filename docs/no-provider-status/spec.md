# No Provider Status

Issue: [#120](https://github.com/cTux/ae2-crafting-time/issues/120)

## Goal

Show `NO PROVIDER` on a crafting-status row when AE2 still has scheduled work
for that output but no connected Pattern Provider currently offers the planned
pattern.

## Player behavior

- Show `NO PROVIDER` only after the server observes an actual dispatch attempt
  whose provider lookup is empty.
- Treat a removed pattern, disconnected or unpowered provider, lost channel,
  network split, or unloaded provider the same. The status reports the verified
  missing-provider fact, not an unverified root cause.
- Do not show the status when any connected provider still offers that exact
  pattern.
- Allow the status while some batches of the same output are active if other
  scheduled batches have no provider.
- Refresh through the existing one-second status request cycle. After a
  provider returns, allow the previous status to remain for at most one refresh.
- Render the row as bold red `NO PROVIDER` / `Без провайдера`.
- Add these tooltip lines:
  - `No connected Pattern Provider currently offers this pattern.`
  - `Restore a connected provider or put the pattern back.`
- Give `NO PROVIDER` priority over `NO POWER`, `Waiting`, `DELAYED`, TTC, and
  `Collecting data` for the same row.

## Compatibility

- Support every row in `scripts/release-matrix.json`.
- Keep the logical server authoritative in singleplayer and multiplayer.
- Apply the same lifecycle to standard AE2 CPUs and AdvancedAE Quantum CPUs
  where the optional integration exposes the verified dispatch seam.
- Update English and Ukrainian together.
- Keep request keys, collection sizes, and decoded status values bounded.

## Not included

- Naming which block, cable, channel, chunk, or player removed the provider.
- Repairing the network or restoring a pattern automatically.
- Showing this status in the craft plan, Crafting Tree, or ME Requester.
- Persistence across world reloads.
- A config option or a new screen.

## Acceptance criteria

- Removing the only provider or its pattern makes the affected scheduled row
  show `NO PROVIDER` after the next status refresh.
- A second connected provider offering the same pattern prevents the status.
- Restoring a provider removes the status within one refresh.
- One blocked pattern can mark its combined output row even when another batch
  of that output is active.
- Finish, cancellation, disable, and runtime reload clear the state.
- Unknown or stale server status values never crash the client.
- The visible status and tooltip fit and read naturally in English and
  Ukrainian on every supported target.
- New executable branches and packet boundaries have full line and branch
  coverage.
