package com.ctux.ae2craftingtime.mc1201;

import appeng.core.localization.GuiText;
import com.ctux.ae2craftingtime.core.CraftingRowState;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class PlanAmountLines {
    private PlanAmountLines() {
    }

    public static MutableComponent compact(List<Component> lines, long stored, String storedText,
            long craft, String craftText) {
        var expected = List.of(GuiText.FromStorage.text(storedText == null ? "" : storedText),
                GuiText.ToCraft.text(craftText == null ? "" : craftText));
        var positions = new int[] {-1, -1};
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (!(line.getContents() instanceof TranslatableContents text)) continue;
            for (int category = 0; category < expected.size(); category++) {
                var nativeText = (TranslatableContents) expected.get(category).getContents();
                if (!text.getKey().equals(nativeText.getKey())) continue;
                long amount = category == 0 ? stored : craft;
                if (amount <= 0 || positions[category] >= 0 || !line.equals(expected.get(category))) return null;
                positions[category] = i;
            }
        }
        if (stored > 0 && positions[0] < 0 || craft > 0 && positions[1] < 0) return null;
        int first = Math.min(positions[0] < 0 ? lines.size() : positions[0],
                positions[1] < 0 ? lines.size() : positions[1]);
        if (first == lines.size()) return null;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (i == positions[0] || i == positions[1]) lines.remove(i);
        }
        var compact = TtcText.statusAmounts(CraftingRowState.compactPlanAmounts(stored, storedText, craft, craftText));
        lines.add(first, compact);
        return compact;
    }
}
