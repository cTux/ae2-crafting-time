package com.ctux.ae2craftingtime.testdriver;

import java.util.Set;

final class CraftingTreeScenario {
    static final String SCENARIO = "crafting-tree-screen";
    static final String RECOVERY = "crafting-tree-read-recovery";

    static boolean supports(String scenario) { return SCENARIO.equals(scenario) || RECOVERY.equals(scenario); }

    static boolean fullChatRates(net.minecraft.network.chat.Component message, String output, long amount,
            String perTick, String perSecond) {
        if (!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents summary))
            return message.getSiblings().stream().anyMatch(child -> fullChatRates(child, output, amount, perTick, perSecond));
        if (!summary.getKey().equals("text.ae2craftingtime.chat.summary")) {
            return java.util.Arrays.stream(summary.getArgs()).filter(net.minecraft.network.chat.Component.class::isInstance)
                    .map(net.minecraft.network.chat.Component.class::cast)
                    .anyMatch(child -> fullChatRates(child, output, amount, perTick, perSecond));
        }
        if (summary.getArgs().length != 3 || !output.equals(summary.getArgs()[0])
                || !Long.toString(amount).equals(summary.getArgs()[1].toString())) return false;
        return message.getSiblings().stream().anyMatch(component -> {
            if (!(component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents details))
                return false;
            int start = details.getKey().equals("text.ae2craftingtime.chat.details") ? 5
                    : details.getKey().equals("text.ae2craftingtime.chat.details.rate") ? 1 : -1;
            return start >= 0 && details.getArgs().length == start + 4
                    && perTick.equals(details.getArgs()[start]) && perSecond.equals(details.getArgs()[start + 2]);
        });
    }
    private static final Set<String> SCREENS = Set.of(
            "com.neuvillette.ae2ct.gui.CraftingTreeScreen", "com.vcwdfca.ae2ct.gui.CraftingTreeScreen");

    static boolean isScreen(String name) {
        return SCREENS.contains(name);
    }

    static boolean tooltipReady(UiSnapshot snapshot) {
        return Set.of("text.ae2craftingtime.ttc", "text.ae2craftingtime.details_hint", "text.ae2craftingtime.reset_hint")
                .stream().allMatch(key -> snapshot.tooltip().stream().filter(text -> text.key().equals(key)).count() == 1);
    }

    static UiSnapshot.ObservedText nodeTtcText(String screen, UiSnapshot.ObservedText text) {
        if (!isScreen(screen) || !text.key().equals("literal")
                || !text.rendered().matches("(?:⏱ )?~\\d+(?:s|:\\d{2}(?::\\d{2})?)")) return text;
        return new UiSnapshot.ObservedText("text.ae2craftingtime.tree_ttc", text.rendered(), text.arguments(),
                text.bounds(), text.color(), text.bold());
    }

    static boolean nodeTtcDrawn(UiSnapshot snapshot, UiSnapshot.Row node) {
        return snapshot.text().stream().anyMatch(text -> text.bounds() != null
                && text.key().equals("text.ae2craftingtime.tree_ttc")
                // The centered label starts 21 pixels below the 16-pixel icon, before the Tree pose scales it.
                && Math.abs(text.bounds().centerX() - node.cell().centerX()) <= 2
                && Math.abs(text.bounds().y() - node.cell().y() - node.cell().height() * 21.0 / 16) <= 2);
    }

    private CraftingTreeScenario() {
    }
}
