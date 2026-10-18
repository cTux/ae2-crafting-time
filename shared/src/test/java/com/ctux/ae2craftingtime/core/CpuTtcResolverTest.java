package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CpuTtcResolverTest {
    @Test
    void cpuCardTextStaysReadableWithOrWithoutBadgeBackground() {
        assertEquals(0x404040, CpuTtcLayout.textColor(false));
        assertEquals(0xE0E0E0, CpuTtcLayout.textColor(true));
    }

    @Test
    void sameNamesAndOutputsStillResolveThroughDistinctCpuIdentities() {
        record Cpu(String name, String output, long estimate) { }
        var first = new Cpu("CPU", "stone", 5);
        var second = new Cpu("CPU", "stone", 9);
        var values = CpuTtcResolver.resolve(List.of(2, 1), Map.of(first, 1, second, 2),
                List.of(first, second), cpu -> true, cpu -> Optional.of(new TtcEstimate(cpu.estimate(), true)));
        assertEquals(List.of(new CpuTtcCache.Entry(2, Optional.of(new TtcEstimate(9, true))),
                new CpuTtcCache.Entry(1, Optional.of(new TtcEstimate(5, true)))), values);
    }

    @Test
    void returnsOneOrderedValueForEveryRequestedLiveBusyCpu() {
        var entries = CpuTtcResolver.resolve(List.of(2, 1, 3, 4), Map.of("a", 1, "b", 2, "idle", 3),
                Set.of("a", "b", "idle", "foreign"), cpu -> !cpu.equals("idle"),
                cpu -> cpu.equals("a") ? Optional.of(new TtcEstimate(10, true)) : Optional.empty());

        assertEquals(List.of(2, 1, 3, 4), entries.stream().map(CpuTtcCache.Entry::serial).toList());
        assertFalse(entries.get(0).estimate().isPresent());
        assertEquals(10, entries.get(1).estimate().orElseThrow().durationMs());
        assertFalse(entries.get(2).estimate().isPresent());
        assertFalse(entries.get(3).estimate().isPresent());
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
        var onePixel = CpuTtcLayout.badge(80, 32, 54, 0.8, 1);
        assertEquals(1, onePixel.scale());
        assertEquals(54, onePixel.textWidth());
        assertEquals(58, onePixel.width());
        assertEquals(9, onePixel.height());
        assertEquals(18, onePixel.availableNameWidth());
        assertEquals(54, CpuTtcLayout.badge(80, 32, 400, 0.8, 1).textWidth());
        assertEquals(0, CpuTtcLayout.badge(7, 32, 400, 0.8, 1).textWidth());
        assertEquals(0, CpuTtcLayout.badge(80, 32, 0, 0.8, 1).textWidth());
        assertEquals(CpuTtcLayout.badge(80, 32, 20, 0.8), CpuTtcLayout.badge(80, 32, 20, 0.8, 2));
    }
}
