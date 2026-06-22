package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatsChatLinesTest {
    @Test
    void formatsEveryCachedAggregateStat() {
        var stats = new ProfileStats(3, 41.25, 2.5, 50.0, 80, ProfileUnit.ITEM);

        var lines = StatsChatLines.lines(new ProfileKey("minecraft:iron_ingot"), 125, stats);

        assertEquals(List.of(
                new StatsChatLines.Line("Item", "minecraft:iron_ingot"),
                new StatsChatLines.Line("Amount", "125 items"),
                new StatsChatLines.Line("Samples", "3"),
                new StatsChatLines.Line("Average", "41.25 ticks (2.06s)"),
                new StatsChatLines.Line("Latest", "80 ticks (4.00s)"),
                new StatsChatLines.Line("Throughput", "2.50 items/t, 50.00 items/s"),
                new StatsChatLines.Line("TTC", "~3s")),
                lines);
    }
}
