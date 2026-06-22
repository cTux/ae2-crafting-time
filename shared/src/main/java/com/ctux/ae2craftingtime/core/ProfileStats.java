package com.ctux.ae2craftingtime.core;

import java.util.List;

public record ProfileStats(
        int sampleCount,
        double averageDurationTicks,
        double amountPerTick,
        double amountPerSecond,
        long lastDurationTicks,
        ProfileUnit unit,
        boolean reliableEstimate,
        int usedSampleCount,
        double outlierMultiplier,
        List<Long> sampleDurationTicks) {
    public ProfileStats {
        sampleDurationTicks = List.copyOf(sampleDurationTicks);
    }

    public ProfileStats(int sampleCount, double averageDurationTicks, double amountPerTick, double amountPerSecond,
            long lastDurationTicks, ProfileUnit unit) {
        this(sampleCount, averageDurationTicks, amountPerTick, amountPerSecond, lastDurationTicks, unit, true);
    }

    public ProfileStats(int sampleCount, double averageDurationTicks, double amountPerTick, double amountPerSecond,
            long lastDurationTicks, ProfileUnit unit, boolean reliableEstimate) {
        this(sampleCount, averageDurationTicks, amountPerTick, amountPerSecond, lastDurationTicks, unit,
                reliableEstimate, sampleCount, 4.0, List.of());
    }
}
