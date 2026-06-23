package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatsChatLinesTest {
    @Test
    void formatsEveryCachedAggregateStat() {
        var stats = new ProfileStats(3, 41.25, 2.5, 50.0, 80, ProfileUnit.ITEM);

        var lines = StatsChatLines.lines("Iron Ingot", 125, stats);

        assertEquals(List.of(
                new StatsChatLines.Line("Item", "Iron Ingot"),
                new StatsChatLines.Line("Amount", "125 items"),
                new StatsChatLines.Line("Samples", "3"),
                new StatsChatLines.Line("Average", "41.25 ticks (2.06s)"),
                new StatsChatLines.Line("Latest", "80 ticks (4.00s)"),
                new StatsChatLines.Line("Throughput", "2.50 items/t, 50.00 items/s"),
                new StatsChatLines.Line("TTC", "~3s")),
                lines);
    }

    @Test
    void includesConfidenceWhenEstimateIsLowConfidence() {
        var stats = new ProfileStats(5, 208, 0.1, 2.0, 1000, ProfileUnit.ITEM, false,
                4, 4.0, List.of(10L, 10L, 10L, 10L, 1000L));

        var lines = StatsChatLines.lines("Iron Ingot", 100, stats);

        assertEquals(new StatsChatLines.Line("Used Samples", "4/5"), lines.get(6));
        assertEquals(new StatsChatLines.Line("Outlier Filter", "4.00x"), lines.get(7));
        assertEquals(new StatsChatLines.Line("Durations", "10, 10, 10, 10, 1000 ticks"), lines.get(8));
        assertEquals(new StatsChatLines.Line("Confidence", "low (outliers filtered)"), lines.get(9));
        assertEquals(new StatsChatLines.Line("TTC", "~50s?"), lines.get(10));
    }

    @Test
    void compactsChatToTwoMessages() {
        var stats = new ProfileStats(5, 208, 0.1, 2.0, 1000, ProfileUnit.ITEM, false,
                4, 4.0, List.of(10L, 10L, 10L, 10L, 1000L));

        var messages = StatsChatLines.compactMessages("Iron Ingot", 100, stats);

        assertEquals(List.of(
                "Iron Ingot x100: ~50s?",
                "5 samples, avg 10.40s, latest 50.00s, 2.00 items/s, used 4/5, low confidence"),
                messages);
    }
}
