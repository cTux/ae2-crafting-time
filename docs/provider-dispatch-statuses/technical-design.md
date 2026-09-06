# Provider dispatch statuses: technical design

Implements the proposed [specification](spec.md). No production code changes
are part of this planning task.

## Evidence and version boundaries

Repository baseline: [`51edf0f8f531c8f12fe4e192f934669359011390`](https://github.com/cTux/ae2-crafting-time/tree/51edf0f8f531c8f12fe4e192f934669359011390),
fetched and inspected 2026-09-06. This replaces the original `728e279e`
research baseline. Existing seams:

- `CraftingCpuLogicMixin` and `AdvancedCraftingCpuLogicMixin` capture the exact
  dispatch pattern and observe the selected provider iterable and simulated
  dispatch energy. The hook is at `Iterable.iterator()`, after addon lookup
  wrappers/replacements, rather than at `CraftingService.getProviders()`.
- Both `ProfilerBridge` variants feed runtime-only `CraftProfiler` state.
  `DispatchPowerTracker` already expires observations after 20 ticks;
  `MissingProviderTracker` separately revalidates missing providers.
- `StatsRequestHandler`, `StatsPacketCodec`, the four `StatsSnapshotS2C`
  wrappers, and `ClientStatsCache` carry a bounded output-to-`CraftingBlockReason`
  map with menu/CPU context. The renderer and sorting already consult that map.

### Repository changes since the original plan

Paths below are relative to the repository root. These are source findings,
not a claim that the proposed hooks have passed compiled or runtime tests.

| Verified current code | Consequence for #216 |
| --- | --- |
| `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingCpuLogicMixin.java` and `shared/src/neoforge/java/com/ctux/ae2craftingtime/mc1201/mixin/AdvancedCraftingCpuLogicMixin.java`: capture the pattern local and redirect the selected iterable's `iterator()` call. | Extend the existing observation point. Do not enumerate the original service lookup separately or add a competing redirect. The current `hasNext` boolean alone does not establish all alternatives. |
| Both `shared/src/{mc1201,mc2612}/java/com/ctux/ae2craftingtime/mc1201/ProfilerBridge.java` variants: `observeProviders` also calls `ProviderStartTracker.noteDispatch`; `blockReasons` remembers every live reason as either NO POWER or NO PROVIDER. | Preserve provider-locate observation. Explicitly whitelist the two old reasons for persistence before adding new enum values, or every new reason would be saved as NO PROVIDER. |
| `CraftProfiler.rememberedReasons(scope)` and both bridges, fixed in [#284](https://github.com/cTux/ae2-crafting-time/pull/284). | Preserve selected-scope suppression of remembered reasons for live pending outputs. Test recovery with remembered old reasons present. |
| `CraftProfiler`, `ProfileStats`, and `StatsPacketCodec`, updated in [#287](https://github.com/cTux/ae2-crafting-time/pull/287); `CraftingJobEstimate`, updated in [#272](https://github.com/cTux/ae2-crafting-time/pull/272). | Keep tick-batched live completion intervals, sample amounts on the wire, and the server's critical-path total TTC. Dispatch failure is not a completed output or a throughput sample. |
| `TtcText` and renderer changes in [#288](https://github.com/cTux/ae2-crafting-time/pull/288); `BlockReasonNotifier` and `DelayedNotificationServer`. | Add only the requested badge/tooltips. Keep accuracy rows removed; new reasons do not create chat or locate/plate transitions. |
| `scripts/get-ui-smoke-plan.ps1`, `ui-smoke-groups.json`, `ui-smoke-impact.json`, and both `TestDriverRuntime` variants. | Register new leaf scenarios in the driver and host planner, with evidence requirements and change-impact selection. Preserve fresh-world and terminal-screen waits between suite cases. |

The four AE2 dependency defaults in `versions/*/build.gradle` are unchanged
from the original research. All four pinned `PatternProviderLogic` source
files below were fetched again on 2026-09-06.

Read the pinned upstream `PatternProviderLogic` sources, not only latest main:

| Target | Source | Relevant API difference |
| --- | --- | --- |
| 1.20.1 Forge | [15.0.10](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/forge/v15.0.10/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java) | Four-argument ICraftingMachine.of; no supportsPushInputsToExternalInventory guard here. |
| 1.20.1 Fabric | [15.0.10](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fabric/v15.0.10/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java) | Same relevant provider flow as Forge. |
| 1.21.1 NeoForge | [19.0.24](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/neoforge/v19.0.24/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java) | Three-argument ICraftingMachine.of and explicit external-inventory eligibility guard. |
| 26.1.2 NeoForge | [26.1.10-beta](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/v26.1.10-beta/src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java) | Same eligibility guard; different round-robin loop shape. |

All four return early for busy/inactive/missing-pattern providers, check the
actual `getCraftingLockedReason()`, try dedicated crafting machines first,
then inspect generic targets. Blocking mode checks pattern inputs; simulated
insertion rejects when an input accepts zero. Positive partial acceptance can
still lead to success with leftovers queued. Source evidence establishes the
design; exact compiled injection descriptors and addon overrides still need
contract checks before implementation is considered complete.

Pulse handling also crosses a version boundary: AE2 15 uses `PULSE`, whereas
19 and 26 distinguish `REDSTONE_POWER` and `REDSTONE_PULSE`. When dispatch
starts with redstone already high, the newer versions require the signal to
fall and rise before unlocking. Fixtures must follow each pinned version's
`onPushPatternSuccess`/`updateRedstoneState` flow. Classification still uses
the returned `LockCraftingMode`, never an inferred internal event.

## Observe execution without repeating it

Extend the existing native and AdvancedAE CPU mixins at their real dispatch
calls. Reuse captured CPU, grid, exact pattern, tick, and positive output keys.
Do not probe machines by calling pushPattern again, rerun inventory simulation,
extract inputs, force chunk loads, or change original results/exceptions.

Use one short-lived, server-thread dispatch context around each real provider
`pushPattern` invocation, with try/finally restoration of the previous context.
Nested invocations get independent frames. Provider observations must match
the current provider identity and pattern; calls outside this context do
nothing. This context belongs to MC-facing code, not the Minecraft-free core.

Add provider-method observation mixins, shared where descriptors agree and
split by API boundary where they differ. Observe returned values of AE2's
existing checks. No provider-held long-lived last-error field is allowed.
Record whether traversal reached target selection, whether a dedicated
machine accepted plans, eligible generic target count, unexplained rejection,
and explicit blocking/insertion failures. A reached lock check returning a
non-NONE reason records LOCKED. Interpret only the outer call's final result:

| Actual outcome | Classification |
| --- | --- |
| pushPattern returns true | Success, overriding every intermediate failure. |
| Active lock branch rejects the dispatch | LOCKED. |
| Target traversal finishes, no dedicated accepting-plans machine and no eligible generic destination | NO TARGET. Respect the version's external-inventory guard. |
| At least one eligible generic destination, every eligible route rejected by observed blocking mode or zero-acceptance check, no unexplained dedicated-machine refusal | INPUT BLOCKED. |
| Busy/inactive/missing pattern, incomplete traversal, dedicated-machine refusal, unsupported override, exception, or missing evidence | Unknown; no new status. |

In 1.20.1, generic-target eligibility follows that version's actual traversal;
do not backport newer AE2 behavior. In newer versions, an inventory is not an
eligible destination when the actual external-inventory guard disallows it.
A dedicated machine that accepts plans but rejects this call is unknown,
never proof of missing target or blocked input.

## Aggregate by attempt, then pattern, then row

Observe the iterator returned by the existing CPU redirect, delegating each
normal `hasNext`/`next` call exactly once and preserving order. Record visited
provider identities, busy results, and push outcomes for this exact-pattern
evaluation. A naturally observed exhausted iterator proves that all selected
alternatives were visited; an early exit does not. Do not copy, pre-enumerate,
or obtain a second iterator from a possibly lazy addon iterable. An addon
replacement remains eligible when it uses this observed traversal; bypassing
the traversal leaves unknown evidence. Preserve the existing
`observeProviders` call and its `ProviderStartTracker` side effect. Add no
extra busy checks or pushes and no competing redirect at the same call site.

Use one evaluation per outer task visit, starting at the stored pattern local.
Repeated batches within that visit share the evaluation: any success suppresses
its warning until a later visit can establish a wholly blocked attempt. Finalize
the preceding evaluation when moving to another pattern or leaving
executeCrafting. Never carry an unfinished evaluation into the next invocation.
Missing inputs, budget/power exits, unvisited candidates, busy/unknown results,
and incomplete enumeration cannot produce a new warning. Only a complete,
nonempty set of agreeing provider failures yields one new reason. Mixed new
reasons yield unknown. This avoids a misleading arbitrary provider priority.

Add one Minecraft-free `ProviderDispatchTracker`, owned by `CraftProfiler`,
for completed observations: CPU identity -> exact pattern -> positive output
keys, reason, observation tick. Reuse the existing power tracker's bounded
per-job lifetime pattern rather than making a generic diagnostics framework.
Replace/clear that exact pattern on each evaluation; retain separate patterns
sharing an output. Expire on tick rollback or age >=20 ticks. Clear with every
existing profiler lifecycle reset and discard empty maps. Beyond the existing
CPU identity and pattern scope, retain no provider/world references in completed
observations; no state enters SavedData.

Merge fresh pattern reasons by the spec's explicit precedence in
`CraftProfiler.blockReasons`; leave existing missing-provider revalidation and
power observation behavior intact. Do not use enum ordinal as priority.
Snapshots never dispatch work or discover a new failure. They only read,
expire, combine, and filter observations to requested keys and current network.

Both bridges now persist existing live block reasons before merging remembered
fallbacks. Replace their binary NO POWER/otherwise NO PROVIDER conversion
with an explicit whitelist for those two existing reasons. Do not add the
three new reasons to `StatusKind`, `PersistedOutputStatus`, or
`PersistedStatusTag`. Keep `rememberedReasons(scope)` rather than its unscoped
overload, preserving #284. Test that new reasons never enter saved statuses,
including when `BlockReasonNotifier.maybeNotifyPower` reads the same map.
Expiry/recovery must leave neither a new warning nor a newly persisted
NO PROVIDER alias. Existing unrelated remembered entries retain their policy.

Do not call `start`, `complete`, or sample-reset methods to clear a diagnostic.
The live completion intervals introduced by #287 are independent of dispatch
evidence. Preserve `ProviderStartTracker`, delayed notification episodes,
locate records, and red-plate lifecycle; these statuses add no new side effects
to those systems.

## Transport, UI, and compatibility

Append `NO_TARGET`, `INPUT_BLOCKED`, and `LOCKED` to `CraftingBlockReason`.
Reuse the current map; do not add a second packet or a detail DTO. Generic
two-line tooltips are intentional: subtype diagnosis is outside this scope.
Advance Forge protocol 15 -> 16, Fabric stats_snapshot_v8 -> v9, and both
NeoForge registrars 14 -> 15 together. These are the current `StatsNetwork`
values on the research baseline. If another feature has advanced the base
before implementation, advance that current boundary once instead; never
reuse a protocol identifier for incompatible enum values. Preserve Fabric's
capability check and the existing mismatched-peer behavior.

`StatsPacketCodec` keeps MAX_KEYS, key validation, requested-key membership,
and invalid-enum rejection. No client-supplied CPU, provider, or position is
accepted. Keep server menu authorization and request rate limits. Replacement
semantics clear omitted requested keys even without learned samples; late
packets are hidden unless their menu/CPU context matches.

Reuse `TtcText.blockReason`, adding the three label/explanation/suggestion keys
in both locale files. Add one translated mixed-active/pending tooltip sentence
for these new reasons only. Extend `CraftingRowState` badge recognition and
test the shared table renderer plus both API variants of status-screen
sorting. Keep NO SPACE's stored-only predicate, old tooltips, total TTC,
Craft Plan, Crafting Tree, and ME Requester unchanged.

## Optional integration policy

Native AE2 and the existing AdvancedAE CPU adapter are the required dispatch
paths. Inherited provider methods can reuse core observations; a custom
provider's false return without complete observed checks remains unknown.
Do not infer support from class ancestry alone or add dependencies for these
statuses. NeoEco/LightningTech custom CPU status adapters and bespoke provider
adapters are not added. Preserve existing profiling and adapters.

Retain older supported adapter variants and use the existing integration
selection policy if a descriptor difference needs a variant. Contract and
packaging coverage apply to every retained variant; runtime smoke applies
only to the newest implemented one in English. An unsupported optional path
must not publish new reasons or crash core operation. Required AE2 contract
failure is a failed build/verification, not a silently disabled feature.

## Decision-protecting alternatives

- Do not classify every false push as INPUT BLOCKED: it loses locks, missing
  destinations, unknown machines, and alternative routes.
- Do not poll and replay dispatch to refresh errors: observation must not
  alter inventory, redstone, or provider scheduling.
- Do not retain these warnings indefinitely: unlike missing-provider lookup,
  insertion rejection cannot safely be revalidated without fresh execution.
- Do not classify partial insertion as rejection: AE2 queues leftovers after
  a successful dispatch. Its own boolean result is authoritative.

Acceptance-to-test mapping and delivery gates are in the
[implementation plan](implementation-plan.md).
