package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public final class ClientStatsCache {
    private final Map<ProfileKey, StatsEntry> stats = new HashMap<>();
    private final Map<ProfileKey, Long> waitingTicks = new HashMap<>();

    public void replace(List<StatsEntry> entries) {
        for (var entry : entries) {
            stats.put(entry.key(), entry);
        }
    }

    public void replace(List<ProfileKey> requestedKeys, List<StatsEntry> entries) {
        for (var key : requestedKeys) {
            stats.remove(key);
        }
        replace(entries);
    }

    public Optional<ProfileStats> get(ProfileKey key) {
        return Optional.ofNullable(stats.get(key)).map(StatsEntry::stats);
    }

    public Optional<TtcAccuracyStats> accuracy(ProfileKey key) {
        return Optional.ofNullable(stats.get(key)).flatMap(StatsEntry::accuracy);
    }

    public Optional<StallDiagnostic> stall(ProfileKey key) {
        return Optional.ofNullable(stats.get(key)).flatMap(StatsEntry::stall);
    }

    public OptionalLong waitingTicks(ProfileKey key) {
        var ticks = waitingTicks.get(key);
        return ticks == null ? OptionalLong.empty() : OptionalLong.of(ticks);
    }

    public void replaceWaiting(List<ProfileKey> requestedKeys, Map<ProfileKey, Long> values) {
        requestedKeys.forEach(waitingTicks::remove);
        waitingTicks.putAll(values);
    }

    public void remove(ProfileKey key) {
        stats.remove(key);
        waitingTicks.remove(key);
    }

    public void clear() {
        stats.clear();
        waitingTicks.clear();
    }
}
