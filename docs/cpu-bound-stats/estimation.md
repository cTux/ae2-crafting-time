# CPU-Bound Stats: Estimation UI And Accuracy

Part of `cpu-bound-stats/`. See `index.md`, `data-model.md`, `collection.md`.

This document defines the visible behavior in the Crafting Plan window (and the
detail/chat views) once CPU-bound stats exist.

## Three rules that drive the UI

1. **Pinned CPU = all stats relative to it.** When the player selects a CPU in the
   Crafting Plan window, every number shown there — per-row TTC, Total TTC,
   accuracy, stall diagnostics, and the Ctrl-click detail/chat — uses that CPU's
   stats. No number is blended with other CPUs.
2. **`*` means "depends on CPU".** A TTC is rendered as `TTC: ~1:23:34*` whenever
   the relevant CPU has no measured data of its own and a network-level fallback was
   used, or when no CPU is selected at all. The UI must explain `*` as
   "depends on CPU".
3. **Unchosen headline targets the auto-selected CPU.** With no CPU selected, the
   Total TTC is the TTC of the CPU AE2 would auto-pick (smallest whose
   `getAvailableStorage()` fits the plan), shown with a per-CPU breakdown; the
   fastest-CPU TTC is shown as a note.

## No CPU selected (automatic)

The plan is about to be auto-submitted, and AE2 will pick a fitting CPU itself.
Behavior:

- The server sends `cpuSummaries` for every CPU on the grid (`data-model.md`).
- The client computes each CPU's Total TTC from its per-output `amountPerSecond`
  and the plan row amounts, using only that CPU's own stats (`resolveStats`). A row
  with no CPU-specific data for that CPU falls back to the network rate and marks
  that CPU's total as a fallback.
- **Headline Total TTC = the TTC of the CPU AE2 would auto-select**, i.e. the
  smallest CPU whose `getAvailableStorage()` fits the plan. Show it with `*` and the
  legend "depends on CPU", because no specific CPU is locked in by the player.
  *Implementation note:* mirror AE2's exact auto-select tie-break (co-processor vs
  storage order) rather than a literal "smallest storage"; the client computes the
  plan's total bytes via AE2's stack-byte helper to compare against
  `availableStorage`.
- Show the **fastest-CPU TTC as a note** (the minimum across CPUs), so the player
  sees the best case too:

  ```text
  Total TTC: ~000:03:10*   (depends on CPU)
  Fastest CPU: ~000:02:15
  ```

- Show the per-CPU breakdown as **multi-line text** (not one crowded line) so it
  never overflows the screen width:

  ```text
  Per CPU:
    Alpha (4 co-proc) ~2:15*
    Beta (1 co-proc) ~3:10*
    Gamma (4 co-proc) ~4:40*
  ```

  List each CPU **by name** (so `Alpha` and `Gamma` stay on separate lines even
  when both are `cpuId: "4"`). Multiple named CPUs sharing a `cpuId` are exactly the
  over-merge case: mark them with the *"your setup has differently performant
  machines"* tooltip rather than collapsing them. In unchosen mode every entry
  carries `*` because no CPU is pinned (rule 2); the network-fallback nuance only
  toggles `*` in **pinned** mode, when a selected CPU lacks data for a row.
- If a CPU has zero usable data, it is omitted from the breakdown but still
  contributes its network-fallback total to the auto-select/min computation.
- **Over-merge signal:** if a `cpuId` aggregates CPUs that actually perform
  differently (same co-processor count, different attached machines, and the
  machine hash was omitted), its blended accuracy drops. When an id's accuracy is
  low, append a tooltip line: *"your setup has differently performant machines"*.
  This tells the player why the estimate is uncertain without blaming a specific
  CPU.

## Player selects a CPU

When a CPU is selected (the CraftConfirmScreen CPU selector), the UI switches to
**fully pinned** mode:

- All stats come from that CPU's key `(networkId, cpuId, outputId)` with the
  network-level fallback only when that CPU lacks data (`data-model.md` lookup
  helper).
- Per-row TTC, Total TTC, accuracy, and stall all reflect that CPU.
- If the chosen CPU has enough own samples for every row, no `*` is shown — the
  number is that CPU's measured time.
- If the chosen CPU lacks data for some rows, those rows (and the Total, if any row
  fell back) show `*` with the same "depends on CPU" legend. Example:

  ```text
  Crafting CPU: Alpha
  Total TTC: ~000:02:15*
  * depends on CPU (no data for some rows)
  ```

  The breakdown list from the unchosen state is hidden while pinned, because the
  focus is the selected CPU.

## The `*` marker mechanics

- `*` is a display suffix on the formatted ETA, never part of the time math.
- Derive it from the lookup result's `cpuSpecific` flag (`data-model.md`):
  `cpuSpecific == false` → append `*`.
- When no CPU is selected, treat the whole view as `cpuSpecific == false`, so the
  headline and every CPU entry carry `*`.
- Always render the legend text near the first `*`, e.g. a tooltip line or a
  sub-line: `* depends on CPU`. Keep the wording plain.
- Add `TtcText` helpers: `totalTtcCpuDependent(eta)` and a `cpuDependentLegend()`
  component, plus the matching translation keys.

## What stays unchanged

- Per-row estimate still shows nothing when `amountPerSecond == 0` or
  `craftAmount <= 0`, and requests stats as today
  (`CraftConfirmScreenMixin.java:203-216`).
- The running Crafting Status screen (per-active-CPU) is unchanged; it already
  scopes to the selected CPU.
- Accuracy and stall stay **diagnostic only** — they never feed back into throughput
  or TTC math (`time-to-craft-plan.md`, Prediction Accuracy). Diagnostic only does
  not mean runtime-only: per-CPU accuracy is persisted and reloads after a restart
  (`data-model.md`, Persisting accuracy).

## Accuracy tracking

Keep the existing network-level accuracy as the default key, but record `cpuId`
into the accuracy key from `startJob` (`collection.md`) so per-CPU accuracy exists.
When a CPU is pinned, the detail/chat shows that CPU's accuracy; otherwise it shows
network-level accuracy with a `*` if the pinned CPU had none. The MAPE / ratio
metrics are unchanged. Per-CPU accuracy is persisted (`data-model.md`) and survives
restarts, so the detail/chat reflects historical accuracy for that exact CPU.

A low per-CPU accuracy is also the over-merge signal: when the same `cpuId`
blends CPUs with different real throughput, show the *"your setup has differently
performant machines"* tooltip (see the unchosen section). Resolve stats through
`resolveStats` (`data-model.md`) so the `*` marker and the fallback use one rule.

## Files touched

- `shared/src/mc1201/.../mixin/CraftConfirmScreenMixin.java` — per-CPU min + breakdown, `*` suffix, legend, pinned mode.
- `shared/src/.../mc1201/ProfilerBridge.java` — `CpuStatsResult` lookup, cpu-scoped accuracy/stall.
- `shared/src/mcCommon/.../StatsRequestHandler.java` — enumerate grid CPUs into `cpuSummaries`.
- `shared/src/mcCommon/.../StatsRequestContext.java` — read selected CPU from `CraftConfirmMenu`.
- `shared/src/mcCommon/.../net/StatsPacketCodec.java` — `cpuSummaries` section + `cpuAware` flag.
- `shared/src/mcCommon/.../TtcText.java` — `*` helpers + legend + translation keys.
- `CraftingCPUMenuAccessor`-style accessor for the CraftConfirmMenu selected CPU.
