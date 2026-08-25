# Release

Each supported Minecraft/mod-loader combination is one row in `scripts/release-matrix.json`.
Do not add a row until the matching Gradle module builds a real jar.

Current rows:

```text
1.20.1-forge -> :mc_1_20_1_forge:distMod -> dist/ae2-crafting-time-<version>-forge_1.20.1.jar
1.20.1-fabric -> :fabric_1_20_1:distMod -> dist/ae2-crafting-time-<version>-fabric_1.20.1.jar
1.21.1-neoforge -> :mc_1_21_1_neoforge:distMod -> dist/ae2-crafting-time-<version>-neoforge_1.21.1.jar
```

Every uploaded file follows `<mod-name>-<loader-specific-mod-version>-<mod-loader>_<minecraft-version>.jar`.
The loader id is lowercase and matches the release matrix.

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

## Fast release and deploy

Start from a clean branch based on `origin/master`. The real deploy builds every
JAR needed by the GitHub Release, so do not run `build-all-versions.ps1` first.

Windows user environment variables are not automatically added to an already
running Codex process. Load the existing user-scoped secrets into the current
PowerShell process without printing them, run the cheap dry run, then deploy:

```powershell
$env:MODRINTH_TOKEN = [Environment]::GetEnvironmentVariable("MODRINTH_TOKEN", "User")
$env:CURSEFORGE_TOKEN = [Environment]::GetEnvironmentVariable("CURSEFORGE_TOKEN", "User")
if ([string]::IsNullOrWhiteSpace($env:MODRINTH_TOKEN)) { throw "MODRINTH_TOKEN is missing at user scope" }
if ([string]::IsNullOrWhiteSpace($env:CURSEFORGE_TOKEN)) { throw "CURSEFORGE_TOKEN is missing at user scope" }
$env:RELEASE_TYPE = "release"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1 -Deploy -DryRun
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1 -Deploy
```

The dry run validates the release plan without building or uploading. The real
deploy builds changed rows at `modVersion`, rebuilds unchanged rows at their
latest released versions for the complete GitHub asset set, uploads changed
rows, creates the GitHub Release, bumps `modVersion`, commits the release state,
and pushes the branch.

After it succeeds:

1. Verify the GitHub Release contains one loader-explicit JAR for every matrix row.
2. Verify each new Modrinth version is listed with the expected loader and game version.
3. Confirm CurseForge accepted every upload. Public visibility can lag, and the author upload token may not be authorized for the public files read endpoint.
4. Merge the hook-created release PR and verify `origin/master` contains the next patch `modVersion`.

If an upload fails, use the response body printed by the script. Before retrying,
check the affected platform, GitHub releases, `.release-state.json`, and the
working tree for partial completion; retry only when the rejected version was
not created.

`deploy-changed.ps1` fingerprints only jar inputs: root build files, shared main code, and the matrix row's version main code. Test-only edits do not build or deploy.

For every affected row, `-Deploy` publishes the current `modVersion`, builds the loader-explicit jar, generates that jar's changelog from commits since its previous release, and uploads the jar plus changelog to both Modrinth and CurseForge. Modrinth version numbers include the matrix row id because version numbers must be unique across the whole project. CurseForge uploads include the project's Client and Server environment versions. It also rebuilds the unchanged rows at their current released versions so the GitHub Release always attaches the complete latest supported JAR set. The release title and body list only affected versions and their per-jar changelogs. After every successful deploy, it bumps `modVersion` to the next patch and commits `gradle.properties` together with `.release-state.json`; every later commit and normal build then belongs to that new version. The repository's post-commit hook pushes that commit and creates the branch PR when needed.

Run `scripts/setup-git.ps1` once after cloning. It installs the tracked post-commit hook, which automatically pushes every fix, feature, and release commit and creates one PR per branch. Existing PRs are reused, so later commits update them without duplicates. Work on a branch: the hook intentionally refuses to push a detached HEAD. GitHub CLI must be installed and authenticated for PR creation.

Release metadata can come from the matrix row or from environment overrides. `RELEASE_TYPE`, `MODRINTH_PROJECT_ID`, and `CURSEFORGE_PROJECT_ID` override the row values for all entries in the current run. `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` are required only for a real `-Deploy`; GitHub CLI must also be authenticated.

`-Deploy` fails fast unless both platform project ids resolve for every affected row. The current Modrinth and CurseForge project ids are stored per row in the release matrix.

Check the release script after changing `deploy-changed.ps1` or the release matrix:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```
