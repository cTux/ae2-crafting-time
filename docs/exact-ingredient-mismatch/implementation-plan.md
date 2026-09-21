# Exact ingredient mismatch diagnostics: implementation plan

Status: revised planning; implement after these documents and the approved
issue #327 update are delivered. Follow the [specification](spec.md) and
[technical design](technical-design.md). This change publishes no release.

## 1. Prove the verification prerequisites

Before implementation, inventory all four compatible prepared profiles:
Forge/Fabric 1.20.1 on Java 17, NeoForge 1.21.1 on Java 21, and NeoForge 26.1.2
on Java 25. Verify guest `launch.json`, native loader installation, Java,
dependency identity, disposable world markers, and loopback dedicated source
markers/launchers against the chosen profile. Use the existing CodexVM and
prepared-client workflow; host builds stay on the host.

The bounded connected runner exists, but `stored-variant-plan` does not yet.
Adding that leaf through existing scenario/control seams belongs to this issue.
Creating a general server provisioner does not. If matching immutable server
sources are absent, deliver the separately documented provisioning prerequisite
before this issue's connected verification; do not invent an unapproved setup
path or weaken the gate. Guest manifests and source availability must be live
verified, not inferred from the existence of scripts.

Inspect actual `CraftConfirmMenu`, `StorageService.interestManager`,
`StackWatcher`, `IStorageWatcherNode`, `CraftingPlanSummary`, `AEItemKey`, and
storage descriptors for both 1.20.1 artifacts and both NeoForge targets. Confirm
the common accessor and 1.20.1 mapped counterpart against compile-minimum artifacts
and the selected runtime graph.

## 2. Classify rows and model replacement updates

Own pure-Java `PlanStoredVariantDetector` and update/lifecycle state under
`shared/src/main/java`, with tests under `shared/src/test/java`. Keep AE2
conversion in mcCommon. Build positive exact-key and primary-item sets from
one snapshot; return original positive missing item-row indices only when a
near-match exists and the exact key does not. Cover exact-only, exact-plus-near,
different item/equal name, zero/negative availability, zero missing amount,
fluids, empty storage, multiple variants, unrelated rows, and unchanged inputs.

Reuse the existing `PlanRecurrenceChunk` validator. Cover replacement masks,
all-zero clears, monotonic per-chunk update watermarks, dirty coalescing,
irrelevant notifications, no work while clean, lifecycle resets and counter
overflow. Cases include 1/256/257 rows, partial final chunks, reordered and
duplicate updates, wrong menu/summary/count, negative/overflow bounds, invalid
trailing bits, and stale chunks after an identical replacement plan. All new
pure-Java decisions require 100% line and branch coverage.

## 3. Bind observation and delivery to the native menu

Extend the shipped #320 menu revision and entry carrier. Own the common and
1.20.1 `Srg` `CraftConfirmMenu` mixins, entry mixin/interface, a thin menu-owned
storage watcher, and common `StorageService` accessor. Use AE2's existing
watch-all callbacks, filter relevant primary items, and mark dirty only. Refresh
at the next normal broadcast; do not poll storage or add a grid node. Release
the watcher on replacement, close, disconnect, invalid menu or grid change.
Clear and suspend an old plan when its grid changes until a new native summary.

Add independent `PlanStoredVariantsS2C` wrappers and registrations on all four
targets. Encode positive update revision before the existing bounded chunk
codec, send changed replacement masks including zero masks, and keep initial
delivery after the native summary. Preserve recurrence wire bytes and flags.
Increment Forge protocol and both NeoForge registrar versions; guard Fabric
receiver support. Extend effective mixin-registration checks.

Extend shared `net/PlanRecurrencePacketTest` patterns for the new payload, plus
the existing lifecycle/recipient boundary tests. Prove native summary handling
precedes diagnostics on each loader; reject early, late and malformed packets
without changing the plan. Client watermarks allocate only from native entries.

## 4. Render the independent warning

Extend shared `CraftConfirmTableRendererMixin`, the single mcCommon `TtcText`,
and both shared language files. Add the gold normal-weight label and two exact
tooltip sentences before TTC's missing-only guard. Preserve Missing/Recurrent,
amounts, sorting, Start behavior, and existing TTC formatting. Keep rendering
predicates pure and covered; extend `TtcTextTest` for keys and style, and check
matching English/Ukrainian keys, placeholders and exact prescribed text.

## 5. Add the bounded real-UI scenario

Implement `stored-variant-plan` under the existing standard-AE2 scenario,
fixture, observation and connected-control seams described in the
[driver spec](../test-driver/spec.md#stored-variant-plan-scenario-planned) and
[driver design](../test-driver/technical-design.md#stored-variant-plan-scenario-planned).
Reuse damaged item stacks to represent distinct exact NBT/components and real
processing patterns. Never seed production flags or invoke the renderer to
stand in for actual UI evidence.

Register the leaf in both driver runtimes, supported-case/SuitePlan checks,
host scenario validation, groups/impact/selection, connected scenario dispatch,
and required result/screenshot contracts. Keep the fixture isolated and update
the existing dependency coverage documentation when the leaf is implemented.
No new general launcher or simultaneous-client scheduling is needed.

Exercise none -> near -> none -> near+exact -> near while the same native
summary and confirmation menu remain open. Capture no-warning, warning/tooltip,
near-removed, exact-present and restored states; record authoritative storage
and synchronization facts. Cover exact-only, ordinary shortage, other item,
fluid and successful controls, multiple/unrelated rows, recurrence coexistence,
all TTC sorts without samples and narrow layout. Prove event coalescing, no
refresh while clean, watcher cleanup, quantities and grid topology unchanged.

Run the new leaf plus `standard-plan-controls` and `recurrent-plan` regressions
on all four compatible targets in English. Run the new leaf's live transitions,
network switch, replan, cancellation and reconnect on matching disposable
dedicated targets with one client at a time. Recipient-boundary unit tests and
server UUID/menu/revision acknowledgements prove isolation; do not claim
simultaneous-player evidence. Check Ukrainian resources/components statically.

## 6. Commit and verify the current head

Implement and self-review before the conventional feature commit; follow
`AGENTS.md` so the hook creates the PR before local tests. Run the selected
pure-Java, codec, component, registration and scenario-contract checks; build
all four production/driver targets and require shared JaCoCo coverage. Report
GitHub CI separately from local verification.

Use the prepared-smoke `-Changed` plan after PR creation and retain its selection
reasons. Confirm the explicit cases above are selected. Run cheap checks first,
then the new integrated leaf on Forge 1.20.1, then the remaining integrated
targets and regressions, then connected targets. Schedule one integrated suite
and one connected launch per target for the final clean evidence: eight client
launches, sequentially. Record observed cold-start cost and a wall-time budget
before starting. Keep existing 20-second callback and 60-second active-checkpoint
watchdogs; diagnose from retained evidence before another clean campaign.

## Acceptance and completion gate

| Criteria | Required evidence |
| --- | --- |
| V1-V2 | Classifier boundaries and same-open-plan live transitions, exact-plus-near suppression and restoration; unchanged quantities/Start state. |
| V3-V4 | Non-item/ordinary/success controls, multi-variant and unrelated rows, original exact-key identity and indices. |
| V5 | Chunk replacement/zero masks, per-chunk ordering tests, native-summary ordering, watcher cleanup and connected network/replan/reconnect controls. |
| V6 | Four-target English row/tooltip/layout/sort evidence, no samples, recurrence regression/coexistence and static checks for both locales. |
| V7 | Unchanged native plan, grid topology, storage/pattern/profiler/persistence state; recipient isolation and malformed-data fallback. |

Complete only when V1-V7 have passing evidence on the reviewed implementation
head, all four builds and required CI pass, visual evidence is reviewed and
archived, and review blockers are resolved. Record exact artifact/profile
identities and timings. A missing prerequisite, screenshot alone, or queued
check is not completion evidence.
