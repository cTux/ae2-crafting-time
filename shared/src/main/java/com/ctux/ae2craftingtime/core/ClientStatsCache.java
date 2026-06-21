package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientStatsCache {
    private final Map<ProfileKey, ProfileStats> stats = new HashMap<>();

    public void replace(List<StatsEntry> entries) {
        for (var entry : entries) {
            stats.put(entry.key(), entry.stats());
        }
    }

    public Optional<ProfileStats> get(ProfileKey key) {
        return Optional.ofNullable(stats.get(key));
    }
}
