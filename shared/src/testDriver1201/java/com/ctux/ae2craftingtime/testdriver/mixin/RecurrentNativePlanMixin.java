package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingPlanSummary;
import com.ctux.ae2craftingtime.testdriver.RecurrentPlanObservation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftConfirmMenu.class)
public abstract class RecurrentNativePlanMixin {
    @Inject(method = "setPlan", at = @At("RETURN"), remap = false)
    private void installed(CraftingPlanSummary plan, CallbackInfo ci) {
        RecurrentPlanObservation.installed((CraftConfirmMenu) (Object) this);
    }
}

