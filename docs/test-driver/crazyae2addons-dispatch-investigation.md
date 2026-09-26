# CrazyAE2Addons CPU fixture dispatch mismatch

Tracking: [#507](https://github.com/cTux/ae2-crafting-time/issues/507).

The issue records one failure of `crazyae2addons-cpu` in the prepared Forge
1.20.1 compatible suite. Repeatability and root cause remain unconfirmed.

## Retained observation

Source: `9afdd6b0228c5c77eef2bae6ecb149cb799fde8d`.

Invocation: `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master`.
That historical selection depends on its base; retain the actual scenario and
graph when reproducing instead of assuming today's changed-file selection matches.

Campaign: `20260922T201206763Z-d068777e`, with the scenario's `result.json`
and `failure.png` under `1.20.1-forge/primary/run/evidence/crazyae2addons-cpu/`.

The failure at `ADDON_CRAFT_SUBMITTED` recorded expected=1, dispatched=1,
returned=0, starts=1, finishes=1, success=true, fastPathCrafts=0, and scope type
`appeng.me.cluster.implementations.CraftingCPUCluster`. Acceptance/completion,
sample, and TTC checks failed with a fixture dispatch-path mismatch.
The suite stopped; later cases are unrun, not passing.

Earlier `standard-plan-controls` and `recurrent-plan` passed. The report
identifies this as separate from Recurrent rendering in
[PR #506](https://github.com/cTux/ae2-crafting-time/pull/506).

## Investigation and acceptance

1. Resolve the recorded compatible graph and exact addon/loader identities.
   Repeat the focused scenario and retain logs, assertion snapshots, and images.
2. Trace the fixture's CPU selection, craft submission, dispatch/return accounting,
   job completion, profile sample, and subsequent TTC lookup. Confirm whether the
   selected CPU is actually the addon path and when each snapshot is captured.
3. Compare the fixture's expected contract with observed addon behavior. Separate
   setup/timing errors from a real adapter or dispatch regression.
4. Correct the confirmed seam. Never make returned=0 pass merely because a
   separate success flag is true; explain and verify any legitimate alternate path.
5. Run the focused case, then the affected suite continuation. Record later cases
   individually rather than inheriting a pass from the earlier aborted run.

Preserve the old failure evidence. Link the smallest reproducer, before/after
results, source commit, dependency graph, reviewed captures, and current-head CI.
The existing [test-driver specification](spec.md) owns general fixture rules.
A documentation merge does not qualify the addon or close this investigation.
