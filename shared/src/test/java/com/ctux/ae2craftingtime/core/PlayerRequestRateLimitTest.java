package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerRequestRateLimitTest {
    @Test
    void permitsNormalBurstAndRejectsExcessUntilNextWindow() {
        var limit = new PlayerRequestRateLimit();
        var player = UUID.randomUUID();

        assertTrue(limit.allow(player, 256, 1000));
        assertTrue(limit.allow(player, 256, 1500));
        assertFalse(limit.allow(player, 1, 1501));
        assertTrue(limit.allow(player, 1, 2000));
    }
}
