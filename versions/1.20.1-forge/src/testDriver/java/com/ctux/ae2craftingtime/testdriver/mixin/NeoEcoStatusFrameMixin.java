package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuStatusPanel", remap = false)
public abstract class NeoEcoStatusFrameMixin {
    @Shadow private long estimatedRemainingMillis;

    @Inject(method = "drawBackgroundAdditional", at = @At("TAIL"), require = 0)
    private void observeRenderedEstimate(CallbackInfo callback) {
        NeoEcoStatusFixture.rendered(estimatedRemainingMillis);
    }
}
