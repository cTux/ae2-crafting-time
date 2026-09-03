package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DispatchPowerTrackerTest {
    private final ProfileKey output = new ProfileKey("grid", "minecraft:iron_ingot");
    private final Object cpu = new Object();

    @Test
    void exactAeThresholdPositiveOutputsAndPatternRecovery() {
        var tracker = new DispatchPowerTracker();
        var other = new ProfileKey("grid", "minecraft:gold_ingot");
        for (var extracted : new double[] {0, 9.98, 9.99, 10, 11}) {
            tracker.observe(cpu, "a", Map.of(output, 1L, other, 0L), 10, extracted, 50);
            assertEquals(extracted < 9.99 ? Map.of(output, CraftingBlockReason.NO_POWER) : Map.of(),
                    tracker.reasons(cpu, 50, Set.of()));
        }
        tracker.observe(cpu, "a", Map.of(output, 1L), 10, 0, 50);
        tracker.observe(cpu, "b", Map.of(output, 1L), 10, 0, 50);
        tracker.observe(cpu, "a", Map.of(output, -1L), 10, 0, 50);
        assertEquals(Map.of(output, CraftingBlockReason.NO_POWER), tracker.reasons(cpu, 50, Set.of()));
        tracker.observe(cpu, "b", Map.of(output, 1L), 10, 10, 50);
        assertTrue(tracker.reasons(cpu, 50, Set.of()).isEmpty());
    }

    @Test
    void freshnessPriorityCpuIdentityAndNoObservation() {
        var tracker = new DispatchPowerTracker();
        assertTrue(tracker.reasons(cpu, 0, Set.of()).isEmpty());
        tracker.observe(null, "a", Map.of(output, 1L), 10, 0, 50);
        assertTrue(tracker.reasons(null, 50, Set.of()).isEmpty());
        for (var now : new long[] {49, 50, 69, 70}) {
            tracker.observe(cpu, "a", Map.of(output, 1L), 10, 0, 50);
            assertEquals(now >= 50 && now < 70, !tracker.reasons(cpu, now, Set.of()).isEmpty());
        }
        tracker.observe(cpu, "a", Map.of(output, 1L), 10, 0, 50);
        assertTrue(tracker.reasons(new Object(), 50, Set.of()).isEmpty());
        assertEquals(Map.of(output, CraftingBlockReason.NO_PROVIDER), tracker.reasons(cpu, 50, Set.of(output)));
        tracker.observe(cpu, "a", Map.of(output, 1L), 10, 0, 69);
        assertEquals(Map.of(output, CraftingBlockReason.NO_POWER), tracker.reasons(cpu, 70, Set.of()));
        tracker.setEnabled(false);
        tracker.observe(cpu, "a", Map.of(output, 1L), 10, 0, 70);
        assertTrue(tracker.reasons(cpu, 70, Set.of()).isEmpty());
        tracker.setEnabled(true);
        tracker.observe(cpu, "a", Map.of(output, 1L), 10, 0, 70);
        tracker.clear(cpu);
        assertTrue(tracker.reasons(cpu, 70, Set.of()).isEmpty());
    }

    @Test
    void profilerClearsPowerOnEveryJobLifecycleBoundary() {
        var profiler = new CraftProfiler(10);
        for (var cleanup : List.<Runnable>of(() -> profiler.startWaiting(cpu, List.of(output), 50),
                () -> profiler.clearPending(cpu), () -> profiler.setEnabled(false),
                () -> profiler.loadSamples(List.of()))) {
            profiler.setEnabled(true);
            profiler.observeDispatchPower(cpu, "pattern", Map.of(output, 1L), 10, 0, 50);
            assertEquals(Map.of(output, CraftingBlockReason.NO_POWER), profiler.blockReasons(cpu, 50, Set.of()));
            cleanup.run();
            assertTrue(profiler.blockReasons(cpu, 50, Set.of()).isEmpty());
        }
    }

    @Test
    void powerCacheReplacesRequestedRowsAndRejectsAnotherCpuContext() {
        var cache = new ClientStatsCache();
        cache.replaceBlockReasons(List.of(output), Map.of(output, CraftingBlockReason.NO_POWER), 1);
        assertEquals(CraftingBlockReason.NO_POWER, cache.blockReason(output, 1));
        assertNull(cache.blockReason(output, 2));
        cache.replaceBlockReasons(List.of(output), Map.of(), 1);
        assertNull(cache.blockReason(output, 1));
    }
}
