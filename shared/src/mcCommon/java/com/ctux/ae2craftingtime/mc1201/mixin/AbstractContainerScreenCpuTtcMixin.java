package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenCpuTtcMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void ae2craftingtime$clearCpuTtcOnClose(CallbackInfo ci) {
        if (((AbstractContainerScreen<?>) (Object) this).getMenu() instanceof CraftingStatusMenu menu) {
            CpuTtcClient.close(menu);
        }
    }
}
