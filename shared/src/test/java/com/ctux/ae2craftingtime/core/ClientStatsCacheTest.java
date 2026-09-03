package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class ClientStatsCacheTest {
    @Test
    void missingProvidersReplaceWithoutStatsAndClearWithCpuState() {
        var cache = new ClientStatsCache();
        var iron = new ProfileKey("minecraft:iron_ingot");
        var gold = new ProfileKey("minecraft:gold_ingot");
        assertFalse(cache.missingProvider(iron, 7));
        cache.replaceMissingProviders(List.of(iron, gold), java.util.Set.of(iron, gold), 7);
        assertTrue(cache.missingProvider(iron, 7));
        assertFalse(cache.missingProvider(iron, 8));
        assertTrue(cache.get(iron).isEmpty());
        cache.replaceMissingProviders(List.of(iron), java.util.Set.of(), 7);
        assertFalse(cache.missingProvider(iron, 7));
        assertTrue(cache.missingProvider(gold, 7));
        cache.remove(gold);
        assertFalse(cache.missingProvider(gold, 7));
        cache.replaceMissingProviders(List.of(iron), java.util.Set.of(iron), 7);
        cache.clearCpuState();
        assertFalse(cache.missingProvider(iron, 7));
        cache.replaceMissingProviders(List.of(iron), java.util.Set.of(iron), 7);
        cache.clear();
        assertFalse(cache.missingProvider(iron, 7));
        cache.replaceMissingProviders(List.of(iron), java.util.Set.of(iron), 7);
        cache.replaceMissingProviders(List.of(gold), java.util.Set.of(gold), 8);
        assertFalse(cache.missingProvider(iron, 8));
        assertTrue(cache.missingProvider(gold, 8));
        assertFalse(cache.missingProvider(gold, 7));
    }

    @Test
    void replacesStatsByOutputKey() {
        var cache = new ClientStatsCache();
        var key = new ProfileKey("minecraft:iron_plate");

        cache.replace(List.of(new StatsEntry(key, new ProfileStats(1, 20, 0.05, 1, 20, ProfileUnit.ITEM))));
        cache.replace(List.of(new StatsEntry(key, new ProfileStats(2, 10, 0.1, 2, 8, ProfileUnit.ITEM))));

        var stats = cache.get(key).orElseThrow();
        assertEquals(2, stats.sampleCount());
        assertEquals(10, stats.averageDurationTicks());
    }

    @Test
    void missingStatsStayEmpty() {
        var cache = new ClientStatsCache();

        assertFalse(cache.get(new ProfileKey("minecraft:copper_plate")).isPresent());
    }

    @Test
    void clearDropsCachedStats() {
        var cache = new ClientStatsCache();
        var key = new ProfileKey("minecraft:iron_plate");

        cache.replace(List.of(new StatsEntry(key, new ProfileStats(1, 20, 0.05, 1, 20, ProfileUnit.ITEM))));
        cache.clear();

        assertFalse(cache.get(key).isPresent());
    }

    @Test
    void removeDropsOneCachedStat() {
        var cache = new ClientStatsCache();
        var iron = new ProfileKey("minecraft:iron_plate");
        var copper = new ProfileKey("minecraft:copper_plate");

        cache.replace(List.of(
                new StatsEntry(iron, new ProfileStats(1, 20, 0.05, 1, 20, ProfileUnit.ITEM)),
                new StatsEntry(copper, new ProfileStats(1, 10, 0.1, 2, 10, ProfileUnit.ITEM))));
        cache.remove(iron);

        assertFalse(cache.get(iron).isPresent());
        assertEquals(1, cache.get(copper).orElseThrow().sampleCount());
    }

    @Test
    void replaceDropsRequestedStatsMissingFromResponse() {
        var cache = new ClientStatsCache();
        var iron = new ProfileKey("minecraft:iron_plate");
        var copper = new ProfileKey("minecraft:copper_plate");

        cache.replace(List.of(
                new StatsEntry(iron, new ProfileStats(1, 20, 0.05, 1, 20, ProfileUnit.ITEM)),
                new StatsEntry(copper, new ProfileStats(1, 10, 0.1, 2, 10, ProfileUnit.ITEM))));
        cache.replace(List.of(iron), List.of());

        assertFalse(cache.get(iron).isPresent());
        assertEquals(1, cache.get(copper).orElseThrow().sampleCount());
    }

    @Test
    void exposesOptionalDiagnostics() {
        var key = new ProfileKey("minecraft:iron_plate");
        var accuracy = new TtcAccuracyStats(1, 1, 1, 0, 0, 1, 10, 10, 10, 1, 1);
        var stall = new StallDiagnostic(600, 20, 1, 0, 0);
        var cache = new ClientStatsCache();

        cache.replace(List.of(new StatsEntry(key,
                new ProfileStats(1, 20, 0.05, 1, 20, ProfileUnit.ITEM), Optional.of(accuracy), Optional.of(stall))));

        assertEquals(accuracy, cache.accuracy(key).orElseThrow());
        assertEquals(stall, cache.stall(key).orElseThrow());
        assertFalse(cache.accuracy(new ProfileKey("minecraft:missing")).isPresent());
        assertFalse(cache.stall(new ProfileKey("minecraft:missing")).isPresent());
    }

    @Test
    void waitingValuesReplaceOnlyRequestedKeysAndClearWithTheCache() {
        var cache = new ClientStatsCache();
        var iron = new ProfileKey("minecraft:iron_plate");
        var copper = new ProfileKey("minecraft:copper_plate");

        cache.replaceWaiting(List.of(iron, copper), Map.of(iron, 20L, copper, 40L));
        cache.replaceWaiting(List.of(iron), Map.of());

        assertFalse(cache.waitingTicks(iron).isPresent());
        assertEquals(40L, cache.waitingTicks(copper).orElseThrow());
        cache.remove(copper);
        assertFalse(cache.waitingTicks(copper).isPresent());
        cache.replaceWaiting(List.of(iron), Map.of(iron, 60L));
        cache.clearCpuState();
        assertFalse(cache.waitingTicks(iron).isPresent());
        cache.replaceWaiting(List.of(iron), Map.of(iron, 80L));
        cache.clear();
        assertFalse(cache.waitingTicks(iron).isPresent());
    }
}
