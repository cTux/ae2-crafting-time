# AE2 Crafting Time Test Driver

The test driver is a development-only companion mod that observes and controls
a real Minecraft client during UI smoke tests. It is installed beside the normal
AE2 Crafting Time JAR and never replaces it.

The first artifact is:

```text
ae2-crafting-time-1.0.12-forge-1.20.1-test-driver.jar
```

It tests this production artifact:

```text
ae2-crafting-time-1.0.12-forge-1.20.1.jar
```

The driver version in the filename identifies the exact production mod contract
it was built to test. A driver must refuse a different AE2 Crafting Time version
unless that combination is explicitly allowed.

This document defines the intended driver. The artifact does not exist yet.

See [Automated UI Testing](automated-ui-testing.md) for the complete client and
optional-dependency checklist that drives it.

## Keep it outside the production JAR

The driver may inspect screens, send synthetic input, prepare test state, expose
a loopback control endpoint, take screenshots, and close the client. None of
that belongs in a player release.

Build it with a dedicated task such as:

```text
:mc_1_20_1_forge:testDriverJar
```

Write it under `build/test-driver`, not `dist`. Release, deploy, and normalized
JAR checks must reject test-driver artifacts and test-driver classes in player
JARs.

The driver has its own mod ID, for example `ae2craftingtime_test_driver`, and
requires the exact Minecraft, loader, and AE2 Crafting Time version it tests. It
accepts any AE2 version allowed by that production JAR so the same driver can
exercise both compatible and latest profiles. It must not embed production AE2
Crafting Time classes.

## Two modes

### Automatic scenario mode

The UI smoke script starts the client with a named scenario. The driver enters
the disposable fixture world, performs the fixed checklist, writes `result.json`
and screenshots, and closes the client. This is the repeatable pass/fail path.

The first scenario should cover only the standard Crafting Plan vertical slice:

1. Open the fixture terminal.
2. Select its known craftable output.
3. Wait for `CraftConfirmScreen` and stable plan data.
4. Inspect TTC rows, total, badge geometry, and sort button.
5. Cycle all sort modes.
6. Hover a known row and inspect the tooltip.
7. Save a screenshot and result.
8. Close the client.

Add Crafting Status and optional-addon scenarios after this path is reliable.

### Interactive MCP mode

Interactive mode exposes a small Model Context Protocol server so Codex can
inspect a failed client without guessing from pixels. Bind it only to
`127.0.0.1` and enable it only with an explicit development launch option.

Start with the tools that serve UI diagnosis:

```text
minecraft_get_state
minecraft_get_screen
minecraft_get_ui_snapshot
minecraft_click_widget
minecraft_hover_row
minecraft_press_keys
minecraft_take_screenshot
minecraft_get_logs
minecraft_quit
```

Add AE2-specific tools only when the fixed scenarios need them:

```text
ae2_open_fixture_terminal
ae2_select_fixture_craft
ae2_submit_fixture_craft
ae2_get_visible_ttc
ae2_get_craft_status
```

Do not expose arbitrary Java calls, shell execution, unrestricted Minecraft
commands, filesystem reads, or generic world editing. MCP is for diagnosis; the
automatic scenario remains the release-facing result.

## Observe the result independently

The production mod must not pass by reporting that its own method ran. The
driver observes AE2 after all normal screen hooks have contributed their result.
Its thin mixins and accessors can record:

- active screen and menu classes;
- final row description and tooltip components;
- widget identity, state, tooltip, and bounds;
- TTC badge and total bounds;
- visible row order and scroll position;
- outgoing details/reset interaction and returned chat component; and
- job status and expected fixture output.

Use translation keys and output IDs as the semantic identity. Rendered English
text is useful in reports but is not a stable programmatic key.

Minecraft client state is read and changed only on the client thread. Integrated
server state is read and changed only on the server thread. The control endpoint
queues work onto the owning thread and waits with a bounded timeout; it never
touches live game objects from the HTTP or MCP thread.

## Screenshots and geometry

Capture the actual framebuffer after the screen and data remain stable for a
small fixed number of frames. Move the cursor to the requested row only for
tooltip images and move it away for base-screen images.

Semantic observations include rectangles. The driver checks that TTC content
stays inside the GUI and does not overlap AE2-owned buttons, item icons, table
cells, titles, or addon content. Screenshots remain the human-readable evidence
for alignment, clipping, spacing, and color.

## Fixture boundary

The driver runs only in a copied local world whose name and marker identify it
as an AE2 Crafting Time test fixture. It must refuse:

- multiplayer connections;
- an unmarked local world;
- a world opened outside explicit test-driver mode; and
- destructive reset or setup operations after a scenario timeout.

The PowerShell runner owns copying and later removing the disposable world. The
driver owns only actions inside the running copy.

## Endpoint safety

Interactive control must have all of these boundaries:

- listen on loopback only;
- remain disabled by default;
- require a per-run token supplied outside command-line logs;
- accept one controller at a time;
- use a fixed tool allowlist and bounded arguments;
- cap request and response sizes;
- apply per-tool timeouts;
- stop the endpoint when Minecraft exits; and
- reject multiplayer and unmarked worlds before enabling mutation tools.

Read-only state and screenshot tools may remain available after a scenario
failure. Mutation tools stop once the scenario is cancelled or the fixture is
no longer active.

## Result contract

The driver writes one atomic JSON result. A partial file is never a pass.

```json
{
  "driver": "ae2-crafting-time-1.0.12-forge-1.20.1-test-driver.jar",
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

The outer PowerShell runner verifies the driver name, target, profile, scenario,
completion marker, required check set, screenshot existence, clean client exit,
and absence of fatal log entries.

## Porting boundary

Do not build a general cross-loader framework before the first Forge 1.20.1
scenario works. Keep checklist/result parsing in pure Java, keep Minecraft 1.20.1
screen access in one adapter, and keep the Forge bootstrap thin. Reuse that core
only when the second target proves which boundary is actually shared.

The production source-set split is still the likely destination: common result
logic, an `mc1201` adapter for 1.20.1 and 1.21.1, an `mc2612` adapter for 26.1.2,
and loader bootstraps only where registration differs.

## First artifact acceptance

`ae2-crafting-time-1.0.12-forge-1.20.1-test-driver.jar` is ready when it:

- refuses the wrong AE2 Crafting Time version;
- stays inactive without the explicit test option;
- runs the Crafting Plan scenario in the copied fixture;
- proves screen, TTC row, total, sorting, tooltip, and layout behavior;
- saves semantic JSON plus screenshots;
- provides bounded read-only MCP diagnosis after a failure;
- closes the exact client cleanly; and
- cannot enter `dist` or a published player JAR.
