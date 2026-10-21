package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.stacks.AEKey;
import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.testdriver.TrinityGraphObservation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ProfilerBridge.class, remap = false)
public abstract class TrinityRestoredGraphObservationMixin {
    @Shadow private static CraftProfiler PROFILER;

    @Inject(method = "completeRestoredEstimate", at = @At("RETURN"), remap = false)
    private static void ae2craftingtime_test_driver$advanced(String networkId, Object scope,
            AEKey what, long amount, CallbackInfo ci) {
        if (scope != null && scope.getClass().getName().startsWith(
                "com.fish_dan_.data_energistics.common.crafting.trinity."))
            PROFILER.jobEstimate(scope).ifPresent(estimate -> TrinityGraphObservation.capture(scope, estimate));
    }
}
