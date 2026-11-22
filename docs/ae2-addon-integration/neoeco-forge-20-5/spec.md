# NeoEco 20.5 crafting time on Forge 1.20.1

Status: ready-to-implement

Scope: Live profiling and remaining-time estimates for standard-quantity ECO CPU
jobs on NeoEco 20.5.9 and 20.5.10.

Issue: [#760](https://github.com/cTux/ae2-crafting-time/issues/760)

Planning: [Technical design and research](technical-design.md) and
[implementation plan](implementation-plan.md).

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
