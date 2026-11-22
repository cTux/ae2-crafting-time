package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.CraftPlanScenario;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.client.gui.crafting.ECOCraftConfirmScreenIntegration", remap = false)
public abstract class NeoEcoConfirmationFixtureMixin {
    @Inject(method = "route", at = @At("HEAD"), cancellable = true, require = 0)
    private static void keepObservedAe2Plan(CallbackInfo callback) {
        // The fixture watches AE2's plan rows while the server runs the actual ECO CPU.
        if (CraftPlanScenario.keepNativeNeoEcoConfirmation()) callback.cancel();
    }
}
