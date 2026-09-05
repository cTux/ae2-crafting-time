# AE2 Crafting Time Test Driver Spec

All future smoke follows the [shared policy](../automated-ui-testing/spec.md#smoke-policy):
newest adapter per dependency/target and English (`en_us`) only. The English-only
scenario requirements below are the required next state; removing existing
bilingual driver states is follow-up implementation work. Keep Ukrainian
product translations and static resource checks.

## No-provider status scenario

`no-provider-status` submits a real 64-output processing job to an isolated
native AE2 CPU. Blocking mode and a chest hold the first batch active while
later batches remain scheduled. Removing its pattern must show NO PROVIDER in
that combined row. Check the rendered badge and both tooltip sentences in
English (`en_us`). Restore the pattern and require recovery without
reopening the menu. Install an equivalent pattern in a second provider, remove
the first pattern, and require no warning for two refresh cycles. Remove that
second provider block, observe the warning again, then replace/reconnect it and
require recovery. Cancel the job and confirm its diagnostic clears.

The scenario uses real provider inventories, grid lookups, job submission,
menu synchronization, and final frame observations. It never seeds the warning
or invokes a production reporting hook to make a UI assertion pass. Capture
before, mixed-row warning, English tooltips, both recoveries, redundant
provider, provider removal, and cancellation checkpoints. Keep the scenario
available across the shared 1.20.1/1.21.1 driver and the 26.1.2 API counterpart;
a passing smoke result applies only to the target actually launched.


## No-space status scenario

`no-space-status` uses an isolated native AE2 CPU and a full item cell in the
disposable world. A full external furnace first leaves the warning absent.
The fixture seeds retained CPU contents through AE2's inventory API; AE2's
normal tick and menu synchronization must report rejected storage. Observe the
rendered warning, badge, and tooltip in English (`en_us`). Replace the full
cell with writable capacity and require the warning to clear in the same screen.
Keep before, English tooltip, and recovered screenshots. Pure row tests
cover active and scheduled exclusions; the fixture does not simulate a craft.

## Standard AE2 acceptance scenario

`standard-ae2` is a host-expanded group of six independently runnable leaves:
`standard-plan-controls`, `standard-status-controls`, `waiting-status`,
`running-status`, `delayed-status`, and `craft-lifecycle`. Each has a fresh
native grid and its own seeded estimates; no case depends on an earlier reset,
world, job, or cached observation. Standard leaves copy only world metadata and
the marker (including separate native world-generation settings when present),
then generate fresh chunks; importing saved chunks could restore
incompatible CPU jobs before the driver starts. Other scenarios keep their
tracked layouts. The host keeps one process for the group.

The [leaf contracts](../automated-ui-testing/technical-design.md#groups-and-independent-standard-flow)
retain every original assertion. Waiting and running require real dependency
progress. Delayed checks the active row, bold red label, diagnostic tooltip and
recovery after actual output. It observes the stone provider plate clear while
smooth stone remains active, withholds the final furnace output until its own
delayed plate arrives, then imports that one completed output with all menus
closed. Require CPU completion, fresh samples, stored output, and client plate
clearing without an intermediate progressing craft. Capture held-output status,
world highlight, world completion, and reopened idle status. The world camera
shows an AE2 terminal directly in front of the final provider to review red
background and item-icon occlusion. Client plate state proves packet lifecycle;
inspect world screenshots to verify rendering. Lifecycle follows terminal, amount, plan, Start,
status, real furnace output and new samples, then reopens the idle CPU screen.
The raw JVM property accepts leaf IDs or `suite`; use the host alias for the group.

## Goal

Provide a development-only companion mod that drives and observes a real
Minecraft client for repeatable AE2 Crafting Time UI smoke tests. It runs beside
the production mod and never replaces or ships inside it.

The driver covers the standard AE2 Crafting Plan screen and add-on crafting CPU
scenarios on Minecraft 1.20.1 Forge/Fabric and 1.21.1 NeoForge. New optional-mod scenarios must plug into
the shared add-on fixture flow without adding another branch to the UI state
machine.

Wireless-terminal addons are the exception because they add terminals, not
crafting CPUs. Their scenarios open the real wireless terminal, check the
craftable-entry TTC tooltip, and follow the normal AE2 Crafting Plan flow.

ME Requester has its own screen scenario. It places and configures a real
requester on the disposable grid, opens that screen normally, and checks its
rendered TTC row, header total, badge layout, and screenshot.

This covers [issue #126](https://github.com/cTux/ae2-crafting-time/issues/126).

## Fabric full-client suite

Run `scripts/invoke-ui-smoke-codexvm.ps1 -Target 1.20.1-fabric -Scenario suite`.
The full pinned compatible graph runs in one maximized 8 GiB client with eight
cases: Crafting Plan, ExtendedAE, Applied Botanics, AE2 Things DISK storage,
MEGA Cells, AE2 Wireless Terminals, ME Requester, and NO SPACE. Each case gets a fresh
copy of the tracked 1.20.1 fixture, screenshots, and checked semantic results.
JEI and transitive libraries load with the graph but have no dedicated assertions.
Crafting Tree and Network Analyser are not pinned in this Fabric graph.

The same `-Target` works for single scenarios, latest profiles, and interactive
runs. Full suites also run as separate latest diagnostics. Fabric uses its own version-matched
`ae2-crafting-time-<mod-version>-fabric-1.20.1-test-driver.jar`, remapped by Loom,
installed into `run/mods` or `run-latest/mods`. Player JARs stay independent.

## NeoForge 1.21.1 full-client suite

Run `scripts/invoke-ui-smoke-codexvm.ps1 -Target 1.21.1-neoforge -Scenario suite`.
Use JDK 21 and the complete pinned compatible graph in one maximized 8 GiB
client. The 22 cases in `scripts/ui-smoke-neoforge-suite.json` cover the base
plan, Crafting Tree, all pinned CPU/provider fixtures, four wireless-terminal
flows, ME Requester, and NO SPACE. Each case uses a fresh copy of the native 1.21.1 world and retains
its own semantic results and screenshots. JEI, GuideME, and transitive libraries
load with the graph but have no dedicated UI assertions. Expanded AE remains
excluded from the compatible graph because of its recorded OmniSequence conflict.
Applied Botanics, AE2 Things, and Network Analyser are not pinned in this graph.

Compatible and latest launchers install the exact
`ae2-crafting-time-<mod-version>-neoforge-1.21.1-test-driver.jar` in their managed
`mods` directory. Single scenarios and interactive diagnosis use the same target;
full suites also run as separate latest diagnostics. The companion stays inert without explicit
scenario options and never enters the production JAR or `dist`.

## NeoForge 26.1.2 full-client suite

Run `scripts/invoke-ui-smoke-codexvm.ps1 -Target 26.1.2-neoforge -Scenario suite`.
The pinned compatible graph runs eleven cases in one maximized 8 GiB client:
Crafting Plan, AdvancedAE, ExtendedAE, BM Addon, Lightning Tech, OMNI Cells,
Applied Flux, AE2 Wireless Terminals, Import Export Card, Infinity Booster,
and NO SPACE. Neo Vitae supports the BM Addon recipe; GuideME, JEI, and
transitive libraries load with the graph without dedicated assertions.

Each case uses a fresh disposable copy of the native 26.1.2 world. The driver
builds a native AE2 grid in that copy and checks real crafting, new profiling
samples, final UI observations, and checkpoint screenshots. The Gradle launcher
uses JDK 21 and selects the JDK 25 client toolchain. The exact companion JAR
is installed by both compatible and latest launchers and stays out of player
artifacts and `dist`. Full suites also run as separate latest diagnostics.

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
8. Save base, each sort-mode, and tooltip screenshots and an atomic semantic result.
9. Close the exact client cleanly.

The fixture supplies retained samples so this scenario checks rendered TTC,
not the separate `No data yet` state. A fixed resolution, GUI scale,
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

The `ae2things-cpu` fixture mounts the loader's real DISK inventory, removes
pre-existing cobblestone from the disposable grid, and supplies the craft
ingredients through that DISK. A native CPU craft must produce a fresh profile
sample and visible TTC. Removed Forge machines are not part of this scenario.

The `expandedae-cpu` fixture joins Expanded AE's real two-thread accelerator
to native crafting storage, verifies the CPU reports two co-processors, and
requires a new profile sample and TTC after crafting. Run it as a focused
latest-profile scenario: the full compatible graph excludes Expanded AE
because of its existing conflict with OmniSequence.

The `lightningtech-cpu` fixture submits a smooth-stone smelting job to a real
Tianshu pool, using a pattern provider, furnace, hopper, and ME interface.
It checks that the pool receives the job and requires a fresh
profiling sample for the final output and visible TTC on the next Crafting Plan.
This is a regression check for standalone outputs that go directly to ME storage.

## Single-launch suites

A named-pack or prepared-client campaign runs all selected scenarios in one Minecraft process.
Load mods and textures once, then run each case, save its screenshots and result,
unload its world normally, and open the next fresh disposable fixture copy.
World reloads isolate blocks, inventories, jobs, and saved profiler data without
restarting the client. Never reuse a mutated world for another case.

Use `scenario=suite` with the usual profile, output directory, and first world
properties. The output directory contains `suite-plan.json`: schema 1 and a
`cases` array of unique `{scenario, world}` entries (1–32 cases). All worlds must
be pre-created, marked disposable copies. Validate the whole plan before acting.
Interactive mode remains single-case only.

Each case writes under `<output>/<scenario>/`. The root `result.json` records
one JVM process ID, ordered case outcomes and timings, and the overall result.
Missing cases never count as passes. Stop on the first failure, retain its
screenshot/result, mark later cases `NOT_RUN`, and close the exact client.
Only a complete suite with every case passing can report `PASS`.

The existing single-case option and result schema remain unchanged. Selecting a
suite does not silently add mods or skip missing integrations.

Prepare a suite with `scripts/prepare-ui-smoke-suite.ps1 -RuntimeDirectory <game>
-OutputDirectory <new-evidence-directory> -Scenarios <ordered-names>`. It returns
the first world and launch properties. Pass `scenario=suite`, that world, profile,
and output through the existing JVM properties, with the first world as Prism's
quick-play world. The helper never changes the pack's mod graph.

For the full prepared Forge compatible graph, run
`scripts/invoke-ui-smoke-codexvm.ps1 -Scenario suite`. The ordered cases live in
`scripts/ui-smoke-forge-suite.json`; each uses a fresh world in one client run.
The wrapper validates every per-case result and screenshot plus the overall
suite result, retains shared logs, and cleans all disposable worlds. The suite
allows 40 minutes including dependency resolution/build; single cases keep their
8-minute limit. Archive the evidence before another invocation.

This suite rejects `-Latest`, `-ProjectId`, and `-Interactive`: its case list
matches the full compatible graph. The graph includes the Applied Botanics fork;
the colliding original and incompatible Expanded AE require separate graphs.
Documentation/recipe viewers and transitive libraries are loaded
but have no dedicated UI assertions. Do not report those as scenario passes.

## Optional add-on CPU fixtures

`advancedae-cpu` builds a valid Quantum Computer enclosure containing its core,
an Accelerator, and a Data Entangler. CPU selection verifies the added threads
and multiplied storage. Submission must create a job in that exact cluster
before a fresh profile sample and the resulting plan TTC can pass.

## Crafting Tree scenario

`crafting-tree-screen` opens the real tree toolbar button from a populated
Crafting Plan. It supports the original and Refreshed widget packages. Check
the rendered node badges, their bounds against node icons, and a hovered
crafted node's TTC and details/reset hints. Capture the tree and its tooltip
separately. A missing tree, badge, or tooltip fails the scenario; opening a
normal plan alone cannot pass it. Run against the prepared compatible graph
and the original Crafting Tree artifact in Project Infinity 0.1.

## Optional fixture contract

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
configures a deterministic out-of-stock diamond request and profiler sample, opens its
screen through block use, and checks `screen`, `ttc-row`, `total-ttc`, and
`layout`. The layout check includes active menu item slots, so a badge drawn
under an item cannot pass just because its text draw call was observed.
Its screenshot is `merequester-screen.png`.

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
  "screenshots": ["craft-plan.png", "craft-plan-sort-1.png", "craft-plan-sort-2.png", "craft-plan-sort-3.png", "craft-plan-tooltip.png"]
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
  OmniSequence: Transfinite, LightningTech's Tianshu multidimensional CPU pool, and ProjectCell.
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
- ME Requester has dedicated Forge and Fabric 1.20.1 screen scenarios.
- Run it against both the compatible and latest AE2 profiles already owned by
  `scripts/run-client-versions.json`.
- Share identical 1.20.1 driver code. Keep loader entrypoints in their modules.
- The driver may compile against production classes but may not alter the
  production packet protocol, saved-data format, runtime behavior, or JAR.

## Not included

- Optional-addon behavior outside the registered CPU fixture contract.
- Dedicated-server or multiplayer support.
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
- Both loaders refuse a driver paired with the wrong AE2 Crafting Time version.
- The driver remains inactive without the explicit test option and refuses
  multiplayer, the tracked fixture, and unmarked worlds.
- One command copies the fixture, runs the compatible Crafting Plan scenario,
  validates the result and logs, saves five screenshots, closes the exact
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

## No-power status scenario

`no-power-status` reuses the real processing-job fixture with 64 cobblestone
per dispatch and an unfuelled furnace. Verify no warning while network energy
is sufficient. Replace the creative source with a real energy cell and keep
only enough energy for idle demand, below the next dispatch cost. Observe an
active CPU, one active output, and scheduled work, then the rendered NO POWER
badge and complete tooltip in English (`en_us`). Restore energy, require
another real dispatch and warning recovery in the same menu, cancel, and check
that an inactive CPU alone produces no warning. Retain each distinct English checkpoint.
Every full compatible suite includes this scenario and the NO PROVIDER
regression. Driver checks observe final frames and real AE2 state, never seed
production diagnostics. Shared pure tests cover threshold, expiry, priority,
CPU switching and lifecycle; packet tests cover the shared transport boundary.
