# Release

Each supported Minecraft/mod-loader combination is one row in `scripts/release-matrix.json`.
Do not add a row until the matching Gradle module builds a real jar.

Current rows:

```text
1.20.1-forge -> :mc_1_20_1_forge:distMod -> dist/ae2-crafting-time-1.20.1-Forge-<version>.jar
1.20.1-fabric -> :fabric_1_20_1:distMod -> dist/ae2-crafting-time-1.20.1-Fabric-<version>.jar
1.21.1-neoforge -> :mc_1_21_1_neoforge:distMod -> dist/ae2-crafting-time-1.21.1-NeoForge-<version>.jar
```

Build every matrix row:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

Build only changed jars and bump only those patch versions:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1
```

Release changed jars to Modrinth, CurseForge, and GitHub:

```powershell
$env:RELEASE_TYPE = "release"
$env:MODRINTH_PROJECT_ID = "..." # until each row has its Modrinth project id
$env:MODRINTH_TOKEN = "..."
$env:CURSEFORGE_TOKEN = "..."
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1 -Deploy
```

`deploy-changed.ps1` fingerprints only jar inputs: root build files, shared main code, and the matrix row's version main code. Test-only edits do not bump or deploy.

For every affected row, `-Deploy` bumps its patch version, builds the loader-explicit jar, generates that jar's changelog from commits since its previous release, and uploads the jar plus changelog to both Modrinth and CurseForge. It then creates one GitHub Release whose title lists every affected Minecraft/loader version and whose body contains the per-jar changelogs. Finally, it commits `.release-state.json`; the repository's post-commit hook pushes that commit automatically.

Run `scripts/setup-git.ps1` once after cloning. It installs the tracked post-commit hook, which automatically pushes every fix, feature, and release commit. Work on a branch: the hook intentionally refuses to push a detached HEAD.

Release metadata can come from the matrix row or from environment overrides. `RELEASE_TYPE`, `MODRINTH_PROJECT_ID`, and `CURSEFORGE_PROJECT_ID` override the row values for all entries in the current run. `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` are required only for a real `-Deploy`; GitHub CLI must also be authenticated.

`-Deploy` fails fast unless both platform project ids resolve for every affected row. The Modrinth project does not currently resolve by the repository slug, so set `MODRINTH_PROJECT_ID` until its id is added to the matrix.

Check the release script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```
