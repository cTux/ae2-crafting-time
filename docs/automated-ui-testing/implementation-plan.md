# Automated UI Testing Implementation Plan

Lifecycle: see scope status and evidence for [base workflow](spec.md) and [unattended qualification](spec.md#unattended-evidence-gate).

The runner, evidence gate, shared-world suites, change selection, and prepared
client qualification are complete. Their current contracts live in the
[specification](spec.md) and [technical design](technical-design.md); merged pull
requests retain the implementation history.

## Dependency selection cache identity

Lifecycle and criteria: [cache identity](spec.md#dependency-selection-cache-identity).
Implementation follows the [design](technical-design.md#dependency-selection-cache-identity).

1. Add the smallest shared canonical selection calculation used by the matrix
   path and cache seal. Use effective `graph.projectId`, preserve case, sort
   ordinally and remove duplicates. Extend the seal schema and compare the full
   selection on reuse; reject old or missing contracts (**DC-01–03**).
2. Wire the matrix and all cache consumers: `prepare-ui-smoke-adapters.ps1`,
   `invoke-ui-smoke-codexvm.ps1` and `dedicated-source-contract.ps1`. Preserve
   request-aware versus sealed-bundle semantics and all existing validation
   and evidence retention (**DC-04**).
3. Extend `test-ui-smoke-bundle-cache.ps1` and `test-ui-smoke-matrix.ps1` with
   same-source focused/full/different-set cases, order and duplicate reuse,
   base/catalogue and target/profile separation, mismatched reuse, legacy and
   missing selection rejection, and unchanged bytes after rejection
   (**DC-01–04**). Assert resolver-call counts and sealed identities, not just
   path strings. The focused-then-full regression must fail on the original code.
4. Update fixture callers in `test-prepare-ui-smoke-adapters.ps1`,
   `test-invoke-ui-smoke-bundle-contract.ps1` and
   `test-prepare-dedicated-ui-smoke-server.ps1` as needed. Exercise changed
   adapter/connected consumer branches with temporary deterministic fixtures;
   do not silently require packaged JARs or a new runtime campaign (**DC-04**).
5. After the hook creates the implementation PR, run the cache, matrix and
   planner self-tests plus affected consumer checks under Windows PowerShell
   5.1 and PowerShell 7. Both interpreters and Git are available; existing cache
   and matrix checks use synthetic temporary fixtures. The existing adapter
   integration test needs production/test-driver JARs and a JDK: use a
   deterministic fixture for the changed script boundary unless those artifacts
   are explicitly prepared. Keep adapter catalogue behavior unchanged. Record
   exact commands, fixture limits and current-head CI separately.

Done means **DC-01–04** pass, every changed script branch has deterministic
coverage, review and required CI pass, and implementation is merged. Update the
spec's scope status with that evidence. No Minecraft or VM launch is required.

## Java runtime metadata

Lifecycle and criteria: [Java runtime metadata](spec.md#java-runtime-metadata).
Implementation follows the [design](technical-design.md#java-runtime-metadata).

1. In `scripts/run-ui-smoke.ps1`, initialize and reset the native launch state
   before preparation. Change the shared status writer to derive `javaHome`
   from the successfully prepared executable, or emit null when unavailable.
   Record the wrapper environment separately as `wrapperJavaHome` (**JM-01–04**).
2. Extend `scripts/test-run-ui-smoke.ps1` using its existing production-AST test
   seam. Read emitted receipts and cover:
   - different wrapper/native homes and matching homes (**JM-01**, **JM-04**);
   - unresolved initial state, preparation failure, failed continuation after
     an earlier successful selection, and the legacy wrapper path (**JM-02**);
   - prepare-only state with no PID, successful running/validating/final state,
     startup failure after preparation, and a later phase's selection
     (**JM-03**).
   Exercise every changed branch and preserve/restore the test's environment.
   The mismatch case must fail with the original status writer.
3. Review that native launch and target/Java validation remain unchanged and
   schema 2/process identity still agree with the documented fields (**JM-04**).
4. After the hook creates the implementation PR, run
   `scripts/test-run-ui-smoke.ps1` and
   `scripts/test-prepare-ui-smoke-launch.ps1`. The first checks receipt behavior;
   the second guards the adjacent prepared-launch selection/validation boundary.
   Use Windows PowerShell, which the existing fake-client runner invokes.
   These checks use temporary synthetic fixtures and need no VM, installed JDK,
   prepared Minecraft client or new test infrastructure. Report GitHub CI
   separately and bind all results to the implementation head.

Done means all four criteria pass their receipt checks, adjacent launcher checks
pass, review and required CI pass, and implementation is merged. Update this
scope's canonical status with the implementation and verification evidence;
retain the completed baseline's status. No Minecraft smoke campaign is required
for this metadata-only correction.

## Enforce newest-adapter and English-only smoke policy

Implement [SP-01 through SP-04](spec.md#smoke-policy):

1. Reuse the adapter catalogue and selection snapshot to choose required direct
   cases per target. Record older adapters as policy skips. Add a focused newest
   adapter fixture only when compatible pins exercise an older contract.
2. Set and verify `en_us` in the shared and 26.1.2 drivers. Remove language
   switching and Ukrainian screenshot requirements while keeping static English
   and Ukrainian translation checks.
3. Cover selection mismatch, older-only graphs, newest-fixture setup failure,
   and language enforcement with the existing runner and driver tests.
4. Run the required newest-adapter smoke in English on applicable targets. Record
   selected IDs, artifacts, language, screenshots, and policy skips.

Done means every required target verifies its newest supported adapter in
English, older variants keep non-smoke coverage, and no duplicate language or
old-adapter campaign remains in the completion gate.
