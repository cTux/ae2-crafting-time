# Automated UI Testing Implementation Plan

Implement the suite in working vertical slices. Each slice leaves one runnable
check and does not add the next loader or scenario until the current one passes.

## Slice 1: Runner contracts

1. Add `scripts/ui-smoke-coverage.json`, keyed by target and profile, without
   duplicating dependency versions from either matrix.
2. Add `scripts/run-ui-smoke.ps1` matrix selection, result directories, status
   classification, and exact-process-tree cleanup.
3. Reuse `scripts/run-client.ps1 -ResolveOnly`; add only the launch arguments
   needed for driver mode and result paths. Run clients with `--no-daemon` so
   each launch owns its Gradle and Minecraft process tree.
4. Add `scripts/test-run-ui-smoke.ps1` with temporary fake matrices/results to
   cover matrix mismatch, missing dispositions, compatible versus latest exit
   behavior, incomplete results, missing screenshots, and exact cleanup targets.

Gate: runner contract tests pass without launching Minecraft, and every current
top-level matrix project has one explicit coverage disposition (**A1**, **A3**,
**A6**, **A7**).

## Slice 2: Forge 1.20.1 driver artifact

1. Add the smallest `testDriver` source set and `testDriverJar` task to
   `:mc_1_20_1_forge`.
2. Forward the `uiSmoke*` Gradle properties to the client as the documented
   `ae2ct.uiSmoke.*` system properties.
3. Register `ae2craftingtime_test_driver` only in explicit test mode and enforce
   the exact production-mod version contract.
4. Add pure result/checklist code under `test-driver/src/main/java` and the
   Forge bootstrap plus 1.20.1 screen adapter only where needed.
5. Write the driver JAR to `build/test-driver`; keep it out of `dist`.
6. Extend artifact checks to reject the driver mod ID/classes from production
   JARs and reject driver artifacts from release/deploy discovery.
7. Add unit/structural checks for activation, version mismatch, atomic result
   writing, bounded fields, and packaging separation.

Gate: the driver and production JAR build separately, the driver is inert by
default, and production/release artifacts contain no driver content (**A8**).

## Slice 3: Marked disposable fixture

1. Create the minimal Forge 1.20.1 source world under
   `versions/1.20.1-forge/run/saves/ae2-crafting-time/` with one terminal and a
   deterministic craftable output.
2. Store fixture schema, target, scenario IDs, logical markers, expected output,
   and required block positions in the driver marker.
3. Make the runner hash, copy, uniquely name, and later remove the disposable
   world.
4. Make the driver reject multiplayer, source-world names, missing/mismatched
   markers, and non-test launches.
5. Add runner checks proving cleanup targets stay under the selected runtime
   directory and the source fixture hash is unchanged.

Gate: the driver enters only the copied marked world, and a complete run leaves
the tracked fixture byte-for-byte unchanged (**A2**).

## Slice 4: Forge Crafting Plan scenario

1. Implement the bounded tick-driven states through `PLAN_ASSERTIONS`.
2. Observe final Crafting Plan rows, translation/output identities, TTC or
   `No data yet`, total TTC, badge/widget/item rectangles, and visible order.
3. Click the real sort widget through all modes and verify visible order after
   each click.
4. Hover a known row and verify its tooltip; exercise details and reset input
   against the row that is visibly targeted.
5. Capture stable base and tooltip screenshots, write the atomic result, and
   request clean shutdown once.
6. Run the scenario against the pinned Forge client and retain its result as the
   first end-to-end smoke artifact.

Gate: `run-ui-smoke.ps1 -Target 1.20.1-forge -Scenario craft-plan` completes
unattended with semantic evidence and screenshots, while a deliberately changed
expectation fails (**A4**, **A5**, **A7**).

## Slice 5: Crafting Status and job completion

1. Extend the fixture with deterministic waiting, running, delayed, and
   completed states without adding generic world-edit tools.
2. Extend the state machine through submission, Crafting Status, and output
   completion.
3. Verify row TTC, header total placement, sort modes, tooltip, details/reset
   targeting, state transitions, and expected produced amount.
4. Add named failure fixtures or test-only expectations for each state-machine
   timeout and evidence path.

Gate: the standard AE2 scenario satisfies every behavior in **A4**, and failed
states still produce complete evidence before shutdown.

## Slice 6: Remaining targets

Port in this order so reuse is proven before abstraction:

1. 1.20.1 Fabric: reuse only code demonstrated loader-independent by Forge.
2. 1.21.1 NeoForge: reuse the 1.20.1 screen adapter only where APIs match.
3. 26.1.2 NeoForge: add its separate Minecraft/AE2 adapter.

For each target, add its driver task, bootstrap, marked fixture, packaging
checks, and pinned standard-scenario result before starting the next port.

Gate: the no-argument command runs all four compatible targets sequentially and
fails for any required target or scenario (**A1**-**A8**).

## Slice 7: Optional integrations and latest diagnostics

1. Add direct UI and direct behavior scenarios in the spec's table order only
   when the matching fixture is runnable.
2. Report all other installed projects through their declared coexistence,
   tooling, exclusion, or not-applicable checks.
3. Report Fabric Crafting Tree as `NOT_APPLICABLE` while upstream has no Fabric artifact; require a direct fixture if a compatible artifact becomes available.
4. Run latest profiles with the same scenario/result contract and normalize
   resolution/startup failures to `DIAGNOSTIC_FAILURE`.
5. Add interactive loopback MCP only after automatic failure evidence is stable;
   verify token, allowlist, size, timeout, thread, world, and multiplayer bounds.

Gate: each selected profile produces a complete coverage ledger, compatible
results retain release-gate semantics, and latest failures remain attributable
diagnostics (**A3**, **A5**, **A6**).

## Final verification

After each implementation commit's hook-created PR exists, run only the checks
owned by that slice, then read GitHub CI separately. Before completion:

1. Run PowerShell runner contract tests and Gradle unit/structural tests.
2. Run all four compatible clients sequentially through the standard scenario.
3. Run latest profiles and confirm their failures do not change compatible
   classifications.
4. Inspect every result directory, screenshot, resolved-mod manifest, log, and
   cleanup record.
5. Compare the source fixture hashes and production JAR contents.
6. Run the repository warning/error sweep and fix repository-owned warnings.
7. Run `git diff --check` and documentation-link checks.

Done means **A1** through **A8** pass, CI is green, every required scenario is
runnable, and no test-driver file can enter a published artifact.
