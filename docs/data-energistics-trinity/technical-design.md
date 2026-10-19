# Trinity CPU integration design

Scope and delivery status: [specification](spec.md).

## Evidence and current path

Repository investigation used `aad756929a7bd998ef6ea679cfa9e97bd0e2754f`.
Upstream references below use the official 3.3.5 revision
[`9eefdd72b8fde5c88d26642ae152b420ecd24135`](https://github.com/ModularMCLib/DataEnergistics/tree/9eefdd72b8fde5c88d26642ae152b420ecd24135).
The published artifact SHA-256 is
`f17f15068a638c59c1d3600c83a259ab24757e4a271d82112e1f421618bd58d0`.
Investigation verified the SHA-256 of the actual
`curse.maven:data-energistics-1565514:9095664` JAR and successfully inspected
`TrinityDataCoreVirtualCpu` with Java 21 `javap`.
Use `javap -p -s -c -classpath <jar> <class>` to verify every selected descriptor
and call site before adding hooks; the complete binary contract is still a gate.

[`TrinityDataCoreVirtualCpu`](https://github.com/ModularMCLib/DataEnergistics/blob/9eefdd72b8fde5c88d26642ae152b420ecd24135/src/main/java/com/fish_dan_/data_energistics/common/crafting/trinity/execution/cpu/TrinityDataCoreVirtualCpu.java)
implements `ICraftingCPU`. CPU zero coordinates submission through
`TrinityDataCoreCraftingRuntime`; nonzero workers run
[`TrinityDataCoreCpuLogic`](https://github.com/ModularMCLib/DataEnergistics/blob/9eefdd72b8fde5c88d26642ae152b420ecd24135/src/main/java/com/fish_dan_/data_energistics/common/crafting/trinity/execution/cpu/TrinityDataCoreCpuLogic.java).
That class does not extend AE2 `CraftingCpuLogic`. Therefore the native
`shared/src/mcCommon/java/.../mixin/CraftingCpuLogicMixin.java` cannot supply
Trinity dispatch, output, finish, submit or capacity observations.

`ProfilerBridge` in `shared/src/mc1201/java` already accepts an `Object` scope.
`CpuTtcRequestHandler.collect` passes each listed `ICraftingCPU` directly to
`ProfilerBridge.remainingJobEstimate(cpu, grid, tick)`. Use the exact worker
instance everywhere; do not key the bridge by its logic object or coordinator.
No new CPU resolver, network message, or estimator is needed for these entries.
Verify the actual Trinity Status/menu path at runtime before assuming an
existing renderer covers it.

## Adapter placement and contract

Keep target-specific mixins/accessors under
`versions/1.21.1-neoforge/src/main/java/com/ctux/ae2craftingtime/mc1201/` and register
them only in that target's `ae2craftingtime.mixins.json`. Reuse the existing
`@Pseudo`, string-target and `IntegrationMixinPlugin` pattern. Add a
`data_energistics` candidate to `IntegrationCatalog`, restricted to
`1.21.1-neoforge`, with a descriptive variant such as `trinity-3.3.5`.
Check every consumed field/method descriptor, including nested record accessors,
before selection. A version string alone is insufficient. Compare bytecode in
the pinned JAR with source before freezing the candidate contract.

Use compile-only Data classes where signatures require them; never add a required
runtime dependency or package the addon. The existing test-driver dependency can
remain test-only unless production signatures need the same coordinate.
Put all arithmetic, deduplication and lifecycle decisions in covered pure Java
under `shared/src/main/java/.../core`; keep mixins as observation and conversion.

The following are the required observation seams, not permission to copy native
CPU hooks verbatim. Names refer to `TrinityDataCoreCpuLogic` unless qualified.

| Upstream seam | Required observation |
| --- | --- |
| Field `cpu: TrinityDataCoreVirtualCpu`; worker `grid(): IGrid`, `level(): Level`, `number(): int` | Access the current worker and service grid. The grid is nullable before attachment; no profile may start without a valid attached identity. |
| `trySubmitJob(IGrid, ICraftingPlan, IActionSource, ICraftingRequester): ICraftingSubmitResult` | At successful return, capture concrete worker/job identity and start its estimate. Rejected/offline/busy/too-small submissions create nothing. Do not also start at coordinator `submitJob`. |
| `commitPatternPush(ExecutingJob, TaskProgress, PreparedPatternCommit): void` and `commitTrinityPatternPush(ExecutingJob, TrinityPlanExecution, Work, long, PreparedPatternCommit): void` | Observe accepted `expectedOutputs` and `expectedContainerItems` exactly once. They already contain committed quantities; do not multiply by `count` again. Do not observe prepare or capacity probes as dispatch. |
| `dispatchExactBatch` accepted-commit callback | Cover the separate `BigInteger` waiting additions, which bypass both ordinary commit methods. Feed checked amounts to the same accounting helper; reject unrepresentable jobs as TR-05 specifies. |
| `insert(AEKey, long, Actionable): long` and `insertExact(AEKey, BigInteger, Actionable): BigInteger` | Observe actual modulated accepted quantities, not requested amounts. Preserve the planned output key when dynamic output matching consumes a different actual key. A local waiting-counter decrement inside these methods identifies consumed planned output; unrelated withdrawal operations do not count as completion. |
| `registerReusableWaiting(ExecutingJob, Submission)`, `rollbackReusableWaiting(ExecutingJob, Submission)`, `accountReusable(ExecutingJob, Work, Submission, ObjectList)` | Registration can be rolled back. Publish starts only for accepted ownership, once per submission identity. Cancelled suffixes retract unfinished accounting without recording output. Keep product/remainder quantities separate from resident reusable assets. |
| `completeReusableOutputs(UUID, Submission, long)` and `receiveReusableSettlement(Session, Settlement): boolean` | Follow outputs through their actual acceptance path. Settlement receipts can be replayed, contain cancelled work, or return tools. Do not count a receipt and its subsequent insertion twice; never treat returned assets as production. |
| `finishJob(boolean): void` | Capture the old job and confirm it actually became terminal. Success may return early while reusable custody remains unsettled. Preserve output-before-finish ordering when `insert` calls `finishJob` internally; a bare HEAD or RETURN hook without the job transition check is wrong. |
| `writeToTag(HolderLookup.Provider): CompoundTag`, `readFromTag(CompoundTag, HolderLookup.Provider): void` | Persist/restore only the existing estimate codec in a namespaced tag. Bind recovery to the worker's real crafting UUID, final output and service network. Never alter Data's schema or inventory tags. |
| `dispatchStep(IEnergyService, CraftingService, CraftingDispatchWindow, CraftingDispatchBudget)` and `tickCraftingLogic` with the same arguments | Both execution entrypoints exist. Recovery must occur before work on either path; do not rely only on the legacy full-worker tick. |

`ExecutingJob` above abbreviates `TrinityDataCoreExecutingCraftingJob`.
`PreparedPatternCommit` is a private nested record; its typed accessors require a
checked accessor boundary if selected. Inspect the exact JVM descriptors for
the nested `Submission`, `Session`, `Settlement`, `Work` and commit types rather
than guessing their package names in a mixin selector.

The [counted dispatch contract](https://github.com/ModularMCLib/DataEnergistics/blob/9eefdd72b8fde5c88d26642ae152b420ecd24135/docs/api/crafting/counted-dispatch-contract.md)
makes prepare read-only and commit single-use. Ownership can have transferred
even when a provider subsequently fails. Follow the CPU's committed accounting,
not only the provider's boolean result. The
[reusable contract](https://github.com/ModularMCLib/DataEnergistics/blob/9eefdd72b8fde5c88d26642ae152b420ecd24135/docs/api/crafting/packaged-reusable-inputs.md)
separates products, remainders, resident tools and settlement. Upstream tests are
useful examples, but do not prove our hooks work.

## Accounting and recovery rules

Use one job-owned accounting state per worker, bound to the real job UUID.
Dispatch starts and output completions use the same planned key and normalized
amount. Batch operations stay aggregate. A job/worker change invalidates pending
state; another worker producing the same output must remain independent.

The helper must distinguish tentative reusable registration from committed work,
and cancellation from accepted output. If the existing profiler lacks a partial
pending-withdrawal operation, add the smallest covered operation there and a thin
bridge delegate; do not approximate cancellation by completing the withdrawn
quantity. Keep already completed samples. On genuinely unrepresentable amounts,
clear only that job's live state and mark it unsupported until its next job.

Observe insert completion before terminal cleanup. For a return-based observer,
defer the matching finish notification until the accepted output is processed;
alternatively observe the actual accepted waiting decrement before upstream
finishes. Do not issue two completions through both observations.

Reuse `CraftingJobRecovery`, `EstimateRecoveryAccess` and `ProfilerBridge`'s
save/read/restore methods. Existing history persistence remains unchanged.
Unknown pre-reload dispatch timestamps must not become new duration samples.
Test valid recovery, changed job/network/output, a late return immediately after
load, cancellation, and disable/re-enable. Restoration is server-owned.

Trinity's recent-operation load counts physical submissions; storage bytes and
worker slots have other meanings. Do not call `updateCapacity` with invented
native `coProcessors + 1` values. Preserve unknown capacity unless the observed
upstream quantity actually represents the profiler's parallel-slot contract.
Required boundary evidence includes too-small admission, exhausted provider
capacity, and a counted submission greater than one.

Add `DATA_ENERGISTICS_TRINITY` with key `dataEnergisticsTrinity` to the server
integration options, default On. Map Trinity worker scopes in both
`ServerOptionsRuntime.scopeEnabled` and `ProfilerBridge.integration`; include it
in disable/recovery invalidation. Follow existing option serialization and
compatibility conventions; append rather than reorder identifiers where needed.
Clear live tracking on disable and do not backfill work observed while disabled.
Use the existing configuration UI, with English/Ukrainian labels and guide text.

## Fixture and boundaries

Extend the lazy `AddonCpuFixture` registry with `data-energistics-trinity-cpu` and a
target-local fixture. Reuse `DataEnergisticsFixture`'s real processing layout where
possible, but replace CPU placement/lookup with actual Trinity structures.
[`TrinityDataCoreBlockEntity.autoBuildTrinityStructure`](https://github.com/ModularMCLib/DataEnergistics/blob/9eefdd72b8fde5c88d26642ae152b420ecd24135/src/main/java/com/fish_dan_/data_energistics/blockentity/trinity/TrinityDataCoreBlockEntity.java#L879)
provides a native builder. Build the main structure first, wait for formation,
then build the CPU child. Check failure diagnostics, current published partitions,
grid identity and worker job identity. The packaged
`data/data_energistics/multiblock/trinity_data_core/main.json` has a fixed
27-by-28-by-32 extent. Read `cpu.json` and `crafting.json` alongside it, compute
the rotated union including selected repetitions, and check empty space and build
height before placing anything. The marker identifies a disposable world and
terminal, not a predefined clear volume. Record the chosen bounds and restrict
cleanup to that owned volume. Do not use synthetic `setCpuContribution` or fake-host
overrides to claim a formed machine.

The existing scenario waits for profiler observations. Baseline evidence must
also observe native submission, accepted output, job completion and stored output
independently, so missing profiler hooks cause a meaningful reproduction rather
than an unexplained timeout. `DispatchObservation` observes `ProfilerBridge`
calls and cannot supply that independent proof. Add test-driver-only, non-mutating
Trinity observers and compare the final network inventory count. Never synthesize
a sample or call `ProfilerBridge` from the fixture to make the baseline pass.
Add only the issue-specific observation/state needed
to the shared driver; reuse its deadline, screenshot and result infrastructure.
For counted/reusable proof, also form the native crafting child and mount an
encoded `AECraftingPattern` for vanilla `minecraft:book_cloning` in its real
pattern core. Supply one generation-zero written book and multiple writable
books. `VanillaRetainedInputRules` retains the original; the production
`PersistentTrinityPatternCore` routes through `TrinityReusableCraftingHost` and
`NativeReusableCrafting`, whose `maximumBatch` supports this recipe. No custom
recipe or external mod is needed. The pinned upstream
`src/test/java/com/fish_dan_/data_energistics/common/crafting/trinity/reusable/rules/VanillaRetainedInputRulesGameTest.java`
shows the exact book components and `AECraftingPattern.encode` setup in
`registeredBookRulePreservesActualOriginal` and `verify`. Reuse that input setup,
not its standalone core/host construction: runtime proof must traverse the
formed, published structure. Test helper arithmetic separately from that path.

No new wire format, world format, generic runner or dependency upgrade is planned.
If a native UI presentation hook is necessary, keep it optional (`require = 0`)
and verify the remapped production target plus client startup. A presentation
fallback must not conceal missing required accounting hooks.
