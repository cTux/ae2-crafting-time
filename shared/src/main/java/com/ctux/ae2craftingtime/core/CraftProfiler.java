package com.ctux.ae2craftingtime.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CraftProfiler {
    private final int maxSamples;
    private final double outlierMultiplier;
    private final Map<ProfileKey, ArrayDeque<PendingCraft>> pending = new HashMap<>();
    private final Map<ProfileKey, ArrayDeque<CraftSample>> samples = new HashMap<>();
    private boolean enabled = true;

    public CraftProfiler(int maxSamples) {
        this(maxSamples, 4.0);
    }

    public CraftProfiler(int maxSamples, double outlierMultiplier) {
        if (maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples must be positive");
        }
        if (outlierMultiplier < 1.0) {
            throw new IllegalArgumentException("outlierMultiplier must be at least 1");
        }
        this.maxSamples = maxSamples;
        this.outlierMultiplier = outlierMultiplier;
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

    public boolean complete(ProfileKey key, long amount, long tick) {
        if (!enabled || amount <= 0) {
            return false;
        }

        var queue = pending.get(key);
        if (queue == null) {
            return false;
        }

        var recorded = false;
        var remaining = amount;
        while (remaining > 0 && !queue.isEmpty()) {
            var craft = queue.peekFirst();
            var consumed = Math.min(remaining, craft.remainingAmount);
            craft.remainingAmount -= consumed;
            remaining -= consumed;

            if (craft.remainingAmount == 0) {
                queue.removeFirst();
                addSample(key, new CraftSample(craft.totalAmount, craft.unit, tick - craft.startedTick));
                recorded = true;
            }
        }

        if (queue.isEmpty()) {
            pending.remove(key);
        }

        return recorded;
    }

    public Optional<ProfileStats> stats(ProfileKey key) {
        var queue = samples.get(key);
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }

        long durationTotal = 0;
        long lastDuration = 0;
        var filtered = filteredSamples(queue, outlierMultiplier);
        var sampleDurationTicks = new ArrayList<Long>(queue.size());
        long weightedDurationTotal = 0;
        long weightedAmountTotal = 0;
        long weight = 1;
        ProfileUnit unit = null;

        for (var sample : queue) {
            durationTotal += sample.durationTicks;
            lastDuration = sample.durationTicks;
            sampleDurationTicks.add(sample.durationTicks);
            unit = sample.unit;
        }
        for (var sample : filtered) {
            weightedDurationTotal += sample.durationTicks * weight;
            weightedAmountTotal += sample.amount * weight;
            weight++;
        }

        var averageDuration = (double) durationTotal / queue.size();
        var amountPerTick = weightedDurationTotal == 0 ? 0.0 : (double) weightedAmountTotal / weightedDurationTotal;
        return Optional.of(new ProfileStats(
                queue.size(),
                averageDuration,
                amountPerTick,
                amountPerTick * 20.0,
                lastDuration,
                unit,
                queue.size() >= 3 && filtered.size() == queue.size(),
                filtered.size(),
                outlierMultiplier,
                sampleDurationTicks));
    }

    private static List<CraftSample> filteredSamples(ArrayDeque<CraftSample> queue, double outlierMultiplier) {
        if (queue.size() < 5) {
            return List.copyOf(queue);
        }

        var samples = List.copyOf(queue);
        var durationsPerUnit = new ArrayList<Double>(samples.size());
        for (var sample : samples) {
            durationsPerUnit.add((double) sample.durationTicks / sample.amount);
        }
        durationsPerUnit.sort(Double::compare);
        var median = durationsPerUnit.get(durationsPerUnit.size() / 2);
        var filtered = new ArrayList<CraftSample>();
        for (var sample : samples) {
            var durationPerUnit = (double) sample.durationTicks / sample.amount;
            if (durationPerUnit >= median / outlierMultiplier && durationPerUnit <= median * outlierMultiplier) {
                filtered.add(sample);
            }
        }
        return filtered.isEmpty() ? samples : filtered;
    }

    private void addSample(ProfileKey key, CraftSample sample) {
        var queue = samples.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        queue.addLast(sample);
        while (queue.size() > maxSamples) {
            queue.removeFirst();
        }
    }

    public List<PersistedOutputSamples> snapshotSamples() {
        var snapshot = new ArrayList<PersistedOutputSamples>();
        for (var entry : samples.entrySet()) {
            var persisted = new ArrayList<PersistedCraftSample>();
            ProfileUnit unit = null;
            for (var sample : entry.getValue()) {
                unit = sample.unit;
                persisted.add(new PersistedCraftSample(sample.amount, sample.durationTicks));
            }
            if (unit != null && !persisted.isEmpty()) {
                snapshot.add(new PersistedOutputSamples(entry.getKey(), unit, persisted));
            }
        }
        return snapshot;
    }

    public void loadSamples(List<PersistedOutputSamples> persisted) {
        samples.clear();
        pending.clear();
        for (var output : persisted) {
            for (var sample : output.samples()) {
                addSample(output.key(), new CraftSample(sample.amount(), output.unit(), sample.durationTicks()));
            }
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
