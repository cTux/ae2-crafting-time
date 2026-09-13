package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import com.ctux.ae2craftingtime.core.RecurrentMissing;
import com.ctux.ae2craftingtime.mc1201.PlanRecurrence;
import java.util.HashSet;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingCalculation.class)
public abstract class CraftingCalculationMixin implements com.ctux.ae2craftingtime.mc1201.RecurrentCalculation {
    @Unique private final Set<AEKey> ae2craftingtime$observed = new HashSet<>();

    @Inject(method = "runCraftAttempt", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$reset(boolean simulation, long amount, CallbackInfoReturnable<CraftingPlan> cir) {
        ae2craftingtime$observed.clear();
    }

    @Inject(method = "runCraftAttempt", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$attach(boolean simulation, long amount, CallbackInfoReturnable<CraftingPlan> cir) {
        var plan = cir.getReturnValue();
        if (plan == null) return;
        var positive = new HashSet<AEKey>();
        for (var entry : plan.missingItems()) if (entry.getLongValue() > 0) positive.add(entry.getKey());
        ((PlanRecurrence) (Object) plan).ae2craftingtime$recurrentKeys(
                RecurrentMissing.finalKeys(simulation, ae2craftingtime$observed, positive));
    }

    @Unique public void ae2craftingtime$record(AEKey key) { ae2craftingtime$observed.add(key); }
}
