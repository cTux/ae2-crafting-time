# Finite Outlier Configuration and TTC

Issue: [#469](https://github.com/cTux/ae2-crafting-time/issues/469)

## Problem

Fabric accepts `outlierMultiplier=NaN`. Once an output has five retained samples,
the invalid multiplier excludes every sample and produces NaN throughput.
Converting that rate to an estimate can display `~0s?` for positive work.
This path was established from source; no Minecraft reproduction is claimed.

Restore the valid configuration and estimate boundaries described below. The
[profiling specification](../profiling-and-diagnostics/spec.md) still owns the
normal filtering and confidence rules. This fix does not implement the planned
[configuration screen](../configuration-screen/spec.md).

## Acceptance criteria

- **C1 — Config fallback:** Fabric treats nonfinite parsed doubles, including
  `NaN`, `Infinity`, `-Infinity`, and exponent overflow, like malformed numeric
  text: keep the current valid value. On the first load that value is `4.0`.
  A valid value followed by an invalid duplicate key keeps the valid value.
- **C2 — Finite bounds:** Keep finite values in `[1.0, 1000.0]` unchanged and
  clamp finite values below or above that range to its nearest bound. Preserve
  the existing default and malformed-text fallback.
- **C3 — Profiler invariant:** `CraftProfiler` rejects nonfinite multipliers
  and finite multipliers below `1.0` with `IllegalArgumentException`. Preserve
  its existing positive sample-limit check. The core constructor continues to
  accept finite multipliers above `1000.0`; that upper bound belongs to config.
- **C4 — Unknown estimates:** For positive work, nonfinite or nonpositive
  throughput produces an empty estimate from `TimeEstimate.seconds` and
  `TimeEstimate.format`, never a fabricated zero. Nonpositive work also remains
  unknown. Finite positive rates retain ceiling rounding and confidence text;
  positive subsecond work still displays one second.
- **C5 — Filtering boundary:** Using the effective fallback multiplier, both
  four- and five-sample histories produce finite positive throughput and valid
  TTC. Five-sample filtering still includes normal samples, excludes genuine
  outliers, and computes confidence from the existing rule.
- **C6 — Compatibility:** Apply shared validation to every release-matrix
  target and the parsing change only to Fabric 1.20.1. Preserve saved raw samples,
  packet layouts, profile identity, amount units, and existing unknown-display
  behavior. No history reset or format migration is needed.

## Scope and proof

Forge 1.20.1 and NeoForge 1.21.1/26.1.2 retain their loader config backends.
No UI layout, translation, dependency, config-screen, or runtime-reload changes
are included. The multiplier is still read when the profiler is constructed.

Require regression tests for the pure decisions, an actual Fabric file-load
boundary test, shared 100% line and branch coverage, and current-head CI tests
and builds. Minecraft smoke is not required for this validation-only change;
the [implementation plan](implementation-plan.md) defines the checks and limits.
