# NeoEco 20.5 crafting time on Forge 1.20.1

Status: finished

Scope: Live profiling and remaining-time estimates for standard-quantity ECO CPU
jobs on NeoEco 20.5.9 and 20.5.10.

Issue: [#760](https://github.com/cTux/ae2-crafting-time/issues/760)

Planning: [Technical design and research](technical-design.md) and
[implementation plan](implementation-plan.md).

Implementation: [PR #762](https://github.com/cTux/ae2-crafting-time/pull/762) and
the [Forge 1.20.1 external CPU API](../../../versions/1.20.1-forge/src/main/java/com/ctux/ae2craftingtime/api/ExternalCpuProfiling.java).

Verification: The [PR verification results](https://github.com/cTux/ae2-crafting-time/pull/762)
record focused Forge 47.4.26 and AE2 15.4.10 runs against the production API
verified by [CI on the implementation PR head](https://github.com/cTux/ae2-crafting-time/actions/runs/45007371356).
NeoEco 20.5.10 passed normal
and FastPath crafting, and 20.5.9 passed normal crafting. Both showed the
learned estimate in NeoEco's status screen and its fallback after a restart.
The 20.5.10 run also verified that a native exact order delivered its output
without adding a Crafting Time sample. The published Crafting Time 1.3.1
baseline delivered an output on 20.5.10 with NeoEco's native elapsed-time
display. Forge 20.3.0 and 20.4.2 passed their focused legacy craft cases. The
base-only Forge craft passed, and NeoForge 1.21.1 passed the catalog and
packaging checks.

NeoEco moved its active CPU logic in the 20.5 line. The 20.5.9 beta in the
report no longer matches Crafting Time's older Forge dispatch adapters. The
stable 20.5.9 and 20.5.10 releases already know how to send crafting events
and ask for time remaining. Crafting Time 1.3.1 lacks the API they call.

| ID | Acceptance criterion |
| --- | --- |
| NE760-1 | On Forge 1.20.1, both stable releases can bind the optional server API and profile committed, standard-quantity ECO CPU work. Returned outputs advance the estimate and create samples only for the CPU that expected them. Simulation, rejected pushes, excess inventory and receipts for other CPUs cannot contribute samples. |
| NE760-2 | NeoEco's existing status page shows Crafting Time's estimate for the selected running ECO CPU when available. Idle, cancelled, restarted without live tracking, disabled and unknown jobs fall back to NeoEco's own status behavior. A failed submission or terminal event leaves no stale tracking for the next job. |
| NE760-3 | Respect the existing global profiling, NeoEco and key-type settings. Pause and resume follow NeoEco's reported state. Disabling tracking clears the affected live state and keeps learned history. Exact big-order jobs continue through NeoEco's native path. |
| NE760-4 | Preserve Forge `pending-accounting` and `batched-long`, NeoForge 1.21.1 `batched-int`, and native startup/crafting with NeoEco absent. Keep the current dependency floors and the separate NeoForge migration in [#457](https://github.com/cTux/ae2-crafting-time/issues/457). |

Earlier 20.5 builds without the optional caller are research context. Restoring
active estimates across restart, exact big-order profiling, provider-location
data, and parallel-capacity reporting are outside this first API contract.

The [per-item Status TTC follow-up](status-item-ttc/spec.md) covers the native
NeoEco item grid.
