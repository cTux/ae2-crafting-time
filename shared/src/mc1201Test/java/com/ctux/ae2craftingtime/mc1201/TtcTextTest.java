package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

class TtcTextTest {
    @ParameterizedTest
    @CsvSource({"en_us, No data yet", "uk_ua, Даних ще немає"})
    void missingRowStatsUseTheExistingNoDataWording(String locale, String expected) throws IOException {
        var resource = "/assets/ae2craftingtime/lang/" + locale + ".json";
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            var translations = JsonParser.parseReader(reader).getAsJsonObject();
            var rowText = translations.get("text.ae2craftingtime.collecting_data").getAsString();
            assertEquals(expected, rowText);
            assertEquals(translations.get("text.ae2craftingtime.no_stats").getAsString(), rowText);
        }
    }

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

    @ParameterizedTest
    @CsvSource({"en_us, NO SPACE", "uk_ua, Немає місця"})
    void noSpaceHasNormalWarningStyleAndTranslatedAdvice(String locale, String expected) throws IOException {
        var lines = TtcText.noSpaceTooltip();
        assertEquals(3, lines.size());
        assertFalse(lines.get(0).getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), lines.get(0).getStyle().getColor());
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
            var translations = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(expected, translations.get("text.ae2craftingtime.no_space").getAsString());
            for (var line : lines) {
                var contents = (TranslatableContents) line.getContents();
                assertTrue(!translations.get(contents.getKey()).getAsString().isBlank());
                assertEquals(0, contents.getArgs().length);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"en_us, NO_PROVIDER, NO PROVIDER", "uk_ua, NO_PROVIDER, Без провайдера",
            "en_us, NO_POWER, NO POWER", "uk_ua, NO_POWER, Немає енергії"})
    void blockerHasNormalWarningStyleAndTranslatedAdvice(String locale,
            com.ctux.ae2craftingtime.core.CraftingBlockReason reason, String expected) throws IOException {
        var lines = TtcText.blockReasonTooltip(reason);
        assertEquals(3, lines.size());
        assertFalse(lines.get(0).getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), lines.get(0).getStyle().getColor());
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
            var translations = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(expected, translations.get("text.ae2craftingtime." + reason.name().toLowerCase(java.util.Locale.ROOT)).getAsString());
            for (var line : lines) {
                var contents = (TranslatableContents) line.getContents();
                assertTrue(!translations.get(contents.getKey()).getAsString().isBlank());
                assertEquals(0, contents.getArgs().length);
            }
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

        assertFalse(collectingData.getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), collectingData.getStyle().getColor());
    }

    @Test
    void statsLinesOnlyShowProductionRateWithoutRecordedSamples() {
        var stats = new ProfileStats(4, 646.5, 0.01, 0.18, 109, ProfileUnit.ITEM);

        assertEquals(1, TtcText.statsLines(stats).size());
    }

    @Test
    void statsLinesCombineRecordedSamplesWithTheirCount() {
        var stats = new ProfileStats(10, 100, 0.02, 0.4, 100, ProfileUnit.ITEM,
                true, 10, 4.0, List.of(100L), List.of(2L));

        var lines = TtcText.statsLines(stats);

        assertEquals(2, lines.size());
        assertTrue(lines.get(1).getString().endsWith("(10)"));
    }

    @Test
    void delayedChatKeepsMatchingPlaceholders() throws IOException {
        String key = "text.ae2craftingtime.chat.delayed";
        String en;
        String uk;
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/en_us.json"), StandardCharsets.UTF_8)) {
            en = JsonParser.parseReader(reader).getAsJsonObject().get(key).getAsString();
        }
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/uk_ua.json"), StandardCharsets.UTF_8)) {
            uk = JsonParser.parseReader(reader).getAsJsonObject().get(key).getAsString();
        }
        assertEquals(4, en.split("%s", -1).length - 1);
        assertEquals(4, uk.split("%s", -1).length - 1);
    }

    @Test
    void blockedChatKeepsMatchingPlaceholders() throws IOException {
        String key = "text.ae2craftingtime.chat.blocked";
        String en;
        String uk;
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/en_us.json"), StandardCharsets.UTF_8)) {
            en = JsonParser.parseReader(reader).getAsJsonObject().get(key).getAsString();
        }
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/uk_ua.json"), StandardCharsets.UTF_8)) {
            uk = JsonParser.parseReader(reader).getAsJsonObject().get(key).getAsString();
        }
        assertEquals(3, en.split("%s", -1).length - 1);
        assertEquals(3, uk.split("%s", -1).length - 1);
    }

    @Test
    void highlightingChatKeepsMatchingPlaceholders() throws IOException {
        String key = "text.ae2craftingtime.chat.highlighting";
        String en;
        String uk;
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/en_us.json"), StandardCharsets.UTF_8)) {
            en = JsonParser.parseReader(reader).getAsJsonObject().get(key).getAsString();
        }
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/uk_ua.json"), StandardCharsets.UTF_8)) {
            uk = JsonParser.parseReader(reader).getAsJsonObject().get(key).getAsString();
        }
        assertEquals(3, en.split("%s", -1).length - 1);
        assertEquals(3, uk.split("%s", -1).length - 1);
    }

    @Test
    void delayedWordAndHintsAreTranslated() throws IOException {
        for (var locale : List.of("en_us", "uk_ua")) {
            try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
                var translations = JsonParser.parseReader(reader).getAsJsonObject();
                for (var key : List.of("text.ae2craftingtime.chat.delayed.word",
                        "text.ae2craftingtime.chat.delayed.hint",
                        "text.ae2craftingtime.chat.delayed.expired",
                        "text.ae2craftingtime.chat.provider",
                        "text.ae2craftingtime.chat.teleport.hint",
                        "text.ae2craftingtime.locate_hint",
                        "text.ae2craftingtime.chat.no_power.word",
                        "text.ae2craftingtime.chat.no_space.word")) {
                    assertTrue(!translations.get(key).getAsString().isBlank(), locale + " " + key);
                }
            }
        }
    }
}
