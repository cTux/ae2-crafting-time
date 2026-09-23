package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class FeatureOptionsTest {
    @Test
    void everySwitchHasOneStableKeyAndStartsEnabled() {
        var keys = new HashSet<String>();
        var client = new FeatureOptions(OptionFeature.Owner.CLIENT);
        var server = new FeatureOptions(OptionFeature.Owner.SERVER);
        for (var feature : OptionFeature.values()) {
            assertTrue(keys.add(feature.key()));
            assertTrue(feature.group() != null);
            assertTrue((feature.owner() == OptionFeature.Owner.CLIENT ? client : server).enabled(feature));
        }
        assertTrue(client.disabled().isEmpty());
        assertTrue(server.disabled().isEmpty());
    }

    @Test
    void switchesAreIndependentAndWorkingCopiesDoNotShareEdits() {
        var client = new FeatureOptions(OptionFeature.Owner.CLIENT);
        client.setEnabled(OptionFeature.RECEIVE_CRAFT_WARNINGS, false);
        assertFalse(client.enabled(OptionFeature.RECEIVE_CRAFT_WARNINGS));
        assertTrue(client.enabled(OptionFeature.DELAYED_STATUS));
        assertEquals(1, client.disabled().size());

        var copy = client.copy();
        copy.setEnabled(OptionFeature.RECEIVE_CRAFT_WARNINGS, true);
        assertTrue(copy.enabled(OptionFeature.RECEIVE_CRAFT_WARNINGS));
        assertFalse(client.enabled(OptionFeature.RECEIVE_CRAFT_WARNINGS));
        client.reset();
        assertTrue(client.disabled().isEmpty());
    }

    @Test
    void environmentBoundaryRejectsWrongOrMissingSwitches() {
        var server = new FeatureOptions(OptionFeature.Owner.SERVER);
        assertThrows(IllegalArgumentException.class, () -> server.setEnabled(OptionFeature.PLAN_ROWS, false));
        assertThrows(IllegalArgumentException.class, () -> server.enabled(OptionFeature.PLAN_ROWS));
        assertThrows(NullPointerException.class, () -> server.enabled(null));
        assertThrows(NullPointerException.class, () -> new FeatureOptions(null));
        server.setEnabled(OptionFeature.NOTIFY_ON_DELAYED, false);
        assertFalse(server.enabled(OptionFeature.NOTIFY_ON_DELAYED));
    }
}
