package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.ctux.ae2craftingtime.mc1201.ProviderDispatchContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderExternalPushMixin {
    @Redirect(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/crafting/IPatternDetails;supportsPushInputsToExternalInventory()Z"), remap = false)
    private boolean ae2craftingtime$observeExternalPush(IPatternDetails pattern) {
        var supported = pattern.supportsPushInputsToExternalInventory();
        ProviderDispatchContext.externalPush(this, supported);
        return supported;
    }
}
