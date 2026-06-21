package com.ctux.ae2cpd.mc1201.mixin;

import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2cpd.core.TimeEstimate;
import com.ctux.ae2cpd.mc1201.AeKeyAmounts;
import com.ctux.ae2cpd.mc1201.ClientStats;
import com.ctux.ae2cpd.mc1201.ClientStatsRequests;
import com.ctux.ae2cpd.mc1201.ProfilerBridge;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CraftConfirmTableRenderer.class)
public abstract class CraftConfirmTableRendererMixin {
    @Inject(method = "getEntryDescription", at = @At("RETURN"), remap = false)
    private void ae2cpd$appendVisibleTimeToCraft(CraftingPlanSummaryEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        ae2cpd$appendTimeToCraft(entry, cir.getReturnValue());
    }

    @Inject(method = "getEntryTooltip", at = @At("RETURN"), remap = false)
    private void ae2cpd$appendTooltipTimeToCraft(CraftingPlanSummaryEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        ae2cpd$appendTimeToCraft(entry, cir.getReturnValue());
    }

    private static void ae2cpd$appendTimeToCraft(CraftingPlanSummaryEntry entry, List<Component> lines) {
        if (entry.getCraftAmount() <= 0) {
            return;
        }

        var key = ProfilerBridge.key(entry.getWhat());
        ClientStats.CACHE.get(key).ifPresentOrElse(stats -> TimeEstimate
                .format(AeKeyAmounts.normalize(entry.getWhat(), entry.getCraftAmount()), stats)
                .ifPresent(eta -> lines.add(Component.literal("Time To Craft: " + eta))),
                () -> ClientStatsRequests.request(key));
    }
}
