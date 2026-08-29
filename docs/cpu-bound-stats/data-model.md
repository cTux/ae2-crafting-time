# CPU-Bound Stats: Data Model And Migration

Part of `cpu-bound-stats/`. See `index.md` for the goal and decisions.

## Sample key

Today the key is `ProfileKey(networkId, outputId)`:

```java
// shared/src/main/java/com/ctux/ae2craftingtime/core/ProfileKey.java
public record ProfileKey(String networkId, String outputId) { ... }
```

Add an optional `cpuId` that is `""` for network-level samples. Its shape is a
config-derived id, starting with the CPU's co-processor count and optionally a hash
of its attached machines, e.g. `"4"` or `"4-a1b2"` (see `collection.md`):

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
    cpuId: "4"                     # specific CPU: 4 co-processors
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

To let the client compute the auto-select headline and the per-CPU breakdown
without fetching every CPU's raw samples, the server appends a compact
`cpuSummaries` list to the snapshot (gated by the same `cpuAware` flag). For the
craft-plan request the server already has the grid, so it enumerates
`grid.getCraftingService().getCpus()` and, for each CPU and each requested output,
includes the CPU-specific `ProfileStats` **aggregate** (omit the raw
`sampleDurationTicks` / `sampleAmounts` lists to bound size):

```text
cpuAware: true
cpuSummaries: [
  { cpuId: "4", name: "Alpha", coProcessors: 4, availableStorage: 32768,
    outputs: { "minecraft:iron_ingot": <aggregate ProfileStats> } },
  { cpuId: "1", name: "Beta",  coProcessors: 1, availableStorage: 8192,
    outputs: { "minecraft:iron_ingot": <aggregate ProfileStats> } }
]
entries: [ ... full samples for the displayed CPU (network or pinned) ... ]
```

The client owns the plan amounts, so it multiplies each CPU's per-output
`amountPerSecond` by the row amount to build that CPU's Total TTC. `availableStorage`
(bytes, from `ICraftingCPU.getAvailableStorage()`) lets the client reproduce AE2's
auto-select: the headline is the smallest CPU whose `availableStorage` fits the
plan's byte total, and the fastest-CPU TTC is shown as a note (`estimation.md`).
Raw samples are sent only for the *displayed* CPU (the existing `entries`
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

Add a helper, named `resolveStats`, that resolves stats with CPU preference but
falls back to network-level, and **reports whether it fell back** so the UI can
render the `*` "depends on CPU" marker (`estimation.md`). Use `reliableEstimate()`
as the single fallback decision — do not introduce a separate sample-count
threshold, since `ProfileStats.reliableEstimate()` already encodes the same rule
(`CraftProfiler.java:192`):

```java
public record CpuStatsResult(ProfileStats stats, boolean cpuSpecific) { }

public static CpuStatsResult resolveStats(String networkId, String cpuId, AEKey what) {
    if (cpuId != null && !cpuId.isEmpty()) {
        var specific = stats(new ProfileKey(networkId, cpuId, what.getId().toString()));
        if (specific.isPresent() && specific.get().reliableEstimate()) {
            return new CpuStatsResult(specific.get(), true);
        }
    }
    // fallback to network-level (cpuId = "")
    return new CpuStatsResult(stats(new ProfileKey(networkId, what.getId().toString())), false);
}
```

When `cpuId` is empty (no CPU selected), the result is always
`cpuSpecific = false`, so the headline shows `*`. The accuracy tracker uses the same
`resolveStats` resolution rule.

## Accuracy and stall are CPU-scoped too

When a CPU is known, the accuracy key and stall diagnostic are built from the same
`(networkId, cpuId, outputId)` key, not just `(networkId, outputId)`. This keeps
the "all stats relative to the chosen CPU" rule consistent for the detail/chat
views. The network-level keys remain the fallback through the same helper.

Accuracy is **persisted** per CPU via the `accuracy` list above, so it survives
restarts. Stall diagnostics stay runtime-only: they describe the currently delayed
in-flight output on a selected CPU, which is not a learned historical value and has
no meaning after a restart.

## Persisting accuracy, not just samples

Craft samples are not the only learned data. `TtcAccuracyTracker` keeps a rolling
window of `AccuracySample` per `ProfileKey` (`TtcAccuracyTracker.java:12`). To honor
"per-CPU accuracy survives restarts", persist that window too, keyed by the same
cpu-aware `ProfileKey`.

Add a parallel persisted structure next to `PersistedOutputSamples`:

```java
public record PersistedAccuracySample(
        long predictedSeconds, double actualTickSeconds,
        double actualWallSeconds, int knownRows, int totalRows) { }

public record PersistedAccuracySamples(ProfileKey key, List<PersistedAccuracySample> samples) { }
```

NBT (same `version: 2` document, new top-level `accuracy` list; read optionally so
old `version: 2` saves without it still load):

```text
version: 2
outputs: [ ... existing samples with cpuId ... ]
accuracy: [
  {
    networkId: "minecraft:overworld|10,64,10"
    cpuId: "4"
    key: "minecraft:iron_ingot"
    samples: [
      { predictedSeconds: 135, actualTickSeconds: 138.0, actualWallSeconds: 142.5,
        knownRows: 4, totalRows: 4 }
    ]
  }
]
```

Wire it through `ProfilerBridge`:

- `TtcAccuracyTracker` gains `snapshotAccuracy()` and `loadAccuracy(List<PersistedAccuracySamples>)`,
  mirroring `snapshotSamples` / `loadSamples` in `CraftProfiler`.
- `ProfilerBridge.load(...)` calls both `PROFILER.loadSamples(...)` and
  `ACCURACY.loadAccuracy(...)`; the re-snapshot/migrate step already in
  `ProfilerBridge.load` (`ProfilerBridge.java:167`) covers accuracy too.
- On `ACCURACY.finish(...)` (a completed job), `ProfilerBridge` calls
  `savedData.replaceFrom(profiler.snapshotSamples(), accuracy.snapshotAccuracy())`
  and `savedData.setDirty()`, exactly like the existing sample-dirty path
  (`ProfilerBridge.java:51-54, 138-141`).
- CPU identifiers are already carried by `ProfileKey`, so accuracy and samples load
  back into the correct `(networkId, cpuId, outputId)` bucket with no extra mapping.

No old accuracy data exists (it was never persisted before), so there is nothing to
migrate; the `accuracy` list is additive under `version: 2`.

## Risks

- `cpuId` is config-derived (co-processor count, optional machine hash), so moving
  or renaming a CPU does **not** change its id and old samples stay reachable. Only
  a *rebuilt* CPU with a different co-processor count stops matching its prior
  samples; the network-level fallback still serves estimates, so this is a quiet
  degradation, not a break. The player name is display-only and irrelevant to the id.
- Two distinct CPUs can legitimately share a `cpuId` (same co-processor count,
  different attached machines, machine hash omitted). That is the over-merge case
  from `collection.md`/`estimation.md`: the blended accuracy drops and the UI
  surfaces the "differently performant machines" tooltip. Adding the machine hash
  separates them when it matters.
- The save file grows per (network, cpu, output) tuple instead of (network,
  output). Bounded by distinct CPUs times distinct outputs, still small.
- Keep `version: 1` decoding around; removing it would corrupt old worlds on
  first load.

See `implementation-plan.md` for the task order and tests.
