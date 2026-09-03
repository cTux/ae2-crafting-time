package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingStatusMenu.class)
public abstract class CraftingStatusMenuMixin {
    @Inject(method = "selectCpu", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$clearWaitingOnCpuSelection(int serial, CallbackInfo ci) {
        var menu = (CraftingStatusMenu) (Object) this;
        if (menu.isClientSide() && menu.getSelectedCpuSerial() != serial) {
            ClientStats.CACHE.clearCpuState();
            ClientStatsRequests.clear();
        }
    }
}
