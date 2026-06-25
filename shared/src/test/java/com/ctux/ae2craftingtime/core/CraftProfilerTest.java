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
        assertEquals(3.0 / 100.0, stats.amountPerTick());
        assertEquals(20.0 * 3.0 / 100.0, stats.amountPerSecond());
        assertEquals(40, stats.lastDurationTicks());
    }

    @Test
    void weightsRecentSamplesMoreForThroughput() {
        var profiler = new CraftProfiler(10);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 0);
        profiler.complete(ironPlate, 1, 100);
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 100);
        profiler.complete(ironPlate, 1, 110);

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(3.0 / 120.0, stats.amountPerTick());
        assertEquals(20.0 * 3.0 / 120.0, stats.amountPerSecond());
    }

    @Test
    void ignoresExtremeDurationOutliersForThroughput() {
        var profiler = new CraftProfiler(10);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        for (var i = 0; i < 4; i++) {
            profiler.start(ironPlate, 1, ProfileUnit.ITEM, i * 20L);
            profiler.complete(ironPlate, 1, i * 20L + 10);
        }
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 100);
        profiler.complete(ironPlate, 1, 1_100);

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(5, stats.sampleCount());
        assertEquals(2.0, stats.amountPerSecond());
        assertFalse(stats.reliableEstimate());
    }

    @Test
    void outlierMultiplierCanBeTuned() {
        var profiler = new CraftProfiler(10, 200.0);
        var ironPlate = new ProfileKey("minecraft:iron_plate");

        for (var i = 0; i < 4; i++) {
            profiler.start(ironPlate, 1, ProfileUnit.ITEM, i * 20L);
            profiler.complete(ironPlate, 1, i * 20L + 10);
        }
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 100);
        profiler.complete(ironPlate, 1, 1_100);

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(15.0 / 5_100.0 * 20.0, stats.amountPerSecond());
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
    void networkScopedIdentityKeepsSamplesSeparate() {
        var profiler = new CraftProfiler(10);
        var networkA = new ProfileKey("net-a", "minecraft:iron_plate");
        var networkB = new ProfileKey("net-b", "minecraft:iron_plate");

        profiler.start(networkA, 1, ProfileUnit.ITEM, 0);
        profiler.complete(networkA, 1, 10);

        assertEquals(1, profiler.stats(networkA).orElseThrow().sampleCount());
        assertFalse(profiler.stats(networkB).isPresent());
    }

    @Test
    void importKeepsScopedNetworkFragmentsSeparate() {
        var imported = new CraftProfiler(10);

        imported.loadSamples(List.of(
                new PersistedOutputSamples(new ProfileKey("net-a", "minecraft:iron_plate"), ProfileUnit.ITEM,
                        List.of(new PersistedCraftSample(1, 10))),
                new PersistedOutputSamples(new ProfileKey("net-b", "minecraft:iron_plate"), ProfileUnit.ITEM,
                        List.of(new PersistedCraftSample(1, 20)))));

        var networkA = imported.stats(new ProfileKey("net-a", "minecraft:iron_plate")).orElseThrow();
        var networkB = imported.stats(new ProfileKey("net-b", "minecraft:iron_plate")).orElseThrow();

        assertEquals(1, networkA.sampleCount());
        assertEquals(10.0, networkA.averageDurationTicks());
        assertEquals(1, networkB.sampleCount());
        assertEquals(20.0, networkB.averageDurationTicks());
        assertEquals(List.of(
                new ProfileKey("net-a", "minecraft:iron_plate"),
                new ProfileKey("net-b", "minecraft:iron_plate")),
                imported.snapshotSamples().stream().map(PersistedOutputSamples::key).sorted((a, b) -> a.networkId()
                        .compareTo(b.networkId())).toList());
    }

    @Test
    void clearsOnlyRequestedOutputSamples() {
        var profiler = new CraftProfiler(10);
        var ironPlate = new ProfileKey("net-a", "minecraft:iron_plate");
        var copperPlate = new ProfileKey("net-a", "minecraft:copper_plate");

        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 0);
        profiler.complete(ironPlate, 1, 10);
        profiler.start(copperPlate, 1, ProfileUnit.ITEM, 0);
        profiler.complete(copperPlate, 1, 20);

        profiler.clearSamples(ironPlate);

        assertFalse(profiler.stats(ironPlate).isPresent());
        assertEquals(1, profiler.stats(copperPlate).orElseThrow().sampleCount());
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
