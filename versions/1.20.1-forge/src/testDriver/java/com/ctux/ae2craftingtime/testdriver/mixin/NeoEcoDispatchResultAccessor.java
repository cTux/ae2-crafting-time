package com.ctux.ae2craftingtime.testdriver.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingDispatchResult", remap = false)
public interface NeoEcoDispatchResultAccessor {
    @Accessor("acceptedCrafts")
    long ae2craftingtime_test_driver$acceptedCrafts();
}
