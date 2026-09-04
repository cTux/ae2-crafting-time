# Automated UI Testing Implementation Plan

## Planned change-based selection and standard-case split

This is the next feature, not a claim that the historical slices below are
unfinished. Implement CS-01 through CS-09 from [the spec](spec.md), following
[the researched design](technical-design.md#change-based-selection-research-and-design).
The current task delivers planning documents only; do not launch Minecraft or
implement the feature as part of creating its issue.

### 1. Define and test the plan contract

- Add `scripts/get-ui-smoke-plan.ps1`, `ui-smoke-impact.json` and
  `ui-smoke-groups.json`. Implement the exact source ownership, group membership,
  CLI constraints, path fallback and language-key rules in the design.
- Use temporary Git repositories in `scripts/test-ui-smoke-plan.ps1` to prove
  merge-base behavior, changes committed/staged/unstaged/untracked, empty diffs,
  renames across targets, deletion, non-ASCII/whitespace/newline filenames,
  conflicted files, missing refs, and native-command failure handling.
- Cover unions, broad-rule dominance, docs/test-only NOT_REQUIRED, unknown
  runtime fallback, common versus version source sets, JSON values/keys,
  malformed/duplicate-key JSON, and a DELAYED-only change in a mixed renderer
  deliberately selecting all status cases.
- Validate all IDs against release/suite/coverage sources. Test mapping errors,
  unsupported cases, nested/duplicate groups, and source-set inclusion/exclusion
  parity, including shared standard-flow use by 26.1.2.
- Test fingerprints before build and bundle sealing; stale HEAD, local content
  or rule changes reject execution. Plan-only must have zero build/VM effects.

Gate: CA-01 through CA-07, CA-10 and CA-12 have deterministic executable checks;
the report explains every selected or omitted path. No runtime is needed yet.

### 2. Split the standard flow without losing checks

- Refactor shared `StandardAe2Scenario` into the six leaf modes specified in the
  design, using named stages and the existing `StandardCraftFixture` helpers.
  Trace all callers first: `CraftPlanScenario`, `AddonCpuFixture`, `DriverOptions`,
  `DriverResult`, both target runtime implementations, and host result validation.
- Keep setup local to each leaf. Add actual waiting recovery, running progress,
  delayed tooltip/styling/recovery assertions and their named screenshots.
  Preserve UI submission and real output/sample checks in `craft-lifecycle`.
- Register leaf IDs and exact check sets; remove the monolithic standard ID as
  a raw driver case only after host alias expansion works. Retain ordinary
  `craft-plan` and the three existing blocked-status names.
- Reuse the common driver source for all four targets. Change a target adapter
  only when its native APIs require it; check 26.1.2 input/render/runtime
  replacements explicitly. Add no production source changes solely for tests.
- Extend `TestDriverCoreTest` and relevant driver boundary checks for leaf
  dispatch, required-check rejection, independent initialization, transitions,
  timeout/failure evidence, and modifier cleanup. Put Minecraft-free decisions
  in tested shared driver code; keep normal project coverage gates intact.

Gate: every old standard check maps to a runnable leaf, all new leaf checks have
bounded failure paths, and the full lifecycle still proves the original journey.

### 3. Carry selected case lists through the existing runner

- Add changed/plan-only parameters to the public host and matrix scripts.
  Preserve no-argument full mode. Reject conflicting changed-mode overrides.
- Pass per-target lists through `invoke-ui-smoke-codexvm.ps1`,
  `run-ui-smoke-codexvm.ps1`, `run-ui-smoke.ps1` and
  `prepare-ui-smoke-suite.ps1`; inspect the actual dispatch argument encoding
  so PowerShell arrays cannot be collapsed or interpreted as commands.
- Expand and deduplicate host groups before Java launch; preserve target lists
  containing `standard-ae2`. Raise the PowerShell and `SuitePlan` maximum to 64,
  retaining unique scenario/world validation. Test 1, 32, 34, 64 and 65 cases.
- Extend `test-ui-smoke-matrix.ps1`, `test-run-ui-smoke.ps1`,
  `test-prepare-ui-smoke-suite.ps1`, `test-prepare-ui-smoke-launch.ps1` and
  `SuitePlanTest` at the affected boundaries. Prove per-target differences,
  standard alias expansion, one launch per graph, manual leaf mode, adapter
  obligations, unchanged timeouts, and refusal to continue with an unconfirmed PID.

Gate: CA-08 and CA-09 work through the runner contract tests; Forge's 34-case
expanded list is accepted without truncation or an extra launch.

### 4. Preserve coverage meaning and update workflow documentation

- Extend `get-ui-smoke-coverage.ps1` and campaign aggregation for group-to-leaf
  results. Only six passing leaves produce a standard-group pass; unselected
  leaves stay NOT_RUN. Test failures, missing screenshots, partial suites,
  setup failures, stale reports and latest diagnostic classifications.
- Record selection reasons, commit/worktree identity, graph/adapter identity,
  immutable artifact hashes, and leaf evidence. Keep old archive contents intact.
- Update this feature's original standard-flow sections, `docs/test-driver`
  spec/design/plan, `docs/ui-smoke-evidence.md`, `docs/dev-client.md`, and
  `docs/dependencies.md` where group/coverage wording changes. Explain the
  raw JVM `standard-ae2` migration and unchanged host alias.
- Update development and prepared-smoke skills to choose changed mode for
  authorized focused verification and full mode for full/release requests.
  Update the Prism suite recipe for alias expansion without changing pack
  selection or adding automatic pack launches. Do not add a GitHub VM workflow
  or modify the post-commit hook to launch clients.

Gate: CA-09 through CA-11 are reflected consistently in reports and instructions;
no document describes focused coverage as a full release gate.

### 5. Verify all four real clients and deliver

Follow repository commit ordering: self-review the full diff, make one
conventional feature commit, and let the hook create the PR before local test
execution. Then run the selector and changed runner self-tests above; report
GitHub build/test/coverage checks separately. Never weaken coverage thresholds.

After contract checks pass, use `run-ae2-client-smoke` and CodexVM:

1. Run each of the six leaves as an independent invocation on each of the four
   targets. They must succeed without a prior leaf run or retained world.
2. Run `standard-ae2` as a group on each target and verify the six leaf outcomes,
   fresh worlds and single recorded PID. Compare check coverage with the old
   18-check standard contract, not historical screenshot pixels.
3. Exercise a genuine target-local change and a shared delayed-specific change
   in disposable verification branches. Capture plan and actual launch identities:
   one target in the first case; delayed leaf on all four in the second. Remove
   verification-only edits before the final candidate build. Broad fallback and
   failure paths are covered by deterministic tests, not invented smoke passes.
4. Run all four expanded full suites, plus required newest-adapter focused graphs
   under SP-01/SP-02. Do not rebase automatically or repeat full smoke solely
   because of a rebase. Do not demand runtime smoke of older adapters.
5. Review every distinct English screenshot and semantic result, archive evidence,
   confirm fixture hashes, exact PID exit and removal of disposable worlds, and
   verify no driver/planner material entered production JARs. Record measured
   timings, failed attempts and any timeout evidence. No estimated speedup claim.
6. Read the PR and CI state back. A known unrelated integration failure, including
   [ProjectCell #213](https://github.com/cTux/ae2-crafting-time/issues/213), remains
   visible and blocks a full PASS; never omit it to certify the selector.

Completion requires all CA-01 through CA-12, preserved old standard assertions,
reviewed independent/group/full runtime evidence, truthful coverage reports and
green required CI. Named-modpack installation is not required to prove this
prepared-client selector; existing explicit Prism behavior must remain intact.

| Requirements | Implementation slices |
| --- | --- |
| CS-01/02/03, CA-01..07/12 | 1, 3, 4, 5 |
| CS-04/07, CA-10 | 1, 3, 4 |
| CS-05/06, CA-08 | 2, 3, 5 |
| CS-08, CA-11 | 3, 4, 5 |
| CS-09, CA-09 | 2, 3, 4, 5 |

## Follow-up: newest adapter and English-only smoke

Implement [SP-01 through SP-04](spec.md#smoke-policy) before claiming the current
runner enforces them:

1. Reuse the adapter catalogue/selection snapshot to choose required direct
   cases per target. Record older-adapter cases as policy skips. Add focused
   newest-adapter fixtures only where compatible pins exercise an older one;
   preserve named modpack versions and the existing development locks.
2. Set and verify `en_us` in the shared and 26.1.2 drivers. Remove language-switch
   states, Ukrainian screenshot requirements, and matching result-validator
   requirements together. Retain every distinct English behavior checkpoint.
3. Cover selection mismatch, older-only graphs, newest-fixture setup failure,
   and language enforcement with the existing runner/driver tests. Preserve
   translation-key/placeholder checks for both supported product languages.
4. Run required newest-adapter smoke in English on applicable targets. Record
   selected IDs, artifacts, language, screenshots, and policy skips. Do not run
   an old-adapter or Ukrainian campaign as an additional completion gate.

The existing implementation slices below follow this policy. Done means newest
adapter IDs and `en_us` are verified, old variants retain non-smoke coverage,
and no required scenario fails merely because an old/language duplicate was
intentionally removed.

Implement the suite in working vertical slices. Each slice leaves one runnable
check and does not add the next loader or scenario until the current one passes.

## Slice 1: Runner contracts

1. Add `scripts/ui-smoke-coverage.json`, keyed by target and profile, without
   duplicating dependency versions from either matrix.
2. Add `scripts/run-ui-smoke.ps1` matrix selection, result directories, status
   classification, and exact-process-tree cleanup.
3. Reuse `scripts/run-client.ps1 -ResolveOnly -Packaged` for host-built bundles.
   Launch the installed native loader on guest-local NTFS and record its exact
   process identity; Gradle stays on the host.
4. Add `scripts/test-run-ui-smoke.ps1` with temporary fake matrices/results to
   cover matrix mismatch, missing dispositions, compatible versus latest exit
   behavior, incomplete results, missing screenshots, and exact cleanup targets.

Gate: runner contract tests pass without launching Minecraft, and every current
top-level matrix project has one explicit coverage disposition (**A1**, **A3**,
**A6**, **A7**).

## Slice 2: Forge 1.20.1 driver artifact

1. Add the smallest `testDriver` source set and `testDriverJar` task to
   `:mc_1_20_1_forge`.
2. Pass the documented `ae2craftingtime.test.*` system properties through the
   prepared native launch manifest.
3. Register `ae2craftingtime_test_driver` only in explicit test mode and enforce
   the exact production-mod version contract.
4. Add shared result/checklist code under `shared/src/testDriver1201/java` and the
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
