package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.RowTextColor;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a small learned TTC line inside the native Trinity material cell. */
@Pseudo
@Mixin(targets = "com.fish_dan_.data_energistics.client.screen.crafting.confirm.TrinityCraftConfirmMaterialTable", remap = false)
public abstract class TrinityMaterialTableMixin {
    @Inject(method = "drawEntry", at = @At("RETURN"), remap = false, require = 0)
    private void ae2craftingtime$drawTtc(GuiGraphics graphics, Minecraft minecraft, Font font,
            CraftingPlanSummaryEntry entry, int x, int y, CallbackInfo ci) {
        if (!ClientOptionsRuntime.enabled(OptionFeature.PLAN_ROWS) || !ClientOptionsRuntime.profilingEnabled()
                || entry.getCraftAmount() <= 0) return;
        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.requestBackground(key);
        ClientStats.CACHE.get(key).flatMap(stats -> TimeEstimate.milliseconds(
                AeKeyAmounts.normalize(entry.getWhat(), entry.getCraftAmount()), stats)).ifPresent(eta -> {
            var label = TtcText.ttc(eta);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 2, y + 17, 200);
            graphics.pose().scale(0.5f, 0.5f, 1);
            TtcBadge.fillRoundedRect(graphics, -2, -2, font.width(label) + 2,
                    font.lineHeight + 2, TtcBadge.BACKGROUND);
            graphics.drawString(font, label, 0, 0,
                    RowTextColor.planTotalColor(ClientOptionsRuntime.current()),
                    ClientOptionsRuntime.textShadow());
            graphics.pose().popPose();
        });
    }
}
