# No Space Status Technical Design

## Evidence and ownership

AE2's `CraftingCpuLogic` sets `cantStoreItems` only when there is no active job,
the CPU tries to insert its remaining inventory into network storage, and some
inventory remains. `CraftingCPUScreen` already receives that synchronized flag
and appends AE2's red `Can't store items` title warning.

The client therefore has exact state and does not need profiling, polling, a
new request field, or persistence. The status means only that the ME network
rejected some CPU-held item. It cannot identify which storage provider or rule
caused the rejection.

## Row decision

Extend the covered pure-Java crafting-row resolver used by the status table.
Choose `NO_SPACE` only when all of these are true:

```text
menuCantStoreItems
storedAmount > 0
activeAmount == 0
pendingAmount == 0
```

`NO_SPACE` is the only eligible state for a stored-only row. Active and pending
rows continue through the blocker, waiting, delayed, TTC, and collecting-data
order unchanged.

## Client integration

In `CraftingStatusTableRendererMixin`, read the current
`CraftingCPUScreen` menu's public `isCantStoreItems()` value when building the
row description and tooltip. Reuse the current screen directly; do not add a
global cache or mirror AE2's flag.

Add a small `TtcText` method for the visible line and tooltip helpers. Reuse the
existing compact dark badge and warning-red style. Keep AE2's title untouched.
The tooltip adds the exact explanation first and the practical suggestion
second.

Add matching English and Ukrainian keys:

```text
NO SPACE / Немає місця
The ME network can't accept this item.
Free space in storage cells or add more storage.
```

Use natural Ukrainian equivalents for both tooltip sentences and preserve the
same meaning.

## Unchanged boundaries

- No `CraftProfiler`, `ProfilerBridge`, request handler, packet, cache, protocol,
  or saved-data changes.
- No optional integration changes.
- No attempt to enumerate or mutate network storage.
- TTC sorting remains irrelevant because stored-only rows have no TTC.

## Failure handling

- If the current screen or menu is unavailable, resolve `menuCantStoreItems` as
  false.
- If AE2 clears the flag, the next render removes the status.
- The status never appears from stored amount alone.
