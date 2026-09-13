# NO CHANNEL implementation plan

Issue: [#405](https://github.com/cTux/ae2-crafting-time/issues/405).
Implement the [specification](spec.md) through the
[technical design](technical-design.md). This is future implementation work;
the planning PR changes documentation only and must not close the issue.

## 1. Extend the existing dispatch facts and evaluator

Ownership: `shared/src/main/java/com/ctux/ae2craftingtime/core/
{CraftingBlockReason,ProviderDispatchTracker}.java` and their existing tests.

- Append the new block reason and attempt result; add a pure predicate for
  failed activity + node present + powered + booted + unmet channels.
- Extend explicit result mapping, dispatch-reason admission, and row priority.
  Preserve unanimous candidates, exact-pattern isolation, the 20-tick TTL,
  positive output filtering, and cleanup. No second tracker.
- Define tests before editing: all predicate conditions independently false,
  all true, success/unknown/busy/partial iteration/empty iteration, unanimous
  channel failures, and mixed channel/lock/input/target causes. Exercise both
  provider orders, two patterns sharing an output, positive/zero output amounts,
  ticks 19/20 and backwards time, replacement, clear, and disabled tracking.
- Extend `CraftProfilerTest` for NO PROVIDER/NO POWER priority, lifecycle
  cleanup, separate CPUs/networks, and the persistence whitelist. Verify
  `StatusKind`, saved samples, and completion learning remain unchanged.

Delivers AC-02/03 and the core parts of AC-01/04/05. Every changed pure line
and branch must remain covered by the existing 100% JaCoCo gate.

## 2. Observe the inactive-provider guard on every target

Ownership: `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/
{ProviderDispatchContext,ProviderDispatchObserver}.java` and
`mixin/PatternProviderLogicMixin.java`. Keep the native CPU observer in
`mixin/CraftingCpuLogicMixin.java` and the AdvancedAE observer in
`shared/src/neoforge/java/com/ctux/ae2craftingtime/mc1201/mixin/
AdvancedCraftingCpuLogicMixin.java` as the shared entry paths.

- Wrap exactly the `IManagedGridNode.isActive()Z` call in `pushPattern`,
  retaining original invocation count/result. Feed nullable-node facts to the
  covered predicate and frame. SUCCESS must override channel evidence.
- Extend existing `ProviderDispatchContextTest` and
  `ProviderObservationInjectionTest` boundary coverage: guard occurs before
  the lock call, the send queue short-circuits it, missing node is unknown,
  nested/unmatched frames are isolated, and no original operation is replayed.
- Confirm the descriptor against all four pinned AE2 artifacts, and all
  retained supported adapter variants. Verify newest AdvancedAE's actual CPU
  path reaches this observer on Forge 1.20.1 and both NeoForge versions.
  Custom paths that skip the guard remain unknown. Core-only/dedicated-server
  loading must not resolve missing addon/client classes.

Delivers source/contract evidence for AC-01/02/03/06; it is not live UI proof.

## 3. Carry and display the new reason

Ownership: shared `CraftingRowState`, `ClientStatsCache`, `StatsPacketCodec`,
`TtcText`, `CraftingStatusTableRendererMixin`; both API variants of
`ProfilerBridge`/`CraftingCPUScreenMixin`; each `versions/<target>`
`StatsNetwork` and snapshot wrapper; `shared/src/main/resources/assets/
ae2craftingtime/lang/{en_us,uk_ua}.json` and both GuideME status trees.

- Apply the protocol increments in the design, preserving exact enum decoding,
  MAX_KEYS, requested-key checks, rate limits, and menu/CPU authorization.
- Add the badge and mixed-row qualifier using existing rendering/sorting.
  Add exact English/Ukrainian text and a GuideME status page/index entry in
  each locale. Update `docs/dependencies.md` with the verified scope.
- Extend `CraftingRowStateTest`, text tests, `ClientStatsCacheTest`, and
  `StatsPacketTest` boundaries for the new enum: zero pending, mixed amounts,
  no samples, stale/omitted keys, foreign CPU contexts, unknown ordinals,
  malformed keys, maximum/excessive maps, and all four codec wrappers.
- Confirm no new StatusKind/NBT entry, chat/locate/plate action, total-TTC
  algorithm, dependency, or optional UI feature was introduced.

Delivers AC-04/05/06 transport and static translation evidence.

## 4. Add and run actual channel-starvation scenarios

Ownership: existing `DispatchStatusFixture` and dispatch scenario support in
`shared/src/testDriver1201` and
`versions/26.1.2-neoforge/src/testDriver`, addon fixtures in
`shared/src/testDriverAddons`, both driver runtimes, `SuitePlan`, and the
smoke selection/evidence files under `scripts`.

Register `no-channel-status` with the host groups, impact rules, driver
selection, planner tests, and evidence requirements. Reuse existing native
and AdvancedAE CPU construction. Do not mock status maps or force the node's
channel fields for the acceptance scenario.

1. Build a powered controller network. Put the CPU, terminal, and required
   storage on a healthy branch. Saturate a separate normal-cable branch with
   channel-using devices, then connect the tested provider beyond that
   bottleneck. Wait for AE2 pathing to finish. Check real node predicates and
   ensure that the chosen provider is starved; do not assume allocation order.
2. Encode a unique processing pattern, provide its ingredients and a usable
   target, and submit a real job through the normal crafting flow. Verify
   positive scheduled amount, failed dispatch, the NO CHANNEL badge without
   samples, tooltip content, style/bounds, and unknown-time sorting.
3. Free a channel on that branch or provide a sufficient route. Wait for
   reboot to finish, verify channel access, badge recovery within NC-05, real
   dispatch, returned output, and successful job completion.
4. Repeat with a healthy duplicate-pattern provider on the healthy branch;
   prove it can dispatch and the channel badge is absent. Cover a busy
   alternative, different blockers, and mixed active/pending/shared-output
   rows through deterministic fixtures and the evaluator tests.
5. Exercise reboot, power-only loss, missing inputs, and infinite mode in an
   isolated test world, restoring the original mode during teardown. Assert
   actual node state and absence of fresh false channel evidence. An inactive
   CPU must not create a new provider-channel diagnosis.
6. Cover CPU switch/late reply, cancel/replace, disable/reload, and save/reopen
   without persistent NO CHANNEL or an alias. Regress existing NO PROVIDER,
   NO POWER, NO SPACE, Waiting, DELAYED, dispatch statuses, and total TTC.

Run direct English status/recovery cases on all four native targets and all
three applicable AdvancedAE targets, using newest implemented adapters.
Generic AdvancedAE startup or throughput smoke is insufficient. Keep older
variants' contract/packaging coverage. Verify Ukrainian keys/placeholders and
meaning statically. Capture target/profile/adapter identity, result.json,
screenshots, logs, and dependency manifest; inspect tooltip/layout screenshots.

Delivers live evidence for AC-01 through AC-06. Unavailable runtime evidence
is a named incomplete gate, never replaced with source assertions.

## 5. Verification and delivery

- Review the full diff and the AC mapping above. Commit the implementation as
  one conventional change through the repository hook; run no local tests
  before that hook creates the PR. Planning-only changes need link/content
  checks, not fake unit tests or a Minecraft launch.
- After PR creation, run the applicable shared logic, codec, contract,
  packaging, and script checks. Required GitHub CI runs
  `./gradlew test jacocoTestReport`; report it separately from local evidence.
- Review `scripts/run-ui-smoke.ps1 -Changed -BaseRef origin/master -PlanOnly`,
  then use the prepared-client smoke skill to run the changed-scope selection
  in CodexVM. Ensure the explicit four-native/three-AdvancedAE matrix above is
  represented; generic or narrower selection cannot waive it. Keep
  `selection.json` and bind all evidence to the implementation commit.
- Check production JARs exclude the driver; check documentation links,
  translations, protocol agreement, and `git diff --check`.
- Update this plan's implementation state and issue evidence after AC-01
  through AC-06 pass. Implementation merge/release needs its own user scope;
  the current request authorizes merging these planning documents only.

Completion means every AC has matching core/boundary/live evidence, required
CI is green at the reviewed head, and no unverified addon or runtime result
is presented as supported. No blocking implementation dependency was found;
the existing #216 and #120 code is already on the researched baseline.
