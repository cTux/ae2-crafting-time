package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class NeoEcoStatusRowsTest {
    @Test
    void countsActiveAndScheduledWithoutOverflow() {
        var zero = BigInteger.ZERO;
        var one = BigInteger.ONE;
        var max = BigInteger.valueOf(Long.MAX_VALUE);
        assertEquals(0, NeoEcoStatusRows.remaining(zero, zero));
        assertEquals(7, NeoEcoStatusRows.remaining(BigInteger.valueOf(3), BigInteger.valueOf(4)));
        assertEquals(Long.MAX_VALUE, NeoEcoStatusRows.remaining(max.subtract(one), one));
        assertEquals(0, NeoEcoStatusRows.remaining(max, one));
        assertEquals(0, NeoEcoStatusRows.remaining(one.negate(), max));
        assertEquals(0, NeoEcoStatusRows.remaining(max, one.negate()));
    }

    @Test
    void rejectsInvalidWireDurations() {
        assertEquals(new TtcEstimate(550, true), NeoEcoStatusRows.decode(550, true));
        assertEquals(new TtcEstimate(0, false), NeoEcoStatusRows.decode(0, false));
        assertNull(NeoEcoStatusRows.decode(-1, false));
        assertNull(NeoEcoStatusRows.decode(Double.NaN, true));
        assertNull(NeoEcoStatusRows.decode(Double.POSITIVE_INFINITY, true));
    }

    @Test
    void fitsNativeCellAtTheWidthAndHeightBoundaries() {
        assertTrue(NeoEcoStatusRows.fits(3, 9, 88));
        assertFalse(NeoEcoStatusRows.fits(3, 9, 89));
        assertFalse(NeoEcoStatusRows.fits(4, 9, 88));
        assertFalse(NeoEcoStatusRows.fits(3, 11, 30));
    }
}
