package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.DispatchObservation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic", remap = false)
public abstract class NeoEcoFastPathObservationMixin {
    // Driver only: this overload is absent on retained older artifacts.
    @Inject(method = "recordPushedPattern(Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob;Lcn/dancingsnow/neoecoae/impl/crafting/fastpath/ECOExtractedPatternExecution;JZ)V",
            at = @At("HEAD"), require = 0)
    private void observe(@Coerce Object job, @Coerce Object execution, long count, boolean fast, CallbackInfo ci) {
        if (fast) DispatchObservation.fastPath(this, count);
    }
}
