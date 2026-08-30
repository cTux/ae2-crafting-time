---
name: ae2-crafting-time-release
description: Build or release AE2 Crafting Time distributions. Use for release-matrix rows, artifact naming, deploy scripts, uploads, and release checks.
---

# AE2 Crafting Time Release

## Workflow

1. Read `AGENTS.md` and `docs/release.md`; they own the complete release order.
2. Read [the writing skill](../ae2-crafting-time-writing/SKILL.md) before
   changing changelogs, release notes, or text published to CurseForge,
   Modrinth, or GitHub.
3. Treat `scripts/release-matrix.json` as the source of truth.
4. Keep jar names loader-explicit. Do not add a matrix row until its module
   builds a real jar.
5. Run the release self-check after script or matrix edits.
6. Before retrying an upload, inspect the platform error and confirm whether it
   created a partial version, GitHub release, local state, or version bump.
7. Write multipart JSON only through the script's BOM-free `Write-Json` helper.
   Keep the no-BOM regression check in `test-deploy-changed.ps1`.
8. In a managed environment, check GitHub authentication and user-scoped tokens
   with the same elevated permissions as the real deploy. Treat a sandbox-only
   failure as non-authoritative and never print token values.
9. Before the dry run, read the relevant commit subjects as player copy. If
    automatic conversion would still sound like a commit log, prepare a manual
    categorized `-Changelog` in the project voice.
10. In the final report, immediately after the completed-work list, list every
    versioned JAR deployed to CurseForge or Modrinth with its changelog. When
    both services received the same JAR and changelog, list the file once, name
    both services, and show the changelog once. Split entries only when the
    services received different changelogs.

## Checks

After changing the deploy script or matrix:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```

After changing Gradle release tasks or artifact naming:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

For upload work, require both platform tokens only for a real `-Deploy`. Follow
the secure Windows token-loading path in `docs/release.md` without printing
values.
