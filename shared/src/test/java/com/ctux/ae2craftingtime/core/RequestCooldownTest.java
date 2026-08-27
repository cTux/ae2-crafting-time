package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestCooldownTest {
    @Test
    void allowsRefreshAfterCooldown() {
        var cooldown = new RequestCooldown(1000);
        var key = new ProfileKey("minecraft:iron_plate");

        assertTrue(cooldown.markIfAllowed(key, 10_000));
        assertFalse(cooldown.markIfAllowed(key, 10_999));
        assertTrue(cooldown.markIfAllowed(key, 11_000));

        cooldown.clear();
        assertTrue(cooldown.markIfAllowed(key, 11_001));
    }
}
