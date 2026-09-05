# CPU-Bound Stats: Estimation UI

Part of `cpu-bound-stats/`. See `index.md`, `data-model.md`, and
`collection.md`.

## Automatic selection

When the selector says Automatic, keep using the network-wide rate. Append `*`
to known TTC values and show the short legend `* depends on CPU`.

Do not predict AE2's future CPU choice on the client. At submission time AE2 can
consider whether a CPU is active, busy, large enough, allowed for automatic
selection, preferred for the action source, and how co-processors and storage are
ordered. That state can change after the plan is drawn.

There is no per-CPU breakdown in this version. It would require a much larger
snapshot and would still present unavailable or soon-busy CPUs as choices.

## Explicit selection

When the player selects a named CPU:

- Request the visible rows again.
- Use reliable stats from that CPU where available.
- Fall back row by row to the network rate.
- Show no `*` on a row backed by that CPU's own reliable history.
- Show `*` on a fallback row.
- Show `*` on the total `TTC` when any included row fell back.
- Omit unknown rows from the total exactly as today.

Example:

```text
Crafting CPU: Alpha
TTC: ~000:02:15*
* depends on CPU (some rows use network history)
```

Sorting uses the same resolved row data as rendering. Ctrl-click details use the
same resolved scope, so the detailed sample list agrees with the visible TTC.
Ctrl-Alt-click clears only that scope's retained samples.

## Marker rules

`*` is display metadata, not part of the duration:

- `cpuSpecific = true`: no marker.
- `cpuSpecific = false`: append `*`.
- Always explain the first visible marker with `* depends on CPU`.

Add one reusable `TtcText` suffix/legend path and matching English and Ukrainian
translations. Do not create separate formatting helpers for rows and totals.

## What stays unchanged

- Plan-row TTC uses `craftAmount`.
- Unknown or nonpositive rows have no estimate.
- Plan total adds known row estimates and does not invent values for unknown rows.
- Running-job total uses the selected CPU's remaining dependency critical path.
- Accuracy remains network-scoped and diagnostic-only.
- Stall thresholds and throughput math do not change.
