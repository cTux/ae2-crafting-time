package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public final class ClientStatsCache {
    private final Map<ProfileKey, StatsEntry> stats = new HashMap<>();
    private final Map<ProfileKey, Long> waitingTicks = new HashMap<>();
    private long missingProviderContext = -1;
    private final Set<ProfileKey> missingProviders = new HashSet<>();

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

    public boolean missingProvider(ProfileKey key, long cpuContext) {
        return missingProviderContext == cpuContext && missingProviders.contains(key);
    }

    public void replaceMissingProviders(List<ProfileKey> requestedKeys, Set<ProfileKey> values, long cpuContext) {
        if (missingProviderContext != cpuContext) {
            missingProviders.clear();
            missingProviderContext = cpuContext;
        }
        missingProviders.removeAll(requestedKeys);
        missingProviders.addAll(values);
    }

    public void clearCpuState() {
        waitingTicks.clear();
        missingProviders.clear();
    }

    public void remove(ProfileKey key) {
        stats.remove(key);
        waitingTicks.remove(key);
        missingProviders.remove(key);
    }

    public void clear() {
        stats.clear();
        clearCpuState();
    }
}
