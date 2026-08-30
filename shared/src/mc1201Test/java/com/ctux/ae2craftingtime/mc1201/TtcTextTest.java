package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class TtcTextTest {
    @Test
    void waitingTimeUsesCompletedWholeSeconds() {
        assertWaitingSeconds(-1, 0L);
        assertWaitingSeconds(19, 0L);
        assertWaitingSeconds(20, 1L);
    }

    private static void assertWaitingSeconds(long ticks, long seconds) {
        var contents = (TranslatableContents) TtcText.waiting(ticks).getContents();
        assertEquals("text.ae2craftingtime.waiting", contents.getKey());
        assertEquals(seconds, contents.getArgs()[0]);
    }

    @Test
    void statsLinesOnlyShowProductionRateWithoutRecordedSamples() {
        var stats = new ProfileStats(4, 646.5, 0.01, 0.18, 109, ProfileUnit.ITEM);

        assertEquals(1, TtcText.statsLines(stats, Optional.empty()).size());
    }

    @Test
    void statsLinesCombineRecordedSamplesWithTheirCount() {
        var stats = new ProfileStats(10, 100, 0.02, 0.4, 100, ProfileUnit.ITEM,
                true, 10, 4.0, List.of(100L), List.of(2L));

        var lines = TtcText.statsLines(stats, Optional.empty());

        assertEquals(2, lines.size());
        assertTrue(lines.get(1).getString().endsWith("(10)"));
    }
}
