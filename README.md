# AE2 Crafting Time

AE2 Crafting Time is a Minecraft mod that records Applied Energistics 2 autocrafting performance on the server and shows time-to-craft hints in AE2 crafting UIs.

It currently targets:

| Minecraft | Loader | Gradle module |
| --- | --- | --- |
| 1.20.1 | Forge | `:mc_1_20_1_forge` |
| 1.20.1 | Fabric | `:fabric_1_20_1` |
| 1.21.1 | NeoForge | `:mc_1_21_1_neoforge` |

## Features

- Profiles completed AE2 craft outputs on the logical server.
- Persists retained samples in the world save as `data/ae2-crafting-time.dat`.
- Sends aggregate stats to clients through request/response packets.
- Adds `TTC` lines, color hints, totals, and sort controls to AE2 craft-plan/status views.
- Reuses shared Java code across loaders where AE2/Minecraft APIs match.

## Build

Use Java 17 for 1.20.1 modules. The 1.21.1 NeoForge module declares Java 21.

```powershell
.\gradlew.bat test
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

Built jars are copied to `dist/`.

## Run

```powershell
.\run-1.20.1-forge.bat
.\run-1.20.1-fabric.bat
.\run-1.21.1-neoforge.bat
```

## Docs

- [Working with this project](docs/working-with-project.md)
- [Release process](docs/release.md)
- [Research notes](docs/research.md)
- [Server-owned stats design](docs/server-client-stats.md)
- [World save persistence](docs/world-save-persistence.md)
- [Time To Craft plan UI](docs/time-to-craft-plan.md)
- [TTC sorting](docs/ttc-sorting.md)
- [TTC colored text](docs/ttc-colored-text.md)
- [MVP plan](docs/mvp-plan.md)

## Codex Skills

Local project skills live in `.codex/skills/`:

- `ae2-crafting-time-dev`: use for feature work, TTC UI changes, profiling bugs, packets, persistence, and docs.
- `ae2-crafting-time-release`: use for release matrix rows, build wrappers, jar names, deploy scripts, and release checks.

## Repo Layout

```text
shared/src/main/java       Pure Java profiling, estimates, cache, and helpers
shared/src/mc1201/java     Shared AE2/Minecraft-facing code for current supported loaders
versions/1.20.1-forge      Forge module
versions/1.20.1-fabric     Fabric module
versions/1.21.1-neoforge   NeoForge module
scripts/                   Build and release automation
docs/                      Design, workflow, and release notes
```
