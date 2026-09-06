package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class TimeEstimateTest {
    @Test
    void formatsDiagnosticTickDurations() {
        assertEquals("~12s", TimeEstimate.formatTicks(240));
        assertEquals("~1:01", TimeEstimate.formatTicks(1_201));
    }

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
    void marksLowConfidenceEta() {
        var stats = new ProfileStats(1, 20, 1, 1.0, 20, ProfileUnit.ITEM, false);

        assertEquals("~10s?", TimeEstimate.format(10, stats).orElseThrow());
    }

    @Test
    void zeroThroughputHasNoEstimate() {
        var stats = new ProfileStats(0, 0, 0, 0, 0, ProfileUnit.ITEM);

        assertFalse(TimeEstimate.format(10, stats).isPresent());
        assertFalse(TimeEstimate.seconds(0, stats).isPresent());
    }

    @Test
    void totalCraftEtaAddsKnownRows() {
        var total = TimeEstimate.formatTotal(List.of(OptionalLong.of(4), OptionalLong.empty(), OptionalLong.of(9)));

        assertEquals("~13s", total.orElseThrow());
        assertFalse(TimeEstimate.formatTotal(List.of(OptionalLong.empty())).isPresent());
    }

    @Test
    void negativeTicksFormatAsZero() {
        assertEquals("~0s", TimeEstimate.formatTicks(-1));
    }

    @Test
    void formatsPerUnitTicksWithoutLosingSmallOrFractionalValues() {
        assertEquals("10", TimeEstimate.formatSampleTicks(10).orElseThrow());
        assertEquals("41.1", TimeEstimate.formatSampleTicks(41.1).orElseThrow());
        assertEquals("0.063", TimeEstimate.formatSampleTicks(1.0 / 16.0).orElseThrow());
        assertEquals("0.001", TimeEstimate.formatSampleTicks(0.001).orElseThrow());
        assertEquals("<0.001", TimeEstimate.formatSampleTicks(0.0001).orElseThrow());
        assertFalse(TimeEstimate.formatSampleTicks(0).isPresent());
        assertFalse(TimeEstimate.formatSampleTicks(Double.POSITIVE_INFINITY).isPresent());
        assertFalse(TimeEstimate.formatSampleTicks(Double.NaN).isPresent());
    }

    @Test
    void derivesPerUnitDetailsWithoutChangingRawSamples() {
        var stats = new ProfileStats(2, 95, 0.1, 2, 100, ProfileUnit.ITEM, true, 2, 4,
                List.of(90L, 100L), List.of(9L, 1L));

        assertEquals(10, stats.sampleTicksPerUnit(0).orElseThrow());
        assertEquals(100, stats.latestTicksPerUnit().orElseThrow());
        assertEquals(55, stats.averageTicksPerUnit().orElseThrow());
        assertFalse(stats.sampleTicksPerUnit(-1).isPresent());
        assertFalse(stats.sampleTicksPerUnit(2).isPresent());

        var unequal = new ProfileStats(1, 1, 1, 1, 1, ProfileUnit.ITEM, true, 1, 4,
                List.of(1L), List.of());
        assertFalse(unequal.averageTicksPerUnit().isPresent());
        assertFalse(unequal.latestTicksPerUnit().isPresent());

        var invalid = new ProfileStats(1, 1, 1, 1, 1, ProfileUnit.ITEM, true, 1, 4,
                List.of(0L), List.of(1L));
        assertFalse(invalid.averageTicksPerUnit().isPresent());

        var zeroAmount = new ProfileStats(1, 1, 1, 1, 1, ProfileUnit.ITEM, true, 1, 4,
                List.of(1L), List.of(0L));
        assertFalse(zeroAmount.sampleTicksPerUnit(0).isPresent());

        var mismatched = new ProfileStats(2, 1, 1, 1, 1, ProfileUnit.ITEM, true, 2, 4,
                List.of(1L), List.of(1L, 1L));
        assertFalse(mismatched.averageTicksPerUnit().isPresent());

        var missingDuration = new ProfileStats(1, 1, 1, 1, 1, ProfileUnit.ITEM, true, 1, 4,
                List.of(), List.of(1L));
        assertFalse(missingDuration.sampleTicksPerUnit(0).isPresent());
    }
}
