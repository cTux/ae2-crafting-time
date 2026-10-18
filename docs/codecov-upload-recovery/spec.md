# Restore Codecov uploads

Status: ready-to-implement

Scope: Restore the Tests workflow's required coverage upload after the outage.

Issue: [#656](https://github.com/cTux/ae2-crafting-time/issues/656)

Planning: [Reviewed implementation plan](implementation-plan.md)

## Behavior to restore

After tests and JaCoCo verification pass, the Tests workflow uploads coverage to
Codecov. An upload or CLI integrity failure fails the job. The temporary outage
guard currently skips that entire step, so a green job does not prove an upload.

Remove the unconditional skip and its outage comment. Keep the existing action,
OIDC authentication, TLS and CLI integrity verification, upload failure reporting,
and coverage thresholds. This changes CI only; all Minecraft versions, loaders,
player behavior, and saved data stay compatible.

## Acceptance criteria

- **AC1:** The Codecov step runs after successful checks on pull requests and
  pushes to master, with no unconditional skip.
- **AC2:** OIDC, normal TLS verification, GPG signature and SHA256 verification,
  `fail_ci_if_error: true`, and existing JaCoCo thresholds remain intact.
- **AC3:** Rerun the reported failed job on original head
  `2ccc8625fdf32368b4fec7a241a32f0d7dd90fef` and record the recovery outcome.
  If the rerun is unavailable or blocked by an unrelated historical failure,
  record that limitation and its evidence separately.
- **AC4:** The implementation PR's current head passes required CI and review.
  Its Tests log proves the uploader ran, verified the CLI, and successfully
  uploaded coverage. A skipped step or an older passing head is insufficient.

No new uploader, download mirror, retry infrastructure, or Minecraft smoke
campaign is part of this fix. See the [design](technical-design.md) for evidence
and verification boundaries.
