package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Server-side only. Click-scoped locate records plus the per-output fallback
 * (owner, positions, display name) that survives a world reload. Live
 * dispatch data always wins; the persisted copy only fills gaps after a
 * reload.
 */
public final class ProviderLocateRecords {
    public record LocateRecord(UUID id, UUID owner, String dimensionId, List<BlockPos> positions,
            String outputName, long createdTick) {
    }

    public record ProviderStartInfo(UUID owner, List<BlockPos> positions, String outputName) {
        public ProviderStartInfo {
            positions = positions == null ? List.of() : List.copyOf(positions);
        }
    }

    public record StoredStart(ProfileKey key, UUID owner, List<BlockPos> positions, String outputName) {
    }

    private static final int MAX_RECORDS = 256;
    private static final int MAX_STARTS = 256;
    private static final LinkedHashMap<UUID, LocateRecord> RECORDS = new LinkedHashMap<>();
    private static final LinkedHashMap<ProfileKey, ProviderStartInfo> STARTS = new LinkedHashMap<>();

    public static synchronized LocateRecord create(UUID owner, String dimensionId, List<BlockPos> positions,
            String outputName, long tick) {
        var record = new LocateRecord(UUID.randomUUID(), owner, dimensionId, positions == null ? List.of()
                : List.copyOf(positions), outputName, tick);
        RECORDS.put(record.id(), record);
        evictEldest(RECORDS, MAX_RECORDS);
        return record;
    }

    public static synchronized Optional<LocateRecord> ownedBy(UUID owner, UUID id) {
        if (owner == null || id == null) {
            return Optional.empty();
        }
        var record = RECORDS.get(id);
        return record != null && record.owner().equals(owner) ? Optional.of(record) : Optional.empty();
    }

    /**
     * Records who started an output and where its providers were last seen.
     * Empty positions or a missing owner never erase a previously stored
     * entry; only strictly newer information replaces it.
     */
    public static synchronized void noteStart(ProfileKey key, UUID owner, List<BlockPos> positions,
            String outputName) {
        if (key == null) {
            return;
        }
        var previous = STARTS.get(key);
        var mergedOwner = owner != null ? owner : previous == null ? null : previous.owner();
        List<net.minecraft.core.BlockPos> mergedPositions;
        if (positions != null && !positions.isEmpty()) {
            mergedPositions = List.copyOf(positions);
        } else if (previous == null) {
            mergedPositions = List.of();
        } else {
            mergedPositions = previous.positions();
        }
        var mergedName = outputName != null && !outputName.isBlank() ? outputName
                : previous == null ? key.outputId() : previous.outputName();
        if (mergedOwner == null && mergedPositions.isEmpty()) {
            return;
        }
        STARTS.put(key, new ProviderStartInfo(mergedOwner, mergedPositions, mergedName));
        evictEldest(STARTS, MAX_STARTS);
    }

    public static synchronized Optional<ProviderStartInfo> startFor(ProfileKey key) {
        return key == null ? Optional.empty() : Optional.ofNullable(STARTS.get(key));
    }

    /**
     * Overwrites one output's fallback with freshly resolved notify-time data,
     * even when the fresh positions are empty: an empty resolution means "no
     * locatable target right now", not "keep showing an old box".
     */
    public static synchronized void replaceStart(ProfileKey key, UUID owner, List<BlockPos> positions,
            String outputName) {
        if (key == null) {
            return;
        }
        STARTS.put(key, new ProviderStartInfo(owner,
                positions == null ? List.of() : List.copyOf(positions),
                outputName == null || outputName.isBlank() ? key.outputId() : outputName));
        evictEldest(STARTS, MAX_STARTS);
    }

    public static synchronized List<StoredStart> snapshotStarts() {
        var snapshot = new ArrayList<StoredStart>();
        for (var entry : STARTS.entrySet()) {
            var info = entry.getValue();
            if (info.owner() != null) {
                snapshot.add(new StoredStart(entry.getKey(), info.owner(), info.positions(), info.outputName()));
            }
        }
        return List.copyOf(snapshot);
    }

    public static synchronized void restoreStarts(List<StoredStart> stored) {
        STARTS.clear();
        if (stored == null) {
            return;
        }
        for (var entry : stored) {
            if (entry == null || entry.key() == null || entry.owner() == null) {
                continue;
            }
            noteStart(entry.key(), entry.owner(), entry.positions(), entry.outputName());
        }
    }

    public static synchronized void clearAll() {
        RECORDS.clear();
        STARTS.clear();
    }

    private static void evictEldest(LinkedHashMap<?, ?> map, int maximum) {
        while (map.size() > maximum) {
            var eldest = map.keySet().iterator();
            eldest.next();
            eldest.remove();
        }
    }

    private ProviderLocateRecords() {
    }
}
