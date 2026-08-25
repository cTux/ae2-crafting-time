package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TtcAccuracyTrackerTest {
    @Test
    void comparesFrozenPredictionWithCompletedWallAndTickTime() {
        var tracker = new TtcAccuracyTracker(10);
        var output = new ProfileKey("net-a", "minecraft:gear");
        var cpu = new Object();

        tracker.start(output, cpu, 10, 2, 2, 100, 1_000_000_000L);
        tracker.finish(cpu, true, 340, 16_000_000_000L);

        var stats = tracker.stats(output).orElseThrow();
        assertEquals(1, stats.sampleCount());
        assertEquals(1, stats.fullyCoveredSampleCount());
        assertEquals(1.0, stats.averageCoverage());
        assertEquals(5.0, stats.meanSignedErrorSeconds());
        assertEquals(100.0 / 3.0, stats.meanAbsolutePercentageError(), 0.000_001);
        assertEquals(1.5, stats.meanActualToPredictedRatio());
        assertEquals(10, stats.lastPredictedSeconds());
        assertEquals(15.0, stats.lastActualWallSeconds());
        assertEquals(12.0, stats.lastActualTickSeconds());
    }

    @Test
    void excludesCancelledAndIncompleteJobs() {
        var tracker = new TtcAccuracyTracker(10);
        var output = new ProfileKey("minecraft:gear");
        var cancelled = new Object();

        tracker.start(output, cancelled, 10, 1, 1, 0, 1);
        tracker.finish(cancelled, false, 20, 1_000_000_001L);
        var unpredicted = new Object();
        tracker.start(output, unpredicted, 0, 0, 1, 0, 1);
        tracker.finish(unpredicted, true, 20, 1_000_000_001L);

        assertFalse(tracker.stats(output).isPresent());
    }

    @Test
    void aggregatesAccuracyOnlyForFullyCoveredPlans() {
        var tracker = new TtcAccuracyTracker(10);
        var output = new ProfileKey("minecraft:gear");

        var partialScope = new Object();
        tracker.start(output, partialScope, 10, 1, 2, 0, 1);
        tracker.finish(partialScope, true, 20, 20_000_000_001L);
        var fullScope = new Object();
        tracker.start(output, fullScope, 10, 2, 2, 0, 1);
        tracker.finish(fullScope, true, 20, 20_000_000_001L);

        var stats = tracker.stats(output).orElseThrow();
        assertEquals(2, stats.sampleCount());
        assertEquals(1, stats.fullyCoveredSampleCount());
        assertEquals(0.75, stats.averageCoverage());
        assertEquals(10.0, stats.meanSignedErrorSeconds());
    }

    @Test
    void keepsOnlyLatestSamples() {
        var tracker = new TtcAccuracyTracker(2);
        var output = new ProfileKey("minecraft:gear");
        for (var i = 1; i <= 3; i++) {
            var scope = new Object();
            tracker.start(output, scope, i, 1, 1, 0, 1);
            tracker.finish(scope, true, 20, 10_000_000_001L);
        }

        var stats = tracker.stats(output).orElseThrow();
        assertEquals(2, stats.sampleCount());
        assertEquals(3, stats.lastPredictedSeconds());
    }

    @Test
    void clearsRuntimeStateBetweenWorlds() {
        var tracker = new TtcAccuracyTracker(10);
        var output = new ProfileKey("minecraft:gear");
        var scope = new Object();
        tracker.start(output, scope, 10, 1, 1, 0, 1);
        tracker.finish(scope, true, 20, 10_000_000_001L);

        tracker.clear();

        assertFalse(tracker.stats(output).isPresent());
    }
}
