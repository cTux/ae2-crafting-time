package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerMessageRateLimitTest {
    @Test
    void allowsOneMessagePerPlayerEveryTwoSeconds() {
        var limiter = new PlayerMessageRateLimit();
        var player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var other = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertTrue(limiter.allow(player, 1000));
        assertFalse(limiter.allow(player, 2999));
        assertTrue(limiter.allow(other, 2999));
        assertTrue(limiter.allow(player, 3000));
    }
}
