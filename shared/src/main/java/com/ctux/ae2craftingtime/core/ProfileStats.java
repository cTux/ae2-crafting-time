package com.ctux.ae2craftingtime.core;

public record ProfileStats(
        int sampleCount,
        double averageDurationTicks,
        double amountPerTick,
        double amountPerSecond,
        long lastDurationTicks,
        ProfileUnit unit,
        boolean reliableEstimate) {
    public ProfileStats(int sampleCount, double averageDurationTicks, double amountPerTick, double amountPerSecond,
            long lastDurationTicks, ProfileUnit unit) {
        this(sampleCount, averageDurationTicks, amountPerTick, amountPerSecond, lastDurationTicks, unit, true);
    }
}
