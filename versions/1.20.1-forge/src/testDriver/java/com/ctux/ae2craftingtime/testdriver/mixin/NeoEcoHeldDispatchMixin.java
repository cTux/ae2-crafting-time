package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPULogic", remap = false)
public abstract class NeoEcoHeldDispatchMixin {
    @Inject(method = "executeCrafting",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void holdFixtureDispatch(CallbackInfoReturnable<Integer> callback) {
        // Leave the real job active until the status menu has rendered it.
        if (NeoEcoStatusFixture.holdsDispatch(this)) callback.setReturnValue(0);
    }
}
