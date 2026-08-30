# Waiting To Start Spec

## Goal

In AE2's crafting-status screen, show how long a scheduled output has waited
for its first pattern dispatch.

Example:

```text
Waiting to start: 12s
```

This covers [discussion #65](https://github.com/cTux/ae2-crafting-time/discussions/65).

## Player behavior

- Show the waiting line when an output still has scheduled work, has no active
  work, and the selected crafting CPU has never dispatched that output during
  its current job.
- Start the timer when AE2 accepts the job, not when the player opens the
  crafting-status screen.
- Stop showing the waiting line after the output's first successful pattern
  dispatch. It must not return during a later gap between batches.
- After the first dispatch, show the existing TTC or delayed state when its
  current rules apply.
- Show the waiting line even when the output has no retained throughput samples.
- Refresh the displayed whole-second value through the existing one-second stats
  request cycle.
- Before the first stats response arrives, keep the current behavior and show no
  waiting line. The first response may show `0s`.
- Give the waiting line its own neutral style. It does not join the green-to-red
  TTC color scale.
- Treat a waiting row as unknown in shortest/longest TTC sorting because it has
  not started and has no current completion estimate.

## State rules

`Waiting to start` means all of these are true:

1. AE2 accepted the current job on the selected crafting CPU.
2. The accepted plan includes the output.
3. No pattern producing that output has been dispatched by that CPU yet.
4. AE2 reports `activeAmount == 0` and `pendingAmount > 0` for the row.

The duration is the nonnegative number of completed seconds since job
acceptance. A newly accepted job may show `0s`.

The state is runtime-only. Clear it when the job finishes, is cancelled, the
profiler is disabled, or runtime state is reloaded. Do not save it to world NBT.
A suspended job still accumulates waiting time; AE2 already shows suspension as
a separate job-level state.

## Compatibility

- Support 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge, and 26.1.2 NeoForge.
- Use the same server-owned request/response path in singleplayer and on a
  dedicated server.
- Keep AdvancedAE on the same lifecycle contract as standard AE2 CPUs.
- Update English and Ukrainian text together.
- Keep request keys, collection sizes, and decoded durations bounded and
  validated.

## Not included

- A queue-time prediction.
- A new config option or screen.
- Persistence across a server restart.
- Waiting badges in the craft plan, Crafting Tree, or ME Requester.
- A new warning threshold or bottleneck recommendation.

## Acceptance criteria

- The first stats response for a pending-only row may show `0s`, then the value
  increases without reopening the screen.
- Reopening the screen keeps the server-owned elapsed value.
- A row with old throughput samples still shows waiting before its first
  dispatch.
- A row without throughput samples also shows waiting.
- The first dispatch removes the waiting line permanently for that job.
- Later gaps between batches do not bring the line back.
- Finish, cancellation, disable, and runtime reload remove the state.
- Waiting rows remain in stable AE2 order among rows without a sortable TTC.
- Packet round trips and all state branches have full test coverage.
