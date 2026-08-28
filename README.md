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

AE2 Crafting Time is an unofficial Applied Energistics 2 addon that helps you
figure out why an autocrafting job is taking forever—or why it looks stuck.

It tracks how crafts actually perform on the server, then uses that data to
estimate completion times, highlight possible bottlenecks, and point out crafts
that have stopped making progress.

It is not affiliated with or endorsed by the Applied Energistics 2 team.

![Crafting status TTC bottleneck diagnostics](docs/images/crafting-status-ttc-bottleneck-diagnostics.png)

## Supported Versions

| Minecraft | Loader | Gradle module |
| --- | --- | --- |
| 1.20.1 | Forge | `:mc_1_20_1_forge` |
| 1.20.1 | Fabric | `:fabric_1_20_1` |
| 1.21.1 | NeoForge | `:mc_1_21_1_neoforge` |
| 26.1.2 | NeoForge | `:mc_26_1_2_neoforge` |

## Download

You can grab the latest version from:

- [Modrinth](https://modrinth.com/mod/ae2-crafting-time)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time)

## What It Does

- Learns how long different AE2 craft outputs actually take.
- Shows the estimated time to craft (`TTC`) for individual outputs and entire jobs.
- Warns you when an output has gone too long without making progress.
- Gives evidence-based bottleneck hints for machine speed, Pattern Provider
  parallelism, and Crafting Co-Processor dispatch capacity.
- Compares its original TTC prediction with the actual completion time of
  successful jobs.
- Reports prediction accuracy and how much of the crafting plan it could estimate.
- Keeps retained performance samples in
  `data/ae2-crafting-time.dat` inside the world save.
- Sends only aggregate crafting stats to connected clients.
- Adds TTC details, colors, totals, and sorting controls to AE2 crafting screens.
- Lets you Ctrl-Alt-click a TTC entry to forget outdated stats for that output.
- Supports Applied Mekanistics chemicals when the mod is installed.
- Adds TTC hints to ME Requester rows and totals when the mod is installed.
- Shares as much code as possible between supported Minecraft versions and loaders.

## Building It

The project uses Java 17 for Minecraft 1.20.1, Java 21 for Minecraft 1.21.1
NeoForge, and Java 25 for Minecraft 26.1.2 NeoForge. Gradle handles the declared
toolchains.

To build every supported version:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

Pull requests are tested through GitHub Actions.

When the build finishes, the JARs are copied to `dist/`.

## Running a Development Client

Use the script for the version and loader you want to test:

```powershell
.\run-1.20.1-forge.bat
.\run-1.20.1-fabric.bat
.\run-1.21.1-neoforge.bat
.\run-26.1.2-neoforge.bat
```

Normal builds compile against the minimum supported loader and AE2 versions.
Each run script uses the newest loader, AE2, optional integrations, required
dependencies, and JEI available for that Minecraft and loader target.

## Documentation

If you want the more technical details, start here:

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

The project-specific Codex skills live in `.codex/skills/`:

- `ae2-crafting-time-dev`: use for feature work, TTC UI changes, profiling bugs, packets, persistence, and docs.
- `ae2-crafting-time-release`: use for release matrix rows, build wrappers, jar names, deploy scripts, and release checks.
- `ae2-crafting-time-writing`: use for docs, skills, changelogs, translations,
  issue forms, and store or loader descriptions.

## Development Disclosure

Yes, AI helps me write this mod. I'm not really a Java guy, if you know what I
mean, but I have enough engineering experience to keep it from turning into
vibe-coded spaghetti. I direct and review the work, and I expect the code to be
tested, maintainable, scalable, and reusable. I hope that makes it clear where
AI fits into the project.

The mod does not use generative AI while running and does not include
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
