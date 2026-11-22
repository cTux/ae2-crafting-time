package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPULogic", remap = false)
public abstract class NeoEcoHeldReturnMixin {
    @Inject(method = "insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void holdFixtureOutput(CallbackInfoReturnable<Long> callback) {
        // Give the real status menu time to show the running job before its last output returns.
        if (NeoEcoStatusFixture.holdsReturns(this)) callback.setReturnValue(0L);
    }
}
