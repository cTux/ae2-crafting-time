package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MixinConfigTest {
    @Test
    void craftingTreeMixinIsClientOnly() throws IOException {
        var json = Files.readString(Path.of("src/main/resources/ae2craftingtime.mixins.json"));
        var clientIndex = json.indexOf("\"client\"");
        var tableIndex = json.indexOf("\"AbstractTableRendererMixin\"");
        var treeIndex = json.indexOf("\"CraftingTreeWidgetMixin\"");
        var confirmIndex = json.indexOf("\"CraftConfirmTableRendererMixin\"");
        var sortIndex = json.indexOf("\"CraftConfirmScreenMixin\"");
        var statusScreenIndex = json.indexOf("\"CraftingCPUScreenMixin\"");
        var statusTableIndex = json.indexOf("\"CraftingStatusTableRendererMixin\"");
        var serverIndex = json.indexOf("\"mixins\"");
        var cpuIndex = json.indexOf("\"CraftingCpuLogicMixin\"");

        assertTrue(serverIndex >= 0 && cpuIndex > serverIndex);
        assertTrue(tableIndex > clientIndex);
        assertTrue(clientIndex >= 0 && treeIndex > clientIndex);
        assertTrue(confirmIndex > clientIndex);
        assertTrue(sortIndex > clientIndex);
        assertTrue(statusScreenIndex > clientIndex);
        assertTrue(statusTableIndex > clientIndex);
    }

    @Test
    void craftPlanTimeEstimateHooksVisibleDescription() throws IOException {
        var mixin = Files.readString(Path.of(
                "../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java"));

        assertTrue(mixin.contains("method = \"getEntryDescription\""));
        assertTrue(mixin.contains("TTC: "));
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
        assertTrue(tableMixin.contains("TTC: "));
        assertTrue(screenMixin.contains("Total TTC: "));
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
    }
}
