package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

class MissingProviderLifecycleTest {
    @Test
    void jobReplacementFinishCancellationDisableAndReloadClearDiagnostics() {
        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var key = new ProfileKey("network", "minecraft:iron_ingot");
        var cleanups = List.<Runnable>of(
                () -> profiler.startWaiting(cpu, List.of(key), 20),
                () -> profiler.clearPending(cpu),
                () -> profiler.setEnabled(false),
                () -> profiler.loadSamples(List.of()));
        for (var cleanup : cleanups) {
            profiler.setEnabled(true);
            profiler.observeProviders(cpu, "pattern", Map.of(key, 1L), false);
            assertEquals(Set.of(key), profiler.missingProviderOutputs(cpu, pattern -> false));
            assertTrue(profiler.snapshotSamples().isEmpty());
            cleanup.run();
            assertTrue(profiler.missingProviderOutputs(cpu, pattern -> false).isEmpty());
        }
        profiler.setEnabled(false);
        profiler.observeProviders(cpu, "pattern", Map.of(key, 1L), false);
        assertTrue(profiler.missingProviderOutputs(cpu, pattern -> false).isEmpty());
    }
}
