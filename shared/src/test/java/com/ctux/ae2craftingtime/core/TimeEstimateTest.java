package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class TimeEstimateTest {
    @Test
    void formatsRoundedUpEtaForWholeAmount() {
        var stats = new ProfileStats(4, 20, 0.5, 10.0, 20, ProfileUnit.ITEM);

        assertEquals("~8s", TimeEstimate.format(75, stats).orElseThrow());
    }

    @Test
    void formatsMinutesWithoutLeadingHour() {
        var stats = new ProfileStats(1, 20, 1, 1.0, 20, ProfileUnit.ITEM);

        assertEquals("~1:07", TimeEstimate.format(67, stats).orElseThrow());
    }

    @Test
    void formatsHoursWithoutLeadingZeros() {
        var stats = new ProfileStats(1, 20, 1, 1.0, 20, ProfileUnit.ITEM);

        assertEquals("~1000:00:00", TimeEstimate.format(3_600_000, stats).orElseThrow());
    }

    @Test
    void nonZeroWorkRoundsUpToOneSecond() {
        var stats = new ProfileStats(1, 20, 100, 100.0, 20, ProfileUnit.ITEM);

        assertEquals("~1s", TimeEstimate.format(1, stats).orElseThrow());
        assertEquals(1, TimeEstimate.seconds(1, stats).orElseThrow());
    }

    @Test
    void zeroThroughputHasNoEstimate() {
        var stats = new ProfileStats(0, 0, 0, 0, 0, ProfileUnit.ITEM);

        assertFalse(TimeEstimate.format(10, stats).isPresent());
    }

    @Test
    void totalCraftEtaAddsKnownRows() {
        var total = TimeEstimate.formatTotal(List.of(OptionalLong.of(4), OptionalLong.empty(), OptionalLong.of(9)));

        assertEquals("~13s", total.orElseThrow());
    }
}
