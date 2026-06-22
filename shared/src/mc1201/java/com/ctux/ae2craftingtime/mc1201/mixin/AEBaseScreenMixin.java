package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.mc1201.StatsClickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AEBaseScreen.class)
public abstract class AEBaseScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2craftingtime$clickStats(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (button == 2 && this instanceof StatsClickHandler handler
                && handler.ae2craftingtime$showClickedStats(mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }
}
