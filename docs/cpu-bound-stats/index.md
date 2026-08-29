# CPU-Bound Craft Time Stats

Date: 2026-08-30

## Goal

The mod currently learns one throughput rate per `(networkId, outputId)`. That
rate blends work from every crafting CPU on the network.

Keep that network rate, and also learn a rate for the CPU that ran each craft.
When you explicitly select a CPU in the Crafting Plan window, the plan can use
that CPU's history. When selection stays on Automatic, the existing network rate
remains the honest estimate because AE2 has not committed the job to a CPU yet.

## Scope

In scope:

- Add an optional CPU instance id to `ProfileKey`.
- Record every completed production window twice: once for its CPU and once for
  the whole network.
- Prefer reliable CPU-specific stats when a CPU is explicitly selected.
- Fall back to the network rate and mark the TTC as CPU-dependent.
- Migrate saved craft samples without losing the existing network history.

Out of scope:

- Predicting which CPU AE2 will choose for Automatic.
- Sending every CPU and every output rate to the client.
- Treating co-processor count or machine configuration as CPU identity.
- Persisting prediction accuracy. It is runtime-only today and stays that way.
- Changing throughput, accuracy, or stall formulas.

## Decisions

1. `ProfileKey` becomes `(networkId, cpuId, outputId)`. Empty `cpuId` means the
   network-wide bucket.
2. `cpuId` identifies one durable CPU instance. A standard AE2 CPU uses its
   persisted core block as an anchor. AdvancedAE uses its persisted CPU UUID.
   Unknown CPU types do not get a made-up id; they keep network-only stats.
3. CPU and network samples are recorded together. This keeps the fallback fresh
   on new worlds and preserves the current network-wide production-window math.
4. Only an explicitly selected CPU enables CPU-specific lookup. Automatic uses
   the network bucket and shows `* depends on CPU`.
5. A selected CPU falls back row by row until its own `ProfileStats` has a
   `reliableEstimate()`.
6. The server resolves the selected CPU. The client does not send or derive a
   trusted CPU id.
7. Save format version 2 adds `cpuId` to retained craft samples. Version 1 loads
   with empty `cpuId`.
8. The snapshot packet adds only the resolved-scope marker needed by the UI. It
   does not carry a per-CPU summary table.

## Why the earlier design changed

- Co-processor count describes CPU capacity, not CPU identity. Two CPUs can have
  the same count and different histories.
- Crafting providers belong to the network, not to one CPU. There is no stable
  "attached machines" list to hash.
- AE2 automatic selection also considers busy state, selection mode, preferred
  CPUs, storage, co-processors, and submission-time state. Reimplementing that on
  the client would still be a guess.
- Adding a boolean to the front of an old packet does not make the layouts
  compatible. Old readers would interpret the new bytes using the old shape.

## Documents

- `data-model.md` — keys, save migration, lookup result, and packet boundary.
- `collection.md` — CPU identity and dual recording.
- `estimation.md` — selected and Automatic UI behavior.
- `implementation-plan.md` — ordered implementation and verification work.
