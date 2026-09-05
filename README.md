# AE2 Crafting Time

[![Tests](https://github.com/cTux/ae2-crafting-time/actions/workflows/test.yml/badge.svg?branch=master)](https://github.com/cTux/ae2-crafting-time/actions/workflows/test.yml)
[![Coverage](https://codecov.io/gh/cTux/ae2-crafting-time/graph/badge.svg)](https://codecov.io/gh/cTux/ae2-crafting-time)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/LyDZjvxd?logo=modrinth&label=Modrinth%20downloads)](https://modrinth.com/mod/ae2-crafting-time)
[![CurseForge downloads](https://img.shields.io/curseforge/dt/1591476?logo=curseforge&label=CurseForge%20downloads)](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time)
[![GitHub downloads](https://img.shields.io/github/downloads/cTux/ae2-crafting-time/total?logo=github&label=GitHub%20downloads)](https://github.com/cTux/ae2-crafting-time/releases)
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

![Delayed crafting job](docs/images/crafting-status-delayed.png)

[See the screenshot gallery](docs/images/README.md) for the other windows,
tooltips, and TTC states.

## Supported Versions

See [dependencies.md](docs/dependencies.md) for supported Minecraft versions,
loaders, required dependencies, and optional integrations.

## Download

You can grab the latest version from:

- [Latest release](https://github.com/cTux/ae2-crafting-time/releases/latest)
- [Modrinth](https://modrinth.com/mod/ae2-crafting-time)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-time)

## What It Does

- Remembers how long your crafts really take, so its time guesses get better the
  more you play.
- Tells you how long a craft will take — for one item or a whole job — right on
  the AE2 screens, so you know when to come back.
- Marks scheduled work as waiting until its first pattern dispatch, so you can
  tell that it hasn't started yet.
- Alerts you when a craft has stalled, so you can fix it instead of waiting
  forever.
- Shows what's slowing you down — like a machine that's too slow, too few
  Pattern Providers, or not enough Crafting Co-Processors — and how to speed it
  up.
- Shows how close its time guesses were after a craft finishes, so you can trust
  the numbers.
- Adds time, colors, totals, and sort buttons to the AE2 crafting screens, so the
  info is easy to read at a glance.
- Lets you Ctrl-Alt-click an entry to erase old, wrong stats for that item.
- Works with Applied Mekanistics chemicals, and adds time hints to ME Requester
  rows and totals, when those mods are installed.
- Only shares summary stats with other players on the server — never your items
  or your base.
- Keeps what it learned inside your world, so it stays smart after you quit and
  come back.

## Documentation

If you want the more technical details, start here:

- [Building It](docs/building.md)
- [Running a Development Client](docs/dev-client.md)
- [Automated UI testing](docs/automated-ui-testing/spec.md)
- [UI test-driver mod](docs/test-driver/spec.md)
- [Repo Layout](docs/repo-layout.md)
- [Working with this project](docs/working-with-project.md)
- [Codex Skills](docs/codex-skills.md)
- [Dependencies and integrations](docs/dependencies.md)
- [Client and modpack coverage](docs/mod-automation-coverage.md)
- [Known Issues](https://github.com/cTux/ae2-crafting-time/issues?q=is%3Aissue%20is%3Aopen%20label%3Abug)
- [Architecture](docs/architecture.md)
- [Feature documentation map](docs/feature-coverage.md)
- [Profiling and diagnostics](docs/profiling-and-diagnostics/spec.md)
- [Player controls and integrations](docs/player-controls-and-integrations/spec.md)
- [AE2 addon integration](docs/ae2-addon-integration/spec.md)
- [Release process](docs/release.md)
- [Server-owned stats design](docs/server-client-stats.md)
- [World save persistence](docs/world-save-persistence.md)
- [Time To Craft plan UI](docs/time-to-craft-plan.md)
- [TTC sorting](docs/ttc-sorting.md)
- [TTC colored text](docs/ttc-colored-text.md)
- [Waiting to start status](docs/waiting-to-start/spec.md)
- [Provider locate](docs/provider-locate/spec.md)

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
