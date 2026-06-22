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
$env:MODRINTH_TOKEN = "..."
$env:CURSEFORGE_TOKEN = "..."
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-changed.ps1 -Deploy
```

`deploy-changed.ps1` fingerprints only jar inputs: root build files, shared main code, and the matrix row's version main code. Test-only edits do not bump or deploy.

Set `modrinthProjectId` and/or `curseProjectId` in the row to enable that platform. Empty project ids skip that platform.

Check the release script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```
