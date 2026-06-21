package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.me.crafting.CraftingStatusScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatus;
import appeng.menu.me.crafting.CraftingStatusEntry;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

@Mixin(CraftingCPUScreen.class)
public abstract class CraftingCPUScreenMixin<T extends CraftingCPUMenu> extends AEBaseScreen<T> {
    @Unique
    private int ae2craftingtime$ttcSortMode;

    @Shadow(remap = false)
    private CraftingStatus status;

    protected CraftingCPUScreenMixin(T menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$addStatusTtcSortButton(T menu, Inventory playerInventory, Component title,
            ScreenStyle style, CallbackInfo ci) {
        if ((Object) this instanceof CraftingStatusScreen) {
            addToLeftToolbar(new TtcSortButton(this::ae2craftingtime$cycleTtcSortMode,
                    () -> ae2craftingtime$ttcSortMode));
        }
    }

    @ModifyArg(method = "postUpdate", at = @At(value = "INVOKE", target = "Lappeng/menu/me/crafting/CraftingStatus;<init>(ZJJJLjava/util/List;)V"), index = 4, remap = false)
    private List<CraftingStatusEntry> ae2craftingtime$sortStatusByTtc(List<CraftingStatusEntry> entries) {
        if (!((Object) this instanceof CraftingStatusScreen) || ae2craftingtime$ttcSortMode == 0) {
            return entries;
        }

        return TtcSort.copySorted(entries, CraftingCPUScreenMixin::ae2craftingtime$seconds,
                Comparator.naturalOrder(), ae2craftingtime$ttcSortMode == 2);
    }

    @Inject(method = "drawFG", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$drawStatusTotalTtc(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX,
            int mouseY, CallbackInfo ci) {
        if (!((Object) this instanceof CraftingStatusScreen) || status == null) {
            return;
        }

        var estimates = new ArrayList<OptionalLong>();
        for (var entry : status.getEntries()) {
            estimates.add(ae2craftingtime$seconds(entry));
        }

        TimeEstimate.formatTotal(estimates).ifPresent(eta -> {
            var text = Component.literal("Total TTC: " + eta);
            var font = getMinecraft().font;
            guiGraphics.drawString(font, text, 109 - font.width(text) / 2, 162, 0x404040, false);
        });
    }

    @Unique
    private void ae2craftingtime$cycleTtcSortMode() {
        ae2craftingtime$ttcSortMode = (ae2craftingtime$ttcSortMode + 1) % 3;
        if (status != null) {
            postUpdate(status);
        }
    }

    @Shadow(remap = false)
    public abstract void postUpdate(CraftingStatus status);

    @Unique
    private static OptionalLong ae2craftingtime$seconds(CraftingStatusEntry entry) {
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return OptionalLong.empty();
        }

        var key = ProfilerBridge.key(entry.getWhat());
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            return OptionalLong.empty();
        }

        return TimeEstimate.seconds(AeKeyAmounts.normalize(entry.getWhat(), amount), stats.get());
    }
}
