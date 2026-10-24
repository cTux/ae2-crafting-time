package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AelisPlanTtcTest {
    @Test void cellsMatchUpstreamCenteringAndRowStates() {
        assertEquals(32, AelisPlanTtc.textLeft(10, 64, 20));
        assertEquals(10, AelisPlanTtc.textLeft(10, 64, 100));
        assertTrue(AelisPlanTtc.hover(14, 108, 406, 108));
        assertTrue(AelisPlanTtc.hover(338, 129, 406, 108));
        assertFalse(AelisPlanTtc.hover(13, 108, 406, 108));
        assertFalse(AelisPlanTtc.hover(339, 108, 406, 108));
        assertFalse(AelisPlanTtc.hover(14, 107, 406, 108));
        assertFalse(AelisPlanTtc.hover(14, 130, 406, 108));
        assertEquals(0xFFF1D4D9, AelisPlanTtc.rowBackground(true, true, 0));
        assertEquals(0xFFE7C4CB, AelisPlanTtc.rowBackground(true, false, 0));
        assertEquals(0xFFDCD5E6, AelisPlanTtc.rowBackground(false, true, 0));
        assertEquals(0xFFC6C5D4, AelisPlanTtc.rowBackground(false, false, 0));
        assertEquals(0xFFBDBCCD, AelisPlanTtc.rowBackground(false, false, 1));
    }
    @Test void reservesSpaceOnlyWithBothHooksAndEnoughRoom() {
        AelisPlanTtc.screenReady(false); AelisPlanTtc.layoutReady(false);
        assertFalse(AelisPlanTtc.ready());
        AelisPlanTtc.screenReady(true);
        assertFalse(AelisPlanTtc.ready());
        AelisPlanTtc.layoutReady(true);
        assertTrue(AelisPlanTtc.ready());
        AelisPlanTtc.screenReady(false);
        assertFalse(AelisPlanTtc.ready());
        AelisPlanTtc.layoutReady(false);
        assertEquals(360, AelisPlanTtc.panelCap(640, false));
        assertEquals(360, AelisPlanTtc.panelCap(381, true));
        assertEquals(430, AelisPlanTtc.panelCap(382, true));
        assertEquals(0, AelisPlanTtc.reservation(360));
        assertEquals(70, AelisPlanTtc.reservation(370));
    }

    @Test void normalizesBeforeNarrowingWithoutOverflow() {
        assertTrue(AelisPlanTtc.normalized("minecraft:stone", 1, BigInteger.ZERO).isEmpty());
        assertTrue(AelisPlanTtc.normalized("minecraft:stone", 1, BigInteger.valueOf(-1)).isEmpty());
        var max = BigInteger.valueOf(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, AelisPlanTtc.normalized("minecraft:stone", 1, max).orElseThrow());
        assertTrue(AelisPlanTtc.normalized("minecraft:stone", 1, max.add(BigInteger.ONE)).isEmpty());
        assertEquals(1000, AelisPlanTtc.normalized("minecraft:water", 81_000,
                BigInteger.valueOf(81_000)).orElseThrow());
        assertTrue(AelisPlanTtc.normalized("minecraft:water", 81_000, BigInteger.ONE).isEmpty());
        assertTrue(AelisPlanTtc.normalized("minecraft:water", 2, max).isEmpty());
        assertEquals(Long.MAX_VALUE, AelisPlanTtc.normalized("botania:mana", 81_000, max).orElseThrow());
        assertEquals(1_000_000_000_000_000_000L, AelisPlanTtc.normalized("minecraft:water", 81_000,
                new BigInteger("81000000000000000000")).orElseThrow());
    }

    @Test void partialPlansCannotBecomeConfidentTotals() {
        var estimates = List.of(Optional.of(new TtcEstimate(1000, true)));
        assertTrue(AelisPlanTtc.total(estimates, false).orElseThrow().reliable());
        assertFalse(AelisPlanTtc.total(estimates, true).orElseThrow().reliable());
        assertFalse(AelisPlanTtc.total(List.of(estimates.get(0), Optional.empty()), false).orElseThrow().reliable());
        assertTrue(AelisPlanTtc.total(List.of(), false).isEmpty());
    }
}
