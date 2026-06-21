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
        var treeIndex = json.indexOf("\"CraftingTreeWidgetMixin\"");
        var confirmIndex = json.indexOf("\"CraftConfirmTableRendererMixin\"");
        var serverIndex = json.indexOf("\"mixins\"");
        var cpuIndex = json.indexOf("\"CraftingCpuLogicMixin\"");

        assertTrue(serverIndex >= 0 && cpuIndex > serverIndex);
        assertTrue(clientIndex >= 0 && treeIndex > clientIndex);
        assertTrue(confirmIndex > clientIndex);
    }

    @Test
    void craftPlanTimeEstimateHooksVisibleDescription() throws IOException {
        var mixin = Files.readString(Path.of(
                "src/main/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java"));

        assertTrue(mixin.contains("method = \"getEntryDescription\""));
        assertTrue(mixin.contains("TTC: "));
    }
}
