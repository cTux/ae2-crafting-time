package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestCooldownTest {
    @Test
    void isolatesNetworksAndOutputsWithoutExtendingRejectedCooldowns() {
        var cooldown = new RequestCooldown(1000);
        var first = new ProfileKey("grid-a", "minecraft:stone");
        var otherGrid = new ProfileKey("grid-b", "minecraft:stone");
        var otherOutput = new ProfileKey("grid-a", "minecraft:glass");

        assertTrue(cooldown.markIfAllowed(first, 1000));
        assertTrue(cooldown.markIfAllowed(otherGrid, 1500));
        assertTrue(cooldown.markIfAllowed(otherOutput, 1500));
        assertFalse(cooldown.markIfAllowed(first, 1999));
        assertTrue(cooldown.markIfAllowed(first, 2000));
        assertFalse(cooldown.markIfAllowed(otherGrid, 2000));
        assertFalse(cooldown.markIfAllowed(otherOutput, 2000));
        cooldown.clear();
        assertTrue(cooldown.markIfAllowed(first, 2001));
        assertTrue(cooldown.markIfAllowed(otherGrid, 2001));
        assertTrue(cooldown.markIfAllowed(otherOutput, 2001));
    }
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
