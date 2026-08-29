# CPU-Bound Stats: Implementation Plan

Part of `cpu-bound-stats/`. Keep each step small and covered.

## 1. Extend the key

- Add `cpuId` to `ProfileKey` and preserve both existing constructors.
- Normalize null to empty and add `isCpuSpecific()`.
- Cover constructors, validation, equality, and CPU-specific state.

## 2. Derive durable CPU ids

- Add the smallest standard-AE2 adapter for the cluster anchor.
- Add version-checked optional AdvancedAE UUID accessors where supported.
- Return empty for null and unknown CPU types.
- Cover stable formatting and graceful fallback without reflection-heavy generic
  discovery.

## 3. Dual-record throughput

- In `ProfilerBridge.start` and `complete`, always record the network key.
- Also record the CPU key when `cpuId(scope)` is nonempty.
- Keep the same `scope` for pending cleanup and capacity state.
- Complete both keys before replacing SavedData, then save once if either call
  produced a sample.
- Cover separate CPU histories, concurrent network aggregation, completion,
  cancellation, reset, and unknown CPU fallback.

## 4. Save format version 2

- Write `cpuId` with every retained output entry.
- Read version 1 with empty `cpuId`; read version 2 with the stored id.
- Update every supported SavedData codec and its round-trip/migration tests.
- Keep the storage id exactly `ae2-crafting-time`.

## 5. Resolve the selected CPU

- Extend server-side `StatsRequestContext` for `CraftConfirmMenu.selectedCpu`.
- Add a shared pure resolver that prefers reliable CPU stats, then the network
  bucket, and returns `ResolvedStats(stats, cpuSpecific)`.
- Use it in plan snapshots and Ctrl-click show/reset handling.
- Keep Automatic network-only.
- Cover reliable, unreliable, absent, Automatic, and unsupported-CPU paths.

## 6. Update the packet and client cache

- Carry `cpuSpecific` with each resolved `StatsEntry`.
- Update the shared codec and all loader wrappers together.
- Bump the Forge channel protocol version.
- Extend packet round-trip, malformed-size, and boundary tests.
- Invalidate visible plan requests after the CPU selector cycles.

## 7. Render the marker

- Reuse one `TtcText` path for the `*` suffix and legend.
- Apply it to rows, sorting inputs, Total TTC, and details from the same resolved
  cache entry.
- Add matching English and Ukrainian translations.
- Keep the 1.20.1/1.21.1 and 26.1.2 screen implementations aligned.

## Verify

Before the commit, inspect the complete diff and run `git diff --check`; do not
run repository tests locally. Let the post-commit hook create the PR and let the
required GitHub workflow run tests and JaCoCo.

After the PR exists, exercise two differently sized CPUs in the supported client
paths:

- Automatic uses network TTC and shows `*`.
- A selected CPU uses its own reliable history without `*`.
- A selected CPU with too little history falls back and shows `*`.
- Switching CPUs refreshes the visible rows.
- Restart preserves both network and CPU histories.
- Moving or rebuilding a standard CPU starts a new CPU history and safely falls
  back to the network rate.

The implementation is complete only when all four target rows pass required CI
and packet/save compatibility boundaries have direct coverage.
