package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TtcColorTest {
    @Test
    void mapsFastestToDarkGreenAndSlowestToDarkRed() {
        assertEquals(TtcColor.DARK_GREEN, TtcColor.forSeconds(10, 10, 30));
        assertEquals(TtcColor.DARK_RED, TtcColor.forSeconds(30, 10, 30));
    }

    @Test
    void equalRangeUsesDarkGreen() {
        assertEquals(TtcColor.DARK_GREEN, TtcColor.forSeconds(10, 10, 10));
    }

    @Test
    void middleValueInterpolatesBetweenEndpoints() {
        var color = TtcColor.forSeconds(20, 10, 30);

        assertNotEquals(TtcColor.DARK_GREEN, color);
        assertNotEquals(TtcColor.DARK_RED, color);
    }
}
