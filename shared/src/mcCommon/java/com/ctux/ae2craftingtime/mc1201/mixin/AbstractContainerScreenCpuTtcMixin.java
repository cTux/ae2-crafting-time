package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Official-names lifecycle hook for the NeoForge targets. */
@Mixin(Screen.class)
public abstract class AbstractContainerScreenCpuTtcMixin {
    @Inject(method = "removed", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$clearCpuTtcOnClose(CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?> screen
                && screen.getMenu() instanceof CraftingStatusMenu menu) {
            CpuTtcClient.close(menu);
        }
    }
}
