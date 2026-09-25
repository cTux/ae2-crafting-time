package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OptionsModelTest {
    @Test
    void clientDefaultsEditsCopiesAndResets() {
        var config = new ClientConfig();
        for (var color : ClientConfig.Color.values()) {
            assertEquals(color.defaultRgb(), config.color(color));
        }
        assertEquals(176, config.badgeOpacity());
        assertEquals(2, config.planSort());
        assertEquals(2, config.statusSort());
        assertFalse(config.features().enabled(OptionFeature.COMPACT_STATUS_AMOUNTS));

        config.features().setEnabled(OptionFeature.PLAN_ROWS, false);
        config.features().setEnabled(OptionFeature.COMPACT_STATUS_AMOUNTS, true);
        config.setColor(ClientConfig.Color.FAST, 0);
        config.setBadgeOpacity(255);
        config.setPlanSort(0);
        config.setStatusSort(1);
        var copy = config.copy();
        assertFalse(copy.features().enabled(OptionFeature.PLAN_ROWS));
        assertTrue(copy.features().enabled(OptionFeature.COMPACT_STATUS_AMOUNTS));
        assertEquals(0, copy.color(ClientConfig.Color.FAST));
        assertEquals(255, copy.badgeOpacity());
        assertEquals(0, copy.planSort());
        assertEquals(1, copy.statusSort());

        copy.reset();
        assertTrue(copy.features().enabled(OptionFeature.PLAN_ROWS));
        assertFalse(copy.features().enabled(OptionFeature.COMPACT_STATUS_AMOUNTS));
        assertEquals(ClientConfig.Color.FAST.defaultRgb(), copy.color(ClientConfig.Color.FAST));
        assertEquals(176, copy.badgeOpacity());
        assertEquals(2, copy.planSort());
        assertEquals(2, copy.statusSort());
        assertFalse(config.features().enabled(OptionFeature.PLAN_ROWS));
        assertTrue(config.features().enabled(OptionFeature.COMPACT_STATUS_AMOUNTS));
    }

    @Test
    void clientRejectsInvalidValues() {
        var config = new ClientConfig();
        assertThrows(NullPointerException.class, () -> config.color(null));
        assertThrows(NullPointerException.class, () -> config.setColor(null, 0));
        assertThrows(IllegalArgumentException.class, () -> config.setColor(ClientConfig.Color.FAST, -1));
        assertThrows(IllegalArgumentException.class, () -> config.setColor(ClientConfig.Color.FAST, 0x1000000));
        assertThrows(IllegalArgumentException.class, () -> config.setBadgeOpacity(-1));
        assertThrows(IllegalArgumentException.class, () -> config.setBadgeOpacity(256));
        assertThrows(IllegalArgumentException.class, () -> config.setPlanSort(-1));
        assertThrows(IllegalArgumentException.class, () -> config.setStatusSort(3));
        config.setColor(ClientConfig.Color.BADGE, 0xFFFFFF);
        config.setBadgeOpacity(0);
        config.setPlanSort(2);
        config.setStatusSort(0);
        assertEquals(0xFFFFFF, config.color(ClientConfig.Color.BADGE));
    }

    @Test
    void serverDefaultsEditsCopiesAndResets() {
        var config = new ServerConfig();
        assertEquals(10, config.maxSamples());
        assertEquals(4.0, config.outlierMultiplier());
        assertEquals(10, config.minimumNoProgressSeconds());
        assertEquals(2.0, config.typicalDurationMultiplier());
        config.features().setEnabled(OptionFeature.NOTIFY_ON_DELAYED, false);
        config.setMaxSamples(100);
        config.setOutlierMultiplier(1.0);
        config.setMinimumNoProgressSeconds(3600);
        config.setTypicalDurationMultiplier(1000.0);
        var copy = config.copy();
        assertFalse(copy.features().enabled(OptionFeature.NOTIFY_ON_DELAYED));
        assertEquals(100, copy.maxSamples());
        assertEquals(1.0, copy.outlierMultiplier());
        assertEquals(3600, copy.minimumNoProgressSeconds());
        assertEquals(1000.0, copy.typicalDurationMultiplier());
        copy.reset();
        assertTrue(copy.features().enabled(OptionFeature.NOTIFY_ON_DELAYED));
        assertEquals(10, copy.maxSamples());
        assertEquals(4.0, copy.outlierMultiplier());
        assertEquals(10, copy.minimumNoProgressSeconds());
        assertEquals(2.0, copy.typicalDurationMultiplier());
        assertFalse(config.features().enabled(OptionFeature.NOTIFY_ON_DELAYED));
    }

    @Test
    void serverRejectsEveryInvalidBoundary() {
        var config = new ServerConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setMaxSamples(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxSamples(101));
        assertThrows(IllegalArgumentException.class, () -> config.setMinimumNoProgressSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMinimumNoProgressSeconds(3601));
        assertThrows(IllegalArgumentException.class, () -> config.setOutlierMultiplier(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> config.setOutlierMultiplier(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> config.setOutlierMultiplier(0.9));
        assertThrows(IllegalArgumentException.class, () -> config.setOutlierMultiplier(1000.1));
        assertThrows(IllegalArgumentException.class, () -> config.setTypicalDurationMultiplier(0.9));
        assertThrows(IllegalArgumentException.class, () -> config.setTypicalDurationMultiplier(1000.1));
        config.setMaxSamples(1);
        config.setMinimumNoProgressSeconds(1);
        config.setTypicalDurationMultiplier(1.0);
        assertEquals(1, config.maxSamples());
    }
}
