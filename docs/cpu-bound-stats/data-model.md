# CPU-Bound Stats: Data Model And Migration

Part of `cpu-bound-stats/`. See `index.md` for the goal and decisions.

## Sample key

Today the key is `ProfileKey(networkId, outputId)`:

```java
// shared/src/main/java/com/ctux/ae2craftingtime/core/ProfileKey.java
public record ProfileKey(String networkId, String outputId) { ... }
```

Add an optional `cpuId` that is `""` for network-level samples. Its shape is
`"<x>,<y>,<z>#<index>"` — the CPU's world block coordinates plus its 0-based index
in `grid.getCraftingService().getCpus()` (see `collection.md`):

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

Everything that builds a key today passes `(networkId, outputId)`. New CPU-aware
calls pass `(networkId, cpuId, outputId)`. No existing caller breaks because the
two-arg constructor is preserved.

## Where keys are built

`ProfilerBridge` centralizes key construction:

```java
// shared/src/.../mc1201/ProfilerBridge.java:147-153
public static ProfileKey key(AEKey key) {
    return new ProfileKey(key.getId().toString());
}
public static ProfileKey key(String networkId, AEKey key) {
    return new ProfileKey(networkId, key.getId().toString());
}
```

Add:

```java
public static ProfileKey key(String networkId, String cpuId, AEKey key) {
    return new ProfileKey(networkId, cpuId, key.getId().toString());
}
```

The CPU-aware `start` / `complete` / `startJob` paths in `ProfilerBridge` and the
`mixin` callers then pass the derived `cpuId`. See `collection.md`.

## Persisted shape

`PersistedOutputSamples` already wraps `ProfileKey`:

```java
// shared/src/main/java/com/ctux/ae2craftingtime/core/PersistedOutputSamples.java
public record PersistedOutputSamples(ProfileKey key, ProfileUnit unit,
        List<PersistedCraftSample> samples) { ... }
```

No change to that record is required; the new `cpuId` travels inside `key`.

World-save NBT today (from `world-save-persistence.md`):

```text
version: 1
outputs: [
  {
    networkId: "minecraft:overworld|10,64,10"
    key: "minecraft:iron_ingot"
    unit: "item"
    samples: [ { amount: 10, durationTicks: 220 } ]
  }
]
```

New shape adds `cpuId` per output entry. Absent / empty `cpuId` means
network-level (old save):

```text
version: 2
outputs: [
  {
    networkId: "minecraft:overworld|10,64,10"
    cpuId: ""                      # network-level, old save
    key: "minecraft:iron_ingot"
    unit: "item"
    samples: [ { amount: 10, durationTicks: 220 } ]
  },
  {
    networkId: "minecraft:overworld|10,64,10"
    cpuId: "12,64,10#2"           # specific CPU: block coords + getCpus() index
    key: "minecraft:iron_ingot"
    unit: "item"
    samples: [ { amount: 10, durationTicks: 180 } ]
  }
]
```

Bump the top-level `version` to `2`. The `Ae2CraftingTimeSavedData` encoder/decoder
in each version module reads `cpuId` when present and defaults to `""` otherwise.

## Packet codec

`StatsPacketCodec` writes `entry.key().outputId()` today
(`StatsPacketCodec.java:45`) and reads `new ProfileKey(readUtf(...))`
(`StatsPacketCodec.java:75`). Both spots must carry `cpuId`.

Server write (`writeSnapshot`):

```java
buffer.writeUtf(entry.key().networkId(), MAX_OUTPUT_ID_LENGTH);
buffer.writeUtf(entry.key().cpuId(), MAX_OUTPUT_ID_LENGTH);
buffer.writeUtf(entry.key().outputId(), MAX_OUTPUT_ID_LENGTH);
```

Client read (`readSnapshot`):

```java
var networkId = buffer.readUtf(MAX_OUTPUT_ID_LENGTH);
var cpuId = buffer.readUtf(MAX_OUTPUT_ID_LENGTH);
var outputId = buffer.readUtf(MAX_OUTPUT_ID_LENGTH);
var key = new ProfileKey(networkId, cpuId, outputId);
```

Packet compatibility: the mod is not a public API and all clients and servers are
the same version in a play session, so a plain field add is acceptable. Still gate
it behind a small protocol marker so a mismatched client does not silently
misread. Options:

- Add one `boolean cpuAware` flag at the front of the snapshot, written by the
  server and read by the client; when `false`, client code reads the old
  three-field key shape. This keeps the door open for older client builds during
  testing.

Keep the flag simple: write `true` always from the new server, and have the new
client read `cpuId` only when the flag is `true`.

## Per-CPU summary section

To let the client compute the min-across-CPUs headline and the per-CPU breakdown
without fetching every CPU's raw samples, the server appends a compact
`cpuSummaries` list to the snapshot (gated by the same `cpuAware` flag). For the
craft-plan request the server already has the grid, so it enumerates
`grid.getCraftingService().getCpus()` and, for each CPU and each requested output,
includes the CPU-specific `ProfileStats` **aggregate** (omit the raw
`sampleDurationTicks` / `sampleAmounts` lists to bound size):

```text
cpuAware: true
cpuSummaries: [
  { cpuId: "12,64,10#2", name: "Alpha", coProcessors: 4,
    outputs: { "minecraft:iron_ingot": <aggregate ProfileStats> } },
  { cpuId: "20,64,10#0", name: "Beta",  coProcessors: 1,
    outputs: { "minecraft:iron_ingot": <aggregate ProfileStats> } }
]
entries: [ ... full samples for the displayed CPU (network or pinned) ... ]
```

The client owns the plan amounts, so it multiplies each CPU's per-output
`amountPerSecond` by the row amount to build that CPU's Total TTC, then takes the
minimum. Raw samples are sent only for the *displayed* CPU (the existing `entries`
section) so the detail/chat view stays fully detailed. CPU counts are small, and
dropping raw samples from the summary keeps the packet within `PacketLimits`.

## Migration of old saves

No data loss, by construction:

1. Old `version: 1` files load with `cpuId = ""` because the decoder treats a
   missing `cpuId` as empty.
2. `ProfilerBridge.load(...)` already re-snapshots and writes back migrated data
   (`ProfilerBridge.java:167-175`). After the first save, the file becomes
   `version: 2` with `cpuId: ""` entries.
3. New craft completions write CPU-specific entries alongside the old
   network-level ones. The lookup fallback (next section) makes sure players keep
   seeing estimates immediately.

`version: 1` decode must remain supported so a downgrade or partial save does not
throw. Keep the `version == 1` branch reading the old shape.

## Lookup fallback

Add a helper that resolves stats with CPU preference but falls back to
network-level, and **reports whether it fell back** so the UI can render the `*`
"depends on CPU" marker (`estimation.md`):

```java
public record CpuStatsResult(ProfileStats stats, boolean cpuSpecific) { }

public static CpuStatsResult stats(String networkId, String cpuId, AEKey what) {
    if (cpuId != null && !cpuId.isEmpty()) {
        var specific = stats(new ProfileKey(networkId, cpuId, what.getId().toString()));
        if (specific.isPresent() && specific.get().sampleCount() >= MIN_CPU_SAMPLES) {
            return new CpuStatsResult(specific.get(), true);
        }
    }
    // fallback to network-level (cpuId = "")
    return new CpuStatsResult(stats(new ProfileKey(networkId, what.getId().toString())), false);
}
```

`MIN_CPU_SAMPLES` is the same `3` threshold already used for `reliableEstimate`
(`CraftProfiler.java:192`). Below that, the CPU-specific sample is too thin and we
use the network blend, which is exactly when the `*` marker belongs. The accuracy
tracker uses the same resolution rule. When `cpuId` is empty (no CPU selected), the
result is always `cpuSpecific = false`, so the headline shows `*`.

## Accuracy and stall are CPU-scoped too

When a CPU is known, the accuracy key and stall diagnostic are built from the same
`(networkId, cpuId, outputId)` key, not just `(networkId, outputId)`. This keeps
the "all stats relative to the chosen CPU" rule consistent for the detail/chat
views. The network-level keys remain the fallback through the same helper.

## Risks

- A moved CPU changes its block anchor, so its old samples become unreachable by
  `cpuId`. The network-level fallback still serves estimates, so this is a quiet
  degradation, not a break. Renaming a CPU is safe because the name is display-only.
- Two CPUs at the same block position is impossible in one network, so the anchor
  is unique within `networkId`.
- The save file grows per (network, cpu, output) tuple instead of (network,
  output). Bounded by distinct CPUs times distinct outputs, still small.
- Keep `version: 1` decoding around; removing it would corrupt old worlds on
  first load.

See `implementation-plan.md` for the task order and tests.
