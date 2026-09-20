# Finite Outlier Validation Implementation Plan

Implement [#469](https://github.com/cTux/ae2-crafting-time/issues/469) from the
[specification](spec.md) and [technical design](technical-design.md).
This is a plan; implementation and test results are not claimed.

## 1. Add the three validation boundaries

- Add the small shared `ConfigNumbers` helper and delegate Fabric's existing
  double parsing to it. Preserve current fallback, bounds, and file-load flow.
- Add the finite-value guard to `CraftProfiler` without imposing a new upper
  limit. Add the finite-throughput guard to `TimeEstimate.seconds`.
- Recheck all callers listed in the design and both bridge variants. Keep
  Forge/NeoForge config, UI adapters, wire layouts, and persistence unchanged.

Gate: C1-C4 have one owner each, with no duplicate caller-specific guards.

## 2. Add focused regression coverage

- `ConfigNumbersTest`: NaN, both infinities, positive/negative exponent overflow,
  malformed text, finite values inside the range, both exact bounds, and finite
  values outside each bound. Use a nondefault valid fallback as well as `4.0`.
- `CraftProfilerTest`: reject every nonfinite multiplier and a finite value
  below one; accept one, the default, and a finite value above 1000. Preserve
  the invalid sample-limit assertion.
- `TimeEstimateTest`: reject nonfinite, zero, and negative rates for positive
  work, and nonpositive amounts with valid rates. Assert both empty APIs and
  preserve normal rounding and low-confidence formatting.
- Add a four/five-sample trace using the parser's effective fallback. Assert
  finite positive rate and nonzero TTC at both counts, and verify normal
  five-sample filtering with an excluded outlier and unchanged confidence.
- Add `versions/1.20.1-fabric/src/test/java/com/ctux/ae2craftingtime/mc1201/
  Ae2CraftingTimeConfigTest.java`. Load temporary config files through `load`,
  covering invalid initial input, valid-then-invalid duplicate keys, finite
  clamping and valid values. Restore the static multiplier before/after cases
  using valid config. Do not expose new production setters for tests.

Gate: the new assertions distinguish the original invalid-input behavior and
cover every added shared line and branch. Test execution waits until the PR.

## 3. Commit, then verify the exact implementation head

Follow `AGENTS.md`: one conventional implementation commit, with the configured
post-commit hook creating/updating the PR before local test execution.
Use the host's verified Java 21 for Gradle and installed 17/21/25 toolchains;
do not launch the multi-project build on Java 25. After the PR exists, run:

```powershell
.\gradlew.bat :shared:test :shared:jacocoTestReport
.\gradlew.bat :fabric_1_20_1:test --tests '*Ae2CraftingTimeConfigTest'
git diff --check
```

`jacocoTestReport` depends on shared 100% line/branch verification. Fabric's test
task also checks production/test-driver artifacts; report a failure in that
prerequisite instead of bypassing it. The checked-in wrapper is Gradle 8.12;
JUnit Jupiter and JaCoCo are already configured. No new test runner is needed.

Verify current-head GitHub `Gradle tests`, all mod JAR builds, and required
coverage checks separately from local results. CI's full tests and all-target
builds cover compilation and existing regression gates for Forge 1.20.1,
Fabric 1.20.1, NeoForge 1.21.1, and NeoForge 26.1.2. Follow the live repository
merge policy rather than treating this list as a substitute for required checks.

## Minecraft smoke decision

Minecraft smoke is **not required** for this fix. Changed decisions are pure
numeric validation, and the actual Fabric file-loader boundary can run in its
existing JUnit task without a game. No event hook, renderer, loader registration,
protocol, or world lifecycle is changed. Existing optionals carry unknown values
to consumers; source review verifies that contract without a new visual claim.

Do not add a malformed-config driver scenario or change smoke selection policy.
The current `-Changed` selector has no narrow rule for these source files and
would request full consuming suites, including all four targets for shared
code. That broad fallback is not a specific reproduction of invalid startup
config. This plan does not invoke a smoke campaign or count a focused run as a
full-suite pass. If implementation reaches a game-facing seam outside this
design, revise the plan and inspect its runtime prerequisites before testing.

## Traceability and completion

| Criteria | Change | Required proof |
| --- | --- | --- |
| C1-C2 | Shared parser and Fabric delegation | Pure parser cases and real temporary-file load tests |
| C3 | Shared constructor guard | Invalid/valid multiplier and sample-limit tests |
| C4 | Shared estimate guard | Unknown-input, finite rounding and confidence tests |
| C5 | Existing filter fed valid fallback | Four/five-sample trace, rate/count/confidence/TTC assertions |
| C6 | Shared reuse, unchanged formats/adapters | Caller/diff review and current-head CI tests/builds |

Finish only when all mapped checks pass on the reviewed implementation head,
shared coverage remains 100%, and CI meets the live merge policy. Report any
unrun check as pending and do not claim Minecraft reproduction or visual proof.
