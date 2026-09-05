package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Manual rainbow locating covers every resolvable active crafting item,
 * including normal TTC rows, not only cached delayed rows. The server
 * validates resolvability and answers edge-only, so red never changes.
 */
class ProviderManualLocateTest {
    @Test
    void shouldLocateAllowsNormalAndDelayedItems() {
        ClientStats.CACHE.clear();
        assertTrue(ProviderLocateClick.shouldLocate("minecraft:iron_ingot"));
        assertTrue(ProviderLocateClick.shouldLocate("minecraft:copper_plate"));
    }

    @Test
    void shouldLocateRejectsBlankOutputIds() {
        assertFalse(ProviderLocateClick.shouldLocate(null));
        assertFalse(ProviderLocateClick.shouldLocate(""));
        assertFalse(ProviderLocateClick.shouldLocate("   "));
    }
}
