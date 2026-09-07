# Screenshot UI Scale Implementation Plan

Implement this plan under [#319](https://github.com/cTux/ae2-crafting-time/issues/319).
The planning change does not alter clients or rerun historical screenshots.

## Phase 1: Configure every prepared client

1. Change the generated `options.txt` in `scripts/run-ui-smoke.ps1` from the
   fixed scale to Minecraft automatic GUI scaling.
2. Extend the fake client in `scripts/test-run-ui-smoke.ps1` to require exactly
   one automatic GUI-scale entry alongside the existing accessibility setting.
3. Run `scripts/test-run-ui-smoke.ps1` and confirm every target, profile,
   focused case, and suite still uses the shared options seam.

Completion gate: the shared runner test proves that all four prepared targets
request automatic scaling and rejects a fixed or malformed entry.

## Phase 2: Cover staged named modpacks

1. Update `.codex/skills/launch-prism-test-modpack/SKILL.md` to require
   automatic GUI scale before a screenshot-bearing launch.
2. Add the exact staged-`options.txt` update and readback steps to
   `.codex/skills/launch-prism-test-modpack/references/launch-and-verify.md`.
3. Preserve every unrelated option, normalize duplicate GUI-scale entries to
   one automatic entry, and fail preflight if the staged runtime or readback is
   uncertain.
4. Update `.codex/skills/run-ae2-client-smoke/SKILL.md` to state that the shared
   runner owns the prepared-client setting and the captured effective scale is
   still reviewed.

Completion gate: both smoke skills route screenshot runs to one unambiguous
adaptive-scale rule without changing a managed Prism source instance.

## Phase 3: Tighten evidence validation

1. Extend the screenshot-sidecar checks in `scripts/run-ui-smoke.ps1` and
   `scripts/get-ui-smoke-results.ps1` to require a finite positive effective
   scale, positive scaled dimensions, and GUI containment.
2. Add the smallest matching cases to `scripts/test-run-ui-smoke.ps1` and
   `scripts/test-ui-smoke-results.ps1` for missing or invalid scale data and
   out-of-bounds GUI rectangles.
3. Update `docs/ui-smoke-evidence.md` with the scale, framebuffer, and visual
   comparison fields required in a reviewed report.

Completion gate: local script tests reject invalid configuration and semantic
evidence while accepting a smaller effective scale that is still valid and
contained.

## Phase 4: Verify the live behavior

1. After the implementation PR exists, run the PowerShell script tests changed
   in Phases 1 and 3 and `git diff --check`.
2. Run a changed-scope prepared-client campaign from the PR with
   `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master`.
3. At a fixed maximized framebuffer, capture and inspect at least one prepared
   client before and after the change. Record configured automatic mode,
   effective scale, scaled dimensions, GUI bounds, and required UI visibility.
4. Run one eligible named modpack through the updated Prism workflow and record
   the same before-and-after fields without modifying its managed source
   instance.
5. Confirm both after images use a larger effective scale when their viewport
   permits it, show less unused space, and keep every required screen, tooltip,
   item cell, button, and text region visible.
6. Archive the new evidence under a fresh run ID and confirm both exact clients
   are stopped.

Completion gate: retained prepared-client and named-modpack evidence satisfies
SUI-A4 through SUI-A6, with any unsupported larger scale explained by the
captured viewport rather than guessed.

## Phase 5: Reconcile and finish

1. Map SUI-A1 through SUI-A6 to the implementation diff, script-test output,
   semantic sidecars, and reviewed screenshots.
2. Confirm no production source, packet, save data, translation, historical
   archive, normal Prism instance, or global Prism setting changed.
3. Require green GitHub checks and resolve every blocking review finding before
   merge.

## Acceptance coverage

| Acceptance criterion | Implementation and evidence |
| --- | --- |
| SUI-A1 | Phase 1 shared runner change and all-target fake-client test |
| SUI-A2 | Phase 2 staged-instance workflow, option readback, and source-instance boundary |
| SUI-A3 | Phase 1 fixed, missing, duplicate, and malformed option failures |
| SUI-A4 | Phase 3 sidecar validation and script-test cases |
| SUI-A5 | Phase 4 same-framebuffer prepared-client and named-modpack comparisons |
| SUI-A6 | Phase 4 semantic containment plus visual review of every required region |

Done means all acceptance criteria have current evidence, every supported path
uses the shared behavior assigned to it, and the implementation PR is merged.
