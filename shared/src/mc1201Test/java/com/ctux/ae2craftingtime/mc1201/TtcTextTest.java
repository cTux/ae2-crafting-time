package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class TtcTextTest {
    @Test
    void tooltipLabelsEstimatesAndCollectingDataWithoutChangingCompactText() {
        for (var value : List.of(TtcText.ttc("~1s"), TtcText.ttcCollectingData())) {
            var tooltip = TtcText.tooltipTtc(value);
            var label = (TranslatableContents) tooltip.getContents();
            assertEquals("text.ae2craftingtime.stats.ttc", label.getKey());
            assertEquals(List.of(Component.literal(": "), value), tooltip.getSiblings());
            assertEquals(value.getStyle(), tooltip.getStyle());
            assertTrue(value.getSiblings().isEmpty());
            assertEquals("text.ae2craftingtime.ttc", ((TranslatableContents) value.getContents()).getKey());
        }
    }

    @Test
    void waitingUsesTranslationWithoutArguments() {
        var contents = (TranslatableContents) TtcText.waiting().getContents();
        assertEquals("text.ae2craftingtime.waiting", contents.getKey());
        assertEquals(0, contents.getArgs().length);
    }

    @Test
    void collectingDataUsesTtcWrapperAndCollectingDataKey() {
        var collectingData = TtcText.ttcCollectingData();
        var contents = (TranslatableContents) collectingData.getContents();
        assertEquals("text.ae2craftingtime.ttc", contents.getKey());
        assertEquals(1, contents.getArgs().length);

        var nestedContents = (TranslatableContents) ((Component) contents.getArgs()[0]).getContents();
        assertEquals("text.ae2craftingtime.collecting_data", nestedContents.getKey());

        assertTrue(collectingData.getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), collectingData.getStyle().getColor());
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
