---
name: ae2-crafting-time-release
description: Work on AE2 Crafting Time build, release, distribution, and deployment automation. Use when changing release-matrix rows, Gradle dist tasks, jar naming, run wrappers, deploy scripts, platform upload metadata, or release checks.
---

# AE2 Crafting Time Release

## Workflow

1. Read `docs/release.md`.
2. Read [the writing skill](../ae2-crafting-time-writing/SKILL.md) before
   changing changelogs, release notes, or text published to CurseForge,
   Modrinth, or GitHub.
3. Treat `scripts/release-matrix.json` as the source of truth.
4. Confirm Gradle project names with `.\gradlew.bat projects`.
5. Keep jar names loader-explicit.
6. Do not add a matrix row until the matching module builds a real jar.
7. Run the release script self-check after script or matrix edits.
8. For a normal release, start from a clean branch based on `origin/master`, run one `-Deploy -DryRun`, then one real `-Deploy`. Do not pre-run `build-all-versions.ps1`; deploy already builds the complete GitHub asset set.
9. On Windows, if tokens are absent from the process, load `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` from user scope with `[Environment]::GetEnvironmentVariable(..., "User")`. Never print token values.
10. After deploy, verify GitHub assets and Modrinth versions, confirm CurseForge accepted each upload, merge the hook-created release PR, and verify `origin/master` has the next patch `modVersion`.
11. Before retrying a failed upload, inspect the printed platform error and confirm no partial version, GitHub release, local state, or version bump was created.
12. Before the dry run, read the relevant commit subjects as player copy. If
    automatic conversion would still sound like a commit log, prepare a manual
    categorized `-Changelog` in the project voice.
13. Write changelogs as natural player-facing sentences grouped under `ADDED`, `FIXED`, `IMPROVED`, `DELETED`, or `CHANGED`. Omit empty categories and never publish raw commit logs.
14. When a change applies to every supported version, open the changelog with an `All versions:` line that lists those common changes first. After the common block, list any atomic changes for a specific version under that version's label (for example `1.20.1:`) only when such per-version changes exist.
15. Keep CurseForge and Modrinth descriptions casual and useful: say what the mod helps with, name important compatibility details, and skip hype. When a task changes either live description, read the published page back before calling it done.
16. Name GitHub Releases with only the mod version, such as `1.0.5`; keep loader, Minecraft version, artifact, and changelog details in the body.
17. In the final report, immediately after the completed-work list, list every
    versioned JAR deployed to CurseForge or Modrinth with its changelog. When
    both services received the same JAR and changelog, list the file once, name
    both services, and show the changelog once. Split entries only when the
    services received different changelogs.

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
