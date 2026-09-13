package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecurrentMixinRegistrationTest {
    @Test
    void registersEveryMixinRequiredByRecurrentPlanRendering() throws IOException {
        var config = getClass().getClassLoader().getResourceAsStream("ae2craftingtime.mixins.json");
        assertNotNull(config);
        try (config) {
            var text = new String(config.readAllBytes(), StandardCharsets.UTF_8);
            for (var mixin : List.of("CraftingCalculationMixin", "CraftConfirmMenuMixin",
                    "CraftingPlanMixin", "CraftingPlanSummaryMixin", "CraftingPlanSummaryEntryMixin",
                    "CraftingTreeNodeMixin")) {
                assertTrue(text.contains("\"" + mixin + "\""),
                        () -> "1.21.1 must register recurrent-plan mixin " + mixin);
            }
        }
    }
}
