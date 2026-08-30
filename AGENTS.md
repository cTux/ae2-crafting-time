# Repository Workflow

- Work on a branch. Complete each fix or feature as one conventional commit.
- Run `scripts/setup-git.ps1` once per clone. Its post-commit hook pushes each
  commit and creates or updates the branch PR.
- Do not run local tests before the hook creates the PR. Afterward, run only the
  checks required by the applicable skill and report GitHub CI separately.
- Every PR gets the automatic OpenCode review in
  `.github/workflows/opencode-review.yml`. Re-run it with `/oc review` when
  needed; the workflow requires the `OPENCODE_API_KEY` repository secret.
- Let the discovered repository skills own development, writing, release, issue,
  and Prism workflows. Do not duplicate their detailed rules here.
