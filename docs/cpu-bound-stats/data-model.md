# CPU-Bound Stats: Data Model And Migration

Part of `cpu-bound-stats/`. See `index.md` for the decisions.

## Profile key

Extend the existing key without breaking output-only callers:

```java
public record ProfileKey(String networkId, String cpuId, String outputId) {
    public ProfileKey(String networkId, String outputId) {
        this(networkId, "", outputId);
    }

    public ProfileKey(String outputId) {
        this("", "", outputId);
    }

    public ProfileKey {
        networkId = networkId == null ? "" : networkId;
        cpuId = cpuId == null ? "" : cpuId;
        if (outputId == null || outputId.isBlank()) {
            throw new IllegalArgumentException("outputId must not be blank");
        }
    }

    public boolean isCpuSpecific() {
        return !cpuId.isEmpty();
    }
}
```

The empty CPU id remains the network bucket. Do not use co-processor count, CPU
name, or collection order as an id.

## Saved samples

`PersistedOutputSamples` already contains a `ProfileKey`, so only the NBT codec
changes.

```text
version: 2
outputs: [
  {
    networkId: "minecraft:overworld|10,64,10"
    cpuId: "ae2:minecraft:overworld:12,65,8"
    key: "minecraft:iron_ingot"
    unit: "item"
    samples: [ { amount: 10, durationTicks: 180 } ]
  }
]
```

Migration rules:

- Keep decoding version 1.
- Missing `cpuId` becomes `""`, preserving old network-wide samples.
- Write version 2 after the in-memory snapshot changes.
- Do not persist pending work, stalls, or accuracy as part of this feature.
- Update and test every supported SavedData codec.

## Lookup result

The server needs to tell the UI whether it used the selected CPU or the fallback:

```java
public record ResolvedStats(ProfileStats stats, boolean cpuSpecific) {}
```

Resolution is small:

1. If a CPU is selected and has `reliableEstimate()`, use it and set
   `cpuSpecific = true`.
2. Otherwise use the network key and set `cpuSpecific = false`.
3. If neither key has stats, omit the entry as today.

Use the same decision for TTC rows, sorting, totals, and Ctrl-click details. Do
not add a second sample-count rule beside `reliableEstimate()`.

## Snapshot packet

Keep client cache keys output-only. Add a `cpuSpecific` boolean to each returned
entry so the UI can render the fallback marker. The server still sends only the
requested outputs and their bounded retained sample data.

This is a coordinated wire-layout change:

- Update the shared codec and all four loader packet wrappers together.
- Bump the Forge `SimpleChannel` protocol version, which is the only explicit
  channel-version field in the current loader adapters.
- Add round-trip and malformed/bounded packet coverage.
- Do not add a leading `cpuAware` flag and call it backward compatible. An old
  reader cannot discover a new leading field without already knowing the new
  layout.

Clients and servers must use the same mod protocol for this release.

## Reset behavior

Ctrl-Alt-click clears the retained samples that produced the visible TTC: the CPU
bucket for a CPU-specific entry, or the network bucket for Automatic and fallback
entries. It must not silently erase both buckets or every CPU's history.
