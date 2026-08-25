package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.me.crafting.CraftingStatusTableRenderer;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcColorContext;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CraftingStatusTableRenderer.class)
public abstract class CraftingStatusTableRendererMixin {
    @Inject(method = "getEntryDescription", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$appendVisibleTimeToCraft(CraftingStatusEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        ae2craftingtime$appendTtc(entry, cir.getReturnValue());
    }

    @Inject(method = "getEntryTooltip", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$appendTooltipTimeToCraft(CraftingStatusEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return;
        }

        ae2craftingtime$appendStatsTooltip(entry, cir.getReturnValue());
        cir.getReturnValue().add(TtcText.detailsHint().withStyle(ChatFormatting.GRAY));
        cir.getReturnValue().add(TtcText.resetHint().withStyle(ChatFormatting.GRAY));
    }

    private static void ae2craftingtime$appendTtc(CraftingStatusEntry entry, List<Component> lines) {
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return;
        }

        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.request(key);
        ClientStats.CACHE.get(key).ifPresent(stats -> TimeEstimate
                .format(AeKeyAmounts.normalize(entry.getWhat(), amount), stats)
                .ifPresent(eta -> lines.add(ttcLine(key, eta))));
    }

    private static void ae2craftingtime$appendStatsTooltip(CraftingStatusEntry entry, List<Component> lines) {
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        var key = ProfilerBridge.key(entry.getWhat());
        var normalized = AeKeyAmounts.normalize(entry.getWhat(), amount);
        ClientStatsRequests.request(key);
        ClientStats.CACHE.get(key).ifPresent(stats -> {
            lines.addAll(TtcText.statsLines(entry.getWhat().getDisplayName().getString(), normalized, stats,
                    ClientStats.CACHE.accuracy(key)));
        });
    }

    private static Component ttcLine(com.ctux.ae2craftingtime.core.ProfileKey key, String eta) {
        var line = TtcText.ttc(eta);
        var color = TtcColorContext.get(key);
        return color.isPresent()
                ? line.withStyle(style -> style.withColor(TextColor.fromRgb(color.getAsInt())).withBold(true))
                : line.withStyle(style -> style.withBold(true));
    }
}
