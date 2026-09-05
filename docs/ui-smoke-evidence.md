# UI smoke evidence

## Startup integration diagnostics, 2026-09-05

Issue #193 / PR #256 remains **verification incomplete**. Shared diagnostics and
reflection tests reached 100% line and branch coverage, and all four packaged
targets built. The first runtime round exposed Fabric's common entrypoint loading
client rendering classes on a dedicated server; separating the client entrypoint
made that dedicated run pass.

At revision `d12696c8`, all four core-only dedicated starts and all four compatible
dedicated starts passed. Each target also passed the six-case `standard-ae2`
client group, using English and fresh worlds. These grouped passes do not replace
the full addon matrix. Initial full-suite failures remain retained: Forge timed
out opening the saved craft-plan terminal; Fabric's saved craft-plan fixture hit
an AE2 `readFromNBT` null final output; NeoForge 1.21 initially failed in FML's
early-window configuration. The initial 26.1 run was rejected as a stale plan.

The initial campaign is `20260905T121910633Z`; the NeoForge 26 core-group retry is
`20260905T123612859Z`; the Fabric core-group retry is `20260905T125344014Z`.
Reports include actual artifact inventories, selected adapters, case timestamps,
screenshots and original logs. `scripts/check-startup-diagnostics.ps1` checks
context, unique inventory, required registration and dedicated-side exclusions;
its `-OptionalAbsent` check requires all 26 optional rows to be skipped.

Still required before approval: core-only client starts, every available supported
addon path, transformed-target recoverable failures with subsequent successful
crafting, and intentional fatal dependency/hook failures. Unavailable Fabric
Crafting Tree runtime coverage remains unverified. No complete smoke pass or
merge readiness is claimed.

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

The primary compatible suites currently contain 34 cases for Forge 1.20.1,
16 for Fabric 1.20.1, 30 for NeoForge 1.21.1 and 19 for NeoForge 26.1.2. Read the
expanded plan for the current case list and any separate required graphs.

For multiple scenarios on the same installed mod graph, launch Minecraft once.
Run the suite sequentially with a fresh disposable world per case, capturing each
case's screenshots before advancing. Retain the suite plan, one process ID,
ordered timestamps, and overall result alongside the per-mod evidence. A crash
or failed case leaves later cases `NOT_RUN`; do not hide it with automatic retries.
Different mod graphs or incompatible original/fork artifacts require separate runs.

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
