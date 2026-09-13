package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecurrentCampaignTest {
    @Test void oneAndTwoRoleCampaignsSerializeEveryPhase() {
        for (var roles : List.of(List.of("alpha"), List.of("alpha", "beta"))) {
            var actions = new HashMap<String, String>();
            for (var phase : List.of("initial", "swap", "complete")) {
                var action = phase.equals("swap") ? "swapped" : phase.equals("complete") ? "captured" : "initial";
                for (var role : roles) {
                    var turn = RecurrentCampaign.turn(roles, actions, phase);
                    assertEquals(role, turn);
                    assertTrue(RecurrentCampaign.allows(role, action, phase, turn, false));
                    assertFalse(RecurrentCampaign.allows(role, "rejoined", phase, turn, true));
                    assertFalse(RecurrentCampaign.allows("unrelated", action, phase, turn, true));
                    actions.put(role, action);
                }
                assertEquals("", RecurrentCampaign.turn(roles, actions, phase));
            }
            assertEquals("alpha", RecurrentCampaign.turn(roles, actions, "reconnect"));
        }
    }

    @Test void reconnectRequiresARealDisconnectAndNoOldPhaseActionIsAccepted() {
        assertEquals("initial", RecurrentCampaign.phase(false, false, false));
        assertEquals("swap", RecurrentCampaign.phase(true, false, false));
        assertEquals("reconnect", RecurrentCampaign.phase(true, true, false));
        assertEquals("complete", RecurrentCampaign.phase(true, true, true));
        assertFalse(RecurrentCampaign.allows("alpha", "rejoined", "reconnect", "alpha", false));
        assertTrue(RecurrentCampaign.allows("alpha", "rejoined", "reconnect", "alpha", true));
        assertFalse(RecurrentCampaign.allows("alpha", "swapped", "reconnect", "alpha", true));
        assertFalse(RecurrentCampaign.allows("alpha", "initial", "swap", "alpha", true));
        assertFalse(RecurrentCampaign.allows("alpha", "swapped", "complete", "alpha", true));
        assertFalse(RecurrentCampaign.allows("alpha", "captured", "unknown", "alpha", true));
        assertThrows(IllegalArgumentException.class, () -> RecurrentCampaign.turn(List.of("alpha"), java.util.Map.of(), "unknown"));
    }

    @Test void capturedRoleStillReceivesAcknowledgementUntilItDisconnects() {
        assertTrue(RecurrentCampaign.publish(true, false, false));
        assertTrue(RecurrentCampaign.publish(true, true, true));
        assertFalse(RecurrentCampaign.publish(false, true, true));
        assertThrows(IllegalStateException.class, () -> RecurrentCampaign.publish(false, true, false));
        assertThrows(IllegalStateException.class, () -> RecurrentCampaign.publish(false, false, true));
    }
}
