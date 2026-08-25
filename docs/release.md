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

Normal builds use the tracked `modVersion` in `gradle.properties`. That is the
next release version, so every commit and every loader-explicit jar uses the
same development version.

Build only changed jars at the current development version:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1
```

Release changed jars to Modrinth, CurseForge, and GitHub:

```powershell
$env:RELEASE_TYPE = "release"
$env:MODRINTH_TOKEN = "..."
$env:CURSEFORGE_TOKEN = "..."
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1 -Deploy
```

`deploy-changed.ps1` fingerprints only jar inputs: root build files, shared main code, and the matrix row's version main code. Test-only edits do not build or deploy.

For every affected row, `-Deploy` publishes the current `modVersion`, builds the loader-explicit jar, generates that jar's changelog from commits since its previous release, and uploads the jar plus changelog to both Modrinth and CurseForge. Modrinth version numbers include the matrix row id because version numbers must be unique across the whole project. CurseForge uploads include the project's Client and Server environment versions. It also rebuilds the unchanged rows at their current released versions so the GitHub Release always attaches the complete latest supported JAR set. The release title and body list only affected versions and their per-jar changelogs. After every successful deploy, it bumps `modVersion` to the next patch and commits `gradle.properties` together with `.release-state.json`; every later commit and normal build then belongs to that new version. The repository's post-commit hook pushes that commit and creates the branch PR when needed.

Run `scripts/setup-git.ps1` once after cloning. It installs the tracked post-commit hook, which automatically pushes every fix, feature, and release commit and creates one PR per branch. Existing PRs are reused, so later commits update them without duplicates. Work on a branch: the hook intentionally refuses to push a detached HEAD. GitHub CLI must be installed and authenticated for PR creation.

Release metadata can come from the matrix row or from environment overrides. `RELEASE_TYPE`, `MODRINTH_PROJECT_ID`, and `CURSEFORGE_PROJECT_ID` override the row values for all entries in the current run. `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` are required only for a real `-Deploy`; GitHub CLI must also be authenticated.

`-Deploy` fails fast unless both platform project ids resolve for every affected row. The current Modrinth and CurseForge project ids are stored per row in the release matrix.

Check the release script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```
