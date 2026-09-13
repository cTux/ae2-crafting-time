package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RecurrentCampaignTest {
    @Test void connectedAddonRouteRequiresTheExactPairAndReportedQuantity() {
        assertFalse(RecurrentCampaign.addonRoute(false, false));
        assertTrue(RecurrentCampaign.addonRoute(true, true));
        assertFalse(RecurrentCampaign.addonRoute(true, false));
        assertFalse(RecurrentCampaign.addonRoute(false, true));
        assertEquals("reported-100", RecurrentCampaign.plan(true));
        assertEquals("ordinary", RecurrentCampaign.plan(false));
        assertEquals(100, RecurrentCampaign.REQUESTED_AMOUNT);
    }

    @Test void onlyTheExactLiveGridCanAcknowledgeTheObservedPlan() {
        var grid = new Object();
        assertTrue(RecurrentCampaign.sameGrid(grid, grid));
        assertFalse(RecurrentCampaign.sameGrid(grid, new Object()));
        assertFalse(RecurrentCampaign.sameGrid(grid, null));
        assertFalse(RecurrentCampaign.sameGrid(null, null));
    }
    @Test void oneClientCampaignSerializesEveryPhase() {
        for (var phase : java.util.List.of("initial", "grid", "swap", "complete")) {
            var action = phase.equals("swap") ? "swapped" : phase.equals("complete") ? "captured" : phase;
            assertEquals("alpha", RecurrentCampaign.turn("", phase));
            assertTrue(RecurrentCampaign.allows(action, phase, false));
            assertFalse(RecurrentCampaign.allows("rejoined", phase, true));
            assertEquals("", RecurrentCampaign.turn(action, phase));
        }
        assertEquals("alpha", RecurrentCampaign.turn("swapped", "reconnect"));
    }

    @Test void reconnectRequiresARealDisconnectAndNoOldPhaseActionIsAccepted() {
        assertEquals("initial", RecurrentCampaign.phase(false, false, false, false));
        assertEquals("grid", RecurrentCampaign.phase(true, false, false, false));
        assertEquals("swap", RecurrentCampaign.phase(true, true, false, false));
        assertEquals("reconnect", RecurrentCampaign.phase(true, true, true, false));
        assertEquals("complete", RecurrentCampaign.phase(true, true, true, true));
        assertFalse(RecurrentCampaign.allows("initial", "grid", false));
        assertFalse(RecurrentCampaign.allows("rejoined", "reconnect", false));
        assertTrue(RecurrentCampaign.allows("rejoined", "reconnect", true));
        assertFalse(RecurrentCampaign.allows("swapped", "reconnect", true));
        assertFalse(RecurrentCampaign.allows("initial", "swap", true));
        assertFalse(RecurrentCampaign.allows("swapped", "complete", true));
        assertFalse(RecurrentCampaign.allows("captured", "unknown", true));
        assertThrows(IllegalArgumentException.class, () -> RecurrentCampaign.turn("", "unknown"));
    }

    @Test void capturedRoleStillReceivesAcknowledgementUntilItDisconnects() {
        assertTrue(RecurrentCampaign.publish(true, false, false));
        assertTrue(RecurrentCampaign.publish(true, true, true));
        assertFalse(RecurrentCampaign.publish(false, true, true));
        assertThrows(IllegalStateException.class, () -> RecurrentCampaign.publish(false, true, false));
        assertThrows(IllegalStateException.class, () -> RecurrentCampaign.publish(false, false, true));
    }
}
