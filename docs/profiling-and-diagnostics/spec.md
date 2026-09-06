# Profiling And Diagnostics Spec

## Goal

Learn how quickly each AE2 network produces an output, then use that history to
estimate work and explain unusually slow jobs. The result should get more useful
as the world runs without pretending that an estimate is exact.

## Learned Throughput

- Identify history by AE2 network and output id. Two separate networks must not
  share samples.
- Measure a continuous production window from the first dispatched batch until
  every matching batch on that network becomes idle.
- Combine concurrent batches for the same output. Do not add parallel durations
  as if the work ran one batch at a time.
- Keep at most `maxSamples` recent completed windows per output. The default is
  10 and the accepted range is 1 through 100.
- Give newer usable samples more weight than older samples.
- Starting with five samples, exclude duration-per-unit values outside the
  median divided or multiplied by `outlierMultiplier`. The default is 4.0 and
  the accepted range is 1.0 through 1000.0.
- Mark an estimate reliable only when at least three samples exist and none was
  excluded. Low-confidence estimates remain visible with a `?`.
- Keep items in item units. Normalize fluids and chemical-style keys to
  millibuckets before profiling and estimating.

## Prediction Accuracy

- Freeze the displayed whole-plan TTC only after AE2 accepts the job.
- Record the predicted seconds, known plan rows, total plan rows, start tick,
  and monotonic start time for that crafting CPU.
- Add an accuracy sample only when the job finishes successfully.
- Exclude cancelled jobs, restored jobs, invalid clocks, and jobs with no usable
  prediction.
- Calculate aggregate error metrics only from fully covered plans. Partial plans
  still contribute coverage and latest-job context.
- Keep accuracy runtime-only and bounded by `maxSamples` per final output.
- Never feed accuracy results back into throughput or TTC calculations.

## Delayed Output Diagnostics

- Diagnose only outputs that still have pending work and retained throughput
  history on the selected crafting CPU.
- Mark an output delayed after both 30 seconds without progress and twice its
  learned average production-window duration.
- Treat partial accepted output as progress and restart the idle timer.
- Show active and scheduled amounts plus recently used parallel slots when that
  capacity sample is no more than 20 ticks old.
- Suggest more parallel Pattern Providers or machines when work is waiting and
  slots are free. Suggest Crafting Co-Processors when all recent slots were used.
  Always suggest speeding up the active machine.
- Keep pending work, capacity, and delay state runtime-only.

## Lifecycle And Reset Rules

- Starting with profiling disabled hides stats and ignores new profiling,
  accuracy, and capacity events. Retained throughput samples stay saved.
- Finishing or cancelling a crafting CPU job clears unmatched work for that CPU
  so it cannot contaminate a later sample.
- Clearing an output removes its retained samples, pending work, busy window,
  and prediction-accuracy history for that network/output key.
- Persist only completed throughput samples through Minecraft `SavedData`.

## Acceptance Checks

- Sequential and concurrent batches produce the correct network-wide rate.
- A same-tick completion lasts at least one tick.
- Invalid, unmatched, disabled, or nonpositive events create no sample.
- The configured sample limit keeps the newest windows.
- Outlier boundaries, recency weighting, reliability, and normalized units are
  deterministic.
- Only successful accepted jobs create accuracy samples, and partial plans do
  not change aggregate error metrics.
- Delay state starts at the combined threshold, resets on progress, and uses
  only fresh parallel-capacity evidence.
- Reset, cancellation, and load clear their documented runtime state. Saved
  output contains only completed throughput samples.

See [technical-design.md](technical-design.md) for ownership and code flow.

## Planned: normalized sample details (#114)

Status: planning only; no runtime behavior has changed in this documentation PR.
Tracking issue: [#114](https://github.com/cTux/ae2-crafting-time/issues/114).
This section extends the existing learned-throughput requirements. Its companion
is the [implementation plan](implementation-plan.md).

### Confirmed meaning

Show effective time per output unit. The maintainer confirmed that nine items
returned over 90 ticks should read `1 item / 10 ticks`, while preserving the
actual observation count. This replaces the issue's original request to create
one timing sample for every Pattern Provider insertion.

A retained sample is an observed production window, not a pattern execution,
machine slot, or individual item. One window containing nine items remains one
sample. Nine separately completed windows remain nine samples. An order for 100
items does not necessarily provide 100 independent observations.

The provider can queue work inside a sequential furnace or feed a parallel
factory. Neither input capacity nor returned stack size proves the machine's
parallelism. No particular eight- or nine-slot capacity is assumed for an
Unobtainium furnace; that depends on the actual mod, version, and configuration.

### Requirements

- **N1 — Meaning:** Each item sample displays `1 item / M ticks`, where M is
  observed elapsed ticks divided by observed completed items. Label this as
  effective throughput, including observed queueing and return transport. It
  does not promise that one isolated item finishes in M ticks.
- **N2 — Precision:** Preserve fractional ticks. Display up to three decimal
  places, round half up, and omit trailing zeros. A positive value below 0.001
  tick displays `<0.001`, never zero. Ticks are server game ticks; seconds in
  explanatory text mean ticks divided by 20, not measured wall time.
- **N3 — Evidence:** Preserve raw amounts, durations, ordering, retention,
  filtering, and sample counts. Never duplicate a bulk observation or replace
  its stored amount with one. The existing live preview stays unretained and
  low confidence; applying normalization must not promote it to learned history.
- **N4 — Estimates:** Preserve throughput, TTC, confidence, and delay behavior
  for the same input history. Normalization makes the evidence comparable; it
  does not by itself correct an inaccurate TTC model.
- **N5 — Details:** Apply the same meaning to standard plan/status sample
  tooltips and client/server compact details. The compact average is the
  arithmetic mean of retained per-unit durations; latest is the latest retained
  per-unit duration. Label both per unit. The weighted production-rate field
  keeps its existing meaning and need not equal the inverse of this average.
- **N6 — Units:** Items use singular `item`. Existing fluid/chemical and mana
  histories use `1 mB` and `1 mana` respectively, after the existing amount
  normalization. Do not interpret one recipe execution as one output item;
  recipes may yield several items or several distinct outputs.
- **N7 — Compatibility:** Existing saved history renders normalized details
  without clearing or rewriting it. Support 1.20.1 Forge, 1.20.1 Fabric, 1.21.1
  NeoForge, and 26.1.2 NeoForge. English and Ukrainian explain the same meaning.
  Optional integrations retain their current target availability.
- **N8 — Missing evidence:** If a detail lacks valid positive amount/duration
  pairs, omit its normalized sample and average/latest details. Keep any valid
  aggregate rate and the existing missing-data behavior. Do not manufacture a
  latest per-unit time from an aggregate rate or a missing amount.

### Acceptance examples

Each row below starts with one retained observation, unless stated otherwise.
The sample count remains one regardless of output amount.

| ID | Raw observation | Required detail or invariant |
| --- | --- | --- |
| A1 | 1 item, 200 ticks | `1 item / 200 ticks` |
| A2 | 9 items, 90 ticks | `1 item / 10 ticks`; still low confidence |
| A3 | 8 items, 90 ticks | `1 item / 11.25 ticks`; no slot-count assumption |
| A4 | 10 items, 411 ticks | `1 item / 41.1 ticks` |
| A5 | 9 items, 1 tick | `1 item / 0.111 ticks`; no one-tick per-item floor |
| A6 | 10,000 items, 1 tick | `1 item / <0.001 ticks` |
| A7 | One recipe yields 4 items in 80 ticks | `1 item / 20 ticks`; one observation |
| A8 | Nine windows, each 1 item in 200 ticks | Nine observations of `1 item / 200 ticks` |
| A9 | 1,000 mB, 20 ticks; or 100 mana, 20 ticks | `1 mB / 0.02 ticks`; `1 mana / 0.2 ticks` |
| A10 | Windows (1 item, 100 ticks), (9 items, 90 ticks) | Average 55 ticks/item; latest 10 ticks/item; original weighted rate preserved |
| A11 | Reset, reload, partial output, concurrent CPUs, cancellation | Existing history/lifecycle behavior preserved; no synthetic samples |
| A12 | Empty, mismatched, zero, or negative detail pairs | No divide-by-zero, NaN, infinity, or invented per-unit history |

### Not included

This work does not change collection boundaries, add per-dispatch timers,
discover machine internals, infer slot counts, change AE2 dispatch, or install
machine-specific integrations. It does not redesign TTC, stall thresholds,
CPU-specific history, packets, persistence, settings, or sample confidence.
True machine service-time measurement would need separate requirements and
observable machine-start events; accepted AE2 outputs alone cannot establish it.
