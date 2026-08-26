package com.ctux.ae2craftingtime.core;

import java.util.Optional;

public record StatsEntry(ProfileKey key, ProfileStats stats, Optional<TtcAccuracyStats> accuracy,
        Optional<StallDiagnostic> stall) {
    public StatsEntry(ProfileKey key, ProfileStats stats) {
        this(key, stats, Optional.empty(), Optional.empty());
    }

    public StatsEntry(ProfileKey key, ProfileStats stats, Optional<TtcAccuracyStats> accuracy) {
        this(key, stats, accuracy, Optional.empty());
    }

    public StatsEntry {
        accuracy = accuracy == null ? Optional.empty() : accuracy;
        stall = stall == null ? Optional.empty() : stall;
    }
}
