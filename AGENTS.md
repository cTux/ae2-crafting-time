# Repository Workflow

- Work on a branch. Complete each fix or feature as one conventional commit.
- Rebase only when the user explicitly requests it. A rebase does not require
  a full smoke run; choose verification from the actual changes and task scope.
- Run `scripts/setup-git.ps1` once per clone. Its post-commit hook pushes each
  commit and creates or updates the branch PR.
- Do not run local tests before the hook creates the PR. Afterward, run only the
  checks required by the applicable skill and report GitHub CI separately.
- When work reveals another issue, handle it based on its impact:
  - Fix it as part of the current task when it is related.
  - Fix it when it is unrelated but blocks the current task.
  - When it is unrelated and non-blocking, leave it out of the current change
    and create a detailed GitHub issue for it.
- Let the discovered repository skills own development, writing, release, issue,
  and Prism workflows. Do not duplicate their detailed rules here.
