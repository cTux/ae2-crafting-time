# AE2 Crafting Time Test Driver Technical Design

## Repository evidence

- `:mc_1_20_1_forge` already owns the Forge 1.20.1 client, Java 17 toolchain,
  AE2 dependency, production source sets, reobfuscation, and `distMod` task.
- `scripts/run-client.ps1` already resolves compatible or latest dependencies
  from `scripts/run-client-versions.json` and selects an isolated run directory
  through `runtimeRunDirectory`.
- Production Crafting Plan behavior is contributed by
  `CraftConfirmScreenMixin`, `CraftConfirmTableRendererMixin`,
  `AbstractTableRendererMixin`, `TtcSortButton`, `ClientStatsRequests`, and
  `StatsChatMessages`.
- `distMod` copies one explicitly named production JAR into `dist`; release and
  deploy scripts derive published artifacts from `release-matrix.json`.
- The repository has no Forge 1.20.1 fixture, driver source set, UI-smoke
  runner, or MCP dependency to reuse.

The design therefore adds one isolated source set and one runner to the existing
Forge module. It does not change production Java, packets, persistence, or the
release matrix.

## Build and source ownership

Add `testDriver` Java and resource source sets in
`versions/1.20.1-forge/build.gradle`:

```text
versions/1.20.1-forge/src/testDriver/java/
versions/1.20.1-forge/src/testDriver/resources/
```

The source set compiles against `sourceSets.main.output` and the Forge/AE2
compile classpath. `testDriverJar` packages only `testDriver.output`, applies
the normal Forge reobfuscation step, and writes to the root project:

```text
build/test-driver/
  ae2-crafting-time-<mod-version>-forge-1.20.1-test-driver.jar
```

The driver JAR contains `ae2craftingtime_test_driver` as a second development
mod. Its `mods.toml` declares an exact dependency range of `[<mod-version>]` on
`ae2craftingtime` and the same Minecraft and Forge ranges as the production
module. Its bootstrap registers behavior only on the physical client and stays
inert without the explicit test option. It relies on the matching production
mod for AE2 compatibility instead of declaring a narrower AE2 range.

The driver has its own mixin config and refmap. No driver directory is added to
`sourceSets.main`, `jar`, `reobfJar`, or `distMod`. The MCP SDK is pinned only in
the driver configuration and included only in the driver artifact. Use the official Java
SDK and its Streamable HTTP server transport; do not implement JSON-RPC framing
or protocol negotiation by hand. Verify and pin a Java 17-compatible SDK version
against the current official protocol during implementation.

## Development client installation

The existing wrappers already route through the shared client scripts, so they
do not gain separate installation logic:

```text
scripts-run/run-1.20.1-forge.bat
  -> scripts/run-client.ps1 -Target 1.20.1-forge

scripts-run/run-1.20.1-forge.sh
  -> scripts/run-client.sh -Target 1.20.1-forge
```

For the Forge 1.20.1 target, each shared script resolves the selected profile,
runs `:mc_1_20_1_forge:testDriverJar` with the same runtime version properties,
removes other `ae2-crafting-time-*-forge-1.20.1-test-driver.jar` files from that
profile's managed mod directory, and copies the exact artifact to:

```text
compatible -> versions/1.20.1-forge/run/resolved-mods/
latest     -> versions/1.20.1-forge/run-latest/resolved-mods/
```

The installed filename joins the managed manifest so dependency readback and
later cleanup see the actual client contents. Driver build, exact-name check,
stale cleanup, or copy failure stops the launch. Other targets do not build or
install a driver.

Add one optional runtime-directory parameter to both shared scripts. It defaults
to the current `run` or `run-latest` directory for the existing wrappers and is
used consistently for dependency installation and Gradle's
`runtimeRunDirectory`. The UI-smoke runner supplies its isolated directory under
`build/ui-smoke`; it does not maintain a second driver-install path.

Forge discovers the installed JAR through the module's existing
`runtimeRunDirectory/resolved-mods` file dependency. The production mod remains
the normal `sourceSets.main` development mod. An ordinary launcher therefore
contains the driver but executes no test behavior; the UI-smoke runner adds the
explicit scenario option.

## Fixture and runner ownership

Track the source fixture at:

```text
versions/1.20.1-forge/run/saves/ae2-crafting-time/
```

The saved player faces a known AE2 terminal. The world contains retained
production stats, a craftable output, and
`.ae2-crafting-time-test-fixture.json` with a fixed schema, scenario ID,
terminal position, output ID, and source-fixture ID. The driver reads only this
known marker path; it does not offer filesystem access.

Add `scripts/run-ui-smoke.ps1`. Its first version supports only `craft-plan`
and `1.20.1-forge`, with `-Latest` and `-Interactive` switches. It:

1. calls the existing Forge 1.20.1 launcher path for the chosen profile, which
   resolves dependencies and installs the matching driver;
2. builds the production development classes;
3. reuses `build/ui-smoke/1.20.1-forge/<profile>/runtime` and supplies it as
   the shared launcher's runtime directory, preserving resolved dependencies
   and Gradle/runtime caches between runs;
4. copies the tracked fixture into that runtime with a new disposable ID;
5. sets fixed client options and the explicit scenario property;
6. starts the module's `runClient` with that runtime directory;
7. records only the launched process tree;
8. validates output and logs after exit; and
9. removes the disposable world after evidence has been copied out.

`run-ui-smoke-codexvm.ps1` incrementally mirrors the shared checkout into one
stable guest-local directory, pins JDK 17, and dispatches the runner into the
logged-in Codex session. `run-ui-smoke.ps1` writes stdout, stderr, and an atomic
`status.json` to the shared report directory. The status exposes the current
phase, exact child PID, Java home, exit code, and artifact paths. OpenSSH is the
normal host transport; VMware `runProgramInGuest` remains a second transport.
Neither requires VNC terminal polling.

The automatic runner requests normal shutdown first. On timeout it terminates
only the process tree it launched. It never searches for or kills Java
processes globally.

The compatible run is the required gate. `-Latest` writes to a separate output
directory and reports upstream setup/startup failure distinctly; it cannot
convert a compatible failure into a pass.

## Driver state machine

`CraftPlanScenario` owns one bounded state machine:

```text
STARTING
  -> WORLD_READY
  -> TERMINAL_OPEN
  |  -> RESULT_WRITTEN (ME Requester)
  -> PLAN_OPEN
  -> PLAN_STABLE
  -> BASE_CHECKED -> SORTS_CHECKED -> TOOLTIP_CHECKED
  |  ADDON_CPU_SELECTED -> ADDON_CRAFT_SUBMITTED
  |  -> ADDON_SAMPLE_RECORDED -> ADDON_PLAN_OPEN
  -> RESULT_WRITTEN
  -> QUIT_REQUESTED
```

Each transition has an absolute deadline and records expected and observed
state on failure. A timeout enters `FAILED`; automatic mode writes evidence and
quits, while interactive mode stops scenario actions and leaves the read-only
diagnostic endpoint available until `minecraft_quit` or process exit.

The fixture positions the player and terminal so the driver can use normal
client interaction. It locates the visible target by output ID, derives click
coordinates from the actual widget or item bounds, and calls the screen's normal
input path on the client thread. Sort modes are changed only by clicking the
real `TtcSortButton`.

`AddonCpuFixture` owns the shared asynchronous place/finish/select lifecycle.
Its registry maps each `*-cpu` scenario to one driver-only implementation.
AdvancedAE, AppliedE, Applied Mekanistics, BM Addon, Crazy AE2 Addons,
ExtendedAE, ExtendedAE-Plus, MEGA Cells, NeoEco AE, OMNI Cells, OmniSequence,
and ProjectCell contain only their
mod-specific fixture code. ExtendedAE replaces the disposable world's AE2 molecular assemblers and
selects an existing idle CPU;
ExtendedAE-Plus reuses that setup after verifying its mod is loaded. BM Addon
places its Blood Assembler, installs a real Blood Pattern, supplies its inputs,
and selects an existing idle CPU. Crazy AE2 Addons places a native AE2 1K
crafting storage CPU and selects that recorded cluster.
ProjectCell removes the normal cobblestone supply, mounts a player-bound EMC
Storage Cell, and verifies that the grid can supply the furnace craft from
ProjectE EMC before selecting an existing idle CPU. AppliedE grants the player
furnace knowledge and EMC, mounts a powered Transmutation Module, and verifies
its native EMC crafting pattern before selecting a native AE2 CPU. Applied
Mekanistics mounts a chemical storage cell, fills it with oxygen through the
addon's native AE2 key, and places a native AE2 256K CPU. A new
optional dependency extends that registry and adds a
`testDriverCompileOnly` dependency when it is not already on the inherited
compile classpath. A fixture may override the marker output only when the add-on
requires its own pattern type; it does not add a scenario branch to
`CraftPlanScenario` or `run-ui-smoke.ps1`.

Wireless terminal scenarios are intentionally separate from the `*-cpu`
registry because they add no crafting CPU. `WirelessTerminalFixture` links the
selected addon's charged terminal to a real wireless access point on the
disposable grid. The scenario opens it through normal item use, captures the
final tooltip drawn for the known craftable entry, and then reuses the standard
Crafting Plan observation path. The current fixtures cover AE2 WCWT and AE2
Wireless Terminals. The AE2 Import Export Card fixture uses the same flow after
its real export and crafting cards automatically submit the fixture craft.

ME Requester also uses a separate screen flow. Its fixture places and configures
one requester with a deterministic profiler sample on the disposable AE2 grid.
The shared scenario opens that block through normal client interaction and validates
the final translation-keyed TTC row, total, badge geometry, and screenshot.

Plan data is stable after the same screen and ordered output IDs are observed
for three consecutive rendered frames. A new screen, changed row order, or
changed plan restarts the count.

## Independent UI observations

Driver mixins target AE2 and Minecraft UI boundaries, not production reporting
methods. Use a lower mixin priority than the production config so observations
run after normal AE2 Crafting Time injections.

`UiObservationStore` retains only the latest completed frame:

- active screen and menu class;
- the list passed into `CraftConfirmTableRenderer.render`, which is the actual
  post-sort visible order;
- final description and tooltip components returned by the AE2 renderer;
- actual `GuiGraphics.drawString` calls for AE2 Crafting Time translation keys;
- actual background-color fill calls, merged into badge rectangles;
- `TtcSortButton` identity, tooltip/state, and bounds;
- the final tooltip rendered while a registered wireless terminal is active;
- the final ME Requester screen text and badge bounds;
- item-cell and AE2-owned widget bounds; and
- GUI bounds and scroll position.

The store swaps frames only after rendering completes, so MCP and scenario
checks never read a partially collected frame. Captured components keep their
translation keys and arguments; rendered text is added only for diagnostics.

The layout validator checks containment and rectangle intersections. It does
not duplicate production TTC calculations. Sort checks compare observed output
IDs across clicks: original AE2 order, ascending known TTC, then descending
known TTC, with unknown rows stable at the end. The tooltip check uses the final
AE2 return value while the cursor is over the known row.

Screenshots use Minecraft's framebuffer capture after three stable frames. The
base image moves the cursor outside the GUI; the tooltip image places it at the
observed row center. Files are limited to the scenario output directory.

## Thread ownership

`DriverScheduler` has one bounded client queue and one bounded integrated-server
queue. A queued call carries a deadline and completes a future. Client screen,
input, widget, and framebuffer access runs through `Minecraft.execute`.
Integrated-server checks run through `MinecraftServer.execute`.

The scenario tick and MCP request threads never retain live game objects across
thread boundaries. They exchange immutable snapshots containing strings,
numbers, booleans, and rectangle values. Queue saturation or timeout returns a
structured failure and does not retry automatically.

## MCP endpoint

Interactive mode starts the official SDK's Streamable HTTP server on an
ephemeral `127.0.0.1` port. The runner supplies a random 256-bit bearer token in
an inherited environment variable and passes the token to the MCP client
without printing or persisting it. The endpoint rejects non-loopback peers,
missing or invalid authorization, a second active controller, requests over 64
KiB, responses over 1 MiB, unknown tools, and calls exceeding five seconds.

The first tool set maps to immutable snapshots:

| Tool | Result |
| --- | --- |
| `minecraft_get_state` | scenario state, step, elapsed time, last failure |
| `minecraft_get_screen` | screen/menu names, GUI bounds, resolution, scale |
| `minecraft_get_ui_snapshot` | observed rows, components, widgets, rectangles |
| `minecraft_take_screenshot` | saves a bounded PNG and returns its relative name |
| `minecraft_get_logs` | bounded tail of this client's current log |
| `minecraft_quit` | queues normal shutdown of this client |

No tool accepts a filesystem path. Screenshot names are generated by the
driver, and log reads are fixed to the current runtime's `latest.log` with a
64-KiB returned tail. The server closes before Minecraft process teardown.

## Result and validation flow

The driver builds one immutable result and writes `result.json.tmp` beside the
destination. It flushes and atomically renames the file to `result.json`; an
unsupported or failed atomic move is a scenario failure, never a pass.

`schema` is `1`, `complete` is true only after every required check and
screenshot write completes, and `checks` contains exactly the required set for
the selected scenario. Failures add a structured object with step, code,
expected, and observed values. Secrets, absolute user paths, and arbitrary log
text are excluded.

After the client exits, `run-ui-smoke.ps1` independently validates the JSON,
required files, production/driver version pair, and exit code. It copies the
managed dependency manifest to `resolved-mods.json`, copies only the launched
client's log, and scans that log for fatal loader, mixin, resource, and crash
signatures. Driver `PASS` plus a fatal log entry is a runner failure.

## Safety and failure handling

- Driver bootstrap stays inert unless the explicit scenario or interactive
  property is present.
- A normal Forge 1.20.1 development launch includes the driver JAR but starts no
  scenario, endpoint, input, fixture access, or result writer.
- World mutation requires integrated singleplayer, the disposable folder ID,
  and a valid marker whose source ID matches the tracked fixture.
- The tracked source fixture path is rejected even if its marker is valid.
- Scenario actions stop after failure or timeout; read-only snapshots remain
  available only in interactive mode.
- A missing plan, target output, retained sample, widget, or tooltip is a
  test failure with evidence, not a fallback or synthetic pass.
- A driver/production version mismatch is rejected by Forge before scenario
  code runs.
- Release tasks remain allowlist-based and unchanged; artifact tests prove that
  neither production JAR nor `dist` contains driver entries.

## Compatibility and later ports

The first implementation stays in the Forge 1.20.1 module. Keep result-model,
state-machine, and rectangle code free of Minecraft types where that falls out
naturally, but do not create shared source sets or loader interfaces yet.

When a second target is approved, move only already-identical code into a shared
test-driver source set. Screen adapters remain at the Minecraft/AE2 API
boundary, and loader bootstraps remain in their version modules.

Client UI checks run inside CodexVM. The checkout is read from VMware's
read/write `projects` share, while each Minecraft runtime stays on guest-local
NTFS. Development clients receive an 8 GiB maximum heap. Test-driver launches
maximize their GLFW window before the scenario starts; other UI checks maximize
the exact client through the VM display before inspection.

## Alternatives rejected

- Production test hooks: they would ship test behavior and make the mod attest
  to its own output.
- A custom MCP protocol implementation: protocol, transport, and authentication
  edge cases are not project value; use the official SDK in the isolated JAR.
- A general cross-loader driver now: only Forge 1.20.1 has a defined scenario,
  so the shared boundary is not yet evidence-backed.
- Full-frame golden images: animated items and renderer differences add noise
  without improving the first semantic checks.
