package com.ctux.ae2craftingtime.core;

import java.util.List;
import java.util.OptionalDouble;

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
        List<Long> sampleDurationTicks,
        List<Long> sampleAmounts) {
    public ProfileStats {
        sampleDurationTicks = List.copyOf(sampleDurationTicks);
        sampleAmounts = List.copyOf(sampleAmounts);
    }

    public ProfileStats(int sampleCount, double averageDurationTicks, double amountPerTick, double amountPerSecond,
            long lastDurationTicks, ProfileUnit unit) {
        this(sampleCount, averageDurationTicks, amountPerTick, amountPerSecond, lastDurationTicks, unit, true);
    }

    public ProfileStats(int sampleCount, double averageDurationTicks, double amountPerTick, double amountPerSecond,
            long lastDurationTicks, ProfileUnit unit, boolean reliableEstimate) {
        this(sampleCount, averageDurationTicks, amountPerTick, amountPerSecond, lastDurationTicks, unit,
                reliableEstimate, sampleCount, 4.0, List.of(), List.of());
    }

    public OptionalDouble sampleTicksPerUnit(int index) {
        if (index < 0 || index >= sampleAmounts.size() || index >= sampleDurationTicks.size()) {
            return OptionalDouble.empty();
        }
        var amount = sampleAmounts.get(index);
        var duration = sampleDurationTicks.get(index);
        return amount > 0 && duration > 0
                ? OptionalDouble.of((double) duration / amount)
                : OptionalDouble.empty();
    }

    public OptionalDouble averageTicksPerUnit() {
        if (sampleAmounts.isEmpty() || sampleAmounts.size() != sampleDurationTicks.size()) {
            return OptionalDouble.empty();
        }
        double total = 0;
        for (var i = 0; i < sampleAmounts.size(); i++) {
            var value = sampleTicksPerUnit(i);
            if (value.isEmpty()) {
                return OptionalDouble.empty();
            }
            total += value.getAsDouble();
        }
        return OptionalDouble.of(total / sampleAmounts.size());
    }

    public OptionalDouble latestTicksPerUnit() {
        return sampleTicksPerUnit(sampleAmounts.size() - 1);
    }
}
