# Keep running-job totals after restart

Status: implementation in progress; pending independent code review, post-PR
coverage/build checks, and clean two-process native restart verification.

Scope: Save and restore the existing remaining-job estimate for supported CPUs.

Issue: [#636](https://github.com/cTux/ae2-crafting-time/issues/636).

Planning: [Design](technical-design.md) and [reviewed plan](implementation-plan.md).

## Behavior

A craft that had a usable total before a normal save/restart should regain its
own total after the server restores that same job. The selected title and CPU
card still use the existing server estimate and fresh client snapshot. They
must stay blank until a fresh response arrives.

The reproduced case on source `de58842a65535872a2fd30653d2f9e57b530d381`
retained running native AE2 jobs and row samples, but lost their in-memory
dependency graphs. This explains that restart case; it does not identify the
original reporter's installation or establish a connection to #616.

Preserve the graph, its remaining normalized amounts, and its partial-data
flag with the CPU's saved job. Continue using current learned samples after
restore. Do not freeze a duration or sum visible row times. Progress received
after restore reduces the saved remaining amounts through the normal path.

Cover native CPUs on all four release rows, AdvancedAE on its three supported
rows, and the existing NeoEco and AE2LT time-wheel integrations on Forge
1.20.1 and NeoForge 1.21.1. Addons using native logic inherit native behavior.
Unknown CPU scopes remain unknown; this does not add integrations.

## Compatibility and limits

- Existing saves remain readable. A job saved before this change has no graph
  to recover and remains unknown until a later newly submitted job provides one.
- Missing, malformed, oversized, or newer-format estimate data only loses the
  optional estimate. It must not cancel a craft, discard inventory, or prevent
  world loading. Never partially restore a rejected graph.
- Save at most 1 MiB, 4,096 nodes and 16,384 dependency edges per job. Jobs beyond
  these bounds continue normally and retain their live estimates, but their
  estimates do not survive restart. The [design](technical-design.md#format)
  defines byte and string limits before allocation.
- A graph belongs to one crafting-link UUID, profiler scope and network. Never
  transfer it to a replacement job, another CPU or another world's profiler.
  A changed network identity invalidates recovery rather than borrowing samples.
- Preserve current unknown, zero, idle, partial, stalled and cyclic-graph
  semantics. Restoring graph structure does not make missing samples known.
- Disabling profiling or a CPU integration discards its active recovery state;
  re-enabling must not revive a graph that missed progress. Existing display
  switches still only control display. No new option or translated status is
  needed. Save-history continues to govern learned samples; the graph contains
  no timing history and does not override that option.
- Do not restore pending timing operations, capacity, accuracy sessions,
  notification ownership or offline elapsed time. Do not change SavedData or
  network packet formats, crafting execution, UI layout or TTC mathematics.

This extension supersedes only the original CPU-total documents' no-persistence
boundary. Their original scope and completion status remain separate.

## Acceptance

| ID | Required result |
| --- | --- |
| R1 | The same saved jobs regain their own server totals and matching native card/title values after a new process starts, with fresh-response/cache-clear proof. No cancellation, resubmission or test-only graph rebind supplies recovery. |
| R2 | Roundtrip preserves remaining amounts, dependencies and reliability; real accepted output decreases the recovered estimate and completion/cancellation removes it. |
| R3 | Legacy, missing, malformed, unsupported and over-limit data safely yields no recovered total while the job and unrelated save fields remain intact. |
| R4 | Startup ordering, same-JVM world changes, repeated reads, profiler reset, disabled tracking, same-output replacement and AE2LT delayed restoration cannot revive or misattribute a graph. |
| R5 | Every supported adapter builds and its actual persistence hook is checked. Native runtime proof covers the three save API families; addon boundary tests and the existing focused addon scenario have the limits stated in the plan. |
| R6 | Both GuideME languages, wiki source and dependency/test coverage descriptions explain the recovery behavior and limits without claiming the original unidentified incident was diagnosed. |

The [plan](implementation-plan.md) maps these criteria to bounded checks. It
does not reopen all unfinished acceptance work from the original #324 scope.
