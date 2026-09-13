# Recurrent crafting status: implementation plan

Status: planned; merge this documentation update before implementation.
Follow the [spec](spec.md) and
[technical design](technical-design.md); leave the feature issue open until the
implementation and its verification are complete.

## 0. Establish the verification prerequisites

The September 13 investigation used base
`8adc78280d4efcabca0e3baacbc3c7a8e3119026`. Host JDK 17/21/25 resolution passed,
but CodexVM was off: no guest runtime, manifest or dedicated marker was verified.
Start/reuse it through `use-codex-vm` during authorized verification, then inventory
the installed loaders, guest JDKs, interactive session, available memory and
prepared manifests before launching Minecraft. Never treat this plan as a
runtime pass. Keep machine paths in local evidence rather than public reports.

Use these compatible profiles, rereading `scripts/run-client-versions.json`
before freezing the campaign:

| Target | Java | Loader | AE2 | Scenario |
| --- | --- | --- | --- | --- |
| 1.20.1 Forge | 17 | 47.4.10 | 15.4.10 | recurrent-plan |
| 1.20.1 Fabric | 17 | 0.19.4 | 15.1.0 | recurrent-plan |
| 1.21.1 NeoForge | 21 | 21.1.238 | 19.2.17 | recurrent-plan |
| 26.1.2 NeoForge | 25 | 26.1.2.99 | 26.1.10-beta | recurrent-plan |

The guest manifest is `prepared/<target>/launch.json` under the prepared root
in `docs/dev-client.md`, or its exact resolved-loader subdirectory. Require its
target, Java and launcher arguments to match the sealed bundle. A missing native
installation must be provisioned with that loader's installer and the existing
prepared-manifest contract before smoke; guest Gradle and Prism are not substitutes.
Verify the newest implemented adapter identity; use a separately named focused
fixture if a compatible pin cannot reach it. Do not upgrade the supported minimums.

For dedicated checks, locate a matching source server with a schema-2
`.ae2-crafting-time-dedicated-fixture.json` marker. If absent, provision a fresh
guest-local fixture using the matching official loader server installation and
a sealed `base` dependency bundle from `scripts/run-client.ps1 -ResolveOnly
-Packaged -BaseOnly` for that target's compatible pins, with no production/driver JARs
in the source. Record target, loader, Java, launcher path/hash and dependency
hashes in its source marker; validate with the existing connected runner's
checks. Both connected clients use that same base bundle; keep optional addon
catalogues in the integrated runs. Verify each resolved base dependency loads
on a server before accepting the fixture; do not bypass hash equality checks. Preserve
the source and run only its separately marked disposable copy on loopback.

The existing runner cannot yet run recurrence or two clients. The smallest
extension described in the technical design is included in this issue. Finish
its static contracts and preflight before the first environment-heavy run.
If provisioning needs unavailable software, resources or credentials, report
that exact prerequisite instead of substituting a different environment.

## 1. Capture recurrence where AE2 rejects recipes

Own pure-Java classification/aggregation under `shared/src/main/java` and thin
AE2 carriers/mixins under `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201`.
Verify the exact buildChildPatterns, request/addMissing, runCraftAttempt, and
CraftingPlan descriptors in the four target artifacts before writing hooks.
Use mc1201/mc2612 overrides only when those signatures differ.

Implement the design's rejected-candidate observation, no-eligible-child rule,
per-attempt reset, immutable plan attachment, and final positive-missing
intersection. Preserve AEKey variant identity. Do not scan the network graph,
change an AE2 return value, or touch profiler persistence.

Write focused coverage under `shared/src/test` for every classification branch:
no candidates, only rejected candidates, an eligible alternative, zero missing,
nonsimulated success, mixed contributions, reset, and independent calculations.
Add AE2-facing boundary tests in the existing shared Minecraft test source set
for self/two/three-node loops, exact variants, storage, emission, substitutes,
and CRAFT_LESS. Assert the original plan quantities/results remain unchanged.

Completion evidence: a simulated plan carries only proven recurrent missing
keys; successful, discarded, and other concurrent plans cannot inherit them.

## 2. Send evidence with its native plan

Own a CraftConfirmMenu mixin and transient summary-entry carrier in mcCommon,
the shared payload codec beside existing `mc1201/net` codecs, and loader glue in
each `versions/*/src/main/java/com/ctux/ae2craftingtime/mc1201/StatsNetwork.java`
and corresponding packet source set. Reuse the installed networking libraries.

Implement summary revisions, clear-on-setPlan, positive-summary intersection,
256-row chunks, and all identity/bounds/bit checks from the design. Associate
flags with original entry objects, not sorted indices or profile ids. Register
the message and update channel/registrar versions together on every target;
check Fabric capability before send. Do not modify AE2 packet bytes.

Extend the nearest packet tests for empty evidence, 1/256/257 rows, the final
partial chunk, duplicate chunks, negative and overflow bounds, wrong counts,
invalid trailing bits, stale/future revisions, wrong or closed menus, new
plans with identical contents, two players/networks, cancellation and disconnect.
Prove native setPlan runs before its mod chunk on each loader's client executor.
Assert no packet-controlled count allocates an arbitrary-sized collection.

Completion evidence: all four targets receive only their current plan's flags;
invalid or unsupported diagnostics leave AE2's plan intact.

## 3. Render and translate the row status

Own `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java`,
the current TtcText source variants, and shared English/Ukrainian language files.
Add small version adapters only where AE2's Component construction differs.

Replace only the native Missing label component for a flagged positive-missing
row. Keep AE2's amount/unit formatting, add the exact tooltip explanation once,
and handle craftAmount == 0. Preserve other descriptions and tooltip lines.
Confirm that AbstractTableRendererMixin does not treat Recurrent as a TTC badge
or recolor it. Do not edit TTC sorting for this feature.

Cover flag/amount/enabled predicates, exact locale keys and placeholders, red
normal-weight style, and unflagged descriptions. Verify sorted entry identity,
no learned samples, mixed rows, large amounts and narrow UI. Check Ukrainian
keys, placeholders and component text statically; smoke stays English only.
Update relevant plan/player docs and `docs/dependencies.md` with the implemented
native-terminal coverage; coordinate status-guide issue #305 without expanding
this change into a guide chapter implementation.

Completion evidence: the row and tooltip follow R1-R7 while existing quantities,
TTC, and other screens retain their behavior.

## 4. Commit, CI, and runtime verification

Use `implement-planned-feature` for the later code task, with development,
test-driver, and prepared-client smoke skills where applicable. Implement and
self-review before creating one conventional feature commit. Follow AGENTS.md:
verify/setup the post-commit hook, let it push and create the PR, then run local
checks. Do not run local tests before PR creation.

After PR creation, run the required shared/boundary tests and JaCoCo; every new
or changed executable branch needs the development skill's 100% line and branch
coverage. Build all four release-matrix targets through the repository build
workflow. Check translations, effective mixin registrations, and diff hygiene.
Read GitHub CI and review findings separately from local results.

Extend the existing standard-AE2 test-driver path with the `recurrent-plan`
leaf on each target. Use real encoded processing patterns and actual plan
menus, not synthetic cached flags. A useful fixture uses inert distinct item
types A/B/C and deliberately circular processing patterns; no machine output is
needed for a failed plan. Create separate seeded and valid-alternative fixtures
and assert that AE2 really finds a successful plan before using them as negative
controls. Reuse `StandardCraftFixture`, `DriverPlatform.processingPattern`,
`CraftPlanScenario`, `UiObservationStore` and `CaptureEvidence`. Add the leaf to
`DriverOptions`, `DriverResult`, host scenario validation, `ui-smoke-groups.json`,
`ui-smoke-impact.json` and the affected suite/coverage/visual contracts. Preserve
existing leaves. Update `docs/test-driver/{spec,technical-design}.md` alongside
implementation with this bounded multiplayer exception and its exact result set.

Extend `scripts/run-connected-dedicated-ui-smoke.ps1` with a scenario selector
whose existing CPU-list default is unchanged. For `recurrent-plan-connected`,
dispatch the recurrence fixture through `DedicatedCpuScenario` and the current
loader `ServerDriverPlatform` entrypoints. Reuse `CpuListTtcControl`'s bounded
atomic rendezvous pattern with separate role files; keep its CPU-list schema
and commands unchanged. Extend `prepare-ui-smoke-launch.ps1` only for validated
offline role identity, forwarded through the existing runner. Use its native
loader launch and `ui-smoke-scheduled-java.ps1` for separate owned client processes.
On NeoForge 1.21.1 enable two roles; other targets need one. Refuse duplicate
identities, shared output/runtime directories, unmarked worlds and non-loopback
servers. Extend `test-run-connected-dedicated-ui-smoke.ps1`,
`test-prepare-ui-smoke-launch.ps1`, the closest readiness check and
`TestDriverCoreTest` for these changed branches, including cleanup on one-client
failure. Run those checks only after the hook-created PR exists.

Run prepared-client smoke in CodexVM, in English, through the smoke skill. First
run `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly` and retain
the selection reasons. Ensure the new leaf and existing `standard-plan-controls`
regression are selected. Required connected runs are additional explicit gates;
the changed selector must not silently omit them.

| Target | Required observed evidence |
| --- | --- |
| 1.20.1 Forge | Integrated recurrent-plan plus standard-plan-controls; connected recurrent-plan-connected with one client proves packet ordering and lifecycle. |
| 1.20.1 Fabric | Same leaves and connected gate; capability fallback and remapped hooks verified. |
| 1.21.1 NeoForge | Same integrated leaves; connected gate with two actual clients on separate grids proves overlapping player isolation and reconnect. |
| 26.1.2 NeoForge | Same integrated leaves and one-client connected gate on its rendering/packet APIs. |

All integrated recurrence leaves include self/two/three-node loops, successful
seed/alternative controls, ordinary/mixed shortages, exact variants, no-sample
display and TTC sort modes. Exercise a real plan larger than 256 entries on
NeoForge 1.21.1; all-target codec tests cover chunk boundaries. Each connected
gate records native setPlan followed by its mod chunk on the client thread,
then menu replacement and reconnect. Driver-only packet interception supplies
late/malformed negative inputs; real positive flags must come from AE2's server
calculation. On the two-player gate, capture both players' opposite outcomes,
swap/replan, and reconnect one while the other retains its valid plan. Retain
actual recipient UUID/menu/revision records and both client frame snapshots.

Capture red row text and its hover explanation. A screenshot alone does not
certify negative controls or isolation. Bind every result, log, PNG/sidecar and
process identity to the same tested head, production/driver hashes, resolved
dependency graph and adapter identity. Review every non-PASS visual checkpoint.

### Launch and time budget

Budget four integrated client launches, one per target, grouping the two leaves
in each process. Follow these with four dedicated-server launches and five
client launches: one connected client per target plus NeoForge 1.21.1's second
player. That is nine client and four server launches before any extra graph
selected for newest-adapter coverage. Reconnect reuses its client process.
Launch targets sequentially; only the explicit two-player gate overlaps clients.
Start with the Forge integrated case, then finish the selected integrated matrix
before dedicated runs. Reuse that first clean pass if its source/artifact identity
is unchanged; it is not an extra mandatory rehearsal.

Cold-start time is not measured in this investigation. Provisionally reserve
eight minutes per integrated launch and twelve minutes per connected target,
plus separately recorded build, staging and evidence-review time. Keep the
existing 300-second client startup, 180-second server startup, 20-second callback
and 60-second active-checkpoint bounds. Record actual phase times and adjust the
campaign estimate from the first launch; budget is not permission to ignore a
stalled checkpoint. On failure retain evidence and diagnose the failed phase;
do not replay passing targets unless a changed input invalidates them. Final
acceptance requires clean complete scenarios on the final head; resumed runs
remain diagnostic-only.

### Criterion-to-check mapping

| Criteria | Change | Required check |
| --- | --- | --- |
| R1-R2 | Node rejection observation, terminal shortage capture, row label/hint | Covered classification tests; real self/A-B/A-B-C loops and row/hover frames on four targets |
| R3-R4 | Per-attempt reset, eligible-child rule, positive intersections, exact AEKeys | Covered seed/alternative/emitter/substitute/CRAFT_LESS/mixed/variant boundaries; successful runtime controls; plan with at least 257 entries |
| R5 | Plan attachment, menu revision and bounded chunks | All-target codec/ordered-handler checks; replan/menu/network/cancel/reconnect cases; two-client recipient/frame evidence |
| R6 | Shared text and renderer, unchanged TTC/sort consumers | English/Ukrainian resource/component checks; English normal-red style, narrow layout, units and three sort modes; standard-plan-controls |
| R7 | Read-only planner hooks, separate payload, optional capability | Native result/quantity comparison; malformed/fallback checks; unchanged persisted formats; all-target builds and driver-artifact isolation |

## Completion gate

R1-R7 each have passing automated or actual runtime evidence as mapped in the
design. All four builds and required CI checks are green, review blockers are
resolved, both translations match, and the implementation PR links the issue
and these documents. Record tested artifact versions and evidence locations.
Do not claim unrun smoke coverage or publish a release as part of implementation.

For this documentation-only PR, the gate is narrower: self-review the three
documents together, validate relative/source/issue links and Markdown structure,
run `git diff --check` after the hook opens the PR, inspect the rendered GitHub
documents, wait for green CI and review readiness, then merge and verify all
three issue links resolve on master. No runtime feature or smoke result is
claimed by the planning merge.
