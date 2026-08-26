---
name: ae2-crafting-time-release
description: Work on AE2 Crafting Time build, release, distribution, and deployment automation. Use when changing release-matrix rows, Gradle dist tasks, jar naming, run wrappers, deploy scripts, platform upload metadata, or release checks.
---

# AE2 Crafting Time Release

## Workflow

1. Read `docs/release.md`.
2. Treat `scripts/release-matrix.json` as the source of truth.
3. Confirm Gradle project names with `.\gradlew.bat projects`.
4. Keep jar names loader-explicit.
5. Do not add a matrix row until the matching module builds a real jar.
6. Run the release script self-check after script or matrix edits.
7. For a normal release, start from a clean branch based on `origin/master`, run one `-Deploy -DryRun`, then one real `-Deploy`. Do not pre-run `build-all-versions.ps1`; deploy already builds the complete GitHub asset set.
8. On Windows, if tokens are absent from the process, load `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` from user scope with `[Environment]::GetEnvironmentVariable(..., "User")`. Never print token values.
9. After deploy, verify GitHub assets and Modrinth versions, confirm CurseForge accepted each upload, merge the hook-created release PR, and verify `origin/master` has the next patch `modVersion`.
10. Before retrying a failed upload, inspect the printed platform error and confirm no partial version, GitHub release, local state, or version bump was created.
11. Keep changelogs player-facing and grouped under `ADDED`, `FIXED`, `IMPROVED`, `DELETED`, or `CHANGED`; omit empty categories and never publish raw commit logs.
12. Name GitHub Releases with only the mod version, such as `1.0.5`; keep loader, Minecraft version, artifact, and changelog details in the body.

## Files

- `scripts/release-matrix.json`: supported build/release rows.
- `scripts/build-all-versions.ps1`: builds all matrix rows into `dist/`.
- `scripts/deploy-changed.ps1`: bumps and optionally uploads changed jars.
- `scripts/test-deploy-changed.ps1`: release automation check.
- `versions/*/build.gradle`: per-loader `distMod` tasks and artifact names.

## Checks

After changing the deploy script or matrix:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```

After changing Gradle release tasks or artifact naming:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

For upload work, require both `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` only for a real `-Deploy`. Read the copy-paste Windows fast path in `docs/release.md` before deploying.
