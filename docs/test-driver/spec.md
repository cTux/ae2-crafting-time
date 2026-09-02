# AE2 Crafting Time Test Driver Spec

## Goal

Provide a development-only companion mod that drives and observes a real
Minecraft client for repeatable AE2 Crafting Time UI smoke tests. It runs beside
the production mod and never replaces or ships inside it.

The driver covers the standard AE2 Crafting Plan screen and add-on crafting CPU
scenarios on Minecraft 1.20.1 Forge. New optional-mod scenarios must plug into
the shared add-on fixture flow without adding another branch to the UI state
machine.

Wireless-terminal addons are the exception because they add terminals, not
crafting CPUs. Their scenarios open the real wireless terminal, check the
craftable-entry TTC tooltip, and follow the normal AE2 Crafting Plan flow.

ME Requester has its own screen scenario. It places and configures a real
requester on the disposable grid, opens that screen normally, and checks its
rendered TTC row, header total, badge layout, and screenshot.

This covers [issue #126](https://github.com/cTux/ae2-crafting-time/issues/126).

## Artifact contract

The driver artifact name is derived from the project version:

```text
ae2-crafting-time-<mod-version>-forge-1.20.1-test-driver.jar
```

It tests exactly:

```text
ae2-crafting-time-<mod-version>-forge-1.20.1.jar
```

The driver has its own mod ID, `ae2craftingtime_test_driver`, and refuses to
load with a different AE2 Crafting Time version. It accepts every AE2 version
allowed by the matching production mod so the same driver can run the
compatible and latest development profiles.

The driver artifact is written under `build/test-driver`, never `dist`. It does
not embed production AE2 Crafting Time classes. Player JARs and published
artifacts must not contain driver classes, resources, metadata, or dependencies.

## Development client installation

`scripts-run/run-1.20.1-forge.bat` builds and installs the matching driver JAR
in its development client's managed mod directory before starting Minecraft:

```text
versions/1.20.1-forge/run/resolved-mods/
  ae2-crafting-time-<mod-version>-forge-1.20.1-test-driver.jar
```

`scripts-run/run-1.20.1-forge-latest.bat` does the same under `run-latest`.
The paired shell launchers have the same behavior. Each launch replaces a stale
driver copy and fails before Minecraft starts if the matching driver cannot be
built or installed.

The installed driver remains inert during an ordinary development-client run.
Only the explicit test-driver launch option starts a scenario or MCP endpoint.
Installation in these managed development clients does not copy it to `dist`
or make it a published mod artifact.

See [Automated UI Testing](../automated-ui-testing/spec.md) for the eventual
cross-target and optional-dependency suite.

## Automatic Crafting Plan scenario

The runner copies the tracked Forge 1.20.1 fixture world, starts the client with
the driver explicitly enabled, and runs this sequence:

1. Enter the disposable fixture world.
2. Open its known AE2 terminal.
3. Select its known craftable output.
4. Wait for `CraftConfirmScreen` and stable plan data.
5. Check TTC rows, total TTC, badge geometry, and the sort button.
6. Cycle AE2 order, shortest first, and longest first through the real button.
7. Hover the known row and check its final tooltip.
8. Save base and tooltip screenshots and an atomic semantic result.
9. Close the exact client cleanly.

The fixture supplies retained samples so this scenario checks rendered TTC,
not the separate `Collecting data` state. A fixed resolution, GUI scale,
language, fixture, and cursor position make the screenshots comparable.

The driver observes the final AE2 screen, renderer, widget, tooltip, and
framebuffer state after normal production hooks have run. A production method
reporting that it executed is not evidence that its output reached the screen.
Translation keys and output IDs are semantic identity; rendered English text is
report evidence, not the assertion key.

The `appbot-cpu` fixture mounts a real Applied Botanics mana cell, verifies
native mana insertion and extraction through the grid, and then runs the shared
craft/sample/TTC flow on a normal AE2 CPU. Separate amount, packet, and saved-data
tests cover raw mana precision and unit migration; the fixture does not claim
to automate a Botania mana-generation recipe.
The `appbot-fork-cpu` scenario reuses that fixture against the separately pinned
fork artifact. Original and fork must never be loaded together.

The `advancedperipherals-cpu` fixture connects a real ME Bridge and CC:Tweaked
computer to the grid. It submits the selected output through the bridge's
`craftItem` API using the attached computer, then observes a new server profile
sample and TTC in the normal AE2 Crafting Plan. This tests the peripheral API,
not an automated Lua editor or a separate ComputerCraft TTC display.

The `ae2things-cpu` fixture mounts the Forge port's real DISK inventory, removes
pre-existing cobblestone from the disposable grid, and supplies the craft
ingredients through that DISK. A native CPU craft must produce a fresh profile
sample and visible TTC. Removed Forge machines are not part of this scenario.

The `expandedae-cpu` fixture joins Expanded AE's real two-thread accelerator
to native crafting storage, verifies the CPU reports two co-processors, and
requires a new profile sample and TTC after crafting. Run it as a focused
latest-profile scenario: the full compatible graph excludes Expanded AE
because of its existing conflict with OmniSequence.

## Optional add-on CPU scenarios

An add-on CPU scenario uses the same disposable world, UI flow, profiler checks,
result schema, and runner. Its fixture owns only the add-on-specific placement,
formation, and CPU selection. Scenario names end in `-cpu`; the runner derives
the standard add-on checks and screenshot name from that convention.

Adding an optional mod requires one fixture implementation, one registry entry,
and a driver-only compile dependency. It must not add a production dependency,
make the optional mod mandatory, or require a new runner branch.

## Wireless terminal scenarios

The `ae2wcwt-terminal` scenario links a charged Wireless Comprehensive Wireless
Terminal to a real wireless access point on the fixture grid. It opens the
terminal through normal item use, hovers the known craftable output, checks the
TTC tooltip, then clicks the entry and verifies TTC on the standard Crafting
Plan screen. The required checks are `screen`, `ttc-tooltip`, and `plan-ttc`.

The `ae2wtlib-terminal` scenario uses the same flow with AE2 Wireless
Terminals' charged wireless crafting terminal.

The `ae2importexportcard-terminal` scenario links a charged standard AE2
wireless terminal, installs a real export card, and verifies the TTC tooltip
and Crafting Plan on the addon's modified terminal screen using a deterministic
profile sample.

The `aeinfinitybooster-terminal` scenario installs a real Infinity Card in the
linked access point, moves the player beyond its normal range, and opens the
standard wireless terminal. It checks `screen` and `plan-ttc`, with terminal and
plan screenshots. The range-only addon adds no terminal TTC tooltip surface.

## ME Requester scenario

The `merequester-screen` scenario places a real requester on the fixture grid,
configures a deterministic out-of-stock request and profiler sample, opens its
screen through block use, and checks `screen`, `ttc-row`, `total-ttc`, and
`layout`. Its screenshot is `merequester-screen.png`.

## AE2 Network Analyser scenario

The `ae2networkanalyser-screen` scenario equips the real network analyser,
opens its configuration screen through normal item use, and verifies the
expected screen/menu identity and that its GUI remains inside the viewport.
It does not assert TTC text because the addon visualizes network topology and
does not expose crafting status.

## Interactive diagnosis

Interactive mode runs the same scenario but pauses at the failed or completed
step and exposes a loopback MCP endpoint. The first artifact provides only:

```text
minecraft_get_state
minecraft_get_screen
minecraft_get_ui_snapshot
minecraft_take_screenshot
minecraft_get_logs
minecraft_quit
```

The first five tools observe the launched client without changing its world.
`minecraft_quit` performs only a normal client shutdown. Synthetic click,
hover, key, command, and world-editing tools are deferred until a later
scenario proves they are needed.

The endpoint:

- listens only on `127.0.0.1` and only in explicit interactive mode;
- requires a fresh per-run secret that is not placed in command-line logs,
  result files, or screenshots;
- accepts one controller at a time;
- uses a fixed tool allowlist and bounded arguments, requests, responses, and
  timeouts;
- stops when Minecraft exits; and
- never exposes arbitrary Java calls, shell execution, Minecraft commands, or
  filesystem access.

Client state is read only on the Minecraft client thread. Integrated-server
state is read only on the server thread. Endpoint work is queued to the owning
thread and fails on a bounded timeout.

## Fixture safety

The driver may act only when all of these are true:

- the explicit test-driver launch option is present;
- the connection is singleplayer;
- the opened world is the runner-created disposable copy; and
- the world contains the expected test-fixture marker and scenario data.

It refuses multiplayer, an unmarked world, the tracked source fixture, or a
world opened without test-driver mode. After a timeout it records failure and
stops taking scenario actions. The runner owns copying and deleting the
disposable world; the driver changes only the running copy.

## Layout checks

Semantic observations include screen-relative rectangles for visible rows,
text, badges, item cells, AE2 buttons, and the test target widget. A required
rectangle fails when it is outside the GUI or overlaps an owned control or item
cell. Screenshots remain the human-readable evidence for clipping, spacing,
color, and unexpected visual changes.

The first slice does not use full-frame golden-image comparison. Add a cropped
golden comparison only after a stable region has a demonstrated regression that
semantic bounds do not catch.

## Result contract

The driver writes `result.json` atomically in the scenario output directory. A
temporary or incomplete file is never a pass.

```json
{
  "schema": 1,
  "complete": true,
  "driver": "ae2-crafting-time-1.1.0-forge-1.20.1-test-driver.jar",
  "target": "1.20.1-forge",
  "profile": "compatible",
  "scenario": "craft-plan",
  "result": "PASS",
  "checks": {
    "screen": true,
    "ttc-row": true,
    "total-ttc": true,
    "sort-cycle": true,
    "tooltip": true,
    "layout": true
  },
  "screenshots": ["craft-plan.png", "craft-plan-tooltip.png"]
}
```

The runner independently checks the schema, completion flag, exact driver name,
target, profile, scenario, required check set, screenshot existence, clean
client exit, and fatal log entries. Missing or invalid output is a failure.

## Compatibility

- First artifact: Minecraft 1.20.1, Forge, Java 17, standard AE2 Crafting Plan.
- Optional add-on CPU fixtures currently cover AdvancedAE, Applied Flux,
  Applied Mekanistics, BM Addon, Crazy AE2 Addons, AppliedE, ExtendedAE,
  ExtendedAE-Plus, MEGA Cells, Modern AE2 Additions, NeoEco AE, OMNI Cells,
  OmniSequence: Transfinite, and ProjectCell.
  ExtendedAE and ExtendedAE-Plus replace the fixture's molecular assemblers
  with ExtendedAE assemblers. BM Addon installs a real Blood Pattern and its
  inputs. Crazy AE2 Addons places a native AE2 1K crafting storage CPU and
  selects that recorded cluster. Modern AE2 Additions builds a native AE2 CPU
  with its 4x co-processor and selects that recorded cluster. ProjectCell
  replaces the fixture's normal cobblestone supply with a bound EMC Storage
  Cell. AppliedE replaces that
  craft path with a player-owned Transmutation Module, furnace knowledge,
  ProjectE EMC, and a native AE2 CPU. Applied Mekanistics mounts a chemical
  storage cell containing oxygen and places a native AE2 256K CPU. The other
  provider scenarios use an existing idle CPU because those addons do not add
  one.
- AE2 WCWT and AE2 Wireless Terminals have separate Forge 1.20.1 terminal
  scenarios because they add no crafting CPU.
- ME Requester has a dedicated Forge 1.20.1 screen scenario.
- Run it against both the compatible and latest AE2 profiles already owned by
  `scripts/run-client-versions.json`.
- Do not create a cross-loader abstraction for this slice. Reuse code only when
  a second supported target proves the shared boundary.
- The driver may compile against production classes but may not alter the
  production packet protocol, saved-data format, runtime behavior, or JAR.

## Not included

- Crafting Status or submitted-craft checks.
- Optional-addon behavior outside the registered CPU fixture contract.
- Fabric, NeoForge, dedicated-server, or multiplayer support.
- General-purpose UI automation, arbitrary world setup, or remote control.
- Pixel-perfect full-frame comparisons.
- Publishing the driver on GitHub, CurseForge, or Modrinth.
- A new production config option, packet, API, or test hook.

## Acceptance criteria

- The dedicated build task creates only the version-matched driver under
  `build/test-driver`.
- The Forge 1.20.1 compatible and latest development launchers install that
  exact driver in their selected `resolved-mods` directory, remove stale driver
  versions, and stop if installation fails.
- Forge refuses a driver paired with the wrong AE2 Crafting Time version.
- The driver remains inactive without the explicit test option and refuses
  multiplayer, the tracked fixture, and unmarked worlds.
- One command copies the fixture, runs the compatible Crafting Plan scenario,
  validates the result and logs, saves two screenshots, closes the exact
  client, and returns zero only on a complete pass.
- The same command can select the latest profile without weakening a compatible
  profile failure.
- The scenario proves the real screen, TTC row, total, three sort modes,
  tooltip, badge bounds, and non-overlap rules from final UI observations.
- A registered add-on CPU scenario reuses the common setup, selection, sample,
  result, and screenshot flow without changing the runner's scenario allowlist.
- Interactive mode exposes only the six bounded tools above and rejects missing
  authentication, a second controller, oversized input, and unknown tools.
- Client and server access stays on the owning game thread and times out rather
  than blocking indefinitely.
- `dist` and every production JAR remain free of driver artifacts and classes.
- Automated tests cover result validation, state transitions, safety refusals,
  endpoint limits, and artifact isolation.
