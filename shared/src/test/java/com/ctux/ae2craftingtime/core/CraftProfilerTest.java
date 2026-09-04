package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CraftProfilerTest {
    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new CraftProfiler(0));
        assertThrows(IllegalArgumentException.class, () -> new CraftProfiler(1, 0.5));
    }

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

        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 0);
        profiler.complete(ironPlate, 1, 1);
        for (var i = 0; i < 4; i++) {
            profiler.start(ironPlate, 1, ProfileUnit.ITEM, 20 + i * 20L);
            profiler.complete(ironPlate, 1, 30 + i * 20L);
        }
        profiler.start(ironPlate, 1, ProfileUnit.ITEM, 120);
        profiler.complete(ironPlate, 1, 1_120);

        var stats = profiler.stats(ironPlate).orElseThrow();

        assertEquals(6, stats.sampleCount());
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
    void importSkipsInvalidSamples() {
        var key = new ProfileKey("net-a", "minecraft:iron_plate");
        var profiler = new CraftProfiler(10);

        profiler.loadSamples(List.of(new PersistedOutputSamples(key, ProfileUnit.ITEM, List.of(
                new PersistedCraftSample(0, 10),
                new PersistedCraftSample(1, 0),
                new PersistedCraftSample(2, 20)))));

        var stats = profiler.stats(key).orElseThrow();
        assertEquals(1, stats.sampleCount());
        assertEquals(20.0, stats.averageDurationTicks());
    }

    @Test
    void importSkipsInvalidOutputs() {
        var key = new ProfileKey("minecraft:iron_plate");
        var profiler = new CraftProfiler(10);

        profiler.loadSamples(Arrays.asList(
                null,
                new PersistedOutputSamples(null, ProfileUnit.ITEM, List.of(new PersistedCraftSample(1, 1))),
                new PersistedOutputSamples(key, null, List.of(new PersistedCraftSample(1, 1))),
                new PersistedOutputSamples(key, ProfileUnit.ITEM, List.of(new PersistedCraftSample(2, 20)))));

        assertEquals(1, profiler.stats(key).orElseThrow().sampleCount());
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
    void combinesParallelCpuBatchesIntoNetworkThroughput() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_ingot");
        var cpuA = new Object();
        var cpuB = new Object();

        profiler.start(key, cpuA, 64, ProfileUnit.ITEM, 0);
        profiler.start(key, cpuB, 64, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpuA, 64, 20);

        assertFalse(profiler.stats(key).isPresent());

        profiler.complete(key, cpuB, 64, 20);

        var stats = profiler.stats(key).orElseThrow();
        assertEquals(128.0, stats.amountPerSecond());
        assertEquals(List.of(128L), stats.sampleAmounts());
        assertEquals(List.of(20L), stats.sampleDurationTicks());
    }

    @Test
    void previewsProgressBeforeContinuousProductionBecomesIdle() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_ingot");
        var cpu = new Object();

        assertFalse(profiler.inProgressStats(key, 0).isPresent());
        profiler.start(key, cpu, 3, ProfileUnit.ITEM, 0);
        assertFalse(profiler.inProgressStats(key, 0).isPresent());
        profiler.complete(key, cpu, 1, 20);

        assertFalse(profiler.stats(key).isPresent());
        var preview = profiler.inProgressStats(key, 20).orElseThrow();
        assertEquals(1.0, preview.amountPerSecond());
        assertFalse(preview.reliableEstimate());
        assertTrue(profiler.snapshotSamples().isEmpty());

        var sameTickKey = new ProfileKey("minecraft:copper_ingot");
        profiler.start(sameTickKey, cpu, 2, ProfileUnit.ITEM, 20);
        profiler.complete(sameTickKey, cpu, 1, 20);
        assertEquals(20.0, profiler.inProgressStats(sameTickKey, 20).orElseThrow().amountPerSecond());
    }

    @Test
    void combinesOneReturnAcrossSeveralDispatchedBatches() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_ingot");
        var cpu = new Object();

        profiler.start(key, cpu, 32, ProfileUnit.ITEM, 0);
        profiler.start(key, cpu, 32, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpu, 64, 20);

        var stats = profiler.stats(key).orElseThrow();
        assertEquals(64.0, stats.amountPerSecond());
        assertEquals(List.of(64L), stats.sampleAmounts());
    }

    @Test
    void cancelledCpuDoesNotPoisonFutureSamples() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_ingot");
        var cancelledCpu = new Object();
        var nextCpu = new Object();

        profiler.start(key, cancelledCpu, 64, ProfileUnit.ITEM, 0);
        profiler.clearPending(cancelledCpu);
        profiler.start(key, nextCpu, 64, ProfileUnit.ITEM, 1_000);
        profiler.complete(key, nextCpu, 64, 1_020);

        var stats = profiler.stats(key).orElseThrow();
        assertEquals(20.0, stats.averageDurationTicks());
        assertEquals(64.0, stats.amountPerSecond());
    }

    @Test
    void sameTickBatchUsesOneTickMinimum() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_ingot");

        profiler.start(key, 64, ProfileUnit.ITEM, 10);
        profiler.complete(key, 64, 10);

        var stats = profiler.stats(key).orElseThrow();
        assertEquals(1.0, stats.averageDurationTicks());
        assertEquals(1_280.0, stats.amountPerSecond());
    }

    @Test
    void clearingSamplesAlsoClearsPendingWork() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_ingot");

        profiler.start(key, 64, ProfileUnit.ITEM, 0);
        profiler.clearSamples(key);
        profiler.complete(key, 64, 1_000);

        assertFalse(profiler.stats(key).isPresent());
    }

    @Test
    void disabledProfilerIgnoresStartsAndCompletions() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:gear");

        profiler.setEnabled(false);
        profiler.updateCapacity(new Object(), 1, 1, 0);
        profiler.start(key, 1, ProfileUnit.ITEM, 1);
        profiler.complete(key, 1, 10);

        assertFalse(profiler.stats(key).isPresent());
        profiler.setEnabled(true);
    }

    @Test
    void ignoresInvalidOrUnmatchedRuntimeEvents() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:gear");
        var scope = new Object();

        profiler.start(key, scope, 0, ProfileUnit.ITEM, 0);
        assertFalse(profiler.complete(key, scope, 0, 1));
        assertFalse(profiler.complete(key, scope, 1, 1));
        profiler.clearPending(scope);
        assertFalse(profiler.clearSamples(key));
        assertFalse(profiler.stall(key, scope, 1_000).isPresent());
        profiler.start(key, scope, 1, ProfileUnit.ITEM, 0);
        assertFalse(profiler.stall(key, scope, 1_000).isPresent());
        profiler.updateCapacity(null, 1, 1, 0);
        profiler.updateCapacity(scope, 1, 0, 0);
    }

    @Test
    void clearingOneCpuRebuildsTheSharedBusyWindow() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:gear");
        var cancelled = new Object();
        var retained = new Object();
        var unrelated = new Object();
        var otherKey = new ProfileKey("minecraft:plate");

        profiler.start(key, cancelled, 1, ProfileUnit.ITEM, 0);
        profiler.start(key, retained, 1, ProfileUnit.ITEM, 10);
        profiler.start(key, retained, 1, ProfileUnit.ITEM, 0);
        profiler.start(key, retained, 1, ProfileUnit.ITEM, 20);
        profiler.start(otherKey, unrelated, 1, ProfileUnit.ITEM, 0);
        profiler.clearPending(cancelled);
        assertTrue(profiler.complete(key, retained, 3, 30));

        var stats = profiler.stats(key).orElseThrow();
        assertEquals(30.0, stats.averageDurationTicks());
    }

    @Test
    void completingOneOutputKeepsOtherWorkInTheSameScope() {
        var profiler = new CraftProfiler(10);
        var scope = new Object();
        var gear = new ProfileKey("minecraft:gear");
        var plate = new ProfileKey("minecraft:plate");

        profiler.start(gear, scope, 1, ProfileUnit.ITEM, 0);
        profiler.start(plate, scope, 1, ProfileUnit.ITEM, 0);

        assertTrue(profiler.complete(gear, scope, 2, 20));
        assertTrue(profiler.complete(plate, scope, 1, 20));
    }

    @Test
    void capacityUsageIsClampedToItsValidRange() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:gear");
        var scope = new Object();

        profiler.start(key, scope, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, scope, 1, 20);
        profiler.start(key, scope, 1, ProfileUnit.ITEM, 100);
        profiler.updateCapacity(scope, 10, 4, 700);

        assertEquals(4, profiler.stall(key, scope, 700).orElseThrow().usedParallelSlots());
        profiler.updateCapacity(scope, -1, 4, 700);
        assertEquals(0, profiler.stall(key, scope, 700).orElseThrow().usedParallelSlots());
    }

    @Test
    void reportsDelayedPendingOutputAfterTypicalThreshold() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var cpu = new Object();

        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpu, 1, 200);
        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 300);
        profiler.updateCapacity(cpu, 1, 4, 300);

        assertFalse(profiler.stall(key, cpu, 699).isPresent());
        profiler.updateCapacity(cpu, 1, 4, 700);
        var diagnostic = profiler.stall(key, cpu, 700).orElseThrow();

        assertEquals(400, diagnostic.idleTicks());
        assertEquals(200.0, diagnostic.typicalDurationTicks());
        assertEquals(1, diagnostic.activeBatches());
        assertEquals(1, diagnostic.usedParallelSlots());
        assertEquals(4, diagnostic.totalParallelSlots());
    }

    @Test
    void reportsDelayedPendingOutputAtTenSecondMinimum() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var cpu = new Object();

        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpu, 1, 20);
        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 100);

        assertFalse(profiler.stall(key, cpu, 299).isPresent());
        assertEquals(200, profiler.stall(key, cpu, 300).orElseThrow().idleTicks());
    }

    @Test
    void partialOutputResetsTheDelayClock() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var cpu = new Object();

        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpu, 1, 20);
        profiler.start(key, cpu, 10, ProfileUnit.ITEM, 100);
        profiler.complete(key, cpu, 1, 650);

        assertFalse(profiler.stall(key, cpu, 849).isPresent());
        assertEquals(200, profiler.stall(key, cpu, 850).orElseThrow().idleTicks());
    }

    @Test
    void staleCapacityIsNotPresentedAsCurrent() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var cpu = new Object();

        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpu, 1, 20);
        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 100);
        profiler.updateCapacity(cpu, 4, 4, 100);

        var diagnostic = profiler.stall(key, cpu, 300).orElseThrow();

        assertEquals(0, diagnostic.usedParallelSlots());
        assertEquals(0, diagnostic.totalParallelSlots());
    }

    @Test
    void tracksWaitingTimeUntilEachOutputsFirstDispatch() {
        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var iron = new ProfileKey("minecraft:iron_plate");
        var copper = new ProfileKey("minecraft:copper_plate");
        var unrelated = new ProfileKey("minecraft:gear");

        profiler.startWaiting(cpu, List.of(iron, copper), 100);

        assertEquals(0, profiler.waitingTicks(iron, cpu, 90).orElseThrow());
        assertEquals(20, profiler.waitingTicks(iron, cpu, 120).orElseThrow());
        assertFalse(profiler.waitingTicks(null, cpu, 120).isPresent());
        assertFalse(profiler.waitingTicks(unrelated, cpu, 120).isPresent());
        assertFalse(profiler.waitingTicks(iron, new Object(), 120).isPresent());

        profiler.start(unrelated, cpu, 1, ProfileUnit.ITEM, 121);
        assertTrue(profiler.waitingTicks(iron, cpu, 121).isPresent());
        profiler.start(iron, cpu, 1, ProfileUnit.ITEM, 122);
        assertFalse(profiler.waitingTicks(iron, cpu, 122).isPresent());
        assertTrue(profiler.waitingTicks(copper, cpu, 122).isPresent());
        profiler.start(copper, cpu, 1, ProfileUnit.ITEM, 123);
        profiler.start(copper, cpu, 1, ProfileUnit.ITEM, 124);
        assertFalse(profiler.waitingTicks(copper, cpu, 124).isPresent());
    }

    @Test
    void replacesAndClearsWaitingRuntimeState() {
        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var iron = new ProfileKey("minecraft:iron_plate");
        var copper = new ProfileKey("minecraft:copper_plate");

        profiler.startWaiting(cpu, List.of(iron), 10);
        profiler.startWaiting(cpu, List.of(copper), 20);
        assertFalse(profiler.waitingTicks(iron, cpu, 30).isPresent());
        assertEquals(10, profiler.waitingTicks(copper, cpu, 30).orElseThrow());

        profiler.clearPending(cpu);
        assertFalse(profiler.waitingTicks(copper, cpu, 30).isPresent());
        profiler.startWaiting(cpu, List.of(iron), 40);
        profiler.loadSamples(List.of());
        assertFalse(profiler.waitingTicks(iron, cpu, 50).isPresent());
        profiler.startWaiting(cpu, List.of(iron), 60);
        profiler.setEnabled(false);
        assertFalse(profiler.waitingTicks(iron, cpu, 70).isPresent());
    }

    @Test
    void ignoresInvalidWaitingRegistrationsAndDropsEmptyReplacement() {        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var key = new ProfileKey("minecraft:iron_plate");

        profiler.startWaiting(null, List.of(key), 0);
        profiler.startWaiting(cpu, null, 0);
        profiler.startWaiting(cpu, Arrays.asList(null, key), 0);
        assertTrue(profiler.waitingTicks(key, cpu, 0).isPresent());
        profiler.startWaiting(cpu, Arrays.asList((ProfileKey) null), 1);
        assertFalse(profiler.waitingTicks(key, cpu, 1).isPresent());
        profiler.setEnabled(false);
        profiler.startWaiting(cpu, List.of(key), 2);
        assertFalse(profiler.waitingTicks(key, cpu, 2).isPresent());
    }

    @Test
    void rejectsStatusWithoutKeyOrKind() {
        var key = new ProfileKey("minecraft:iron_plate");
        assertThrows(NullPointerException.class, () -> new PersistedOutputStatus(null, StatusKind.DELAYED, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> new PersistedOutputStatus(key, null, 0, 0, 0));
    }

    @Test
    void ignoresNullRememberedStatusAndRestoresNullAsEmpty() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");

        profiler.rememberStatus(null);
        assertTrue(profiler.snapshotStatuses().isEmpty());
        assertFalse(profiler.rememberedStall(null).isPresent());
        assertFalse(profiler.rememberedStall(key).isPresent());
        assertFalse(profiler.rememberedWaitingTicks(null, 100).isPresent());
        assertFalse(profiler.rememberedWaitingTicks(new ProfileKey("minecraft:copper_plate"), 100).isPresent());
        assertTrue(profiler.rememberedReasons().isEmpty());
        assertFalse(profiler.hasPending(null));
        assertFalse(profiler.hasPending(key));

        profiler.rememberStatus(new PersistedOutputStatus(key, StatusKind.DELAYED, 300, 100.0, 50));
        profiler.restoreStatuses(null);
        assertTrue(profiler.snapshotStatuses().isEmpty());
    }

    @Test
    void delayedStallSurvivesReloadUntilFreshDispatch() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var cpu = new Object();

        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, cpu, 1, 20);
        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 100);
        assertEquals(1, profiler.pollNewlyDelayed(cpu, 400).size());

        // World save, then a reload clears live state but keeps the snapshot.
        var snapshot = profiler.snapshotStatuses();
        profiler.loadSamples(List.of());
        assertFalse(profiler.rememberedStall(key).isPresent());
        profiler.restoreStatuses(snapshot);

        var diagnostic = profiler.rememberedStall(key).orElseThrow();
        assertEquals(300, diagnostic.idleTicks());
        assertEquals(20.0, diagnostic.typicalDurationTicks());

        // A resumed craft reports live values again instead of the memory.
        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 1000);
        assertTrue(profiler.hasPending(key));
        assertFalse(profiler.rememberedStall(key).isPresent());
    }

    @Test
    void livePendingSuppressesRememberedStall() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var cpu = new Object();

        profiler.start(key, cpu, 1, ProfileUnit.ITEM, 0);
        profiler.rememberStatus(new PersistedOutputStatus(key, StatusKind.DELAYED, 300, 20.0, 0));
        assertFalse(profiler.rememberedStall(key).isPresent());

        profiler.rememberStatus(new PersistedOutputStatus(key, StatusKind.WAITING, 0, 0, 0));
        assertFalse(profiler.rememberedWaitingTicks(key, 100).isPresent());

        profiler.complete(key, cpu, 1, 20);
        assertFalse(profiler.rememberedStall(key).isPresent());
    }

    @Test
    void clearingOneCpuKeepsStatusesForWorkStillPendingElsewhere() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");
        var first = new Object();
        var second = new Object();

        profiler.start(key, first, 1, ProfileUnit.ITEM, 0);
        profiler.start(key, second, 1, ProfileUnit.ITEM, 0);
        profiler.rememberStatus(new PersistedOutputStatus(key, StatusKind.NO_POWER, 0, 0, 0));

        profiler.clearPending(first);
        assertEquals(CraftingBlockReason.NO_POWER, profiler.rememberedReasons().get(key));

        profiler.clearPending(second);
        assertTrue(profiler.rememberedReasons().isEmpty());
    }

    @Test
    void waitingKeysSnapshotAndRestoreForDisplay() {
        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var iron = new ProfileKey("minecraft:iron_plate");
        var copper = new ProfileKey("minecraft:copper_plate");

        profiler.startWaiting(cpu, List.of(iron, copper), 100);
        profiler.rememberStatus(new PersistedOutputStatus(copper, StatusKind.DELAYED, 300, 20.0, 100));

        var snapshot = profiler.snapshotStatuses();
        assertEquals(2, snapshot.size());

        profiler.loadSamples(List.of());
        profiler.restoreStatuses(snapshot);
        assertEquals(20, profiler.rememberedWaitingTicks(iron, 120).orElseThrow());
        // The delayed memory is not a waiting row for the same key.
        assertFalse(profiler.rememberedWaitingTicks(copper, 130).isPresent());
        // The delayed memory wins over the live waiting row for the same key.
        assertTrue(profiler.rememberedStall(copper).isPresent());

        profiler.restoreStatuses(Arrays.asList(null,
                new PersistedOutputStatus(iron, StatusKind.NO_PROVIDER, 0, 0, 0)));
        assertEquals(CraftingBlockReason.NO_PROVIDER, profiler.rememberedReasons().get(iron));
        assertFalse(profiler.rememberedStall(iron).isPresent());
    }

    @Test
    void rememberedReasonsOnlyCoverBlockReasons() {
        var profiler = new CraftProfiler(10);
        var delayed = new ProfileKey("minecraft:iron_plate");
        var waiting = new ProfileKey("minecraft:copper_plate");
        var power = new ProfileKey("minecraft:gear");
        var provider = new ProfileKey("minecraft:stick");

        profiler.rememberStatus(new PersistedOutputStatus(delayed, StatusKind.DELAYED, 1, 1.0, 0));
        profiler.rememberStatus(new PersistedOutputStatus(waiting, StatusKind.WAITING, 0, 0, 0));
        profiler.rememberStatus(new PersistedOutputStatus(power, StatusKind.NO_POWER, 0, 0, 0));
        profiler.rememberStatus(new PersistedOutputStatus(provider, StatusKind.NO_PROVIDER, 0, 0, 0));

        var reasons = profiler.rememberedReasons();
        assertEquals(2, reasons.size());
        assertEquals(CraftingBlockReason.NO_POWER, reasons.get(power));
        assertEquals(CraftingBlockReason.NO_PROVIDER, reasons.get(provider));
    }

    @Test
    void disablingDropsRememberedStatuses() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey("minecraft:iron_plate");

        profiler.rememberStatus(new PersistedOutputStatus(key, StatusKind.DELAYED, 300, 20.0, 0));
        profiler.setEnabled(false);
        assertTrue(profiler.snapshotStatuses().isEmpty());
    }

    @Test
    void scopedKeysListsWaitingAndPendingOutputs() {
        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var iron = new ProfileKey("minecraft:iron_plate");
        var gear = new ProfileKey("minecraft:gear");

        assertTrue(profiler.scopedKeys(null).isEmpty());
        assertTrue(profiler.scopedKeys(cpu).isEmpty());

        profiler.startWaiting(cpu, List.of(iron), 100);
        assertEquals(Set.of(iron), profiler.scopedKeys(cpu));

        profiler.start(gear, cpu, 1, ProfileUnit.ITEM, 101);
        assertEquals(Set.of(iron, gear), profiler.scopedKeys(cpu));

        // Dispatching the waiting key leaves it tracked as pending work.
        profiler.start(iron, cpu, 1, ProfileUnit.ITEM, 102);
        assertEquals(Set.of(iron, gear), profiler.scopedKeys(cpu));

        profiler.clearPending(cpu);
        assertTrue(profiler.scopedKeys(cpu).isEmpty());
    }

    @Test
    void scopedKeysListsPendingWithoutWaiting() {
        var profiler = new CraftProfiler(10);
        var cpu = new Object();
        var copper = new ProfileKey("minecraft:copper_plate");

        profiler.start(copper, cpu, 1, ProfileUnit.ITEM, 0);

        assertEquals(Set.of(copper), profiler.scopedKeys(cpu));
    }
}
