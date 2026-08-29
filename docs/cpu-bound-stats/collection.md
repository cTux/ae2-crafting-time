# CPU-Bound Stats: Collection

Part of `cpu-bound-stats/`. See `index.md` and `data-model.md`.

## CPU identity

The craft hooks already receive the CPU object as `scope`. Derive a durable id
from the concrete CPU type:

- Standard `CraftingCPUCluster`: use its dimension and stable cluster anchor,
  such as `getBoundsMin()`, formatted as
  `ae2:<dimension>:<x>,<y>,<z>`.
- AdvancedAE `AdvCraftingCPU`: expose its persisted `uniqueId` through a
  version-checked optional accessor and format it as `advancedae:<uuid>`.
- Unknown or unsupported CPU type: return empty and record network-only data.

Moving or rebuilding a standard CPU gives it a new identity. That is correct: it
is a new physical CPU, and the network fallback covers it while it learns.
Renaming a CPU does not change its identity.

Do not use:

- `ICraftingCPU.getName()`, because names are optional and mutable.
- `getCpus()` order, because collection order is not persistent.
- co-processor count or storage, because distinct CPUs can share them.
- a hash of "attached machines", because crafting providers are network-wide and
  selected per pattern; they are not attached to one CPU.

## Record both scopes

The current profiler builds one network-wide busy window per output while pending
batches from any CPU remain. Keep that behavior.

For a CPU with a durable id, each expected-output and accepted-output event goes
through two keys with the same CPU object as the pending scope:

```text
(networkId, "", outputId)       -> existing network production window
(networkId, cpuId, outputId)    -> this CPU's production window
```

The network call keeps aggregating concurrent CPUs. The CPU call has a separate
busy window because `cpuId` is part of the key. If no durable id exists, make only
the network call.

Run both completion calls before replacing the SavedData snapshot. Save once when
either call closes a production window, so the CPU sample cannot miss the same
world-save update as its network sample.

This dual write is required. Replacing the network key with the CPU key would
freeze migrated fallback data and leave fresh worlds without a network fallback.

`finishJob(scope)` still clears unmatched pending entries for both keys. Reset
must clear only the requested key and rebuild any affected busy window as today.

## Resolve the selected CPU on the server

`StatsRequestContext` already resolves the active grid and a CPU on the crafting
status screen. Extend it for server-side `CraftConfirmMenu`:

- Read its private `selectedCpu` through the smallest version-specific accessor.
- Keep grid discovery through the menu's actionable target.
- Return `null` for Automatic.

The server derives `cpuId` from that authoritative object and resolves each
requested output. The client never supplies a CPU id.

After the player cycles the CPU selector, invalidate the plan's client request
cache so the next render requests the same visible output ids again. The server
receives the selector action first and answers from its current selection.

## Accuracy and stalls

This feature changes throughput lookup only.

- Accuracy stays network-scoped and runtime-only for now.
- Stall detection already uses the concrete CPU object as `scope`. When a
  selected CPU-specific key exists, query the matching key; on network fallback,
  keep the existing network-key behavior.

Do not claim that every diagnostic is CPU-specific until its collection and
fallback behavior has its own covered change.
