package com.ctux.ae2craftingtime.core;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CraftProfiler {
    private final int maxSamples;
    private final Map<ProfileKey, ArrayDeque<PendingCraft>> pending = new HashMap<>();
    private final Map<ProfileKey, ArrayDeque<CraftSample>> samples = new HashMap<>();
    private boolean enabled = true;

    public CraftProfiler(int maxSamples) {
        if (maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples must be positive");
        }
        this.maxSamples = maxSamples;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            pending.clear();
        }
    }

    public void start(ProfileKey key, long amount, ProfileUnit unit, long tick) {
        if (!enabled || amount <= 0) {
            return;
        }
        pending.computeIfAbsent(key, ignored -> new ArrayDeque<>())
                .addLast(new PendingCraft(amount, unit, tick));
    }

    public void complete(ProfileKey key, long amount, long tick) {
        if (!enabled || amount <= 0) {
            return;
        }

        var queue = pending.get(key);
        if (queue == null) {
            return;
        }

        var remaining = amount;
        while (remaining > 0 && !queue.isEmpty()) {
            var craft = queue.peekFirst();
            var consumed = Math.min(remaining, craft.remainingAmount);
            craft.remainingAmount -= consumed;
            remaining -= consumed;

            if (craft.remainingAmount == 0) {
                queue.removeFirst();
                addSample(key, new CraftSample(craft.totalAmount, craft.unit, tick - craft.startedTick));
            }
        }

        if (queue.isEmpty()) {
            pending.remove(key);
        }
    }

    public Optional<ProfileStats> stats(ProfileKey key) {
        var queue = samples.get(key);
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }

        long durationTotal = 0;
        long amountTotal = 0;
        long lastDuration = 0;
        ProfileUnit unit = null;

        for (var sample : queue) {
            durationTotal += sample.durationTicks;
            amountTotal += sample.amount;
            lastDuration = sample.durationTicks;
            unit = sample.unit;
        }

        var averageDuration = (double) durationTotal / queue.size();
        var amountPerTick = durationTotal == 0 ? 0.0 : (double) amountTotal / durationTotal;
        return Optional.of(new ProfileStats(
                queue.size(),
                averageDuration,
                amountPerTick,
                amountPerTick * 20.0,
                lastDuration,
                unit));
    }

    private void addSample(ProfileKey key, CraftSample sample) {
        var queue = samples.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        queue.addLast(sample);
        while (queue.size() > maxSamples) {
            queue.removeFirst();
        }
    }

    private static final class PendingCraft {
        private final long totalAmount;
        private final ProfileUnit unit;
        private final long startedTick;
        private long remainingAmount;

        private PendingCraft(long amount, ProfileUnit unit, long startedTick) {
            this.totalAmount = amount;
            this.remainingAmount = amount;
            this.unit = unit;
            this.startedTick = startedTick;
        }
    }

    private record CraftSample(long amount, ProfileUnit unit, long durationTicks) {
    }
}
