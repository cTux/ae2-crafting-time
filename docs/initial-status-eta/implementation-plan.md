# Initial Crafting Status ETA Implementation Plan

Lifecycle: see the [scope status and evidence](spec.md).

Tracks [issue #425](https://github.com/cTux/ae2-crafting-time/issues/425).
The #350 helper already exists; this follow-up corrects its screen scope and
the separate 26.1.2 title hook.

1. Retain #425's direct-screen failure evidence as a defect observation. Inspect
   both screen paths and the native title flow independently for the mc1201 and
   mc2612 adapters. Record 26.1.2 evidence separately from the Forge observation.
2. Reuse `TimeEstimate.hasMeasuredProgress` in both existing
   `CraftingCPUScreenMixin` title hooks. Remove the mc1201 terminal-only guard
   and add the missing mc2612 guard. Restore the zero-progress title through
   `getGuiDisplayName(GuiText.CraftingStatus.text())`, preserving custom names
   and the red can't-store warning. Leave measured-progress ETA, null-status
   titles, and the separate learned TTC behavior intact.
3. Keep `TimeEstimateTest` for executing equal, progressed, and reversed-counter
   decisions. Add a focused ASM regression for both compiled hooks checking
   only structure: no terminal-only type gate, `hasMeasuredProgress` and
   `getGuiDisplayName` calls, and warning/copy operations. Review null-status
   handling in source; do not claim it was exercised without a recorded test.
4. Review the diff and create the implementation PR through the repository
   commit hook before running any local tests, including the red demonstration
   against the original hooks. After the PR exists, demonstrate that regression
   fails against the original hooks and passes against the correction, then run
   `./gradlew test jacocoTestReport build`. Report current-head GitHub CI
   separately and preserve the 100% shared line and branch coverage gate.
5. Review `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`,
   archive the selection, and execute the changed-plan checks through the
   prepared-client workflow. Ensure `no-channel-status` runs on 1.20.1 Forge,
   1.20.1 Fabric, 1.21.1 NeoForge, and 26.1.2 NeoForge even if it needs an
   explicit focused selection.
6. Inspect zero-progress titles in the new `no-channel-status` captures. Record
   direct and terminal screens and a custom-name zero-progress case on each
   target; add manual prepared-client captures if named scenarios omit them.
   Use current-source client evidence to verify live behavior and component/name
   preservation. Inspect `standard-status-controls` and `no-space-status` for
   measured-progress ETA, red can't-store text, and learned TTC placement.
   Retain source-bound screenshots and raw failures; distinguish structural
   assertions from runtime/visual proof and old defects from fix verification.
7. Review the final diff and required current-head CI. Complete the authorized
   merge and verify #425 closes only after the required checks and visual
   review pass; report any remaining blocker explicitly. #350 is already closed.
