package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TimeEstimateTest {
    @Test
    void formatsRoundedUpEtaForWholeAmount() {
        var stats = new ProfileStats(4, 20, 0.5, 10.0, 20, ProfileUnit.ITEM);

        assertEquals("~000:00:08", TimeEstimate.format(75, stats).orElseThrow());
    }

    @Test
    void formatsHoursWithAtLeastThreeDigits() {
        var stats = new ProfileStats(1, 20, 1, 1.0, 20, ProfileUnit.ITEM);

        assertEquals("~1000:00:00", TimeEstimate.format(3_600_000, stats).orElseThrow());
    }

    @Test
    void nonZeroWorkRoundsUpToOneSecond() {
        var stats = new ProfileStats(1, 20, 100, 100.0, 20, ProfileUnit.ITEM);

        assertEquals("~000:00:01", TimeEstimate.format(1, stats).orElseThrow());
    }

    @Test
    void zeroThroughputHasNoEstimate() {
        var stats = new ProfileStats(0, 0, 0, 0, 0, ProfileUnit.ITEM);

        assertFalse(TimeEstimate.format(10, stats).isPresent());
    }
}
