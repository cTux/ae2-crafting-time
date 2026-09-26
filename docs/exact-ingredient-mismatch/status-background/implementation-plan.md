# Stored variant background implementation plan

Lifecycle and acceptance criteria: [specification](spec.md).
Implementation decisions: [technical design](technical-design.md).

## 1. Fix and cover shared classification

Add the existing status key to `CraftingRowState` badge membership and width
limiting. Extend `CraftingRowStateTest` so the Stored variant assertions fail on
the original code. Keep coverage for existing statuses, tooltip exclusions,
and widths below, at, and above the current limit. Preserve 100% line and branch
coverage for changed shared logic. Review both renderer counterparts to confirm
they consume the shared decisions without copies. Covers B1, B3, and B5.

## 2. Verify text and appearance through existing seams

Check English/Ukrainian components and resources: unchanged translation keys,
gold normal-weight text, and existing shadow behavior. Extend the existing
stored-variant scenario with observed badge bounds for the actual row. Exercise
background On, Off, restored custom color/opacity, and transparent opacity;
reuse the appearance scenario's controls. Check a visible Missing/Recurrent
row, sorting, and diagnosis removal without setting production flags directly.
Update both GuideME locales and the GitHub wiki to describe the shared appearance
behavior where the status is documented. Covers B1-B4.

## 3. Commit and verify the implementation

Use a conventional commit and let the repository hook create the implementation
PR before running repository tests. Require current-head GitHub Build and Tests
checks, including shared coverage. Follow the existing
[change verification workflow](../../../.codex/skills/ae2-crafting-time-dev/references/testing-and-change-workflow.md).

Review changed-scope smoke selection with
`scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`, then run
the selected prepared-client checks using the repository smoke workflow. Ensure
the stored-variant appearance cases run on all four supported targets, sequentially.
Use English runtime assertions/screenshots and static/component checks for the
longer Ukrainian label; do not add a Ukrainian smoke campaign. Review narrow
row layout, item/icon separation, coexistence, and On/Off restoration from actual
captures. Archive results with the tested revision. Covers B1-B5.

## 4. Record completion

Link the implementation PR, CI, and retained visual evidence from issue #565
and the spec. Keep the status in-progress while any required check is pending.
Mark it finished only when all criteria pass; documentation approval alone
does not prove the UI fix. Merge and close through the authorized issue workflow.
