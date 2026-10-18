# Codecov recovery plan

Scope and status: [spec](spec.md). Evidence and boundaries: [design](technical-design.md).

1. Deliver these documents through the documentation PR before implementation.
   Preserve the original issue report and append the follow-up below so it
   reflects the later outage guard.
2. Rerun the failed job with `gh run rerun 37282324288 --failed`. Read back its
   attempt, original head, conclusion, and uploader log. Record unavailable
   reruns or unrelated historical failures without treating them as upload
   success. This supplies AC3 evidence; it does not validate the new head.
3. On the implementation branch, remove the outage comment and unconditional
   `if` from `.github/workflows/test.yml`. Review the full diff and verify AC1
   and AC2 against the workflow, upstream defaults, and unchanged Gradle gates.
4. Follow the repository's commit/hook ordering to create the implementation PR.
   Do not run local tests before that PR exists. The real Tests job exercises
   the changed configuration; no additional local suite or Minecraft smoke is
   required for this scope.
5. On the current implementation head, inspect successful CLI download,
   signature/checksum verification, and upload in the Tests log. Wait for all
   required checks and configured review jobs, then address every actionable
   review finding. This supplies AC4; repeat affected checks if the head changes.
6. Merge only through the repository's normal gates. Reconcile the spec status
   with merged implementation and verification links, then verify issue closure
   and owned-claim cleanup through the issue workflow.

## Issue follow-up

Current follow-up: the Tests workflow now contains a temporary unconditional
skip for Codecov uploads. Restore the upload after confirming service recovery
by removing that skip and its outage comment. Keep OIDC, TLS verification, CLI
signature/checksum verification, JaCoCo thresholds, and upload failure reporting
unchanged. Verify the original reported head through a rerun where feasible,
and verify an actual successful Codecov upload on the implementation PR's
current head before completion.

## Completion evidence

| Criterion | Change or check |
| --- | --- |
| AC1 | Two-line workflow deletion and current-head uploader execution |
| AC2 | Diff review, unchanged thresholds/permissions/inputs, runner integrity logs |
| AC3 | Original run attempt and outcome, or exact rerun limitation evidence |
| AC4 | Current-head upload log, required CI and review, verified merge |

Keep the original rerun result separate from current-head evidence. A green
workflow while Codecov is disabled does not satisfy this plan.
