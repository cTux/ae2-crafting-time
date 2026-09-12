package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.mc1201.CpuTtcRequests;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;
import com.ctux.ae2craftingtime.testdriver.CpuTtcPacketControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CpuTtcRequests.class, remap = false)
public abstract class CpuTtcRequestsMixin {
    @Inject(method = "enabled", at = @At("HEAD"), cancellable = true)
    private static void ae2craftingtime_test_driver$enabled(CallbackInfoReturnable<Boolean> cir) {
        var available = CpuTtcPacketControl.channelAvailable();
        if (available != null) cir.setReturnValue(available);
    }

    @Inject(method = "send", at = @At("HEAD"))
    private static void ae2craftingtime_test_driver$request(CpuTtcPacketCodec.Request request, CallbackInfo ci) {
        CpuTtcPacketControl.observeRequest(request);
    }
}
