package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TtcChatEstimateTest {
    @Test
    void readsOnlyBoundedInvariantEstimateMetadata() {
        var reliable = new TtcEstimate(1.25, true);
        assertEquals(reliable, TtcChatEstimate.read(TtcChatEstimate.key(reliable),
                new Object[] {"translated fallback", "1.25"}).orElseThrow());
        var low = new TtcEstimate(5, false);
        assertEquals(low, TtcChatEstimate.read(TtcChatEstimate.key(low), new Object[] {"5ms?", "5"}).orElseThrow());
        assertTrue(TtcChatEstimate.read("other", new Object[] {"5ms", "5"}).isEmpty());
        assertTrue(TtcChatEstimate.read(TtcChatEstimate.RELIABLE, new Object[0]).isEmpty());
        for (var value : new Object[] {1.0, null, "x".repeat(33), "bad", "NaN", "Infinity", "-1"})
            assertTrue(TtcChatEstimate.read(TtcChatEstimate.RELIABLE, new Object[] {"fallback", value}).isEmpty());
        assertEquals(0, TtcChatEstimate.read(TtcChatEstimate.RELIABLE, new Object[] {"0ms", "0"})
                .orElseThrow().durationMs());
    }
}
