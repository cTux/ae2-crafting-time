package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remapped lifecycle hook for the obfuscated 1.20.1 targets. */
@Mixin(Screen.class)
public abstract class AbstractContainerScreenCpuTtcMixinSrg {
    @Inject(method = "removed", at = @At("HEAD"))
    private void ae2craftingtime$clearCpuTtcOnClose(CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?> screen
                && screen.getMenu() instanceof CraftingStatusMenu menu) {
            CpuTtcClient.close(menu);
        }
    }
}
