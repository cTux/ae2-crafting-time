# CPU-Bound Stats: Collection

Part of `cpu-bound-stats/`. See `index.md` and `data-model.md`.

## CPU identity

The craft hooks already receive the CPU object as `scope`. Derive a durable id
from the concrete CPU type:

- Standard `CraftingCPUCluster`: use the block position owned by its core grid
  node, formatted as
  `ae2:<dimension>:<x>,<y>,<z>`.
- AdvancedAE `AdvCraftingCPU`: expose its persisted `uniqueId` through a
  version-checked optional accessor and format it as `advancedae:<uuid>`.
- Unknown or unsupported CPU type: return empty and record network-only data.

Moving the CPU or removing its persisted core block gives it a new identity. That
is correct: it is a new physical anchor, and the network fallback covers it while
it learns. Renaming or expanding the CPU keeps its identity while AE2 preserves
the core block.

Do not use:

- `ICraftingCPU.getName()`, because names are optional and mutable.
- `getCpus()` order, because collection order is not persistent.
- co-processor count or storage, because distinct CPUs can share them.
- a hash of "attached machines", because crafting providers are network-wide and
  selected per pattern; they are not attached to one CPU.

## Record both scopes

The current profiler builds one network-wide completion interval per output and
batches same-tick returns across CPUs. Keep that behavior.

For a CPU with a durable id, each expected-output and accepted-output event goes
through two keys with the same CPU object as the pending scope:

```text
(networkId, "", outputId)       -> existing network completion interval
(networkId, cpuId, outputId)    -> this CPU's completion interval
```

The network call keeps aggregating concurrent CPUs. The CPU call has a separate
completion interval because `cpuId` is part of the key. If no durable id exists,
make only the network call.

Run both completion calls before the shared end-server-tick flush. Snapshot once
after both keys finalize, so the CPU sample cannot miss the same world-save
update as its network sample.

This dual write is required. Replacing the network key with the CPU key would
freeze migrated fallback data and leave fresh worlds without a network fallback.

`finishJob(scope)` still clears unmatched pending entries for both keys. Reset
must clear only the requested key and its unfinalized interval state.

## Resolve the selected CPU on the server

`StatsRequestContext` resolves a CPU today only on the single-CPU crafting status
screen. Extend it for server-side `CraftConfirmMenu`:

- Read its private `selectedCpu` through the smallest version-specific accessor.
  This field exists in each currently supported AE2 runtime artifact; recheck it
  when updating AE2.
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
