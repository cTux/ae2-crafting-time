package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClientStatsCacheTest {
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
}
