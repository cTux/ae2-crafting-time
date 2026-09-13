package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import com.ctux.ae2craftingtime.mc1201.PlanRecurrenceClient;
import com.ctux.ae2craftingtime.testdriver.RecurrentPlanObservation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlanRecurrenceClient.class)
public abstract class RecurrentPacketMixin {
    @Inject(method = "receive", at = @At("HEAD"), remap = false)
    private static void receiving(PlanRecurrenceChunk chunk, CallbackInfo ci) {
        RecurrentPlanObservation.receiving(chunk);
    }
}

