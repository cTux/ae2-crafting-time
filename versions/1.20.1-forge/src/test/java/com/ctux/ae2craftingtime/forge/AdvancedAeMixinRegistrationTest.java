package com.ctux.ae2craftingtime.forge;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AdvancedAeMixinRegistrationTest {
    @Test
    void packagesAndRegistersAdvancedAeMixin() throws IOException {
        var loader = getClass().getClassLoader();
        assertNotNull(loader.getResource(
                "com/ctux/ae2craftingtime/mc1201/mixin/AdvancedCraftingCpuLogicMixin.class"));

        var config = loader.getResourceAsStream("ae2craftingtime-advancedae.mixins.json");
        assertNotNull(config);
        try (config) {
            assertTrue(new String(config.readAllBytes(), StandardCharsets.UTF_8)
                    .contains("\"AdvancedCraftingCpuLogicMixin\""));
        }
    }
}
