# NeoForge Early-Display Config Smoke Implementation Plan

Implement this plan after the documentation PR merges. It restores the behavior
defined in [the spec](spec.md) for
[#357](https://github.com/cTux/ae2-crafting-time/issues/357).

## 1. Preserve and isolate loader config

1. Add the smallest helper in `prepare-ui-smoke-launch.ps1` that, for NeoForge
   only, copies an existing runtime `config/fml.toml` to graph-local evidence,
   records absence otherwise, then removes only the runtime file.
2. After native process completion in `run-ui-smoke.ps1`, copy the generated
   `fml.toml` to graph-local evidence or record absence.
3. Keep loader config evidence out of semantic PASS decisions; startup, cases,
   visual review, archive, and exact-process exit remain authoritative.

Completion gate: each NeoForge graph has isolated loader config and retained
before/after state without editing a prepared installation.

## 2. Lock the script boundary

1. Extend the closest PowerShell self-test with existing-file and missing-file
   cases.
2. Prove byte-exact pre-launch capture, targeted removal, post-launch capture,
   absence markers, and no behavior on Forge/Fabric.
3. Run the focused self-test after the implementation PR exists, following the
   repository's commit-before-test rule.

Completion gate: every new script branch has a deterministic runnable check.

## 3. Verify the real NeoForge path

1. Run the NeoForge 1.21.1 primary graph with the exact compatible dependency
   set through CodexVM and retain the new campaign separately from the original
   failure.
2. Confirm the pre-launch state, FML `earlyWindowSquir` post-launch state,
   32/32 semantic results, required visual evidence, and normal native exit.
3. Run the same primary graph again in a new campaign. Confirm it again begins
   without inherited `fml.toml` and reaches the same result.
4. Review every `REVIEW_REQUIRED` checkpoint and archive both attempts under the
   existing evidence contract.

Done means ED-01 through ED-05 and every acceptance criterion are supported by
retained evidence; the 2026-09-08 failure remains failed.
