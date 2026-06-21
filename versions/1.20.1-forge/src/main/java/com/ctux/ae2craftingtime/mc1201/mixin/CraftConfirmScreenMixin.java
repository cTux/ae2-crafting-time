package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcSort;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

@Mixin(CraftConfirmScreen.class)
public abstract class CraftConfirmScreenMixin extends AEBaseScreen<CraftConfirmMenu> {
    @Unique
    private int ae2craftingtime$ttcSortMode;

    protected CraftConfirmScreenMixin(CraftConfirmMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$addTtcSortButton(CraftConfirmMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style, CallbackInfo ci) {
        ClientStats.CACHE.clear();
        ClientStatsRequests.clear();
        addToLeftToolbar(new TtcSortButton(this::ae2craftingtime$cycleTtcSortMode,
                () -> ae2craftingtime$ttcSortMode));
    }

    @ModifyArg(method = "drawFG", at = @At(value = "INVOKE", target = "Lappeng/client/gui/me/crafting/CraftConfirmTableRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;IILjava/util/List;I)V"), index = 3, remap = false)
    private List<CraftingPlanSummaryEntry> ae2craftingtime$sortPlanByTtc(List<CraftingPlanSummaryEntry> entries) {
        if (ae2craftingtime$ttcSortMode == 0) {
            return entries;
        }

        return TtcSort.copySorted(entries, CraftConfirmScreenMixin::ae2craftingtime$seconds, Comparator.naturalOrder(),
                ae2craftingtime$ttcSortMode == 2);
    }

    @Inject(method = "drawFG", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$drawTotalTtc(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        var plan = getMenu().getPlan();
        if (plan == null) {
            return;
        }

        var estimates = new ArrayList<OptionalLong>();
        for (var entry : plan.getEntries()) {
            estimates.add(ae2craftingtime$seconds(entry));
        }

        TimeEstimate.formatTotal(estimates).ifPresent(eta -> {
            var text = Component.literal("Total TTC: " + eta);
            var font = getMinecraft().font;
            guiGraphics.drawString(font, text, 109 - font.width(text) / 2, 178, 0x404040, false);
        });
    }

    @Unique
    private void ae2craftingtime$cycleTtcSortMode() {
        ae2craftingtime$ttcSortMode = (ae2craftingtime$ttcSortMode + 1) % 3;
    }

    @Unique
    private static OptionalLong ae2craftingtime$seconds(CraftingPlanSummaryEntry entry) {
        if (entry.getCraftAmount() <= 0) {
            return OptionalLong.empty();
        }

        var key = ProfilerBridge.key(entry.getWhat());
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            return OptionalLong.empty();
        }

        return TimeEstimate.seconds(AeKeyAmounts.normalize(entry.getWhat(), entry.getCraftAmount()), stats.get());
    }
}
