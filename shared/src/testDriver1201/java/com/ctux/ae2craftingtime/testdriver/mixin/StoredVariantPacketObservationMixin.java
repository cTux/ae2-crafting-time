package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import com.ctux.ae2craftingtime.mc1201.PlanStoredVariantsClient;
import com.ctux.ae2craftingtime.testdriver.StoredVariantObservation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlanStoredVariantsClient.class, remap = false)
public abstract class StoredVariantPacketObservationMixin {
    @Inject(method = "receive", at = @At("HEAD"))
    private static void receiving(PlanRecurrenceChunk chunk, long revision, CallbackInfo ci) {
        StoredVariantObservation.receiving(chunk);
    }
}
