package com.ctux.ae2craftingtime.mc1201.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.ctux.ae2craftingtime.core.CraftingBlockReason;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StallDiagnostic;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class CraftingStatusTableRendererMixinTest {
    private static final List<String> CONTROLS = List.of(
            "text.ae2craftingtime.locate_hint",
            "text.ae2craftingtime.details_hint",
            "text.ae2craftingtime.reset_hint");
    private static final BooleanSupplier NO_STATS_LOOKUP = () -> fail("Warning or empty row requested stats");
    private static final ProfileStats STATS = new ProfileStats(4, 100, 0.2, 4, 100, ProfileUnit.ITEM);

    @Test
    void noSampleWarningsComposeTheirEntireBodyAndOrderedGrayControls() throws ReflectiveOperationException {
        var lines = nativeTooltip();
        appendTooltip(lines, 0, 0, true, null, NO_STATS_LOOKUP);
        assertTooltip(lines, TtcText.noSpaceTooltip(), CONTROLS);
        for (var reason : CraftingBlockReason.values()) {
            for (long active : new long[] {0, 1}) {
                lines = nativeTooltip();
                appendTooltip(lines, active, 1, false, reason, NO_STATS_LOOKUP);
                assertTooltip(lines, TtcText.blockReasonTooltip(reason, active > 0), CONTROLS);
            }
        }
    }

    @Test
    void delayedBodyHasExactlyOneLocateHintAfterAllAdvice() throws ReflectiveOperationException {
        var lines = nativeTooltip();
        var body = TtcText.stallLines(2, 1, STATS, new StallDiagnostic(960, 240, 1, 1, 4));
        appendTooltip(lines, 1, 1, false, null, () -> {
            lines.addAll(body);
            return true;
        });
        assertTooltip(lines, body, CONTROLS);
    }

    @Test
    void ordinaryAndCollectingRowsKeepTheirOriginalTwoControls() throws ReflectiveOperationException {
        for (var body : List.of(TtcText.statsLines(STATS), List.<Component>of())) {
            var lines = nativeTooltip();
            appendTooltip(lines, 1, 0, false, null, () -> {
                lines.addAll(body);
                return false;
            });
            assertTooltip(lines, body, CONTROLS.subList(1, 3));
        }
    }

    @Test
    void emptyRowsKeepTheNativeTooltipWithoutControlsOrStatsRequests() throws ReflectiveOperationException {
        var lines = nativeTooltip();
        appendTooltip(lines, 0, 0, false, null, NO_STATS_LOOKUP);
        assertEquals(nativeTooltip(), lines);
    }

    private static ArrayList<Component> nativeTooltip() {
        return new ArrayList<>(List.of(Component.literal("Native item").withStyle(ChatFormatting.AQUA)));
    }

    private static void appendTooltip(List<Component> lines, long active, long pending, boolean noSpace,
            CraftingBlockReason reason, BooleanSupplier appendStats) throws ReflectiveOperationException {
        var method = CraftingStatusTableRendererMixin.class.getDeclaredMethod("ae2craftingtime$appendTooltip",
                List.class, long.class, long.class, boolean.class, CraftingBlockReason.class, BooleanSupplier.class);
        method.setAccessible(true);
        method.invoke(null, lines, active, pending, noSpace, reason, appendStats);
    }

    private static void assertTooltip(List<Component> lines, List<Component> body, List<String> controls) {
        var expectedBody = nativeTooltip();
        expectedBody.addAll(body);
        assertEquals(expectedBody, lines.subList(0, expectedBody.size()));
        var suffix = lines.subList(expectedBody.size(), lines.size());
        assertEquals(controls, suffix.stream().map(line ->
                ((TranslatableContents) line.getContents()).getKey()).toList());
        for (var key : CONTROLS) {
            assertEquals(controls.contains(key) ? 1 : 0, lines.stream().filter(line ->
                    line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)).count());
        }
        for (var line : suffix) {
            assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), line.getStyle().getColor());
        }
    }
}
