# Testing And Change Workflow

## Before Editing

1. Run `scripts/setup-git.ps1` once per clone and work on a named branch.
2. Read the closest design doc and inspect the full call chain, tests, target counterparts, resource registrations, and metadata.
3. Decide the highest shared source set that can own the behavior. Pure logic belongs in `shared/src/main/java`.
4. Define the test cases before code: success, every branch, boundaries, invalid inputs, empty/missing state, reset/cancellation, and version/serialization failure where applicable.

Do not run Gradle tests before the first conventional commit creates the PR.
This repo deliberately uses the required GitHub checks as the first test run.

## 100% Coverage Contract

The root Gradle build applies JaCoCo. For `:shared`, `jacocoTestCoverageVerification` requires both line and branch ratios of `1.0`; `jacocoTestReport` depends on that verification. Minecraft-facing `com/ctux/ae2craftingtime/mc1201/**` is excluded from the metric, so never hide new logic there.

For every executable change:

- Put decisions and calculations in `shared/src/main/java` and cover every line and branch under `shared/src/test/java`.
- Use parameterized tests only when they reduce duplication without hiding which boundary failed.
- Test externally visible behavior and state transitions, not private implementation details.
- Include both sides of boolean conditions, switch cases, early returns, exceptional paths, min/max clamps, empty optionals, and collection limits.
- Preserve 100% coverage while fixing a bug by adding a regression test that fails on the original code.
- Keep loader, mixin, packet, NBT, and registration adapters as delegation plus API conversion. Extract any branch that can be pure.
- For PowerShell, extend the closest deterministic self-test to execute every changed branch. Release automation uses `scripts/test-deploy-changed.ps1`; add a focused self-test for other scripts when none exists.
- If necessary adapter behavior cannot be measured directly, add the closest boundary test and report the coverage limitation; do not call the work complete or claim 100% until the logic is covered.
- Never lower thresholds, broaden exclusions, mark code generated, or add meaningless execution-only tests to make the number pass.

Do not add fake tests for prose or static data. Check the thing that can really
break instead: JSON/TOML shape, matching locale keys and placeholders, mixin
membership, dependency truth, links and paths, or release-matrix consistency.

## Existing Test Homes

| Change | Test home |
| --- | --- |
| Profiling, samples, stalls, accuracy, TTC, cache, sort, color, limits, rate limits, DTOs | `shared/src/test/java/com/ctux/ae2craftingtime/core` |
| 1.20.1/1.21.1 packet codecs and bounds | `shared/src/mc1201Test/java/.../StatsPacketTest.java`, included by target modules |
| Loader SavedData formats | `versions/<target>/src/test/java/.../Ae2CraftingTimeSavedDataTest.java` |
| Controller-derived network identity | `versions/1.20.1-forge/src/test/java/.../ProfilerBridgeTest.java` |
| Release automation or matrix | `scripts/test-deploy-changed.ps1`; also use `ae2-crafting-time-release` |

When adding a target, do not copy every test. Reuse shared test source sets, then add only API-boundary tests unique to that loader or Minecraft version.

## Compatibility Sweep

Inspect all affected rows even if the implementation is shared:

- `1.20.1 Forge`: Java 17, SimpleChannel, Forge SavedData/config/events.
- `1.20.1 Fabric`: Java 17, Fabric networking/lifecycle, local config parser.
- `1.21.1 NeoForge`: Java 21, custom payloads, NeoForge SavedData/config/events.
- `26.1.2 NeoForge`: Java 25, AE2 26/Minecraft 26 API ports and reduced optional UI set.

For packets, NBT, mixins, translations, config, or dependency changes, compare every affected implementation and metadata file. Wire-layout changes require a compatible protocol strategy or coordinated version boundary; persisted-format changes require explicit version handling.

## Commit, CI, and Readback

1. Review `git diff --check`, the complete diff, and untracked files. Skill validation and static inspection are allowed before the PR; repository test execution is not.
2. Commit one fix or feature using a conventional title. The tracked post-commit hook pushes and creates or updates the PR.
3. Read the PR back and report the current required-check state. Do not wait for
   GitHub checks to finish unless the user explicitly asks you to wait. The
   authoritative workflow runs `./gradlew test jacocoTestReport`; JaCoCo verifies
   100% line and branch coverage for shared logic and Codecov upload must
   succeed. After the PR exists, run any required PowerShell self-test for
   changed scripts.
4. If a check has already failed, inspect the exact failure, add the smallest
   root-cause correction and regression coverage, commit it separately, and let
   the hook update the PR.
5. Before handoff, read the final PR diff and current check state. Report only
   observed passes, failures, pending checks, remaining warnings, excluded
   adapter boundaries, and uncommitted leftovers.

Do not merge, publish, deploy, or change release state unless the user separately authorizes that operation.
