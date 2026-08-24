# Repository workflow

- Complete each fix or feature as a separate conventional commit after its relevant checks pass.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-git.ps1` once per clone. The tracked post-commit hook pushes every commit and creates the branch PR automatically.
- Work on a branch. Do not commit fixes or features on a detached HEAD.
- Use `scripts/release-matrix.json` as the release source of truth and `scripts/deploy-changed.ps1 -Deploy` for releases.
