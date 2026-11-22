# NeoEco 20.5 Forge profiling design

[Scope and acceptance criteria](spec.md) · [Implementation plan](implementation-plan.md)

## What the tagged source shows

| Version | Finding |
| --- | --- |
| NeoEco 20.5.9-beta | The old `api.me.ECOCraftingCPULogic` is a legacy compatibility superclass. The subclass in `crafting.execution` performs the work. Neither class declares the old `recordPushedPattern` method. [Legacy class](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/2b122023e69c52797229bc4b7d7f5b3a2ea73066/src/main/java/cn/dancingsnow/neoecoae/api/me/ECOCraftingCPULogic.java) · [Active class](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/2b122023e69c52797229bc4b7d7f5b3a2ea73066/src/main/java/cn/dancingsnow/neoecoae/crafting/execution/ECOCraftingCPULogic.java) |
| NeoEco 20.5.9 and 20.5.10 | Both tags ship the same reflection shim. It resolves all six methods together after checking the `ae2craftingtime` mod ID. It passes the real `ECOCraftingCPU` object on the logical server. Missing classes or signatures leave NeoEco's own execution and estimate in use. [20.5.9 shim](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/c0e95ef2d615b4c2a450b008986b8169923c7df2/src/main/java/cn/dancingsnow/neoecoae/integration/ae2craftingtime/ECOCraftingTimeCompat.java) · [20.5.10 shim](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/integration/ae2craftingtime/ECOCraftingTimeCompat.java) |
| Crafting Time 1.3.1 | The catalog checks the exact legacy dispatch descriptor. The Forge variants use long-batch and pending-accounting descriptors. NeoForge 1.21.1 has its own integer-batch descriptor. A class with the old name alone cannot activate these adapters. [Catalog](../../../shared/src/main/java/com/ctux/ae2craftingtime/core/IntegrationCatalog.java) |

The 20.5.10 [lifecycle controller][lifecycle] calls start after installing the
job and finish on termination. [Dispatch accounting][dispatch] reports committed
output and remainder quantities. [Output delivery][output] reports the quantity
actually claimed against the expected key, before final cleanup. For routed
output it reports the expected key even when the delivered key differs. The
[CPU logic][logic] forwards changes to the job's suspension flag. The
[status panel][status] asks for remaining milliseconds only for the selected
running ECO CPU and syncs the optional value in its existing status snapshot.
These are source-level findings. Gameplay and the packaged interface still
need the checks in the implementation plan.

## Server API

Publish `com.ctux.ae2craftingtime.api.ExternalCpuProfiling` in the Forge
1.20.1 production source set. Use public static methods with the types NeoEco
looks up. The class uses AE2 and server-side Crafting Time types, so loading it
does not require NeoEco classes.

| Method | Return | Behavior |
| --- | --- | --- |
| `startJob(ICraftingCPU, IGrid, ICraftingPlan, IActionSource, long)` | `void` | Start the scoped estimate from the committed plan with Crafting Time's existing requester and network resolution. |
| `expectOutput(ICraftingCPU, IGrid, AEKey, long, long)` | `void` | Start timing the reported positive output amount in AE2 units. |
| `acceptOutput(ICraftingCPU, IGrid, AEKey, long, long)` | `void` | Consume timing only for that CPU and key. Reuse the existing amount normalization. |
| `finishJob(ICraftingCPU, boolean, long)` | `void` | Finish or cancel and clear live state, including pending outputs, even if profiling was turned off mid-job. |
| `setSuspended(ICraftingCPU, boolean, long)` | `void` | Mirror NeoEco's suspension transition to the profiler. |
| `remainingMillis(ICraftingCPU)` | `OptionalDouble` | Read the current estimate from the same CPU scope. Return empty when disabled, unknown or detached. |

Use the same CPU object throughout a live job. Use `ProfilerBridge.networkId`
for the grid and preserve shared history keyed by network and output. Keep the
legacy logic-object scopes and descriptors. The public receipt path needs
strict completion: [`ProfilerBridge.complete`][bridge] currently tries
`completeUniquePending` when scoped completion fails. That fallback serves a
legacy reload path. An external receipt without matching dispatch evidence
must leave other CPU scopes alone. Preserve the existing behavior for legacy
callers and reuse its scoped accounting for the new path.

Use the existing `NEO_ECO` option routing for the upstream CPU object and
the existing global and key-level gates. A return without a matched live
job must not revive tracking after cancellation, a server restart or
disable/enable. Support starts with new jobs observed by the API. The six
methods carry neither saved job identity nor restore state. NeoEco skips these
calls for plans marked as exact. The 20.5.10 smoke found that a forced big order
can still reach the start hook through a standard plan with no patterns or
emitted items. The Forge API skips that empty plan so the native order can
deliver its output without recording a Crafting Time sample.

Keep old adapter selection separate from bridge activation. The latter requires
both a compatible caller and a successful API bind. A missing legacy adapter
alone says nothing about whether the new path observed a job. Record runtime
evidence from the new path without treating startup presence as verified craft
coverage.

[lifecycle]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/crafting/execution/ECOCraftingJobLifecycleController.java#L114-L174
[dispatch]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/crafting/execution/ECOCraftingDispatchAccounting.java#L105-L124
[output]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/crafting/execution/ECOCraftingOutputDelivery.java#L114-L161
[logic]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/crafting/execution/ECOCraftingCPULogic.java#L824-L834
[status]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/java/cn/dancingsnow/neoecoae/gui/computation/ComputationCpuStatusPanel.java#L1432-L1439
[bridge]: ../../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProfilerBridge.java
