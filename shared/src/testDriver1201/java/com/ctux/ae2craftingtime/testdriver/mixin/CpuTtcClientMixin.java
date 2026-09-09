package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;
import com.ctux.ae2craftingtime.testdriver.CpuTtcPacketControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CpuTtcClient.class, remap = false)
public abstract class CpuTtcClientMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private static void ae2craftingtime_test_driver$receive(CpuTtcPacketCodec.Snapshot snapshot, CallbackInfo ci) {
        if (!CpuTtcPacketControl.intercept(snapshot)) ci.cancel();
    }
}
