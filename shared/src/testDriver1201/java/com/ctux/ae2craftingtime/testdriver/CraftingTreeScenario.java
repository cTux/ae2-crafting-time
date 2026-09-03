package com.ctux.ae2craftingtime.testdriver;

import java.util.Set;

final class CraftingTreeScenario {
    static final String SCENARIO = "crafting-tree-screen";
    private static final Set<String> SCREENS = Set.of(
            "com.neuvillette.ae2ct.gui.CraftingTreeScreen", "com.vcwdfca.ae2ct.gui.CraftingTreeScreen");

    static boolean isScreen(String name) {
        return SCREENS.contains(name);
    }

    static boolean tooltipReady(UiSnapshot snapshot) {
        return Set.of("text.ae2craftingtime.ttc", "text.ae2craftingtime.details_hint", "text.ae2craftingtime.reset_hint")
                .stream().allMatch(key -> snapshot.tooltip().stream().filter(text -> text.key().equals(key)).count() == 1);
    }

    private CraftingTreeScenario() {
    }
}
