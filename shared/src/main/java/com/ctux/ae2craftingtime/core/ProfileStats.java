package com.ctux.ae2craftingtime.core;

public record ProfileStats(
        int sampleCount,
        double averageDurationTicks,
        double amountPerTick,
        double amountPerSecond,
        long lastDurationTicks,
        ProfileUnit unit) {
}
