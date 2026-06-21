package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class CraftProfilerTest {
    @Test
    void averagesLatestSamplesForSameOutputKey() {
        var profiler = new CraftProfiler(2);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 10);
        profiler.complete(ironPlate, 1, 20);
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 30);
        profiler.complete(ironPlate, 1, 50);
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 60);
        profiler.complete(ironPlate, 1, 100);

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(2, stats.sampleCount());
        assertEquals(30.0, stats.averageDurationTicks());
        assertEquals(1.0 / 30.0, stats.amountPerTick());
        assertEquals(20.0 / 30.0, stats.amountPerSecond());
        assertEquals(40, stats.lastDurationTicks());
    }

    @Test
    void statsExistAfterOneCompletedCraft() {
        var profiler = new CraftProfiler(10);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 10);
        profiler.complete(ironPlate, 1, 30);

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(1, stats.sampleCount());
        assertEquals(20.0, stats.averageDurationTicks());
    }

    @Test
    void tenSampleWindowDropsOlderCrafts() {
        var profiler = new CraftProfiler(10);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        for (var i = 1; i <= 11; i++) {
            profiler.start(ironPlate, 1, ProfileUnit.ITEM, 0);
            profiler.complete(ironPlate, 1, i);
        }

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(10, stats.sampleCount());
        assertEquals(6.5, stats.averageDurationTicks());
    }

    @Test
    void exportsAndImportsCompletedSamples() {
        var key = new ProfileKey("minecraft:iron_plate");
        var profiler = new CraftProfiler(10);
        profiler.start(key, 2, ProfileUnit.ITEM, 5);
        profiler.complete(key, 2, 25);

        var restored = new CraftProfiler(10);
        restored.loadSamples(profiler.snapshotSamples());

        var stats = restored.stats(key).orElseThrow();
        assertEquals(1, stats.sampleCount());
        assertEquals(20.0, stats.averageDurationTicks());
        assertEquals(0.1, stats.amountPerTick());
    }

    @Test
    void importKeepsLatestTenSamples() {
        var key = new ProfileKey("minecraft:iron_plate");
        var imported = new CraftProfiler(10);

        imported.loadSamples(List.of(new PersistedOutputSamples(key, ProfileUnit.ITEM, List.of(
                new PersistedCraftSample(1, 1),
                new PersistedCraftSample(1, 2),
                new PersistedCraftSample(1, 3),
                new PersistedCraftSample(1, 4),
                new PersistedCraftSample(1, 5),
                new PersistedCraftSample(1, 6),
                new PersistedCraftSample(1, 7),
                new PersistedCraftSample(1, 8),
                new PersistedCraftSample(1, 9),
                new PersistedCraftSample(1, 10),
                new PersistedCraftSample(1, 11)))));

        var stats = imported.stats(key).orElseThrow();
        assertEquals(10, stats.sampleCount());
        assertEquals(6.5, stats.averageDurationTicks());
    }

    @Test
    void newProfilerStartsWithoutPersistedSessionStats() {
        var key = new ProfileKey("minecraft:iron_plate");
        var oldSession = new CraftProfiler(10);
        oldSession.start(key, 1, ProfileUnit.ITEM, 1);
        oldSession.complete(key, 1, 2);

        var newSession = new CraftProfiler(10);

        assertFalse(newSession.stats(key).isPresent());
    }

    @Test
    void outputIdentityMergesSamplesFromSameOutput() {
        var profiler = new CraftProfiler(10);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 0);
        profiler.complete(ironPlate, 1, 10);
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 100);
        profiler.complete(ironPlate, 1, 130);

        var stats = profiler.stats(ironPlate).orElseThrow();
        assertEquals(2, stats.sampleCount());
        assertEquals(20.0, stats.averageDurationTicks());
    }

    @Test
    void networkIdentityKeepsSameOutputSeparate() {
        var profiler = new CraftProfiler(10);
        var networkA = new ProfileKey("net-a", "minecraft:iron_plate");
        var networkB = new ProfileKey("net-b", "minecraft:iron_plate");

        profiler.start(networkA, 1, ProfileUnit.ITEM, 0);
        profiler.complete(networkA, 1, 10);

        assertFalse(profiler.stats(networkB).isPresent());
    }

    @Test
    void waitsForPartialCompletionsBeforeRecordingSample() {
        var profiler = new CraftProfiler(10);
        var fluid = new ProfileKey("minecraft:water");

        profiler.start(fluid, 1000, ProfileUnit.MILLIBUCKET, 5);
        profiler.complete(fluid, 250, 10);

        assertFalse(profiler.stats(fluid).isPresent());

        profiler.complete(fluid, 750, 25);

        var stats = profiler.stats(fluid).orElseThrow();
        assertEquals(ProfileUnit.MILLIBUCKET, stats.unit());
        assertEquals(20.0, stats.averageDurationTicks());
        assertEquals(50.0, stats.amountPerTick());
        assertEquals(1000.0, stats.amountPerSecond());
    }

    @Test
    void disabledProfilerIgnoresStartsAndCompletions() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:gear");

        profiler.setEnabled(false);
        profiler.start(key, 1, ProfileUnit.ITEM, 1);
        profiler.complete(key, 1, 10);

        assertFalse(profiler.stats(key).isPresent());
    }
}
