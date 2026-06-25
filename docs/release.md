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

Upload changed jars:

```powershell
$env:RELEASE_TYPE = "release"
$env:MODRINTH_PROJECT_ID = "..."
$env:CURSEFORGE_PROJECT_ID = "..."
$env:MODRINTH_TOKEN = "..."
$env:CURSEFORGE_TOKEN = "..."
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1 -Deploy
```

`deploy-changed.ps1` fingerprints only jar inputs: root build files, shared main code, and the matrix row's version main code. Test-only edits do not bump or deploy.

Release metadata can come from the matrix row or from environment overrides. `RELEASE_TYPE`, `MODRINTH_PROJECT_ID`, and `CURSEFORGE_PROJECT_ID` override the row values for all entries in the current run.

`-Deploy` now fails fast if no publish target resolves for a row instead of silently skipping every upload.

Check the release script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```
