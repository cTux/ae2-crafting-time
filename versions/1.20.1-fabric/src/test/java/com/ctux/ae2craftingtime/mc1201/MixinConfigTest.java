package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MixinConfigTest {
    @Test
    void craftingTreeMixinIsClientOnly() throws IOException {
        var json = Files.readString(Path.of("build/resources/main/ae2craftingtime.mixins.json"));
        var clientIndex = json.indexOf("\"client\"");
        var tableIndex = json.indexOf("\"AbstractTableRendererMixin\"");
        var treeIndex = json.indexOf("\"CraftingTreeWidgetMixin\"");
        var confirmIndex = json.indexOf("\"CraftConfirmTableRendererMixin\"");
        var sortIndex = json.indexOf("\"CraftConfirmScreenMixin\"");
        var statusScreenIndex = json.indexOf("\"CraftingCPUScreenMixin\"");
        var statusTableIndex = json.indexOf("\"CraftingStatusTableRendererMixin\"");
        var meRequesterIndex = json.indexOf("\"MERequesterScreenMixin\"");
        var serverIndex = json.indexOf("\"mixins\"");
        var cpuIndex = json.indexOf("\"CraftingCpuLogicMixin\"");

        assertTrue(serverIndex >= 0 && cpuIndex > serverIndex);
        assertTrue(tableIndex > clientIndex);
        assertTrue(clientIndex >= 0 && treeIndex > clientIndex);
        assertTrue(confirmIndex > clientIndex);
        assertTrue(sortIndex > clientIndex);
        assertTrue(statusScreenIndex > clientIndex);
        assertTrue(statusTableIndex > clientIndex);
        assertTrue(meRequesterIndex > clientIndex);
    }

    @Test
    void craftPlanTimeEstimateHooksVisibleDescription() throws IOException {
        var mixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java"));

        assertTrue(mixin.contains("method = \"getEntryDescription\""));
        assertTrue(mixin.contains("TtcText.ttc"));
        assertTrue(mixin.contains("withBold(true)"));
    }

    @Test
    void craftingStatusTimeEstimateHooksVisibleDescription() throws IOException {
        var tableMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java"));
        var screenMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingCPUScreenMixin.java"));

        assertTrue(tableMixin.contains("method = \"getEntryDescription\""));
        assertTrue(tableMixin.contains("getActiveAmount() + entry.getPendingAmount()"));
        assertTrue(tableMixin.contains("TtcText.ttc"));
        assertTrue(screenMixin.contains("method = \"updateBeforeRender\""));
        assertTrue(screenMixin.contains("TtcText.ttc"));
        assertTrue(screenMixin.contains("AE2CRAFTINGTIME_SCREEN_WIDTH"));
        assertFalse(screenMixin.contains("append(separator).append(total)"));
        assertFalse(screenMixin.contains("drawString(font, text, 109 - font.width(text) / 2, 162"));
        assertTrue(screenMixin.contains("new TtcSortButton"));
        assertTrue(screenMixin.contains("TtcSort.copySorted"));
    }

    @Test
    void statusStatsRequestsUseCraftingCpuMenuGrid() throws IOException {
        var packet = Files.readString(Path.of(
                "src/main/java/com/ctux/ae2craftingtime/mc1201/net/StatsRequestC2S.java"));

        assertTrue(packet.contains("CraftingCPUMenu"));
        assertTrue(packet.contains("getDeclaredMethod(\"getGrid\")"));
    }

    @Test
    void craftingTreeTtcColorsUseSiblingGroups() throws IOException {
        var mixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingTreeWidgetMixin.java"));

        assertTrue(mixin.contains("ae2craftingtime$colorSiblingGroup(subNodes"));
        assertTrue(mixin.contains("ae2craftingtime$colorsByNode"));
        assertTrue(mixin.contains("LABEL_BACKGROUND = 0xD0202028"));
        assertTrue(mixin.contains("EXTRA_SPACING_Y = 8"));
        assertTrue(mixin.contains("font.width(text) * TEXT_SCALE"));
        assertTrue(mixin.contains("color, true"));
        assertFalse(mixin.contains("LABEL_BACKGROUND = 0xFFDBDBDB"));
    }

    @Test
    void statsChatUsesControlClickAndTooltipHint() throws IOException {
        var confirmMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmScreenMixin.java"));
        var statusMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingCPUScreenMixin.java"));
        var treeMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingTreeWidgetMixin.java"));
        var confirmTableMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java"));
        var statusTableMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java"));
        var clickHelper = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/TtcDetailsClick.java"));

        assertTrue(clickHelper.contains("minecraft.screen instanceof StatsClickHandler"));
        assertTrue(clickHelper.contains("handler.ae2craftingtime$handleClickedStats(mouseX, mouseY, button)"));
        assertTrue(confirmMixin.contains("public boolean mouseClicked"));
        assertTrue(statusMixin.contains("public boolean mouseClicked"));
        assertTrue(confirmMixin.contains("TtcDetailsKeyMapping.matchesMouse(button)"));
        assertTrue(statusMixin.contains("TtcDetailsKeyMapping.matchesMouse(button)"));
        assertTrue(confirmMixin.contains("TtcDetailsKeyMapping.matchesResetMouse(button)"));
        assertTrue(statusMixin.contains("TtcDetailsKeyMapping.matchesResetMouse(button)"));
        assertTrue(confirmMixin.contains("getStackUnderMouse(mouseX, mouseY)"));
        assertTrue(statusMixin.contains("getStackUnderMouse(mouseX, mouseY)"));
        assertFalse(confirmMixin.contains("button == 2"));
        assertFalse(statusMixin.contains("button == 2"));
        assertTrue(treeMixin.contains("!TtcDetailsKeyMapping.matchesMouse(button)"));
        assertTrue(confirmTableMixin.contains("TtcText.detailsHint"));
        assertTrue(statusTableMixin.contains("TtcText.detailsHint"));
        assertTrue(treeMixin.contains("TtcText.detailsHint"));
        assertTrue(confirmTableMixin.contains("TtcText.resetHint"));
        assertTrue(statusTableMixin.contains("TtcText.resetHint"));
        assertTrue(treeMixin.contains("TtcText.resetHint"));
        assertTrue(confirmTableMixin.indexOf("ae2craftingtime$appendStatsTooltip(entry, cir.getReturnValue())")
                < confirmTableMixin.indexOf("TtcText.detailsHint"));
        assertTrue(statusTableMixin.indexOf("ae2craftingtime$appendStatsTooltip(entry, cir.getReturnValue())")
                < statusTableMixin.indexOf("TtcText.detailsHint"));
        assertTrue(treeMixin.indexOf("TtcText.ttc") < treeMixin.indexOf("TtcText.detailsHint"));
        assertFalse(confirmMixin.contains("hasShiftDown"));
        assertFalse(statusMixin.contains("hasShiftDown"));
        assertFalse(treeMixin.contains("hasShiftDown"));
    }

    @Test
    void craftingStatusStatsChatWorksOnAnyCpuScreen() throws IOException {
        var statusMixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingCPUScreenMixin.java"));
        var methodStart = statusMixin.indexOf("public boolean ae2craftingtime$handleClickedStats");
        var methodEnd = statusMixin.indexOf("@Unique", methodStart);
        var clickMethod = statusMixin.substring(methodStart, methodEnd);

        assertTrue(clickMethod.contains("if (status == null)"));
        assertFalse(clickMethod.contains("instanceof CraftingStatusScreen"));
    }

    @Test
    void meRequesterIntegrationIsOptionalClientOverlay() throws IOException {
        var mixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/MERequesterScreenMixin.java"));

        assertTrue(mixin.contains("@Pseudo"));
        assertTrue(mixin.contains("targets = \"com.almostreliable.merequester.client.abstraction.AbstractRequesterScreen\""));
        assertTrue(mixin.contains("TtcText.requesterTtc"));
        assertTrue(mixin.contains("TtcText.totalTtc"));
        assertTrue(mixin.contains("AE2CRAFTINGTIME_STATUS_X = 47"));
        assertTrue(mixin.contains("AE2CRAFTINGTIME_STATUS_WIDTH = 118"));
        assertTrue(mixin.contains("AE2CRAFTINGTIME_LABEL_BACKGROUND = 0xD0202028"));
        assertTrue(mixin.contains("TtcColor.forSeconds"));
        assertTrue(mixin.contains("amount <= networkAmount.getAsLong()"));
        assertTrue(mixin.contains("amount - networkAmount.getAsLong()"));
        assertTrue(mixin.contains("color, true"));
        assertTrue(mixin.contains("getMethod(methodName)"));
    }
}
