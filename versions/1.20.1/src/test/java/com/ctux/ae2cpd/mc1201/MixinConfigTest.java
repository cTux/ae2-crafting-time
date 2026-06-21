package com.ctux.ae2cpd.mc1201;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MixinConfigTest {
    @Test
    void craftingTreeMixinIsClientOnly() throws IOException {
        var json = Files.readString(Path.of("src/main/resources/ae2cpd.mixins.json"));
        var clientIndex = json.indexOf("\"client\"");
        var treeIndex = json.indexOf("\"CraftingTreeWidgetMixin\"");
        var confirmIndex = json.indexOf("\"CraftConfirmTableRendererMixin\"");
        var serverIndex = json.indexOf("\"mixins\"");
        var cpuIndex = json.indexOf("\"CraftingCpuLogicMixin\"");

        assertTrue(serverIndex >= 0 && cpuIndex > serverIndex);
        assertTrue(clientIndex >= 0 && treeIndex > clientIndex);
        assertTrue(confirmIndex > clientIndex);
    }
}
