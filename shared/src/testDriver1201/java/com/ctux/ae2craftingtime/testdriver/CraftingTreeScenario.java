package com.ctux.ae2craftingtime.testdriver;

import java.util.Set;

final class CraftingTreeScenario {
    static final String SCENARIO = "crafting-tree-screen";
    static final String RECOVERY = "crafting-tree-read-recovery";

    static boolean supports(String scenario) { return SCENARIO.equals(scenario) || RECOVERY.equals(scenario); }

    static boolean fullChatRates(net.minecraft.network.chat.Component message, String output, long amount,
            String perMillisecond, String perSecond) {
        if (!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents summary))
            return message.getSiblings().stream().anyMatch(child -> fullChatRates(child, output, amount, perMillisecond, perSecond));
        if (!summary.getKey().equals("text.ae2craftingtime.chat.summary")) {
            return java.util.Arrays.stream(summary.getArgs()).filter(net.minecraft.network.chat.Component.class::isInstance)
                    .map(net.minecraft.network.chat.Component.class::cast)
                    .anyMatch(child -> fullChatRates(child, output, amount, perMillisecond, perSecond));
        }
        if (summary.getArgs().length != 3 || !output.equals(summary.getArgs()[0])
                || !Long.toString(amount).equals(summary.getArgs()[1].toString())) return false;
        return message.getSiblings().stream().anyMatch(component -> {
            if (!(component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents details))
                return false;
            if (details.getKey().equals("text.ae2craftingtime.chat.details.ms"))
                return details.getArgs().length == 7 && perSecond.equals(details.getArgs()[5]);
            return details.getKey().equals("text.ae2craftingtime.chat.details.rate.ms") && details.getArgs().length == 5
                    && perMillisecond.equals(details.getArgs()[1]) && perSecond.equals(details.getArgs()[3]);
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
                || !text.rendered().matches("(?:⌚ )?(?:~\\d+(?:s|:\\d{2}(?::\\d{2})?)|\\d+ms)\\??|(?:🥇 )?Instant")) return text;
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
