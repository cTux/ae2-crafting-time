package com.ctux.ae2craftingtime.testdriver.mixin;

import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.ctux.ae2craftingtime.testdriver.DispatchObservation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingFastPathDispatcher", remap = false)
public abstract class NeoEcoExternalFastPathObservationMixin {
    @Shadow @Final private Object dynamicOutputOwner;

    @Inject(method = "tryDispatch", at = @At("RETURN"), require = 0)
    private void observe(CallbackInfoReturnable<Object> callback) {
        if (callback.getReturnValue() instanceof NeoEcoDispatchResultAccessor dispatch
                && DispatchObservation.scope() instanceof ECOCraftingCPU cpu && cpu.getLogic() == dynamicOutputOwner) {
            DispatchObservation.fastPath(cpu, dispatch.ae2craftingtime_test_driver$acceptedCrafts());
        }
    }
}
