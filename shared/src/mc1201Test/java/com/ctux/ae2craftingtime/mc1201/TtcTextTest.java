package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ClientConfig;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

class TtcTextTest {
    @ParameterizedTest
    @CsvSource({"en_us, ITEM", "uk_ua, ITEM", "en_us, MILLIBUCKET", "uk_ua, MILLIBUCKET",
            "en_us, MANA", "uk_ua, MANA"})
    void throughputModesAndClientChatKeepUnitsAndPrecision(String locale, ProfileUnit unit) throws Exception {
        var translations = new java.util.HashMap<String, String>();
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet()
                    .forEach(entry -> translations.put(entry.getKey(), entry.getValue().getAsString()));
        }
        var constructor = net.minecraft.client.resources.language.ClientLanguage.class
                .getDeclaredConstructor(java.util.Map.class, boolean.class);
        constructor.setAccessible(true);
        var language = constructor.newInstance(translations, false);
        var field = java.util.Arrays.stream(net.minecraft.client.resources.language.I18n.class.getDeclaredFields())
                .filter(candidate -> net.minecraft.locale.Language.class.isAssignableFrom(candidate.getType()))
                .findFirst().orElseThrow();
        field.setAccessible(true);
        var original = field.get(null);
        var oldCompact = ClientOptionsRuntime.current().features().enabled(OptionFeature.COMPACT_HOVER_NUMBERS);
        ClientOptionsRuntime.setConnectionSupportForTests(() -> true);
        field.set(null, language);
        try {
            for (double value : new double[] {0.004, 999.995, 999995, 1e18, 9.99995e20, 1e21,
                    Double.NaN, 0, -1, Double.POSITIVE_INFINITY}) {
                var stats = new ProfileStats(2, 95, value, value, 100, unit, true, 2, 4,
                        List.of(90L, 100L), List.of(9L, 1L));
                for (boolean compact : new boolean[] {false, true}) {
                    ClientOptionsRuntime.current().features().setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, compact);
                    var expected = com.ctux.ae2craftingtime.core.ThroughputNumbers.hover(value, compact);
                    var throughput = TtcText.statsLines(stats).get(0).getSiblings().get(1).getString();
                    assertEquals(String.format(java.util.Locale.ROOT,
                            translations.get("text.ae2craftingtime.value.throughput"), expected,
                            translations.get(unit.translationKey()), expected, translations.get(unit.translationKey())),
                            throughput);
                    var chat = TtcText.compactMessages("minecraft:stone", 9_007_199_254_740_993L, stats);
                    assertTrue(chat.get(0).contains("9007199254740993"));
                    assertTrue(chat.get(1).contains(com.ctux.ae2craftingtime.core.ThroughputNumbers.full(value)));
                    assertEquals(9, translations.get("text.ae2craftingtime.chat.details").split("%s", -1).length - 1);
                    assertEquals(5, translations.get("text.ae2craftingtime.chat.details.rate").split("%s", -1).length - 1);
                }
            }
        } finally {
            field.set(null, original);
            ClientOptionsRuntime.current().features().setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, oldCompact);
            ClientOptionsRuntime.setConnectionSupportForTests(null);
        }
    }
    @BeforeEach
    void textOnlyForExistingStructureChecks() {
        ClientOptionsRuntime.setConnectionSupportForTests(() -> true);
        ClientOptionsRuntime.current().features().setEnabled(OptionFeature.SHOW_EMOJI, false);
    }

    @AfterEach
    void restoreDefault() {
        ClientOptionsRuntime.setConnectionSupportForTests(null);
        ClientOptionsRuntime.current().features().setEnabled(OptionFeature.SHOW_EMOJI, true);
    }
    @Test
    void craftingAmountKeysHaveMatchingBilingualSymbolsAndPlaceholders() throws IOException {
        var line = (TranslatableContents) TtcText.statusAmounts("4/10/200").getContents();
        var legend = (TranslatableContents) TtcText.statusAmountsLegend().getContents();
        assertEquals("text.ae2craftingtime.status.amounts", line.getKey());
        assertEquals(List.of("4/10/200"), List.of(line.getArgs()));
        assertEquals("text.ae2craftingtime.status.amounts_legend", legend.getKey());
        assertEquals("text.ae2craftingtime.plan.amounts_legend",
                ((TranslatableContents) TtcText.planAmountsLegend().getContents()).getKey());
        for (var locale : List.of("en_us", "uk_ua")) {
            try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
                var translations = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals("%s", translations.get(line.getKey()).getAsString());
                var wording = translations.get(legend.getKey()).getAsString();
                assertTrue(wording.startsWith("A:"));
                assertTrue(wording.contains(" / C:"));
                assertTrue(wording.contains(" / S:"));
                assertTrue(wording.contains("; -:"));
                var planWording = translations.get("text.ae2craftingtime.plan.amounts_legend").getAsString();
                assertTrue(planWording.startsWith("A:"));
                assertTrue(planWording.contains(" / C:"));
                assertTrue(translations.has("config.ae2craftingtime.compactStatusAmounts"));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"en_us, Stored variant", "uk_ua, Інший варіант у сховищі"})
    void storedVariantHasIndependentLocalizedWarningAndGuidance(String locale, String label) throws IOException {
        var name = TtcText.storedVariant();
        assertEquals("text.ae2craftingtime.plan.stored_variant",
                ((TranslatableContents) name.getContents()).getKey());
        assertTrue(com.ctux.ae2craftingtime.core.CraftingRowState.isBadge(
                ((TranslatableContents) name.getContents()).getKey()));
        assertTrue(com.ctux.ae2craftingtime.core.CraftingRowState.isWidthLimited(
                ((TranslatableContents) name.getContents()).getKey()));
        assertFalse(name.getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), name.getStyle().getColor());
        var hints = TtcText.storedVariantHints();
        assertEquals(2, hints.size());
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
            var translations = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(label, translations.get("text.ae2craftingtime.plan.stored_variant").getAsString());
            for (var hint : hints) {
                var key = ((TranslatableContents) hint.getContents()).getKey();
                assertTrue(translations.has(key));
                assertFalse(translations.get(key).getAsString().isBlank());
            }
            assertEquals(locale.equals("en_us")
                    ? "The ME network stores this item with different saved data."
                    : "У ME-мережі є цей предмет з іншими збереженими даними.",
                    translations.get("text.ae2craftingtime.plan.stored_variant.explanation").getAsString());
            assertEquals(locale.equals("en_us")
                    ? "Re-encode the pattern using the item the network actually produces or stores."
                    : "Перекодуйте шаблон, використавши предмет, який мережа справді виробляє або зберігає.",
                    translations.get("text.ae2craftingtime.plan.stored_variant.suggestion").getAsString());
        }
    }

    @ParameterizedTest
    @CsvSource({"en_us, Recurrent", "uk_ua, Циклічне"})
    void recurrencePreservesNativeAmountAndUsesNormalRedLocalizedText(String locale, String label) throws IOException {
        var amount = "1.25 M mB";
        var component = TtcText.recurrent(amount);
        var contents = (TranslatableContents) component.getContents();
        assertEquals("text.ae2craftingtime.plan.recurrent", contents.getKey());
        assertEquals(List.of(amount), List.of(contents.getArgs()));
        assertFalse(component.getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), component.getStyle().getColor());
        assertTrue(com.ctux.ae2craftingtime.core.CraftingRowState.isBadge(contents.getKey()));
        var hint = (TranslatableContents) TtcText.recurrentHint().getContents();
        assertEquals("text.ae2craftingtime.plan.recurrent_hint", hint.getKey());
        assertEquals(0, hint.getArgs().length);
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
            var translations = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(label + ": %s", translations.get(contents.getKey()).getAsString());
            assertEquals(label + ": " + amount, String.format(translations.get(contents.getKey()).getAsString(), amount));
            assertEquals(locale.equals("en_us")
                    ? "This ingredient is missing because its crafting recipe depends on itself, directly or through other recipes."
                    : "Цього інгредієнта бракує, бо рецепт його виготовлення залежить від нього самого — безпосередньо або через інші рецепти.",
                    translations.get(hint.getKey()).getAsString());
        }
    }

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
    @CsvSource({"en_us, No space", "uk_ua, Немає місця"})
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

    @Test
    void chanceOutputUsesNormalRedForRowAndTooltipHeading() {
        var label = TtcText.chanceOutput();
        assertFalse(label.getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), label.getStyle().getColor());
        assertEquals(label, TtcText.chanceOutputTooltip(40, 5000, 0).get(0));
    }

    @ParameterizedTest
    @CsvSource({"en_us, NO_PROVIDER, No provider", "uk_ua, NO_PROVIDER, Без провайдера",
            "en_us, NO_POWER, No power", "uk_ua, NO_POWER, Немає енергії",
            "en_us, NO_CHANNEL, No channel", "uk_ua, NO_CHANNEL, Немає каналу",
            "en_us, NO_TARGET, No target", "uk_ua, NO_TARGET, Немає приймача",
            "en_us, INPUT_BLOCKED, Input blocked", "uk_ua, INPUT_BLOCKED, Вхід заблоковано",
            "en_us, LOCKED, Locked", "uk_ua, LOCKED, Заблоковано"})
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
    void mixedRowQualifierAppearsOnlyForTransientDispatchReasons() {
        for (var reason : com.ctux.ae2craftingtime.core.CraftingBlockReason.values()) {
            var lines = TtcText.blockReasonTooltip(reason, true);
            var transientReason = reason == com.ctux.ae2craftingtime.core.CraftingBlockReason.NO_CHANNEL
                    || reason == com.ctux.ae2craftingtime.core.CraftingBlockReason.NO_TARGET
                    || reason == com.ctux.ae2craftingtime.core.CraftingBlockReason.INPUT_BLOCKED
                    || reason == com.ctux.ae2craftingtime.core.CraftingBlockReason.LOCKED;
            assertEquals(transientReason ? 4 : 3, lines.size());
            if (transientReason) {
                assertEquals("text.ae2craftingtime.dispatch_status.scheduled_only",
                        ((TranslatableContents) lines.get(3).getContents()).getKey());
            }
        }
        assertEquals(3, TtcText.blockReasonTooltip(
                com.ctux.ae2craftingtime.core.CraftingBlockReason.LOCKED, false).size());
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
        assertEquals(TextColor.fromRgb(ClientConfig.Color.COLLECTING.defaultRgb()),
                collectingData.getStyle().getColor());
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

        assertEquals(3, lines.size());
        assertEquals("text.ae2craftingtime.stats.samples",
                ((TranslatableContents) lines.get(1).getContents()).getKey());
        assertEquals("text.ae2craftingtime.stats.samples.explanation",
                ((TranslatableContents) lines.get(2).getContents()).getKey());
    }

    @ParameterizedTest
    @CsvSource({"en_us, 50 ticks", "uk_ua, 50 тіків"})
    void normalizedSamplesShowOnlyLocalizedTime(String locale, String expected) throws IOException {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/ae2craftingtime/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
            var translations = JsonParser.parseReader(reader).getAsJsonObject();
            var window = translations.get("text.ae2craftingtime.value.window").getAsString();
            assertEquals(expected, String.format(window, "50"));
        }
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
