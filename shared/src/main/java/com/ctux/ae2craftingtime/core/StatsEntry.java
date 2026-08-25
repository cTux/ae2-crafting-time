package com.ctux.ae2craftingtime.core;

import java.util.Optional;

public record StatsEntry(ProfileKey key, ProfileStats stats, Optional<TtcAccuracyStats> accuracy) {
    public StatsEntry(ProfileKey key, ProfileStats stats) {
        this(key, stats, Optional.empty());
    }

    public StatsEntry {
        accuracy = accuracy == null ? Optional.empty() : accuracy;
    }
}
