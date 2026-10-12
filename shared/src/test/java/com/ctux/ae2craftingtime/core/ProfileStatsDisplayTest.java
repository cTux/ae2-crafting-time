package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileStatsDisplayTest {
    private static ProfileStats stats(List<Long> ticks, List<Long> amounts) {
        return new ProfileStats(1, 1, 1, 20, 1, ProfileUnit.ITEM, true, 1, 4, ticks, amounts);
    }

    @Test
    void rateConversionAndSamplePresenceMatchDisplayedMilliseconds() {
        assertEquals(0.02, stats(List.of(), List.of()).amountPerMillisecond());
        assertFalse(stats(List.of(), List.of()).hasSampleTimes());
        assertFalse(stats(List.of(1L), List.of()).hasSampleTimes());
        assertFalse(stats(List.of(0L), List.of(1L)).hasSampleTimes());
        assertFalse(stats(List.of(1L), List.of(0L)).hasSampleTimes());
        assertTrue(stats(List.of(0L, 1L), List.of(1L, 1L)).hasSampleTimes());
    }
}
