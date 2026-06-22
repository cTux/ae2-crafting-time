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

## Files

- `scripts/release-matrix.json`: supported build/release rows.
- `scripts/build-all-versions.ps1`: builds all matrix rows into `dist/`.
- `scripts/deploy-changed.ps1`: bumps and optionally uploads changed jars.
- `scripts/test-deploy-changed.ps1`: release automation check.
- `versions/*/build.gradle`: per-loader `distMod` tasks and artifact names.

## Checks

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

For upload work, require `MODRINTH_TOKEN` and/or `CURSEFORGE_TOKEN` only when `-Deploy` is requested.
