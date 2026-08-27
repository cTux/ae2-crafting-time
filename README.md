# AE2 Crafting Time

[![Tests](https://github.com/cTux/ae2-crafting-time/actions/workflows/test.yml/badge.svg?branch=master)](https://github.com/cTux/ae2-crafting-time/actions/workflows/test.yml)
[![Coverage](https://codecov.io/gh/cTux/ae2-crafting-time/graph/badge.svg)](https://codecov.io/gh/cTux/ae2-crafting-time)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/LyDZjvxd?logo=modrinth&label=Modrinth%20downloads)](https://modrinth.com/mod/ae2-crafting-time)
[![CurseForge downloads](https://img.shields.io/curseforge/dt/1591476?logo=curseforge&label=CurseForge%20downloads)](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time)
[![Issues fixed](https://img.shields.io/github/issues-closed/cTux/ae2-crafting-time?logo=github&label=issues%20fixed)](https://github.com/cTux/ae2-crafting-time/issues?q=is%3Aissue%20is%3Aclosed)
[![Open issues](https://img.shields.io/github/issues/cTux/ae2-crafting-time?logo=github)](https://github.com/cTux/ae2-crafting-time/issues)
[![Discussions](https://img.shields.io/badge/GitHub-Discussions-8250df?logo=github)](https://github.com/cTux/ae2-crafting-time/discussions)
[![Latest release](https://img.shields.io/github/v/release/cTux/ae2-crafting-time?logo=github)](https://github.com/cTux/ae2-crafting-time/releases/latest)
[![License](https://img.shields.io/github/license/cTux/ae2-crafting-time)](LICENSE)

AE2 Crafting Time is an Applied Energistics 2 autocrafting diagnostics mod. It
records real crafting performance on the server to help players find slow or
stalled crafts, understand bottlenecks, and estimate when crafting will finish.
It is an unofficial addon and is not endorsed by the Applied Energistics 2 team.

![Crafting status TTC bottleneck diagnostics](docs/images/crafting-status-ttc-bottleneck-diagnostics.png)

It currently targets:

| Minecraft | Loader | Gradle module |
| --- | --- | --- |
| 1.20.1 | Forge | `:mc_1_20_1_forge` |
| 1.20.1 | Fabric | `:fabric_1_20_1` |
| 1.21.1 | NeoForge | `:mc_1_21_1_neoforge` |
| 26.1.2 | NeoForge | `:mc_26_1_2_neoforge` |

## Download

- [Modrinth](https://modrinth.com/mod/ae2-crafting-time)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time)

## Features

- Profiles completed AE2 craft outputs to learn real throughput and crafting time.
- Estimates per-output and whole-job time to craft (`TTC`).
- Detects delayed outputs with no accepted progress.
- Shows evidence-bounded bottleneck clues for machine speed, Pattern Provider
  parallelism, and Crafting Co-Processor dispatch capacity.
- Compares frozen TTC predictions with successful jobs' real completion time and
  reports prediction accuracy and plan coverage.
- Persists retained samples in the world save as `data/ae2-crafting-time.dat`.
- Sends aggregate stats to clients through request/response packets.
- Adds `TTC` lines, color hints, totals, and sort controls to AE2 craft-plan/status views.
- Lets players Ctrl-Alt-click a TTC entry to forget stale stats for that output.
- Supports Applied Mekanistics chemicals when that optional mod is installed.
- Shows ME Requester row and total TTC hints when that optional mod is installed.
- Reuses shared Java code across loaders where AE2/Minecraft APIs match.

## Build

Use Java 17 for 1.20.1 modules, Java 21 for 1.21.1 NeoForge, and Java 25 for
26.1.2 NeoForge. Gradle provisions the declared toolchains.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

Tests run in GitHub Actions for every pull request.

Built jars are copied to `dist/`.

## Run

```powershell
.\run-1.20.1-forge.bat
.\run-1.20.1-fabric.bat
.\run-1.21.1-neoforge.bat
.\run-26.1.2-neoforge.bat
```

Each run script resolves every compatible optional integration, its required
dependencies, and JEI before starting the development client with this mod.

## Docs

- [Dependencies](DEPENDENCIES.md)
- [Potential dependency integrations](DEPENDENCIES_POTENTIAL.md)
- [Working with this project](docs/working-with-project.md)
- [Architecture](docs/architecture.md)
- [Release process](docs/release.md)
- [Server-owned stats design](docs/server-client-stats.md)
- [World save persistence](docs/world-save-persistence.md)
- [Time To Craft plan UI](docs/time-to-craft-plan.md)
- [TTC sorting](docs/ttc-sorting.md)
- [TTC colored text](docs/ttc-colored-text.md)

## Codex Skills

Local project skills live in `.codex/skills/`:

- `ae2-crafting-time-dev`: use for feature work, TTC UI changes, profiling bugs, packets, persistence, and docs.
- `ae2-crafting-time-release`: use for release matrix rows, build wrappers, jar names, deploy scripts, and release checks.

## Development Disclosure

This project is designed, directed, tested, and reviewed by cTux with substantial
generative-AI assistance for code, documentation, translations, release notes,
and publishing. It does not use generative AI at runtime and ships no
AI-generated visual assets.

## License

AE2 Crafting Time is available under the [MIT License](LICENSE).

## Repo Layout

```text
shared/src/main/java       Pure Java profiling, estimates, cache, and helpers
shared/src/mcCommon/java   AE2/Minecraft code shared by every supported version
shared/src/mc1201/java     Minecraft 1.20.1/1.21.1 API boundary
shared/src/mc2612/java     Minecraft 26.1.2 and AE2 26 API boundary
shared/src/neoforge/java   Code shared by both NeoForge versions
versions/1.20.1-forge      Forge module
versions/1.20.1-fabric     Fabric module
versions/1.21.1-neoforge   NeoForge module
versions/26.1.2-neoforge   Minecraft 26.1.2 NeoForge module
scripts/                   Build and release automation
docs/                      Design, workflow, and release notes
```
