package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoredVariantControlTest {
    @Test void connectedResultsRequireEveryLifecycleCheck() {
        var integrated = DriverResult.requiredChecks("stored-variant-plan", false);
        var connected = DriverResult.requiredChecks("stored-variant-plan", true);
        assertEquals(integrated.size() + 4, connected.size());
        assertTrue(connected.containsAll(java.util.List.of("menu-cancel", "network-switch", "native-replan", "reconnected-fresh")));
        assertFalse(integrated.contains("menu-cancel"));
        var previous = System.getProperty("ae2craftingtime.test.connectedDedicated");
        try {
            System.setProperty("ae2craftingtime.test.connectedDedicated", "true");
            var checks = new java.util.LinkedHashMap<String, Boolean>();
            integrated.forEach(key -> checks.put(key, true));
            assertThrows(IllegalArgumentException.class, () -> new DriverResult(1, true, "driver", "target",
                    "compatible", "stored-variant-plan", "PASS", "en_us", java.util.Map.of(), null,
                    checks, java.util.List.of(), null));
        } finally {
            if (previous == null) System.clearProperty("ae2craftingtime.test.connectedDedicated");
            else System.setProperty("ae2craftingtime.test.connectedDedicated", previous);
        }
    }
    @Test void commandsRequireTheSameCampaignPlayerMenuAndRevision() {
        var old = System.getProperty("ae2craftingtime.test.campaign");
        try {
            System.setProperty("ae2craftingtime.test.campaign", "variant-test");
            var player = UUID.randomUUID();
            var exact = new StoredVariantControl.Command("variant-test", 2, "step-1",
                    player.toString(), 12, 3);
            assertTrue(exact.matches(player, 12, 3, 1));
            assertFalse(exact.matches(player, 12, 3, 2));
            assertFalse(exact.matches(UUID.randomUUID(), 12, 3, 1));
            assertFalse(exact.matches(player, 13, 3, 1));
            assertFalse(exact.matches(player, 12, 4, 1));
            assertFalse(new StoredVariantControl.Command("old", 2, "step-1", player.toString(), 12, 3)
                    .matches(player, 12, 3, 1));
            assertFalse(new StoredVariantControl.Command("variant-test", 2, "step-1", player.toString(), 12, 0)
                    .matches(player, 12, 0, 1));
        } finally {
            if (old == null) System.clearProperty("ae2craftingtime.test.campaign");
            else System.setProperty("ae2craftingtime.test.campaign", old);
        }
    }
}
