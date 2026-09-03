# Automated UI Testing Technical Design

## Current seams

The repository already provides the two sources of truth and one launcher seam
needed by the suite:

```text
release-matrix.json + run-client-versions.json
  -> scripts/run-client.ps1
  -> resolved target/profile mods
  -> :<module>:runClient
```

`scripts/test-run-client.ps1` already proves that target IDs match, compatible
projects have locks, and resolution writes a managed-mod manifest. The UI suite
extends these seams instead of creating another dependency matrix or launcher.

All four targets have isolated drivers and per-target suites. The cross-target
command builds and resolves on the host, then stages those exact JARs into a
guest-local runtime. The guest launches the installed loader directly with its
prepared `launch.json`; it never invokes Gradle. Validate the manifest's target,
Java version and installed loader against the resolved host profile before launch.
Native loader libraries and assets remain in the prepared installation; world,
mods, options, logs and evidence belong to the isolated run.

`standard-ae2` builds a native two-stage smelting grid in its disposable world.
Two real furnaces turn cobblestone into stone, then smooth stone. Distinct known
samples make row ordering observable. The driver checks plan input and returned
chat/reset state, submits using the screen, and observes the status screen while
fuel is withheld and restored. It imports only actual furnace output through
the grid storage API and requires the exact final output and idle CPU. This is
fixture transport, not a production profiling or status hook.

The host records a coverage ledger before resolution. Every matrix project must
have a declared disposition; direct coverage names required scenarios. Missing
declarations and missing scenarios fail compatible runs. Each target executes in
matrix order even if an earlier target fails. Latest results remain separate
diagnostics. Failure paths retain the manifest, available logs and driver evidence.

## Components and ownership

| Component | Ownership |
| --- | --- |
| `scripts/run-ui-smoke.ps1` | Matrix selection, dependency resolution, fixture copy, process ownership, result validation, artifact collection, cleanup, and exit code |
| `scripts/ui-smoke-coverage.json` | Project-ID dispositions and required scenario IDs; no dependency versions |
| `scripts/test-run-ui-smoke.ps1` | Pure PowerShell checks for matrix validation, result parsing, failure classification, and process-target selection |
| Test-driver pure Java | Scenario state machine, bounded JSON result model, checklist status, and atomic result writing |
| Minecraft-version adapter | Screen/menu access, stable-frame observation, framebuffer capture, and synthetic input on the client thread |
| Loader bootstrap | Explicit activation, driver/production version checks, scenario arguments, and lifecycle registration |
| Tracked fixture | Known terminal, craft, CPU states, expected output, and marker; never opened directly |
| Production mod | Unchanged; it exposes only its normal player behavior |

The first implementation is the Forge 1.20.1 Crafting Plan vertical slice. Its
driver source set lives in `versions/1.20.1-forge` and may read shared driver
code from `test-driver/src/main/java` plus the 1.20.1 adapter from
`test-driver/src/mc1201/java`. Do not add the other loader bootstraps until the
first slice works. The second port decides which code is actually reusable.

## Build and packaging

Add a dedicated `testDriver` source set and `testDriverJar` task to a target only
when that target gains a runnable scenario. Register a separate development mod
ID, `ae2craftingtime_test_driver`, and require the exact AE2 Crafting Time
version while allowing the AE2 range already accepted by that production JAR.

Test-driver JARs are written to `build/test-driver` with the production contract
in the filename, for example:

```text
ae2-crafting-time-1.1.0-forge-1.20.1-test-driver.jar
```

`distMod`, release scripts, normalized-JAR checks, and deploy discovery continue
to accept only release-matrix production names. Add explicit checks that reject
the driver mod ID and test-driver classes in `dist` and production JARs. The
driver must not embed production classes.

This protects **A8** without introducing a second publication pipeline.

## Runner flow

The public command is:

```powershell
.\scripts\run-ui-smoke.ps1 [-Target <id>] [-Latest] [-Scenario <name>] [-Interactive]
```

With no target, it selects all release-matrix IDs in matrix order. Without
`-Latest`, it runs compatible profiles and treats every required failure as a
failing exit. `-Latest` selects diagnostic profiles and preserves failures as
`DIAGNOSTIC_FAILURE`. `-Scenario` narrows local diagnosis; the default selects
every implemented required scenario.

The runner passes `--no-daemon` to Gradle. This gives each client run its own
Gradle JVM instead of handing work to an existing daemon whose process tree the
runner does not own.

For each selected target, the runner:

1. Validates matrix parity and coverage declarations.
2. Calls `run-client.ps1 -ResolveOnly` for the selected profile.
3. Builds the matching production and test-driver JARs.
4. Recreates that run's result directory.
5. Validates the source world marker, copies it under a unique disposable name,
   and records a pre-run fixture tree hash.
6. Places only the matching driver JAR in the selected runtime mod directory.
7. Starts the existing `runClient` path with `--no-daemon` and Gradle project
   properties `uiSmokeEnabled`, `uiSmokeResultPath`, `uiSmokeWorld`, and
   `uiSmokeScenario`, plus `uiSmokeTokenPath` only for interactive runs.
8. Tracks the Gradle process and every descendant created after launch.
9. Validates the atomic result and required evidence after exit.
10. Removes the driver JAR and disposable world, then verifies the source fixture
    hash is unchanged.

Each target build forwards those project properties to its client run as the
JVM system properties `ae2ct.uiSmoke.enabled`, `ae2ct.uiSmoke.resultPath`,
`ae2ct.uiSmoke.world`, `ae2ct.uiSmoke.scenario`, and optionally
`ae2ct.uiSmoke.tokenPath`. The token itself never appears on the command line.
Dependency resolution and compatible/latest behavior remain in
`run-client.ps1`. This satisfies **A1**, **A2**, **A3**, **A6**, and **A7** with
one orchestration path.

## Fixture contract

Each implemented target owns one tracked source world at:

```text
versions/<target>/run/saves/ae2-crafting-time/
```

The world contains a small driver marker with fixture schema version, target,
and scenario IDs. The driver refuses multiplayer, an absent or mismatched
marker, a source-world name, or a launch without explicit driver mode. The
runner owns copying and deletion; the driver changes only the active disposable
world.

Each scenario declares stable logical identities rather than coordinates in
code: terminal marker, requested output ID, expected produced amount, and
required CPU state. Version-specific block positions live in the fixture marker
because world layouts may differ across Minecraft versions.

## Scenario state machine

The driver runs all Minecraft access on the owning game thread and advances one
bounded state per client tick:

```text
BOOT -> WORLD -> TERMINAL -> PLAN -> PLAN_ASSERTIONS
  -> SUBMIT -> STATUS -> STATUS_ASSERTIONS -> OUTPUT -> WRITE -> QUIT
```

Every state has a deadline and records its last screen, expectation, and
observation. A timeout or failed assertion transitions once to `WRITE`, captures
failure evidence, and then requests normal shutdown. It never retries an input
because a repeated click can change game state.

The first slice ends after `PLAN_ASSERTIONS`; later slices extend the same state
machine through status and output. Optional integrations use named scenarios
and the same result contract rather than branches hidden inside one enormous
scenario.

## Independent observation

Thin driver mixins/accessors inspect the final AE2 screen after normal hooks
have contributed. Observations include:

- screen and menu class;
- translation key, output ID, rendered text, and row bounds;
- widget identity, enabled state, tooltip, and bounds;
- badge, total, item-cell, and owned-button rectangles;
- visible row order and scroll position;
- details/reset request plus returned chat component; and
- job status and expected fixture output.

The driver clicks actual widgets or row coordinates and verifies the resulting
visible order or server response. It does not accept a production-mod callback
as evidence. This is the design path for **A4**.

## Result contract

The driver writes `result.json.tmp`, closes it, then atomically renames it to
`result.json`. Only the final file can pass. The top-level object contains:

```json
{
  "schema": 1,
  "driver": "ae2-crafting-time-1.1.0-forge-1.20.1-test-driver.jar",
  "production": "ae2-crafting-time-1.1.0-forge-1.20.1.jar",
  "target": "1.20.1-forge",
  "profile": "compatible",
  "scenario": "craft-plan",
  "result": "PASS",
  "completed": true,
  "checks": {},
  "screenshots": []
}
```

The outer runner validates schema, exact artifact names, selected target and
profile, scenario, completion marker, required check IDs, declared screenshot
existence, clean exit, and fatal-log absence. It copies the managed-mod manifest
to `resolved-mods.json` and the current log to `client.log`. This supplies the
complete, attributable evidence required by **A5**.

## Screenshots and geometry

Capture the framebuffer only after screen identity and semantic row data remain
stable for a bounded number of frames. Use fixed resolution, GUI scale,
language, and cursor position. Tooltip captures move the cursor to the target;
base captures move it away.

Geometry checks compare rectangles for containment and overlap. Screenshots are
retained for visual review; no full-screen golden comparator is part of the
initial design.

## Interactive diagnosis

`-Interactive` explicitly enables a loopback-only MCP endpoint for diagnosis.
The runner creates a per-run token file, passes only its path to the client, and
deletes it during cleanup. The endpoint accepts one controller, bounded JSON-RPC
messages, a fixed tool allowlist, and per-tool timeouts. It stops with Minecraft.

Automatic release-facing runs never wait for a controller. Interactive runs may
keep read-only state, screenshot, log, and quit tools available after failure;
mutation tools stop when the scenario is cancelled or the marked fixture is no
longer active. No shell, arbitrary Java call, filesystem read, Minecraft command,
generic world edit, or multiplayer tool is exposed.

## Failure and cleanup

- Resolution or fixture validation failure writes `FAIL_SETUP` without starting
  Minecraft.
- Client assertion failure writes `FAIL` and captures the active state.
- Latest-only setup or runtime failure is normalized to `DIAGNOSTIC_FAILURE`.
- A missing direct scenario is `MISSING_FIXTURE`, never `PASS`.
- An omitted behavior is `NOT_APPLICABLE` only when the coverage declaration
  records why.
- After the clean-exit timeout, the runner terminates only descendants of its
  recorded no-daemon Gradle launch process and records that cleanup as a
  failure.
- Cleanup runs in `finally`; it removes only the exact driver JAR, disposable
  world, and token file created for that run.

## Compatibility and migration

The suite adds no production packet, persistence, or translation change. Its
driver contract is versioned by filename and result schema. A production version
change requires a matching driver build; a result schema change increments
`schema` and updates the runner in the same commit.

Minecraft-facing adapters follow repository boundaries only after ports prove
reuse: 1.20.1/1.21.1 screen access may share an `mc1201` seam, while 26.1.2 uses
its own adapter. Loader bootstraps remain target-local.
