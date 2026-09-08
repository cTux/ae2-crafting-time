# NeoForge Suite Shutdown Implementation Plan

Implement this plan under [#362](https://github.com/cTux/ae2-crafting-time/issues/362).

## Phase 1: Reuse final fixture cleanup

1. Represent whether a completed schema-2 suite should continue or stop after
   the existing reset phase.
2. Route the final passing case through the current server-thread fixture
   restore and two-tick wait.
3. Request normal Minecraft shutdown after cleanup instead of constructing a
   next scenario.
4. Leave schema-1 and single-case completion unchanged.

Completion gate: no passing schema-2 path can stop with the last case's fixture
mutations still loaded.

## Phase 2: Lock the transition behavior

1. Extend the closest `SuitePlanTest` cases to cover intermediate continuation
   and final cleanup-before-stop decisions.
2. Review the shared runtime call chain and both supported shared-driver targets.

Completion gate: the regression check fails if final schema-2 completion can
bypass cleanup.

## Phase 3: Verify the real shutdown

1. After the implementation PR exists, run the repository-required unit and
   coverage checks through GitHub CI.
2. Run the full compatible `1.21.1-neoforge` prepared-client suite in CodexVM.
3. Confirm every planned case passes, the exact client PID exits normally, no
   forced termination occurs, and the retained log has no fatal signature.
4. Archive the semantic and visual evidence with phase timings.

Completion gate: the campaign satisfies NSS-A4 at the implementation head.

## Phase 4: Reconcile and finish

1. Review the implementation diff against NSS-A1 through NSS-A4.
2. Require green current-head CI and no blocking review or merge conflict.
3. Merge the implementation PR, verify issue closure, and remove `in/progress`.

Done means the final shared-world fixture is restored before shutdown and the
previously failing NeoForge suite exits normally with retained evidence.
