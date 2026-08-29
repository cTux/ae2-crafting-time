# Repository workflow

- Complete each fix or feature as a separate conventional commit. Let the post-commit hook open or update the PR, then rely on its required CI checks; do not run tests locally before creating the PR.
- Every PR gets an automatic OpenCode review from `.github/workflows/opencode-review.yml` (`opencode/hy3-free`, `prompt: /review-pr`), posting inline findings via `GITHUB_TOKEN`. Re-run it on demand with a `/oc review` comment. It needs the `OPENCODE_API_KEY` repo secret.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-git.ps1` once per clone. The tracked post-commit hook pushes every commit and creates the branch PR automatically.
- Work on a branch. Do not commit fixes or features on a detached HEAD. Branch from
  `origin/master`, not another in-flight feature branch, or the auto-PR hook will
  drag in unrelated commits. For branch-base and commit-message pitfalls, use
  `.codex/skills/ae2-crafting-time-pr/SKILL.md`.
- Use `.codex/skills/ae2-crafting-time-writing/SKILL.md` for every repo-owned
  document, skill, changelog, translation, issue form, and player-facing text
  field. Keep the voice casual, direct, and technically precise.
- Use `scripts/release-matrix.json` as the release source of truth and `scripts/deploy-changed.ps1 -Deploy` for releases.
- Start releases from a clean branch based on `origin/master`. Do not run `build-all-versions.ps1` first: the deploy script builds the complete current JAR set itself.
- On Windows, load `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` from user-scoped environment variables into the current process without printing them; Codex may not inherit variables added after it started.
- In a managed Codex environment, check `gh auth status` and user-scoped release-token presence with the same elevated sandbox permissions as the real deploy. Treat an in-sandbox authentication or token failure as non-authoritative, and do not report missing credentials until the elevated check also fails. Load the tokens and run the real deploy in the same elevated PowerShell invocation.
- Write Modrinth and CurseForge multipart JSON only through the BOM-free `Write-Json` helper. Never use Windows PowerShell's `Set-Content -Encoding UTF8` for upload metadata because it adds a UTF-8 BOM; keep the no-BOM regression check in `test-deploy-changed.ps1`.
- Run `deploy-changed.ps1 -Deploy -DryRun` immediately before the real deploy. Run `test-deploy-changed.ps1` only when release automation or the matrix changed.
- Write every changelog in the project voice as clear player-facing sentences grouped under `ADDED`, `FIXED`, `IMPROVED`, `DELETED`, or `CHANGED`, omitting empty categories. Never publish raw commit logs as release notes.
- Name GitHub Releases with only the mod version, such as `1.0.5`; keep loader, Minecraft version, artifact, and changelog details in the release body.
- A release is complete only after platform readback and after the hook-created release PR is merged, leaving `origin/master` on the next `modVersion`.
