# Data Energistics Trinity CPU profiling

Status: ready-to-implement

Scope: Trinity CPU profiling with Data Energistics 3.3.5 on NeoForge 1.21.1.

Issue: [#740](https://github.com/cTux/ae2-crafting-time/issues/740)

Planning: [Technical design](technical-design.md) and [implementation plan](implementation-plan.md).

## What needs to change

Trinity runs crafting jobs through its own CPU logic. Crafting Time's native AE2
CPU hooks cannot observe that execution. This is a verified source-level gap;
there is no retained runtime reproduction yet. First build a disposable, real
Trinity fixture and demonstrate the missing profile sample. Add production hooks
only after that checkpoint confirms the gap.

[#518](https://github.com/cTux/ae2-crafting-time/issues/518) qualified a native AE2
CPU with an Adaptive Pattern Provider. Its [qualified boundary](../ae2-addon-integration/data-energistics.md)
stays valid and separate. This work does not qualify every Data Energistics
machine, provider mode, custom key type, or addon combination.

## Required behavior

| ID | Acceptance criterion |
| --- | --- |
| TR-01 | A disposable fixture forms the real Trinity main and CPU structures, publishes their CPUs on the fixture network, selects Trinity, and completes a real craft. Before adapter implementation, retain independent dispatch, accepted-output and completion evidence alongside the missing new server sample. A fake host, direct profiler call, or ordinary CPU is not this baseline. |
| TR-02 | Use the actual executing worker `ICraftingCPU` as the profiler scope. Preserve `networkId + outputId`, normalized item/fluid amounts, job identity and worker isolation. Coordinator submission must not create a second sample or job estimate. A completed supported craft creates exactly the samples justified by its accepted outputs and learns TTC for a reopened Plan and active worker Status. |
| TR-03 | Account committed logical quantities, including counted batches and partial returns. Preparation, rejected dispatch, simulated insertion, duplicate callbacks and returned reusable tools must not create output samples. Include reusable registration, rollback, completed outputs and cancelled suffixes in the accounting contract. |
| TR-04 | Finish or cancel only when the upstream job actually terminates. Clear unfinished accounting on cancellation, failed submission, worker replacement and invalid recovery. A fresh job on the same worker must not inherit pending quantities or timings. Reload must preserve valid learned history and recover only a matching job/network/output estimate through the existing recovery lifecycle; it must not manufacture a sample from unknown pre-reload timing. |
| TR-05 | Quantity and capacity conversions must be checked. Representable counted and reusable quantities work normally. If a quantity cannot fit the existing profiler representation, suppress that job's live profiling/estimate, clear its pending accounting and emit one diagnostic; continue the native craft unchanged. Never truncate, saturate into a misleading sample, or loop once per enormous logical count. CPU storage and provider admission capacity are not measured parallel slots. |
| TR-06 | Select the optional adapter by a checked API contract only on NeoForge 1.21.1. Addon-absent and incompatible-contract cases skip it safely with existing diagnostics. Keep Data Energistics optional, its minimum at 3.3.5 without a new upper cap, and other targets unchanged. Production JARs contain no fixture code or bundled addon classes. |
| TR-07 | Add a server integration switch for Trinity profiling, default On like the existing CPU integrations. Global Profiling and this switch both apply to worker samples, live estimates and recovery. Turning it off clears live tracking; turning it on must not resume stale pending work. Preserve already learned history. Update English/Ukrainian option text, GuideME and wiki documentation together. |
| TR-08 | Verify real Trinity CPU selection, Plan and active/completed Status contents, accepted dispatch/output, learned TTC, cancellation/new job and restart. Retain exact source/JAR identities, observations, screenshots and manual review. Verify ordinary native crafting both with Data present and absent. Keep unsupported or untested modes explicit in the final support boundary. |

## Limits and completion

The exact reference artifact is `data_energistics-1.21.1-3.3.5.jar`, CurseForge
project `1565514`, file `9095664`. Runtime uses the existing focused compatible
Data graph; Lightning Tech and Thunderbolt Core remain excluded because their
metadata rejects Data Energistics. No dependency upgrade or whole-addon audit is
part of this change.

Counted/reusable correctness is mandatory; a hook firing is not proof. Pure
boundary tests can prove arithmetic and lifecycle policy, but actual Trinity
execution must prove the adapter reaches those boundaries. If a required runtime
case cannot be constructed, record it as an unmet gate rather than broadening a
support claim or silently excluding that criterion. Truly unrepresentable
quantities are the explicit fail-closed case in TR-05, not permission to exclude
ordinary counted or reusable work.

Completion requires the fixture baseline, implemented adapter, current-head
review and CI, required focused runtime evidence, and truthful support docs.
Automatic screenshot gates may remain `REVIEW_REQUIRED` without qualified
baselines; record the separate manual review and never relabel them as passed.
