# Recurrent crafting status: technical design

Status: planned. Implements the [specification](spec.md).

## Verified seams

The repository's shared `CraftConfirmTableRendererMixin` adds TTC through
`getEntryDescription` and `getEntryTooltip`. It currently has no missing-reason
input. Tooltip TTC handling exits early when `craftAmount <= 0`; recurrence
handling must run before that guard. The two `CraftConfirmScreenMixin` variants
own TTC sorting, while `AbstractTableRendererMixin` owns compact TTC rendering.

Inspected AE2 15.0.10 sources from the published Fabric source JAR and the
19.0.24 / 26.1.10-beta upstream source revisions have these same seams:

1. `CraftingTreeNode.buildChildPatterns` skips a candidate when
   `parent.notRecursive(details)` returns false.
2. `CraftingTreeNode.request` consumes storage and checks emission before trying
   recipes. Its terminal simulated shortage calls `job.addMissing(what, amount)`.
3. `CraftingCalculation.runCraftAttempt` may run several times, including
   CRAFT_LESS attempts, before returning the final simulated or successful plan.
4. `CraftingPlan` carries missing quantities but no recurrence reason.
5. `CraftingPlanSummary.fromJob` computes displayed missing quantities using the
   current storage and emitter state. Therefore planner evidence alone cannot
   turn a zero-missing summary row into Recurrent.
6. `CraftConfirmMenu.broadcastChanges` obtains the future result, builds the
   summary, and sends `CraftConfirmPlanPacket`. Client `setPlan` installs it.

Source anchors, pinned to inspected revisions:

- [AE2 15.0.10 tree node](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/ffaf14d7535b3dc643e04b8407db5419ef51bee8/src/main/java/appeng/crafting/CraftingTreeNode.java)
- [AE2 19.0.24 tree node](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/crafting/CraftingTreeNode.java)
- [AE2 26.1.10-beta calculation](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/crafting/CraftingCalculation.java)
- [AE2 26.1.10-beta confirm menu](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/main/java/appeng/menu/me/crafting/CraftConfirmMenu.java)

These are source checks, not runtime compatibility or reproduction evidence.
The implementation must verify descriptors against both 1.20.1 loader artifacts
and each target's prepared runtime before declaring coverage.

Reinspection at `8adc78280d4efcabca0e3baacbc3c7a8e3119026` confirms the
renderer still has no recurrence input. The compatible graphs now use AE2
15.4.10 (Forge), 15.1.0 (Fabric), 19.2.17, and 26.1.10-beta. These are runtime
pins, not new minimum supported versions. Verify the common hooks in both the
minimum build artifacts and those resolved runtime artifacts, including Fabric
remapping. Select the newest implemented adapter under the current smoke policy.

## Server evidence and ownership

Observe AE2's recursion decision; do not build a second graph from all network
patterns. That graph would lose storage, substitution, emission, branch choice,
and AE2's own recursion rules.

Add thin shared mixins for `CraftingTreeNode`, `CraftingCalculation`, and
`CraftingPlan`, plus a small typed carrier for immutable recurrence keys. The
carrier stores real AEKey identity, including variant data, not
`ProfilerBridge.key`'s normalized profiling id. Pure-Java classification and
per-attempt aggregation belong in `shared/src/main/java`; AEKey conversion and
Mixin adapters belong in `shared/src/mcCommon/java`.

- While building a node's children, observe the existing notRecursive call
  using a composable MixinExtras expression/wrapper, returning its original
  result unchanged. Remember whether any candidate was rejected. Do not infer
  recurrence from a globally repeated key or call AE2's recursion test twice.
- At the node's actual positive `addMissing` call in simulation, record its key
  only when at least one recipe was rejected for recursion and the node has no
  eligible child recipes. Eligible-but-unsatisfied alternatives remain ordinary
  shortage paths. Rejection metadata lives on that node because AE2 reuses the
  built tree between attempts.
- Clear the calculation's observed missing-key set at each runCraftAttempt
  entry. Record only actual terminal simulated shortages; failed normal attempts
  and speculative candidate construction cannot add a diagnosis.
- On a non-null runCraftAttempt result, attach an immutable copy intersected
  with the final plan's positive missingItems. A nonsimulated result gets an
  empty set. Store it on the returned plan, never in a process-global map or
  ThreadLocal. This also leaves concurrent calculations isolated.
- When creating the summary, intersect again with entries whose missingAmount
  is positive. For an aggregated key, any surviving recurrent contribution is
  sufficient. Preserve AE2's total quantities and row objects.

The worker thread owns mutable calculation observations. Future completion
publishes the immutable plan attachment to the logical server. Cancelled,
failed, discarded, or completed calculations are collected with their owners;
there is no profiler, CPU, world-save, or retained-statistics state to clear.

## Transport and plan identity

Add a dedicated mod-owned `PlanRecurrenceS2C` payload, registered through each
target's existing `StatsNetwork`. Do not extend AE2's native packet format or
reuse CPU-scoped `blockReasons` / `ClientStatsCache`.

Send diagnostic chunks immediately after the native summary packet from the
same broadcast operation. Each menu starts a long summary revision at zero;
the server increments it for every native summary it sends, and the client
increments it for every `setPlan` installation. Both native and mod payload
handlers must execute in connection order on the client thread. The native
summary always clears previous row flags before any matching chunk applies.

Each chunk carries `containerId`, positive `summaryRevision`, `entryCount`,
`offset`, `rowCount` (1..256), and a fixed 32-byte recurrence bit mask. Row indices
refer to the original summary list, before client sorting. Send only chunks
with set bits. No warning means no chunk; setPlan already clears the old state.

Use `PacketLimits.MAX_KEYS` (256) for the row bound and a pure-Java validation
helper. Before applying a chunk, require the active CraftConfirmMenu, matching
container/revision/count, nonnegative offset, offset divisible by 256, and
`rowCount == min(256, entryCount - offset)`. Check bounds without integer
overflow. Bits outside rowCount must be zero. Reject malformed or mismatched
chunks without mutating the current plan. No allocation is sized by an
untrusted entryCount: validate against the already-installed native summary.
Apply idempotently to its existing row objects and ignore flags on rows without
a positive missing amount. Sorting therefore cannot move a warning to a
different ingredient, including two variants of the same item.

There is no C2S diagnostic query or client-selected network access. Packets go
only to the player already receiving that server-authorized plan. Work and
retained state are linear in AE2's existing plan; each extra payload has a
constant size, and at most ceil(entryCount / 256) chunks are sent per summary.
Early/late revisions, a closed menu, and another menu are ignored, not queued.
Ordered-handler verification is a required boundary test on every loader.

Forge adds the message after current registrations and increments its channel
protocol from the value present at implementation time. Both NeoForge targets
add a clientbound registration and increment their current registrar versions.
Fabric adds `plan_recurrence_v1` and checks peer capability before sending.
Keep all existing packet layouts and SavedData unchanged. An unsupported peer
that is otherwise allowed to connect receives native Missing text; retain
existing loader mismatch rejection rules.

## Rendering

Add a transient boolean carrier to `CraftingPlanSummaryEntry`. The renderer
consults it only together with `missingAmount > 0` and the enabled mod state.
Change the Component generated for AE2's missing label at its semantic
construction point in both description and tooltip; never replace translated
strings by text matching or overwrite the whole renderer. Preserve AE2's
existing quantity formatting. Add the explanation once, before TTC-only guards.

`TtcText` supplies `plan.recurrent` and `plan.recurrent_hint` components, with
matching keys under the existing `ae2craftingtime` translation namespace in
`shared/src/main/resources/assets/ae2craftingtime/lang/{en_us,uk_ua}.json`.
Use ChatFormatting.RED without bold. The status must not enter TTC color or
badge classification. Leave sorting and TTC calculations untouched; the existing
#318 missing-first comparator continues to use missingAmount, not this flag.
Disable hides the label immediately; re-enable may reuse evidence attached to
the still-current plan. It must never restore flags from a previous summary.

## Source sets and failures

Share logic and AE2 hooks in mcCommon wherever inspected descriptors match.
Use mc1201 and mc2612 only for differing Minecraft signatures; keep loader
packet glue under each version module. Register common server hooks and the
entry carrier on both physical sides, with renderer hooks client-only. Check
all effective mixin lists, including NeoForge overrides and Fabric remapping.

No new optional dependency or custom-planner adapter is included. Plans without
the carrier have no evidence. Native terminal routes inherit the table hook;
separate Tree and Requester views do not. A missing required hook must fail
verification, not silently ship as supported. Invalid diagnostic input may lose
the extra label but must not change AE2's calculation or damage learned data.

## Verification extension and ownership

The existing connected runner, `scripts/run-connected-dedicated-ui-smoke.ps1`,
is specific to `cpu-list-total-ttc-connected` and one client. Reuse its source
marker, dependency/launcher hash validation, disposable copy, loopback binding,
server-ready barrier, and exact process cleanup. Add an explicit recurrence
scenario selector; preserve the CPU-list default and its checks. This is a
planned extension, not evidence that recurrence multiplayer is already runnable.

Add `recurrent-plan` to the existing driver scenario/result/host selection
contracts. Own its real processing-pattern fixture and frame checks beside
`StandardAe2Scenario` and `StandardCraftFixture` in
`shared/src/testDriver1201/java/com/ctux/ae2craftingtime/testdriver/`;
keep changed 26.1.2 APIs in the existing
version-specific driver source set. Reuse `DriverPlatform.processingPattern`,
`UiObservationStore`, `CaptureEvidence`, and native menu interaction. Extend
`DedicatedCpuScenario` dispatch through a focused recurrence fixture rather
than adding recurrence state to the CPU-list state machine. Reuse the bounded
atomic command/acknowledgement pattern in `CpuListTtcControl`; recurrence commands
address an explicit player role and scenario phase, never an arbitrary action.

The connected leaf is `recurrent-plan-connected`. On each target, its one
client proves native summary/mod-chunk execution order and reconnect behavior.
On 1.21.1 NeoForge, run a second actual client concurrently for player isolation.
Use two distinct offline fixture names/UUIDs, separate runtime, control and
evidence directories, and one campaign identity. Bind role acknowledgements to
the server-observed player UUID and menu, not whichever player joins first.
The two grids use overlapping item keys and opposite recurrence outcomes, then
swap outcomes and replan. Both players must remain connected across the swap;
capture each final frame and the server's matching recipient/plan records.
Disconnect/rejoin one player while the other's plan stays open. A fake player
or two sequential sessions cannot satisfy this overlap check.

Reuse `prepare-ui-smoke-launch.ps1` and the scheduled-Java helpers for isolated
launches. Add only validated fixture-role/offline-identity options needed by this
loopback scenario; do not read account tokens or change ordinary launch identity.
Start the clients one at a time through PID acquisition, keep both running for
isolation, and serialize visible input/captures with both windows maximized.
Give each client its own task name, argument file, log, watchdog and cleanup.
Keep the existing 8 GiB client heap. Insufficient guest resources are a preflight
failure, not permission to replace the second client with synthetic evidence.

Update the test-driver spec/design's currently CPU-list-only multiplayer boundary
in the implementation change to include this exact marked loopback scenario.
Do not broaden its permission to other servers or add a general multiplayer runner.
Keep all new fixture/control code out of production JARs and `dist`.

## Acceptance mapping

| Spec | Design path and evidence required |
| --- | --- |
| R1-R2 | Actual rejection plus terminal missing call; real two-node, self, and three-node fixtures. |
| R3-R4 | No-eligible-child rule, per-attempt reset, final intersections, exact keys; seeded, alternative, mixed, emitter, and variant tests. |
| R5 | Plan attachment, menu revisions, ordered handling, clear on setPlan; cancellation, concurrent jobs, stale chunks, and reconnect tests. |
| R6 | Shared renderer and locale keys; all-target codec/hooks, sorting, no-sample and large-unit visual checks. |
| R7 | Read-only observations, unchanged native packet and quantities; calculation comparison and optional-screen regression checks. |
