package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CpuTtcResolverTest {
    @Test
    void sameNamesAndOutputsStillResolveThroughDistinctCpuIdentities() {
        record Cpu(String name, String output, long seconds) { }
        var first = new Cpu("CPU", "stone", 5);
        var second = new Cpu("CPU", "stone", 9);
        var values = CpuTtcResolver.resolve(List.of(2, 1), Map.of(first, 1, second, 2),
                List.of(first, second), cpu -> true, cpu -> OptionalLong.of(cpu.seconds()));
        assertEquals(List.of(new CpuTtcCache.Entry(2, OptionalLong.of(9)),
                new CpuTtcCache.Entry(1, OptionalLong.of(5))), values);
    }

    @Test
    void returnsOneOrderedValueForEveryRequestedLiveBusyCpu() {
        var entries = CpuTtcResolver.resolve(List.of(2, 1, 3, 4), Map.of("a", 1, "b", 2, "idle", 3),
                Set.of("a", "b", "idle", "foreign"), cpu -> !cpu.equals("idle"),
                cpu -> cpu.equals("a") ? OptionalLong.of(10) : OptionalLong.empty());

        assertEquals(List.of(2, 1, 3, 4), entries.stream().map(CpuTtcCache.Entry::serial).toList());
        assertFalse(entries.get(0).seconds().isPresent());
        assertEquals(10, entries.get(1).seconds().orElseThrow());
        assertFalse(entries.get(2).seconds().isPresent());
        assertFalse(entries.get(3).seconds().isPresent());
    }

    @Test
    void layoutReservesTheBadgeAndRejectsInvalidGeometry() {
        assertEquals(new CpuTtcLayout.Badge(0.6, 12, 16, 8, 71), CpuTtcLayout.badge(80, 32, 20, 0.8));
        var maximum = CpuTtcLayout.badge(80, 32, 400, 0.8);
        assertEquals(0.18, maximum.scale(), 0.0001);
        assertEquals(72, maximum.textWidth());
        assertEquals(0, maximum.availableNameWidth());
        var narrow = CpuTtcLayout.badge(10, 18, 400, 0.8);
        assertEquals(0, narrow.availableNameWidth());
        assertEquals(2, narrow.textWidth());
        assertEquals(0, CpuTtcLayout.badge(7, 15, 20, 0.8).availableNameWidth());
        assertEquals(new CpuTtcLayout.Badge(0.6, 0, 4, 8, 86), CpuTtcLayout.badge(80, 32, 0, 0.8));
        assertThrows(IllegalArgumentException.class, () -> CpuTtcLayout.badge(-1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> CpuTtcLayout.badge(1, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> CpuTtcLayout.badge(1, 1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> CpuTtcLayout.badge(1, 1, 1, 0));
    }
}
