package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CpuTtcRateLimitTest {
    @Test
    void enforcesBothBudgetsAndCountsEmptyPackets() {
        var limit = new CpuTtcRateLimit();
        var player = UUID.randomUUID();
        assertTrue(limit.allow(player, 32, 0));
        assertTrue(limit.allow(player, 32, 0));
        assertTrue(limit.allow(player, 32, 0));
        assertTrue(limit.allow(player, 32, 0));
        assertFalse(limit.allow(player, 0, 0));
        assertTrue(limit.allow(player, 0, 1_000));
        assertFalse(limit.allow(player, -1, 1_000));
        assertFalse(limit.allow(player, 33, 1_000));
    }

    @Test
    void cleanupDropsOnlyTheRequestedPlayersWindow() {
        var limit = new CpuTtcRateLimit();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        for (var ignored : new int[4]) {
            assertTrue(limit.allow(first, 0, 0));
            assertTrue(limit.allow(second, 0, 0));
        }
        limit.clear(first);
        assertTrue(limit.allow(first, 0, 0));
        assertFalse(limit.allow(second, 0, 0));
        limit.clear();
        assertTrue(limit.allow(second, 0, 0));
    }

    @Test
    void serialBudgetIsIndependentFromThePacketBudget() {
        var limit = new CpuTtcRateLimit(4, 40);
        var player = UUID.randomUUID();
        assertTrue(limit.allow(player, 32, 0));
        assertFalse(limit.allow(player, 9, 0));
        assertTrue(limit.allow(player, 8, 0));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcRateLimit(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcRateLimit(1, -1));
    }
}
