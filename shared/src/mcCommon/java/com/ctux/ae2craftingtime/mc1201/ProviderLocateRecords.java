package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Server-side only. Click-scoped locate records plus the per-output fallback
 * (job owner, network, dimension, output, provider positions, display name)
 * that survives a world reload. Live dispatch data always wins; the persisted
 * copy only fills gaps after a reload.
 */
public final class ProviderLocateRecords {
    public record LocateRecord(UUID id, UUID owner, String dimensionId, List<BlockPos> positions,
            String outputName, String outputId, long createdTick) {
    }

    public record ProviderStartInfo(UUID owner, String dimensionId, List<BlockPos> positions, String outputName) {
        public ProviderStartInfo {
            dimensionId = dimensionId == null ? "" : dimensionId;
            positions = positions == null ? List.of() : List.copyOf(positions);
        }

        public ProviderStartInfo(UUID owner, List<BlockPos> positions, String outputName) {
            this(owner, "", positions, outputName);
        }
    }

    public record StoredStart(ProfileKey key, UUID owner, String dimensionId, List<BlockPos> positions,
            String outputName) {
        public StoredStart {
            dimensionId = dimensionId == null ? "" : dimensionId;
            positions = positions == null ? List.of() : List.copyOf(positions);
        }

        public StoredStart(ProfileKey key, UUID owner, List<BlockPos> positions, String outputName) {
            this(key, owner, "", positions, outputName);
        }
    }

    private static final int MAX_RECORDS = 256;
    private static final int MAX_STARTS = 512;
    private static final LinkedHashMap<UUID, LocateRecord> RECORDS = new LinkedHashMap<>();
    private static final LinkedHashMap<ProfileKey, ProviderStartInfo> STARTS = new LinkedHashMap<>();

    public static synchronized LocateRecord create(UUID owner, String dimensionId, List<BlockPos> positions,
            String outputName, String outputId, long tick) {
        var record = new LocateRecord(UUID.randomUUID(), owner, dimensionId, positions == null ? List.of()
                : List.copyOf(positions), outputName, outputId == null ? "" : outputId, tick);
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
     * entry; only strictly newer information replaces it. Dimension is merged
     * the same way so identical outputs on different networks/dimensions stay
     * independent via their distinct profile keys while the stored dimension
     * survives for resync.
     */
    public static synchronized void noteStart(ProfileKey key, UUID owner, List<BlockPos> positions,
            String outputName) {
        noteStart(key, owner, "", positions, outputName);
    }

    public static synchronized void noteStart(ProfileKey key, UUID owner, String dimensionId,
            List<BlockPos> positions, String outputName) {
        if (key == null) {
            return;
        }
        var previous = STARTS.get(key);
        var mergedOwner = owner != null ? owner : previous == null ? null : previous.owner();
        var mergedDimension = dimensionId != null && !dimensionId.isBlank() ? dimensionId
                : previous == null ? "" : previous.dimensionId();
        List<BlockPos> mergedPositions;
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
        STARTS.put(key, new ProviderStartInfo(mergedOwner, mergedDimension, mergedPositions, mergedName));
        evictEldest(STARTS, MAX_STARTS);
    }

    public static synchronized Optional<ProviderStartInfo> startFor(ProfileKey key) {
        return key == null ? Optional.empty() : Optional.ofNullable(STARTS.get(key));
    }

    /**
     * Overwrites one output's fallback with freshly resolved notify-time data,
     * even when the fresh positions are empty: an empty resolution means "no
     * locatable target right now", not "keep showing an old box". The stored
     * dimension travels with the fallback so resync never has to re-derive it
     * from the network id alone.
     */
    public static synchronized void replaceStart(ProfileKey key, UUID owner, List<BlockPos> positions,
            String outputName) {
        if (key == null) {
            return;
        }
        var previous = STARTS.get(key);
        var keptDimension = previous == null ? "" : previous.dimensionId();
        STARTS.put(key, new ProviderStartInfo(owner,
                keptDimension,
                positions == null ? List.of() : List.copyOf(positions),
                outputName == null || outputName.isBlank() ? key.outputId() : outputName));
        evictEldest(STARTS, MAX_STARTS);
    }

    public static synchronized void replaceStart(ProfileKey key, UUID owner, String dimensionId,
            List<BlockPos> positions, String outputName) {
        if (key == null) {
            return;
        }
        STARTS.put(key, new ProviderStartInfo(owner,
                dimensionId == null ? "" : dimensionId,
                positions == null ? List.of() : List.copyOf(positions),
                outputName == null || outputName.isBlank() ? key.outputId() : outputName));
        evictEldest(STARTS, MAX_STARTS);
    }

    /**
     * Snapshot for world save. Excludes entries with no positions (broken or
     * unlocatable): they can never produce a plate, so persisting them would
     * only resurrect stale red after a reload. In-memory keeps empty to avoid
     * showing an old box via fallback. Packet bounds still apply to positions
     * per entry, but active fallbacks are never silently dropped to fit a cap:
     * the cap only bounds persistence size.
     */
    public static synchronized List<StoredStart> snapshotStarts() {
        var snapshot = new ArrayList<StoredStart>();
        for (var entry : STARTS.entrySet()) {
            var info = entry.getValue();
            if (info.owner() != null && info.positions() != null && !info.positions().isEmpty()) {
                snapshot.add(new StoredStart(entry.getKey(), info.owner(), info.dimensionId(), info.positions(),
                        info.outputName()));
            }
            if (snapshot.size() >= MAX_STARTS) {
                break;
            }
        }
        return List.copyOf(snapshot);
    }

    /**
     * Forgets finished, cancelled, or broken outputs so they never return
     * after a reload. Called on job finish/cancel for the scope's keys and on
     * login resync when server-side validation finds all positions broken.
     */
    public static synchronized void removeStarts(java.util.Collection<com.ctux.ae2craftingtime.core.ProfileKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (var key : keys) {
            if (key != null) {
                STARTS.remove(key);
            }
        }
    }

    /**
     * All persisted fallbacks owned by the player for the given output id,
     * across every network. Chat links carry only the output id (no network),
     * so validation tries each network's fallback and uses the first with
     * still-valid provider targets instead of the captured record positions.
     */
    public static synchronized List<StoredStart> startsForOutput(UUID owner, String outputId) {
        var matches = new ArrayList<StoredStart>();
        if (owner == null || outputId == null || outputId.isBlank()) {
            return List.copyOf(matches);
        }
        for (var entry : STARTS.entrySet()) {
            var key = entry.getKey();
            var info = entry.getValue();
            if (key == null || info == null || !outputId.equals(key.outputId())
                    || !owner.equals(info.owner())) {
                continue;
            }
            matches.add(new StoredStart(key, info.owner(), info.dimensionId(), info.positions(),
                    info.outputName()));
        }
        return List.copyOf(matches);
    }

    /**
     * Snapshot click-scoped locate records for world save so active-craft
     * chat links stay usable across reload. Finished, cancelled, and broken
     * records are removed on finish and resync, so only live links persist.
     */
    public static synchronized List<LocateRecord> snapshotRecords() {
        var snapshot = new ArrayList<LocateRecord>();
        for (var record : RECORDS.values()) {
            if (record == null || record.id() == null || record.owner() == null) {
                continue;
            }
            snapshot.add(new LocateRecord(record.id(), record.owner(),
                    record.dimensionId() == null ? "" : record.dimensionId(),
                    record.positions() == null ? List.of() : List.copyOf(record.positions()),
                    record.outputName() == null ? "" : record.outputName(),
                    record.outputId() == null ? "" : record.outputId(),
                    record.createdTick()));
            if (snapshot.size() >= MAX_RECORDS) {
                break;
            }
        }
        return List.copyOf(snapshot);
    }

    public static synchronized void restoreRecords(List<LocateRecord> stored) {
        RECORDS.clear();
        if (stored == null) {
            return;
        }
        for (var record : stored) {
            if (record == null || record.id() == null || record.owner() == null) {
                continue;
            }
            RECORDS.put(record.id(), new LocateRecord(record.id(), record.owner(),
                    record.dimensionId() == null ? "" : record.dimensionId(),
                    record.positions() == null ? List.of() : List.copyOf(record.positions()),
                    record.outputName() == null ? "" : record.outputName(),
                    record.outputId() == null ? "" : record.outputId(),
                    record.createdTick()));
            evictEldest(RECORDS, MAX_RECORDS);
        }
    }

    /**
     * Forgets one click record, for example when its provider targets all
     * validate as broken. Finished and cancelled jobs forget all of their
     * owner's records for the scope's outputs via
     * {@link #removeRecordsForKeys}.
     */
    public static synchronized void removeRecord(UUID id) {
        if (id != null) {
            RECORDS.remove(id);
        }
    }

    /**
     * Forgets every click record for the finished scope's outputs so
     * finished and cancelled chat links expire instead of recreating a
     * highlight. Other owners and other outputs are untouched, so identical
     * outputs on another player's craft keep working links.
     */
    public static synchronized void removeRecordsForKeys(
            java.util.Collection<com.ctux.ae2craftingtime.core.ProfileKey> keys, UUID owner) {
        if (keys == null || keys.isEmpty() || owner == null) {
            return;
        }
        var outputIds = new java.util.HashSet<String>();
        for (var key : keys) {
            if (key != null && key.outputId() != null && !key.outputId().isBlank()) {
                outputIds.add(key.outputId());
            }
        }
        if (outputIds.isEmpty()) {
            return;
        }
        RECORDS.entrySet().removeIf(entry -> {
            var record = entry.getValue();
            return record != null && owner.equals(record.owner()) && outputIds.contains(record.outputId());
        });
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
            var dimension = entry.dimensionId() != null && !entry.dimensionId().isBlank() ? entry.dimensionId()
                    : "";
            noteStart(entry.key(), entry.owner(), dimension, entry.positions(), entry.outputName());
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
