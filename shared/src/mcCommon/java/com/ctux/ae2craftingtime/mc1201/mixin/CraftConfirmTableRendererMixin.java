package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import com.ctux.ae2craftingtime.mc1201.TtcColorContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CraftConfirmTableRenderer.class)
public abstract class CraftConfirmTableRendererMixin {
    @Inject(method = "getEntryDescription", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$appendVisibleTimeToCraft(CraftingPlanSummaryEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        ae2craftingtime$appendTtc(entry, cir.getReturnValue());
    }

    @Inject(method = "getEntryTooltip", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$appendTooltipTimeToCraft(CraftingPlanSummaryEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        if (entry.getCraftAmount() <= 0) {
            return;
        }

        ae2craftingtime$appendStatsTooltip(entry, cir.getReturnValue());
        cir.getReturnValue().add(TtcText.detailsHint().withStyle(ChatFormatting.GRAY));
        cir.getReturnValue().add(TtcText.resetHint().withStyle(ChatFormatting.GRAY));
    }

    private static void ae2craftingtime$appendTtc(CraftingPlanSummaryEntry entry, List<Component> lines) {
        if (entry.getCraftAmount() <= 0) {
            return;
        }

        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.request(key);
        ClientStats.CACHE.get(key).ifPresentOrElse(stats -> TimeEstimate
                .format(AeKeyAmounts.normalize(entry.getWhat(), entry.getCraftAmount()), stats)
                .ifPresentOrElse(eta -> lines.add(ttcLine(key, eta)),
                        () -> lines.add(TtcText.ttcCollectingData())),
                () -> lines.add(TtcText.ttcCollectingData()));
    }

    private static void ae2craftingtime$appendStatsTooltip(CraftingPlanSummaryEntry entry, List<Component> lines) {
        var key = ProfilerBridge.key(entry.getWhat());
        ClientStats.CACHE.get(key).ifPresentOrElse(stats -> {
            lines.addAll(TtcText.statsLines(stats, ClientStats.CACHE.accuracy(key)));
        }, () -> ClientStatsRequests.request(key));
    }

    private static Component ttcLine(com.ctux.ae2craftingtime.core.ProfileKey key, String eta) {
        var line = TtcText.ttc(eta);
        var color = TtcColorContext.get(key);
        return color.isPresent()
                ? line.withStyle(style -> style.withColor(TextColor.fromRgb(color.getAsInt())).withBold(true))
                : line.withStyle(style -> style.withBold(true));
    }
}
