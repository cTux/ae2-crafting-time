# Implement running-job total recovery

Lifecycle and criteria: [spec](spec.md). Architecture: [design](technical-design.md).

## Delivery order

1. Merge this documentation scope before implementation. Add a new #636 comment
   recording the authorized restart scope and linking the merged trio; preserve
   the original report/body and the distinction from its unidentified cause.
2. On the fresh implementation branch, extend `CraftingJobEstimate` snapshot
   support, its pure bounded codec/pending state, and `CraftProfiler`'s minimal
   estimate access. Write focused tests for every new line/branch, including
   limits before allocation, partial progress and malformed payloads (R2-R4).
3. Add lifecycle readiness/generations to both `ProfilerBridge` families and
   the four loader lifecycle entrypoints. Hook tracking-disable transitions in
   `ServerOptionsRuntime`; preserve final save behavior and prevent same-JVM
   or reset revival (R4). Immediately prune disabled scopes from the existing
   estimate map during config acceptance. Test disable then enable with no
   intervening tick for both pending and attached graphs, including an unaffected
   scope that keeps its graph. Keep sample-only reset semantics: a graph may remain
   structurally present but cannot supply an estimate without current samples.
4. Add the exact adapter matrix from the design, its registrations/source-set
   selection and optional integration contracts. Use real UUID/network/scope
   identities and restore before completion; cover AE2LT's deferred job and
   NeoEco's restored grid. Add NBT/value boundary tests and hook/API inspection
   for each signature, including save-before-attachment and absent addons
   (R2-R5). Recheck current pinned/minimum artifacts; do not update dependencies.
5. Strengthen `CpuListTtcScenario` and its focused tests as described below (R1,
   R4). Reuse the existing two-process launcher, continuation and immutable
   resume bundles. No new general runner or addon restart framework.
6. Update existing GuideME English/Ukrainian and wiki source text for CPU totals,
   plus `docs/dependencies.md` where integration/test coverage descriptions
   change (R6). No new UI wording, options, packet or SavedData migration.
7. Follow the required conventional commit/hook-created PR ordering. Only then
   run checks and the focused smoke plan. Fix reviewed defects and repeat only
   affected verification on the new head. Record CI, local checks, manual
   image review and runtime evidence separately before merge/closure.

## Regression and runtime scope

The current relaunch phase uses `requiresJobRefresh` to cancel/resubmit the
second CPU when restored totals are absent. Remove that escape from both
rejoin/relaunch recovery checks. A missing recovered total must fail or time out.
Keep the earlier intentional replacement test as a separate case.

Persist the actual crafting-link UUID in the continuation/server observation;
output ID, amount and position alone do not prove the same job. Compare all
expected known busy jobs, not merely any one recovered total. Preserve a
genuinely unknown job as a control. Hold the first response to prove caches
start empty, then release it and check fresh server totals and matching selected
card/title. Feed real accepted output to a restored job and verify its amount
and estimate decrease without replacing the job; completion/cancellation clears
it. Existing partial/stalled formatting must remain intact.

Use `StandardCraftFixture`'s physical persistent connections for this scenario
and validate their compatibility with its removal/reordering cases. Resume
must attach to saved blocks/jobs without replacement placement. Do not call
`ProfilerBridge.rebindJobEstimate` from any fixture path used as recovery proof;
retain separate diagnostic observations of the first restored state. Extend
existing `TestDriverCoreTest` and scenario helpers for identity, all-job recovery
and failure gates. Keep held-response and clear-cache assertions.

| Check | Scope and acceptance |
| --- | --- |
| Shared unit/coverage | Graph equivalence, partial progress, unknown/zero/cycle semantics, all schema bounds and invalid inputs, lifecycle transitions, reset/disable/re-enable, wrong UUID/network/server and delayed restore. Changed pure behavior retains 100% line/branch coverage (R2-R4). |
| Adapter boundaries | Old tag, registry-aware tag and native26 value conversion; unrelated fields, legacy/no-job/malformed states, actual hook descriptors, optional absent classes and save-before-restore. Exercise native, AdvancedAE, NeoEco and AE2LT scope mappings (R3-R5). |
| Build/remap | All four release rows and affected drivers; inspect shipped hook targets and registrations, including Fabric remapping (R5). |
| Native CodexVM restart | `cpu-list-total-ttc`, compatible base-only: `1.20.1-forge`, `1.21.1-neoforge`, `26.1.2-neoforge`. Each is a complete two-process run at the committed head with reviewed card/title captures (R1, R5). |
| Addon CodexVM | Existing `lightningtech-cpu`, compatible Forge 1.20.1 selected addon graph. Proves live startup/job profiling with the changed hooks. Delayed restore is covered by adapter/state tests; this run is not addon process-restart evidence (R5). |
| Documentation | Relative links, precise coverage claims, English/Ukrainian consistency and wiki source updates (R6). |

Fabric shares the old tag path and requires build/remap/boundary checks rather
than a fourth equivalent restart campaign. Integrated and dedicated servers
share server-owned persistence; this scope does not claim a new dedicated-server
restart run. It does not inherit the original #324 full acceptance matrix.

## Feasibility and budget

The prepared four-target profiles and native launchers already exist. Parent
preflight on 2026-10-18 verified host Java 17/21/25 and a shut-down CodexVM at
`F:/VMs/Codex-Windows11/Codex-Windows11.vmx`. Guest manifests use
`C:/Users/Public/Documents/AE2CraftingTimeSmoke/prepared/<target>/launch.json`.
Recheck exact selected manifests, runtimes, artifact identities and disposable
markers before launching; stage/build through the existing smoke workflow.

Use `run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly` after the PR
exists to inspect selection, then run the named focused targets/scenarios.
Explain any broader inferred selection against this scope; do not silently run
the full suite. Every UI check uses CodexVM, English, 8 GiB heap and a maximized
window. Preserve native API target and selected dependency graph.

Prior native 1.21.1 two-process execution took about eight minutes. Budget six
native launches plus one addon launch, about 30 minutes of runtime, with build,
staging, review and failures measured separately. Use validated phase-two
checkpoints for diagnosis; final proof is a clean complete run. Archive before
another run overwrites evidence. Review every REVIEW_REQUIRED image; missing
visual baselines do not become automatic passes.

The existing addon fixtures have no two-process continuation contract. Building
that infrastructure is outside this scope. Record addon restart as unverified
at runtime while retaining the required boundary/lifecycle checks; never describe
the single-launch addon scenario as delayed-restoration proof.

## Completion

Bind checks, reviewed captures and CI to the final implementation head. Complete
R1-R6, merge under the issue workflow, then record implementation and verification
links in the extension spec and read back #636 closure/claim cleanup. Do not
change the original scope status or call the original screenshot diagnosed.
