package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TtcColorTest {
    @Test
    void mapsFastestToGreenAndSlowestToRed() {
        assertEquals(TtcColor.GREEN, TtcColor.forMilliseconds(10, 10, 30));
        assertEquals(TtcColor.RED, TtcColor.forMilliseconds(30, 10, 30));
    }

    @Test
    void equalRangeUsesGreen() {
        assertEquals(TtcColor.GREEN, TtcColor.forMilliseconds(10, 10, 10));
    }

    @Test
    void middleValueUsesYellow() {
        assertEquals(TtcColor.YELLOW, TtcColor.forMilliseconds(20, 10, 30));
    }

    @Test
    void usesConfiguredPalette() {
        assertEquals(0x102030, TtcColor.forMilliseconds(0, 0, 0, 0x102030, 0x405060, 0x708090));
        assertEquals(0x102030, TtcColor.forMilliseconds(0, 0, 20, 0x102030, 0x405060, 0x708090));
        assertEquals(0x405060, TtcColor.forMilliseconds(10, 0, 20, 0x102030, 0x405060, 0x708090));
        assertEquals(0x708090, TtcColor.forMilliseconds(20, 0, 20, 0x102030, 0x405060, 0x708090));
    }
}
