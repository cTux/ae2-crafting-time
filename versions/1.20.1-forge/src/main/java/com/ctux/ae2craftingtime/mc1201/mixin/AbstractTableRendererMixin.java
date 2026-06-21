package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.me.crafting.AbstractTableRenderer;
import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcColorContext;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Mixin(AbstractTableRenderer.class)
public abstract class AbstractTableRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beginTtcColors(GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<?> entries, int scrollOffset, CallbackInfo ci) {
        if (!((Object) this instanceof CraftConfirmTableRenderer)) {
            return;
        }

        TtcColorContext.set(colors(entries));
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$endTtcColors(GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<?> entries, int scrollOffset, CallbackInfo ci) {
        if ((Object) this instanceof CraftConfirmTableRenderer) {
            TtcColorContext.clear();
        }
    }

    private static Map<ProfileKey, Integer> colors(List<?> entries) {
        var secondsByKey = new HashMap<ProfileKey, Long>();
        var min = Long.MAX_VALUE;
        var max = Long.MIN_VALUE;

        for (var entry : entries) {
            var seconds = seconds(entry);
            if (seconds.isEmpty()) {
                continue;
            }

            var key = ProfilerBridge.key(((CraftingPlanSummaryEntry) entry).getWhat());
            var value = seconds.getAsLong();
            secondsByKey.put(key, value);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        var colors = new HashMap<ProfileKey, Integer>();
        for (var entry : secondsByKey.entrySet()) {
            colors.put(entry.getKey(), TtcColor.forSeconds(entry.getValue(), min, max));
        }
        return colors;
    }

    private static OptionalLong seconds(Object entry) {
        if (!(entry instanceof CraftingPlanSummaryEntry planEntry) || planEntry.getCraftAmount() <= 0) {
            return OptionalLong.empty();
        }

        var key = ProfilerBridge.key(planEntry.getWhat());
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            return OptionalLong.empty();
        }

        return TimeEstimate.seconds(AeKeyAmounts.normalize(planEntry.getWhat(), planEntry.getCraftAmount()),
                stats.get());
    }
}
