package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void middleValueUsesDarkYellow() {
        assertEquals(TtcColor.DARK_YELLOW, TtcColor.forSeconds(20, 10, 30));
    }
}
