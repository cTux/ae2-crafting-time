package com.ctux.ae2craftingtime.core;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class TtcAccuracyTracker {
    private final int maxSamples;
    private final Map<Object, PendingJob> pending = new IdentityHashMap<>();
    private final Map<ProfileKey, ArrayDeque<AccuracySample>> samples = new HashMap<>();

    public TtcAccuracyTracker(int maxSamples) {
        if (maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples must be positive");
        }
        this.maxSamples = maxSamples;
    }

    public void start(ProfileKey finalOutput, Object scope, long predictedSeconds, int knownRows, int totalRows,
            long tick, long nanoTime) {
        if (finalOutput == null || scope == null || predictedSeconds <= 0 || knownRows <= 0 || totalRows < knownRows) {
            return;
        }
        pending.put(scope, new PendingJob(finalOutput, predictedSeconds, knownRows, totalRows, tick, nanoTime));
    }

    public boolean finish(Object scope, boolean success, long tick, long nanoTime) {
        var job = pending.remove(scope);
        if (!success || job == null || tick < job.startedTick || nanoTime <= job.startedNanoTime) {
            return false;
        }

        var queue = samples.computeIfAbsent(job.finalOutput, ignored -> new ArrayDeque<>());
        queue.addLast(new AccuracySample(job.predictedSeconds, (tick - job.startedTick) / 20.0,
                (nanoTime - job.startedNanoTime) / 1_000_000_000.0, job.knownRows, job.totalRows));
        while (queue.size() > maxSamples) {
            queue.removeFirst();
        }
        return true;
    }

    public Optional<TtcAccuracyStats> stats(ProfileKey key) {
        var queue = samples.get(key);
        if (queue == null) {
            return Optional.empty();
        }

        double coverageTotal = 0;
        double signedErrorTotal = 0;
        double absolutePercentageErrorTotal = 0;
        double ratioTotal = 0;
        int fullyCovered = 0;
        for (var sample : queue) {
            coverageTotal += sample.coverage();
            if (sample.knownRows == sample.totalRows) {
                fullyCovered++;
                var error = sample.actualWallSeconds - sample.predictedSeconds;
                signedErrorTotal += error;
                absolutePercentageErrorTotal += Math.abs(error) / sample.actualWallSeconds * 100.0;
                ratioTotal += sample.actualWallSeconds / sample.predictedSeconds;
            }
        }

        var last = queue.getLast();
        return Optional.of(new TtcAccuracyStats(queue.size(), fullyCovered, coverageTotal / queue.size(),
                fullyCovered == 0 ? 0 : signedErrorTotal / fullyCovered,
                fullyCovered == 0 ? 0 : absolutePercentageErrorTotal / fullyCovered,
                fullyCovered == 0 ? 0 : ratioTotal / fullyCovered,
                last.predictedSeconds, last.actualWallSeconds, last.actualTickSeconds, last.knownRows, last.totalRows));
    }

    public void clear(ProfileKey key) {
        samples.remove(key);
        pending.values().removeIf(job -> job.finalOutput.equals(key));
    }

    public void clear() {
        pending.clear();
        samples.clear();
    }

    private record PendingJob(ProfileKey finalOutput, long predictedSeconds, int knownRows, int totalRows,
            long startedTick, long startedNanoTime) {
    }

    private record AccuracySample(long predictedSeconds, double actualTickSeconds, double actualWallSeconds,
            int knownRows, int totalRows) {
        private double coverage() {
            return (double) knownRows / totalRows;
        }
    }
}
