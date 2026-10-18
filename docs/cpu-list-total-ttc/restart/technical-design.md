# Running-job estimate persistence

Lifecycle and acceptance: [spec](spec.md).

## Evidence and ownership

Inspected source: `de58842a65535872a2fd30653d2f9e57b530d381`, also live
`master` during investigation on 2026-10-18.

`ProfilerBridge.startJob` builds a `CraftingJobEstimate` from the accepted plan.
`CraftProfiler.jobEstimates` is an `IdentityHashMap`; `complete` reduces its
remaining amounts even without a pending timing sample, and `clearPending`
removes it. `ProfilerBridge.load` replaces the profiler at server-started.
Row samples have independent SavedData persistence. Both total request handlers
use the same server graph; no transport or renderer change is needed.

AE2's block entity saves its core cluster, which delegates to the logic's
`writeToNBT`. Loading retains `previousState` until cluster formation, then calls
`readFromNBT`. The native logic writes its current job under `job`. The existing
Forge suspension mixin already adds a namespaced field inside that compound.
Store the estimate there so it follows the actual job save and cluster rebuild.
Do not add a separately saved CPU-position registry with stale-record cleanup.
Do not reconstruct from remaining tasks, which omit already dispatched work.

Local upstream source/JAR inspection verified AE2 15.0.10 Forge/Fabric,
19.2.17 and 26.1.10-beta; AdvancedAE 1.3.6-1.20.1, file 7849217 and 26.1.7;
NeoEco 20.3.0; and AE2LT Forge beta.4 / NeoForge beta.5. Check the exact build
and prepared runtime artifacts again in implementation; keep minimum versions.

## Format

`job["ae2craftingtime:estimate"]` is one byte array. Use `CompoundTag` byte-array
conversion before 26; use `ValueInput.read` / `ValueOutput.store` with
`Codec.BYTE_BUFFER` on 26. These adapters carry the same pure-Java payload.

Version 1 uses big-endian `ByteBuffer` fields in this order:

1. One version byte, value `1`.
2. Crafting-link UUID, two longs; reject the all-zero UUID.
3. Network ID: int UTF-8 byte length, then bytes (0..4,096).
4. Completeness byte (`0` or `1`).
5. Node count (int, 1..4,096), then root index (int into the nodes).
6. Each node: output ID byte length (int, 1..128), UTF-8 bytes, remaining amount (long),
   dependency count (int), and dependency indexes (ints).

The node set is the union of the root, remaining-amount keys and dependency
endpoints. A key with no remaining-amount entry has amount zero. Store the
network once; all keys must belong to it. Output IDs must pass existing
`PacketLimits.checkedOutputId` (including its 128-character/resource-ID rule).
Use strict UTF-8 decoding. Negative amounts, invalid indexes, duplicate outputs
or duplicate edges reject the whole snapshot. Cycles are allowed: the existing
estimator already marks their estimates unreliable. Do not sort just for tests;
assert equivalent graph behavior rather than incidental byte order.

The total payload limit is 1,048,576 bytes and the total edge limit is 16,384.
Check counts before constructing node/edge collections; check each string's
length against remaining bytes and its bound before decoding. Require exact
end-of-input. Unsupported versions, invalid fields and truncation produce an
empty recovery result. Catch only expected decode/validation failures.

On write, inspect source collection sizes before copying; walk at most the
node/edge limits, validate strings, and calculate the exact encoded byte count
with bounded arithmetic before allocating the payload. Stop on the first bound
violation. Never truncate a valid graph into a different graph. The limits allow
thousands of outputs and shared dependencies while bounding per-job restore work;
over-limit jobs keep live estimates but omit this optional saved field.

Extend `CraftingJobEstimate` with its immutable snapshot representation and a
bounded snapshot operation. Add one pure `CraftingJobEstimateCodec`, reusing
`ProfileKey`, `PacketLimits` and JDK buffers/UTF-8. Keep pending-recovery decisions
in one small pure state holder, not in copied loader conditionals. Add only
get/remove/restore estimate operations to `CraftProfiler`; restoration must not
call `startJob`, `startWaiting` or the accuracy tracker.

## Restore lifecycle

Give each logic instance a one-shot pending holder containing the validated
snapshot, its owning server lifecycle token and tracking-generation stamp.
The token represents one server object/lifetime, not a persisted identifier.

All four loader entrypoints call `ProfilerBridge.beginServer(server)` before
world loading (Forge/NeoForge `ServerAboutToStartEvent`, Fabric `SERVER_STARTING`).
This creates the token with readiness false. The existing server-started load
initializes samples/config, then marks that token ready. A subsequent whole-profiler
replacement or `loadSamples` reset invalidates the recovery generation. Ordinary
`clearStats` / `clearSamples` removes row samples, not the live job graph, and must
not advance this generation. Its total becomes unknown or partial according to
the remaining samples; later learned samples may make it known again. Clear the active
server at server-stopped, after final saves, not at server-stopping. Existing
stopping cleanup must not cause the graph writer to drop valid state during the
final save: move `ServerOptionsRuntime.clear` to server-stopped as well so
serialization still sees the world's effective tracking settings. Check the
active server object as well as the token at attachment.

Advance recovery generations when global profiling is disabled or a supported
integration is disabled. Use one global counter plus per-integration counters
so switching off one addon does not invalidate another scope's pending graph.
The holder captures the applicable counters at read. Initial configuration load
is bootstrap, not a toggle: attachment checks the resulting enabled flags.
`ServerOptionsRuntime.accept` must invalidate on the enabled-to-disabled
transition, before a later enable can hide it. Do not retain holders in a new
global CPU registry.

Attached graphs also require synchronous invalidation. After applying the new
server config, `ProfilerBridge.configure` must remove disabled scopes from the
existing `CraftProfiler.jobEstimates` map before returning. Add one pure method
that removes entries matching a supplied scope predicate; the bridge supplies
`scope -> !ServerOptionsRuntime.scopeEnabled(scope)`, reusing the existing class
classification. Global disable continues through immediate `setEnabled(false)`.
Do this on the config-accept path, not on a later tick, query or save. A disable
then enable with no intervening tick therefore cannot retain an attached graph.
Once attached, the local pending snapshot has already been consumed, so there
is no second copy to serialize or revive. Unaffected scopes keep their graphs.

| Event | Required transition |
| --- | --- |
| Read begins | Remove the prior scope estimate and replace/clear pending recovery, even if the new data has no estimate. Never keep a previous read's graph. |
| Read succeeds | Retain valid pending state only for a saved job. Do not attach to the pre-start profiler. |
| First ready tick or accepted-output callback | Before dispatch/completion accounting, validate live job UUID, scope, current grid network, server/token/generations and enabled tracking. Attach once, then discard pending state. |
| Grid/job still awaiting initialization | Wait without exposing a total or consuming pending state. If output is accepted before identity/readiness can be established, discard recovery rather than attach amounts that missed that output. |
| Identity mismatch, invalid data or disabled scope | Discard pending state and the scope estimate. Do not retry recovery from cached bytes after re-enable. |
| Successful new submission | Clear pending recovery before storing the new plan graph. An unsuccessful submission does not discard the current job. |
| Finish/cancel/failed restore | Clear pending and attached estimate state; no estimate field is written for an absent job. |
| Save before attachment | Preserve the bounded pending snapshot only if its lifecycle/generations and saved job UUID still match. Do not require profiler readiness for this passthrough. |
| Save after attachment | Encode the current remaining graph only for the matching active job and network; never fall back to the original pending bytes. |

Use the live crafting-link UUID (`getLastLink().getCraftingID()`), or NeoEco's
`getCraftingJobId`, to bind the saved snapshot. The root index identifies the
snapshot's final output ID: compare that node's ID to the live job's
`getFinalJobOutput().what()` converted through the existing profiler key helper.
Adapters pass the live UUID, output ID and network ID to the pure recovery
decision; a CPU serial or output name alone is insufficient. Network
comparison waits for a live grid and preserves the stored network identity;
it never rewrites keys to use another network's samples.

Normal AE2 mutations already dirty/save the job. The snapshot is taken at the
same save boundary, after the normal progress hook has updated the graph.
Do not add periodic independent writes. Repeated reads and cluster transfers
must use this production lifecycle, not the test driver's positional rebind.

## Adapter placement

Keep shared profiler access in the existing `mc1201` and `mc2612` bridges.
Put the pure state decisions above in `shared/src/main/java`. Add thin
persistence mixins beside the existing target integrations, with exact method
descriptors; do not duplicate their dispatch/profiling code.

| Owner | Adapter source and target | Save/read hook |
| --- | --- | --- |
| Native, 1.20.1 Forge/Fabric | One common 1.20.1 adapter under `shared/src/mc1201`, included only by those two modules | `CraftingCpuLogic` with `CompoundTag` |
| Native, 1.21.1 NeoForge | Version module adapter | Same logic with `CompoundTag, HolderLookup.Provider` |
| Native, 26.1.2 NeoForge | `shared/src/mc2612` adapter | Same logic with `ValueInput` / `ValueOutput` |
| AdvancedAE, 1.20.1 Forge | Forge version adapter | Independent `AdvCraftingCPULogic`, `CompoundTag` |
| AdvancedAE, 1.21.1 NeoForge | NeoForge version adapter | Same addon logic, tag plus registry provider |
| AdvancedAE, 26.1.2 NeoForge | Native26 version adapter | Same addon logic, value input/output |
| NeoEco, Forge/NeoForge pre-26 | Shared `mc1201` adapter, selected only for existing NeoEco targets | Independent `ECOCraftingCPULogic`, tag plus registry provider |
| AE2LT, Forge/NeoForge pre-26 | Shared `mc1201` adapter, selected only for existing time-wheel targets | Tag plus registry provider; actual delayed `restoreJobFromNBT` too |

Use a source inclusion/exclusion for the 1.20.1-only adapter, following existing
Gradle source selection. Register the exact adapter in each module's mixin list
and extend existing `IntegrationCatalog` candidates/contracts for optional
classes. Required data hooks do not use silent `require = 0` fallback. Do not
introduce optional addon classes into native or Fabric classloading.

Native and AdvancedAE use cluster/cpu as profiler scope; NeoEco and AE2LT use
their logic instance. The existing execution mixins delegate pending consumption
at tick/accepted-output boundaries and clear it at submit/finish boundaries.
Use a small common access interface for that delegation, without reflective
field enumeration or a second profiler map.

For pre-26 tags, append/remove only the namespaced field in the existing `job`
compound after native serialization. For 26, decorate the existing job
`ValueOutput` as it is passed to the upstream job writer; do not call
`child("job")` again after serialization and risk replacing the saved child.
Read the existing job child after native deserialization. Preserve all unrelated
fields on every path, including invalid optional estimate data.

AE2LT may retain `pendingJobTag` while `hasJob()` is true and `getLastLink()` is
null. Preserve its pending job payload on save. Consume recovery only after
`restoreJobFromNBT` has established the real UUID/grid, before any accepted
output can reduce that job. Its failure path calls `finishJob` and clears
recovery. Refresh the existing mixin's grid from the restored CPU for normal
completion accounting; submission is not the only source of that grid. Apply
the same restored-grid initialization to NeoEco. These hooks must not cause
resubmission, invoke the upstream restore twice, or restore timing samples.

Before AE2LT has a live link, passthrough means preserving its original pending
job tag, not attaching cached estimate bytes to another newly serialized job.
The actual UUID/output/network checks still run when delayed restoration finishes.
