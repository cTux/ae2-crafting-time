# CPU-Bound Craft Time Stats

Date: 2026-08-29

## Goal

Today the mod learns crafting throughput per `(networkId, outputId)` and uses that
blended rate for every Time To Craft estimate. This works, but it hides a real
source of variance: different crafting CPUs in the same network can run the same
output at different speeds because they have different co-processor counts and
parallel budgets.

This change binds collected samples to the CPU that actually ran them, so the
Crafting Plan window can show a per-CPU estimate when the player has picked (or
the game will auto-pick) a CPU. It keeps the existing network-blended estimate as
a fallback so nothing breaks for auto-select and for CPUs with little history.

## Why now

- The chosen CPU is already known at craft start. The mixins run on the specific
  `CraftingCPUCluster` / `AdvCraftingCPU` that the job was submitted to
  (`CraftingCpuLogicMixin.java:45,82`, `AdvancedCraftingCpuLogicMixin.java:47,76`),
  but the CPU is only used as an `IdentityHashMap` scope, never as part of the
  sample key (`CraftProfiler.java:15-19`).
- `CraftingCPUCluster.getName()` exists and returns the player-assigned CPU name
  (`CraftingCPUCluster.java:245`), so a human-readable id is available.
- The Crafting Plan window already estimates before a CPU is committed, so the
  design has to stay correct when no CPU is chosen yet
  (`CraftConfirmScreenMixin.java:99,108,203`).

## Scope

In scope:

- Add an optional `cpuId` to the sample key.
- Capture `cpuId` at craft start and at job accuracy start.
- Keep network-level stats as the fallback for lookups and for auto-select.
- Show per-CPU estimates in the Crafting Plan window when a CPU is chosen.
- Migrate existing world saves and in-flight packets without data loss.

Out of scope:

- Changing how throughput itself is measured (continuous production windows stay).
- Per-CPU capacity already tracked via `updateCapacity` is not folded into the
  rate math in this pass; it stays a diagnostic.
- Using accuracy or stall to feed back into throughput or TTC math. They remain
  diagnostic only, but the learned data is still persisted (see below).

In scope for persistence:

- Craft samples **and** accuracy samples are both saved per `(networkId, cpuId,
  outputId)` and reloaded on world load, so per-CPU throughput and per-CPU accuracy
  survive restarts. CPU identifiers travel inside `ProfileKey`, so no data is lost
  when the world reloads. Stall diagnostics stay runtime-only because they describe
  the currently delayed in-flight output, not a learned historical value.

## Key decisions

1. **Key shape:** `ProfileKey(networkId, cpuId, outputId)` where `cpuId` defaults
   to `""` meaning "network-level / any CPU". Old and new samples coexist.
2. **CPU identity:** `cpuId` is `"<x>,<y>,<z>#<index>"` — the CPU's world block
   coordinates plus its 0-based index in `grid.getCraftingService().getCpus()`.
   Coordinates give a stable, location-meaningful id; the index makes it unique and
   orderable. The player name is display-only. See `collection.md`.
3. **Lookup fallback:** `(networkId, cpuId, outputId)` falls through to
   `(networkId, "", outputId)` when the CPU has too few samples. This avoids
   blank estimates for rarely used CPUs.
4. **Chosen CPU pins everything:** when the player selects a CPU in the Crafting
   Plan window, every stat shown there — per-row TTC, Total TTC, accuracy, stall
   diagnostics, and the Ctrl-click detail/chat — is relative to that CPU
   (`estimation.md`, `collection.md`).
5. **`*` means "depends on CPU":** whenever a TTC is shown but the relevant CPU has
   no measured data of its own (so a network-level fallback was used, or no CPU is
   selected at all), the value is rendered as `TTC: ~1:23:34*` and the UI explains
   `*` as "depends on CPU" (`estimation.md`).
6. **Headline when unchosen is the minimum:** with no CPU selected, the Total TTC
   is the smallest TTC across the network's CPUs (fastest CPU that has data), with
   a per-CPU breakdown list. See `estimation.md`.
7. **Migration:** old saves deserialize with `cpuId = ""` and stay valid. Packets
   without `cpuId` are read as `""` through a version guard. See `data-model.md`.

## Accuracy impact

Binding to CPU improves TTC for heterogeneous networks when the CPU is known
before estimating. It does not help homogeneous networks and it fragments the
sample pool, so the fallback to network-level stats is what keeps small CPUs
honest. Full reasoning is in `estimation.md`.

## Documents in this set

- `data-model.md` — key shape, persistence NBT, packet codec, per-CPU summary,
  save migration.
- `collection.md` — how `cpuId` is derived and wired at craft start, and how the
  grid's CPU list reaches the client.
- `estimation.md` — Crafting Plan window behavior: pinned-CPU stats, `*` marker,
  min-across-CPUs headline, fallback chain, accuracy.
- `implementation-plan.md` — ordered tasks, file map, and tests.
