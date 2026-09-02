# Automated UI Testing Spec

## Goal

Provide one unattended smoke suite that launches every supported development
client, exercises AE2 Crafting Time on real AE2 screens, records semantic and
visual evidence, and closes only the client it started. Starting Minecraft
successfully is not a UI pass.

The runner and companion test driver do not exist yet. See
[AE2 Crafting Time Test Driver](../test-driver/spec.md) for the development-only mod
that observes and controls the client.

Tracking issue: [#124](https://github.com/cTux/ae2-crafting-time/issues/124).

## Source-of-truth matrices

- `scripts/release-matrix.json` owns the published targets.
- `scripts/run-client-versions.json` owns compatible and latest development
  dependency graphs.
- `docs/dependencies.md` owns integrations the project claims to support.

The suite rejects a release target missing from the run-client matrix. Every
top-level project in the selected profile is reported as direct coverage,
coexistence coverage, tooling, excluded with a reason, or not applicable.
Installing an addon alone is not evidence of supported behavior.

## Clients and profiles

Clients run sequentially so their memory, logs, screenshots, ports, and world
state remain attributable.

| Target | Compatible release gate | Latest diagnostic |
| --- | --- | --- |
| 1.20.1 Forge | `run-1.20.1-forge.bat` | `run-1.20.1-forge-latest.bat` |
| 1.20.1 Fabric | `run-1.20.1-fabric.bat` | `run-1.20.1-fabric-latest.bat` |
| 1.21.1 NeoForge | `run-1.21.1-neoforge.bat` | `run-1.21.1-neoforge-latest.bat` |
| 26.1.2 NeoForge | `run-26.1.2-neoforge.bat` | `run-26.1.2-neoforge-latest.bat` |

All four compatible profiles are required release-facing smoke checks. Latest
profiles deliberately resolve current upstream files; a latest-only resolution
or startup failure is a visible diagnostic result, not evidence that the pinned
release is broken.

## Standard AE2 scenario

For every client, the suite:

1. Records the selected profile and every resolved mod file.
2. Copies the target's tracked `ae2-crafting-time` world to a disposable world;
   the checked-in fixture is never opened directly.
3. Launches the client and rejects loader, mixin, resource, and startup errors.
4. Enters the disposable world and opens its known AE2 terminal.
5. Opens Crafting Plan for a known craftable output.
6. Verifies TTC or `No data yet` on eligible rows, total TTC, badge
   geometry, default longest-first order, every sort mode, tooltip content,
   Ctrl-click details, and Ctrl-Alt-click reset.
7. Submits the craft and opens Crafting Status.
8. Verifies row TTC, header total placement, sorting, tooltip and click
   targeting, plus deterministic waiting, running, delayed, and completed
   states.
9. Confirms that the job produces the expected output.
10. Saves results, screenshots, resolved mods, and current logs, then requests
    clean shutdown.

Assertions use the actual screen, renderer, widget, input, or client/server
result. The existence of a translation key or production callback is not proof
that the player saw the expected UI.

## Optional-dependency coverage

- **Direct UI:** exercise the addon's screen, TTC layout, tooltip,
  details/reset targeting, and addon-specific total where present.
- **Direct behavior:** run a real addon CPU or key through standard AE2 plan and
  status screens and verify profiling, TTC, and completion.
- **Coexistence:** verify startup, no mixin errors, and a standard AE2 craft
  while the candidate is installed. This does not claim addon support.
- **Tooling:** required by the development client but not an integration.

| Target | Direct UI | Direct behavior | Coexistence candidates and dependencies | Tooling |
| --- | --- | --- | --- | --- |
| 1.20.1 Forge | AE2 Crafting Tree; ME Requester; AE2 WCWT; AE2 Wireless Terminals | Applied Mekanistics chemical key; NeoEco AE C-series CPU; AE2 Lightning Tech time-wheel CPU; ProjectCell EMC Storage Cell; AppliedE EMC key | AdvancedAE; ExtendedAE; ExtendedAE-Plus; BM Addon; Crazy AE2 Addons; MEGA Cells; OMNI Cells; Applied Flux; Modern AE2 Additions; AE2 Import Export Card; AEInfinityBooster; Advanced Peripherals; Expanded AE; ProjectE | GuideME; JEI |
| 1.20.1 Fabric | ME Requester | None beyond standard AE2 | ExtendedAE; AE2 Wireless Terminals; MEGA Cells; AE2 Things | JEI |
| 1.21.1 NeoForge | AE2 Crafting Tree; ME Requester; AE2 WCWT | Applied Mekanistics chemical key; AdvancedAE Quantum Computer | NeoEco AE; AE2 Lightning Tech; ExtendedAE; ExtendedAE-Plus; BM Addon; AE2 Wireless Terminals; MEGA Cells; OMNI Cells; ProjectCell; AppliedE; Applied Flux; AE2 Import Export Card; AEInfinityBooster; Advanced Peripherals; Expanded AE; ProjectE | GuideME; JEI |
| 26.1.2 NeoForge | None; pre-26 optional UI adapters must be absent | AdvancedAE Quantum Computer | AE2 Lightning Tech; ExtendedAE; BM Addon; Neo Vitae; AE2 Wireless Terminals; OMNI Cells; Applied Flux; AE2 Import Export Card; AEInfinityBooster | GuideME; JEI |

AE2 Crafting Tree is declared for 1.20.1 Fabric but is not a top-level project
in that run-client target. Its direct check remains `MISSING_FIXTURE` until the
matrix contains a compatible installation. Candidate integrations gain a
direct scenario only after their behavior is supported or verified.

## Evidence

Semantic observations identify translation keys and output IDs and include
screen-space bounds. The suite verifies required components, containment, and
non-overlap with owned buttons and item cells. It clicks real widgets and rows,
then verifies visible ordering and the resulting server response so display and
input indices cannot silently diverge.

Screenshots use a fixed resolution, GUI scale, language, fixture, and cursor
position. They remain human evidence for clipping, spacing, and color. Full
frame pixel equality is not required; a cropped golden comparison is added
only for a stable region with a demonstrated regression.

## Results and failures

Each target, profile, and scenario receives its own directory:

```text
build/ui-smoke/1.21.1-neoforge/compatible/standard-ae2/
  result.json
  resolved-mods.json
  client.log
  craft-plan.png
  craft-plan-tooltip.png
  crafting-status.png
  failure.png
```

Every scenario reports one of:

- `PASS`: required behavior was observed.
- `FAIL`: the client ran but behavior was wrong or missing.
- `FAIL_SETUP`: dependency, fixture, or startup failure blocked the scenario.
- `MISSING_FIXTURE`: supported behavior lacks a runnable fixture.
- `NOT_APPLICABLE`: the target intentionally lacks that behavior.
- `DIAGNOSTIC_FAILURE`: a latest-profile graph did not resolve or run.

A failure records the exact step, active screen class, expected and observed
semantic events, screenshot, log, and dependency manifest. The runner requests
normal shutdown, then may terminate only the process tree it launched after a
bounded timeout. It never kills Java processes broadly.

## Non-goals

- Replacing unit, packet, or structural tests.
- Proving an integration from startup alone.
- Pixel-perfect comparison of an animated full screen.
- Running Minecraft clients in parallel.
- Publishing the test-driver mod or including it in a player JAR.
- General-purpose remote control, shell access, world editing, or multiplayer
  automation.

## Acceptance criteria

- **A1:** One command runs all four compatible clients unattended and returns
  non-zero if any required scenario fails or is missing.
- **A2:** Every run uses a disposable copy of a marked fixture and leaves the
  checked-in fixture unchanged.
- **A3:** Every selected matrix project has an explicit coverage disposition;
  the two matrices cannot silently disagree.
- **A4:** The standard scenario proves the Crafting Plan and Crafting Status
  behavior listed above through final UI and server outcomes.
- **A5:** Semantic results, screenshots, resolved mods, and logs are complete
  and attributable to one target, profile, and scenario.
- **A6:** Compatible failures fail the command; latest-only failures remain
  visible but do not reclassify compatible results.
- **A7:** Every started client exits cleanly or only its recorded process tree
  is terminated after timeout.
- **A8:** The test driver is inactive outside explicit test mode and cannot be
  packaged or deployed as a player artifact.
