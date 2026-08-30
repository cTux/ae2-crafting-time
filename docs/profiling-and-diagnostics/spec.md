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
- Reset, cancellation, load, and save boundaries leave no stale runtime state.

See [technical-design.md](technical-design.md) for ownership and code flow.
