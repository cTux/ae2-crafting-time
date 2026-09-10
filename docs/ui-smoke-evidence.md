# UI smoke evidence

## Automated evidence gate

The host campaign now writes `gate.json` and an immutable archive report after
native semantic/log checks. Exit 0 means PASS (or the planner's NOT_REQUIRED),
exit 1 means failure, and exit 2 means visual review is still required. Raw
scenario outcomes stay unchanged. `-ArchiveRoot` selects an explicit archive
location; it must be writable before any client starts.

Each new capture pairs its PNG hash, case/world ID, render-frame ID, framebuffer
size and renderer with the frozen semantic snapshot. The host decodes the PNG
and compares only explicitly qualified regions from `scripts/ui-smoke-visuals.json`.
Unqualified checkpoints remain REVIEW_REQUIRED; an empty catalogue approves no
images. Baselines and masks are reviewed test fixtures, never generated from a
candidate automatically. A mismatch, corrupt image or stale identity cannot pass.

Inspect every REVIEW_REQUIRED checkpoint and every mismatch. A checkpoint with
PASS from a qualified automatic contract needs no separate image interpretation.
This exception applies only to that checkpoint and render identity. Preserve
all full images and results, including failures. Review happens after the suite,
not between actions. Historical campaigns without capture bindings retain the
manual workflow and cannot receive an automatic gate retroactively.

For a named Prism campaign, normalize its results into the same campaign layout
and use `scripts/complete-ui-smoke-evidence.ps1`; launch and graph eligibility
still follow the named-modpack skill. Keep the pack's original mods and resources.

The [unattended smoke research](automated-ui-testing/automation-research.md)
and [#347](https://github.com/cTux/ae2-crafting-time/issues/347) explain the design.
The gate above changes image review only for explicitly qualified checkpoints;
the remaining inspection and archive requirements still apply.

## Startup integration diagnostics, 2026-09-05

Issue #193 / PR #256 adds observed integration status and bounded read recovery.
Shared diagnostics/reflection tests retain 100% line and branch coverage. All
four production targets build. Runtime revisions and exact artifacts are recorded
per campaign; the final NeoEco check uses its exact Forge 47.4.23 / 20.4.2 graph.

| Runtime check | Verified targets |
| --- | --- |
| Core-only and compatible startup, client and dedicated server | All four |
| Core-only client crafting and six native AE2 plan/status leaves | All four |
| Enabled/disabled profiling and original fatal missing-AE2 diagnostic | All four |
| Transformed Requester read failure, preserved host screen, then core crafting in the same JVM | Forge/Fabric 1.20.1 and NeoForge 1.21.1 |
| Transformed Tree read failure, restored layout/tooltip/click behavior, then core crafting in the same JVM | Forge 1.20.1 and NeoForge 1.21.1 |
| Transformed AdvancedAE selected-CPU read failure, retained grid, then core crafting | Forge 1.20.1 and both NeoForge targets |

Five UI recovery runs and three AdvancedAE server runs each retain one WARN with
the original reflection cause. Core crafting passes afterward. No fixture is
packaged in production. Fabric's common entrypoint no longer loads client-only
rendering classes on dedicated startup. The saved Forge fixture's terminal is
reachable and its stale in-progress jobs were removed without changing its
other block data.

The full campaign `20260905T152248022Z/compatible` remains **FAIL**. Its primary
Forge, Fabric and NeoForge 26 runs passed every available addon path before the
pre-existing `no-provider-status` recovery failure. NeoForge 1.21 exposed a second
NeoEco completion event; after removing the redundant cancellation observer,
all 16 remaining scenarios passed. Cancellation already delegates to the required
finish hook. AdvancedAE, native AE2 and LightningTech use the same single-terminal
observation rule; the strict completion assertion was not weakened.

The newest Forge NeoEco graph exposed a long return value where its diagnostic
observer expected an int. The observer now accepts both numeric return types.
Its isolated fixture mounts a native item cell and supplies ingredients instead
of relying on other addons' saved storage. Original setup and crash attempts remain
under `build/issue193/latest-neoeco`, beside the final normal/FastPath evidence.

The NeoForge continuation's runner envelope failed because the manually assembled
bundle lacked `expected-adapters.json`. Generating the catalogue from the packaged
driver and independently validating its results passed all 16 cases. The original
failed envelope is retained. Separate successful runs do not certify the failed
full campaign or assemble a new full-suite PASS.

The provider-restoration failure was reproduced with the pre-issue `aeee5ac4`
build and identical Forge graph/driver. The unchanged profiler retains NO_PROVIDER
after the live reason clears. This gameplay defect is outside the diagnostics fix.
An older NeoForge 26 re-entry run also logged an upstream AE2 clientbound packet
error during configuration; the source stack and original log remain recorded.
Neither failure is presented as a clean runtime result.

Startup inventory and deferred-transition audits passed on all four primary
client logs and the NeoForge continuation, with zero repeated integration/capability
confirmations across repeated screens, jobs and world changes. Unused hooks remain
pending. Shared-hook confirmations retain their addon-specific verification limit.

The maintainer explicitly excluded the unavailable Fabric 1.20.1 Crafting Tree
runtime cell after the original repository and Modrinth fork exposed no matching
build. Its diagnostic row remains present and its runtime coverage unverified.

Evidence is retained in the task workspace under `build/issue193` and
`build/ui-smoke/campaigns`, including inventories, hashes, exact process IDs,
screenshots, logs, original failures and focused reruns. Automatic approval review
blocked export to the external archive below; the worktree must be retained until
that evidence can be archived with authorization. `build/issue193/final-report.md`
maps checks to artifacts and measured timings. JDT LS was unavailable; no LSP pass
is claimed. GitHub build/test checks are reported separately in the PR.

For all new campaigns, follow the [smoke policy](automated-ui-testing/spec.md#smoke-policy):
exercise only the newest adapter per dependency/target and use English (`en_us`)
for every UI assertion and screenshot. Retained older adapters need non-smoke
checks, not extra runtime campaigns. Preserve historical bilingual evidence.
Record the actual selected adapter and language; a profile named `latest` is
not proof that the newest adapter ran. Older checkpoint counts below describe
existing campaigns/drivers and do not require new Ukrainian screenshots.

Keep screenshot evidence for named modpacks and prepared version clients using
the same archive layout:

Use the host campaign entry point `scripts/run-ui-smoke.ps1`. See
[Choosing smoke coverage](dev-client.md#choosing-smoke-coverage) for full,
targeted, grouped and change-based commands. The default full campaign includes
required focused adapter graphs as well as each target's primary suite. The
`invoke-ui-smoke-codexvm.ps1` wrapper also delegates to the campaign runner when
no internal bundle is supplied; use the documented host options to select scope.

The primary compatible suites currently contain 36 cases for Forge 1.20.1,
19 for Fabric 1.20.1, 32 for NeoForge 1.21.1 and 21 for NeoForge 26.1.2. Read the
expanded plan for the current case list and any separate required graphs. The
[2026-09-08 prepared-client report](automated-ui-testing/prepared-clients-2026-09-08.md)
records 122 passing cases across eight graphs, phase timings and remaining qualification work.

For multiple scenarios on the same installed mod graph, launch Minecraft once,
except for the `cpu-list-total-ttc` process-relaunch boundary described below.
Run the suite sequentially in one loaded disposable world with pristine fixture,
player, profiler and client-cache resets between cases, capturing each
case's screenshots before advancing. Retain the suite plan, one process ID,
ordered timestamps, and overall result alongside the per-mod evidence. A crash
or failed case leaves later cases `NOT_RUN`; do not hide it with automatic retries.
Different mod graphs or incompatible original/fork artifacts require separate runs.

Record the suite-plan schema and shared world ID. Schema 2 requires one world
load; a schema-1 diagnostic reload run must not be described as the one-world
benchmark. Include reset durations separately from initial world loading and UI
assertions. A stopped or partially completed run is not a full-suite timing.

The CPU-list regression deliberately uses two client processes. The first process
also performs one normal same-JVM disconnect/rejoin through the outer driver
lifecycle guard, with responses held, to prove the disconnect hook clears client
TTC state. It then writes an atomic continuation record and exits. The runner must
observe that exit before starting a second process and must reject a reused PID or
start time. The second process loads the same marked disposable save (or reconnects
to the same report-owned dedicated server), treats menu IDs, CPU serials, frames,
and monotonic time as phase-local, and matches the persisted physical network,
CPU-position and job identities. Its first status menu is captured while responses
remain held; only a later fresh authoritative response may restore totals. Retain
`relaunch-evidence.json`, the continuation hash, both process identities and logs,
the world and campaign/connection epoch, server/client sequences, all JAR hashes,
and the phase-local `cpu-list-checkpoints*.jsonl` ledgers.

Diagnostic resume provenance also includes the bundle's `base` or `catalogue`
mode and an ordinal hash of every managed JAR name and SHA-256. Phase 1 writes
both into the disposable world marker. Capture and restore must match them to
the selected bundle before scheduling Java; a world created by an addon graph
cannot be relabelled as a reduced-graph continuation.

Connected-dedicated source fixtures live under one explicitly chosen host root,
with one child per supported target. Each child is reusable only when its source
marker has schema 2, `sourceFixtureId: ae2-crafting-time`, role `source`, the
exact target, loader, required Java major, loader-launcher path/hash, and the
sorted dependency JAR name/hash set. The installed dependency set must exactly
match the sealed client bundle after excluding the production and test-driver
JARs, which are injected only into the disposable copy. The runner validates all inputs before copying,
never starts or mutates the source, and creates the only writable server beneath a
new report directory with a role `disposable` marker. Delete or replace only that
validated report-owned copy; preserve the marked sources for later campaigns. An
unmarked directory, a link, a mismatched loader/dependency set, or an existing
report is a setup failure rather than permission to repair the source in place.

### Timing and provenance

Publish timings after each completed target, including its required dependency
graphs. Read phase boundaries from the campaign, native status and driver logs.
Capture-write durations are already inside UI time; gate validation and archive
costs are already inside the campaign wrapper. Do not add them twice. If build,
dependency resolution and staging lack separate boundaries, report combined
preparation rather than inventing individual durations. Keep resets separate
from initial loading and UI work. Concurrent review windows are elapsed windows,
not additive client time; mark unmeasured work explicitly.

Count unique original PNGs separately from screenshot references: several checks
may point to one capture. Keep both the mapping and every original sidecar.

Bind each run to its captured source and JAR hashes. A later documentation-only
commit does not retag old captures. Verification of a small runtime change may
reuse earlier full coverage only after checking the actual delta and running
its affected checks; state both sources and the focused scope. A focused pass
never repairs a failed full-suite result or satisfies a requested new full run.

A host-validator defect may be checked against unchanged archived evidence with
the complete corrected validator. Retain the original verdict and write a new
receipt with the validator revision, artifact hashes and all checked conditions.
Do not rewrite the archive or describe that receipt as a new native execution.
Result check keys are exact and case-sensitive; JSON property order is not part
of the contract. Missing, extra or false checks remain failures.

```text
E:/games/mc-instances/.codex-test-results/ui-smoke/
  <modpacks|clients>/<pack-release-or-target>/<UTC-run-id>/
    report.md
    <mod-id>/<scenario>/<attempt>/
      <checkpoint>.png
      result.json
      latest.log
```

Use a new timestamped run directory; never replace an earlier run or failed
attempt. Runtime `build/ui-smoke` folders are temporary, not the final archive.
Copy the evidence before cleaning a runtime or removing a worktree.
For a single-launch suite, keep shared client logs once in `logs/` and link them
from every case's record instead of copying the same log for each integration.

Capture each distinct UI checkpoint for every tested integration: its screen,
TTC row and total, tooltip, sort modes, and post-craft result where applicable.
Map every requested check in `report.md` to its screenshot and semantic result.
A frame may support several checks only when it visibly shows each one. For a
server-only check such as a new profiling sample, keep the structured assertion
and a screenshot of the resulting UI; do not claim the image proves server state.

Record pass, fail, blocked, or not tested for every requested integration point.
Capture the current failure screen when possible. Never reuse another run's
image as evidence or mark missing driver coverage as a pass.

For screenshot-bearing runs, record the maximized framebuffer, `guiScale:0`
configuration, effective sidecar GUI scale, scaled width and height, and whether
the GUI bounds are contained. Compare a representative prepared client and
named modpack at the same framebuffer when verifying a scale change; keep the
screen, text, item cells, buttons, and tooltips readable and unclipped.

Record the exact pack/project/release or client target/profile, Minecraft and
loader versions, enabled mod inventory, tested commit, production and driver
JAR hashes, scenario, attempt, and timestamps. Preserve logs and result files
beside the screenshots, but exclude account data, tokens, and unrelated worlds.
Inspect the saved images and link the archive report in the final response.

Change-based campaigns also archive `selection.json` with comparison commits,
worktree fingerprint, rule hashes, per-path reasons and selected leaves. Keep
`artifact-hashes.json` with the sealed bundle identity. Standard evidence is
stored under each of the six leaf directories, with a `.json` semantic snapshot
beside every required `.png`. Group PASS requires all six leaves from this run;
unselected leaves stay NOT_RUN. Keep historical monolithic evidence unchanged.

For every targeted report, state the selection mode (`manual` or `changed`),
requested target/group/case, selected graph and cases, and what was not selected.
Keep `NOT_REQUIRED` separate from PASS. A group result cannot be assembled from
successful leaves in different runs, and an isolated rerun cannot turn the
original failed full campaign green. Record missing prepared loader versions
as setup failures; do not substitute a different loader to complete a plan.
