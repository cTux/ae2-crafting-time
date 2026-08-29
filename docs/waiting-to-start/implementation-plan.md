# Waiting To Start Implementation Plan

Implement this as one feature commit. Let the commit hook create the PR, then
use required CI as the first Gradle test run.

## Phase 1: Track the first dispatch

1. Add the per-CPU accepted tick and waiting-output set to `CraftProfiler`.
2. Reuse the crafted output keys collected by `ProfilerBridge.startJob` to begin
   waiting state for standard AE2 and AdvancedAE jobs.
3. Remove one output from waiting state in the existing
   `ProfilerBridge.start` path.
4. Clear waiting state with existing finish, cancellation, disable, and load
   cleanup.
5. Add pure-Java tests for acceptance, elapsed ticks, first dispatch, later
   dispatches, unrelated CPUs and keys, replacement, cleanup, invalid input,
   and tick rollback.

## Phase 2: Send bounded waiting state

1. Add requested waiting ticks to `StatsRequestHandler.Response` without
   requiring retained stats.
2. Extend `StatsPacketCodec.Snapshot` and all four `StatsSnapshotS2C` records
   with the waiting map.
3. Bump the Forge channel protocol from `5` to `6`, the Fabric snapshot payload
   ID from `stats_snapshot_v3` to `stats_snapshot_v4`, and both NeoForge
   registrar versions from `4` to `5`.
4. Replace requested waiting values in `ClientStats`; omitted keys must clear
   old values.
5. Extend packet and cache tests for round trips, empty values, stale removal,
   negative durations, oversized maps, and long output IDs.

Changing the snapshot layout affects every supported loader, so complete these
steps in the same commit.

## Phase 3: Render the state

1. Add English and Ukrainian translations for
   `TTC: Waiting to start: %ss`.
2. Add the smallest `TtcText` formatter for completed whole seconds.
3. In `CraftingStatusTableRendererMixin`, render waiting before delayed and
   estimated TTC, guarded by the current AE2 active/pending amounts.
4. Reuse the existing dark TTC badge and give waiting text one neutral color.
5. Exclude waiting rows from status TTC color and sort estimates in both API
   source sets.
6. Add text and structural/resource tests for the translation keys, badge key,
   renderer precedence, and mixin membership.

## Phase 4: Verify behavior

After the hook-created PR exists:

1. Let required CI run every supported Gradle row and the coverage gate.
2. Check the full warning/error sweep and fix repository-owned warnings.
3. In a development client, start a job whose dependency cannot dispatch yet.
4. Verify the counter survives closing and reopening the status screen.
5. Unblock the dependency and verify the first dispatch permanently replaces
   waiting with normal TTC behavior.
6. Create a later between-batch gap and verify waiting does not return.
7. Repeat finish and cancellation checks, then verify AdvancedAE on its
   supported NeoForge rows.

Done means CI is green, every changed branch is covered, packet limits still
hold, English and Ukrainian keys match, and the four acceptance scenarios from
the spec work in-game.
