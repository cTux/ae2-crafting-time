# Automated UI Testing Spec

## Goal

Provide one unattended smoke suite that launches every supported development
client, exercises AE2 Crafting Time on real AE2 screens, records semantic and
visual evidence, and closes only the client it started. Starting Minecraft
successfully is not a UI pass.

The per-target runners and companion test drivers are implemented. See
[AE2 Crafting Time Test Driver](../test-driver/spec.md) for the development-only mod
that observes and controls the client.

Tracking issue: [#124](https://github.com/cTux/ae2-crafting-time/issues/124).

## Planned change-based smoke selection

Tracking issue: [#218](https://github.com/cTux/ae2-crafting-time/issues/218).

Status: planned, researched on 2026-09-03. This section extends the implemented
runner; it does not claim that automatic selection or independent standard
cases already exist. The original #124 remains the completed baseline.
For this extension, CS-01 through CS-09 supersede the monolithic execution
details below; the original behavior, evidence and safety requirements remain.

### Scope and execution

- **CS-01:** Select affected Minecraft/loader targets and relevant UI cases from
  a Git change set. A change isolated to one target runs only that target.
  Shared code selects every consuming target, not just the folder's apparent
  Minecraft version. Union the requirements of all changed files.
- **CS-02:** Narrow behavior only when a reviewed mapping proves its scope.
  Dedicated delayed-status code or an English delayed-label resource change
  selects `delayed-status` on the affected targets. Mixed status rendering
  selects all status cases. Shared profiling, wire formats, general UI, build,
  or unclassified runtime changes select full affected-target suites. Never
  guess scope from the word `delayed`, an issue title, or an added line alone.
- **CS-03:** Show an inspectable plan before execution: comparison commits,
  local changes, targets, expanded cases, dependency/profile identity, reasons,
  exclusions, fallback decisions, and not-selected coverage. A known docs-only
  or test-only change produces `NOT_REQUIRED`, not a fabricated smoke pass.
- **CS-04:** Automatic selection happens when the host change-based command is
  invoked. The development workflow uses it when UI smoke is required or
  requested, after the hook-created PR exists. A commit does not launch
  Minecraft. GitHub CI keeps builds and non-UI tests; adding a VM scheduler or
  self-hosted CI runner is out of scope. Explicit full runs stay available and
  release-required full coverage cannot be replaced by a focused pass.

### Independently runnable standard cases

- **CS-05:** `standard-ae2` becomes an ordered group of the six cases below.
  Each can run alone on all four release targets, with its own marked fresh
  world, setup, assertions, screenshots, cleanup, and result. No case consumes
  another case's world, job, cached observation, or profiler sample.
- **CS-06:** Group execution uses one client process per target and dependency
  graph. Preserve the existing full journey through real UI submission and
  real machine output. Splitting the checks must not remove coverage.

| Case | Independently verified behavior |
| --- | --- |
| `standard-plan-controls` | Plan rows and total TTC, badge geometry, all sort modes, tooltip, details response, reset response and server sample removal |
| `standard-status-controls` | A real active job, status rows, header and badge geometry, all sort modes, tooltip, details and reset targeting |
| `waiting-status` | A pending output before first dispatch shows WAITING; actual dependency completion starts it and removes WAITING |
| `running-status` | A dispatched output shows its TTC while another output waits; actual progress updates the status/header without a false blocked label |
| `delayed-status` | Actual stalled dispatch becomes DELAYED after the production threshold; correct row, styling, tooltip and layout; real output recovery clears DELAYED |
| `craft-lifecycle` | Terminal -> amount -> plan -> UI Start -> busy CPU -> actual furnace output and new samples -> idle CPU -> reopened empty status |

`craft-plan`, `no-space-status`, `no-provider-status`, `no-power-status`, and
existing integration cases keep their names and remain independently runnable.
The standard group covers the old standard flow; the three blocked-status
cases remain separate members of the full target suite.

### Overrides, failures, and compatibility

- **CS-07:** Keep explicit target/scenario/profile/mod selections. They are
  manual scopes, not proof that all changed behavior was covered. Change-based
  mode cannot silently discard affected targets or cases through an override.
  Missing refs, invalid mappings, unsupported selected cases, and missing
  required fixtures fail before launching; uncertain runtime scope expands.
- **CS-08:** Keep SP-01 through SP-04, newest-adapter identity checks, `en_us`,
  host builds, guest-local runtimes, sequential clients, exact-process cleanup,
  screenshot review and archive requirements. Named modpacks remain an explicit
  Prism workflow against the exact requested pack graph. A source diff never
  automatically chooses, upgrades, or installs a modpack.
- **CS-09:** Every selected leaf must pass for its group to pass. Missing or
  later unrun cases never count as passes. Keep historical reports unchanged;
  new group evidence identifies its actual leaf results. A focused result does
  not imply a full-suite pass, including in the project coverage ledger.

### Acceptance examples

| ID | Change or request | Required outcome |
| --- | --- | --- |
| CA-01 | Runtime file under `versions/1.20.1-forge` only | Forge alone; relevant mapped cases, otherwise its full suite |
| CA-02 | Shared dedicated delayed diagnostics or only the English `ttc_delayed` value | `delayed-status` on all four targets |
| CA-03 | Shared status renderer or status-priority rules | All status cases on all consuming targets, with the reason visible |
| CA-04 | Common profiler or packet change | Full suites on all consuming targets |
| CA-05 | Changes affecting different targets/features together | Union, ordered and deduplicated; no first-match loss |
| CA-06 | Rename/delete, new runtime file, unknown path, missing merge base | Both path identities considered; conservative full fallback or explicit preflight error, never an empty successful plan |
| CA-07 | Docs/images only, or tests only | Explained `NOT_REQUIRED`; normal static/unit checks still apply |
| CA-08 | Each standard leaf run alone, then `standard-ae2` and full suites | Independent passes, complete old-check coverage, one process per suite graph, no 32-case truncation |
| CA-09 | Selected leaf fails or evidence is missing | Nonzero compatible result; group cannot pass; later cases recorded `NOT_RUN` |
| CA-10 | Explicit manual selection excludes changed behavior | Clearly manual coverage; cannot certify the change-based gate |
| CA-11 | Requested pack reaches an older integration adapter | Preserve pack; follow SP-04 and keep newest-adapter proof separate |
| CA-12 | Worktree changes after a saved plan | Reject stale execution and require replanning |

This is bounded test selection, not general semantic code analysis. A DELAYED
edit inside a method shared by several statuses may correctly run more than
`delayed-status`. Finer narrowing requires a reviewed ownership mapping, not
an assertion by the caller that the rest is unaffected.

## Smoke policy

Requirements updated 2026-09-03. These rules apply to future prepared-client,
focused, full-suite, named-modpack, and release smoke work. They supersede older
plans requiring smoke for every retained adapter or multiple languages;
historical evidence remains unchanged. Runner/driver enforcement is follow-up
implementation work, not a result of this documentation change.

- **SP-01:** Runtime smoke exercises only the newest implemented adapter for
  each applicable dependency and Minecraft/loader target. Older adapters remain
  supported and keep unit, contract, and packaging checks, but need no runtime
  smoke. Do not launch extra old-version campaigns to satisfy a coverage gate.
- **SP-02:** Verify the selected adapter identity, not merely the dependency
  version or profile name. Reuse a compatible pin if it reaches the newest
  adapter; otherwise use a focused dependency fixture that does. A latest
  upstream file with an unknown API is not automatically a supported adapter.
  Failure to exercise the newest adapter remains an unmet smoke gate; a pass
  on an older adapter cannot substitute for it.
- **SP-03:** All smoke UI, assertions, and screenshots use English (`en_us`)
  only. Do not switch to Ukrainian or repeat scenarios by language. Keep the
  product's English/Ukrainian translations and static key/placeholder checks.
- **SP-04:** Preserve the requested target/modpack graph. If that graph only
  reaches an older adapter, record its direct adapter smoke as not required by
  SP-01; do not silently upgrade the pack. The required newest-adapter case runs
  in a separate prepared fixture. Core startup/coexistence checks can still run.

Completion evidence names the target, dependency artifact, selected newest
adapter, and `en_us`. Older variants must not appear as missing required smoke
cases. Core-only startup and dedicated-server absence checks remain applicable.

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

Run `scripts/run-ui-smoke.ps1` on the host for all four compatible suites.
Add `-Latest` for the diagnostic matrix. To run one target, add `-Target`
with `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-neoforge`, or
`26.1.2-neoforge`. The ordinary `run-*.bat` clients remain interactive
development launchers; starting one does not produce a smoke result.

All four compatible profiles retain release-facing core and applicable newest-
adapter checks under SP-01 through SP-04. An older-adapter case in a compatible
graph is not a required direct smoke; use the focused newest fixture instead.
Latest profiles deliberately resolve current upstream files; a latest-only resolution
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

The executable project-by-project table is
[`scripts/ui-smoke-coverage.json`](../../scripts/ui-smoke-coverage.json).
The runner checks it against the current client matrix and writes each
project's disposition, required scenario, result and exclusion reason into
`coverage.json`. This avoids a second dependency inventory in this spec.

Fabric Crafting Tree is `NOT_APPLICABLE`: the upstream
[CurseForge files](https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-tree/files/all)
and Modrinth version metadata provide Forge/NeoForge releases and no Fabric
1.20.1 artifact (verified 2026-09-03). The dormant Fabric optional metadata is
not evidence of a runnable integration. If an official compatible Fabric
artifact appears, add it to the matrix and require its direct scenario.
## Evidence

Semantic observations identify translation keys and output IDs and include
screen-space bounds. The suite verifies required components, containment, and
non-overlap with owned buttons and item cells. It clicks real widgets and rows,
then verifies visible ordering and the resulting server response so display and
input indices cannot silently diverge.

Screenshots use the maximized VM window, English (`en_us`), each scenario's fixture,
and cursor position. Snapshots record GUI dimensions and scale. They remain
human evidence for clipping, spacing, and color. Full
frame pixel equality is not required; a cropped golden comparison is added
only for a stable region with a demonstrated regression.

## Results and failures

Each target, profile, and scenario receives its own directory:

```text
build/ui-smoke/1.21.1-neoforge/compatible/standard-ae2/
  status.json
  launcher.stdout.log
  launcher.stderr.log
  evidence/
    result.json
    resolved-mods.json
    latest.log
    plan-default.png
    status-progress.png
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
