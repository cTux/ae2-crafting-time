# Repository workflow

- Complete each fix or feature as a separate conventional commit after its relevant checks pass.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-git.ps1` once per clone. The tracked post-commit hook pushes every commit and creates the branch PR automatically.
- Work on a branch. Do not commit fixes or features on a detached HEAD.
- Use `scripts/release-matrix.json` as the release source of truth and `scripts/deploy-changed.ps1 -Deploy` for releases.
- Start releases from a clean branch based on `origin/master`. Do not run `build-all-versions.ps1` first: the deploy script builds the complete current JAR set itself.
- On Windows, load `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` from user-scoped environment variables into the current process without printing them; Codex may not inherit variables added after it started.
- Run `deploy-changed.ps1 -Deploy -DryRun` immediately before the real deploy. Run `test-deploy-changed.ps1` only when release automation or the matrix changed.
- A release is complete only after platform readback and after the hook-created release PR is merged, leaving `origin/master` on the next `modVersion`.
