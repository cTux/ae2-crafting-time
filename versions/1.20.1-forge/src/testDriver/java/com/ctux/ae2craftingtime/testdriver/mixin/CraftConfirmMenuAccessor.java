package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftConfirmMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftConfirmMenu.class)
public interface CraftConfirmMenuAccessor {
    @Accessor(value = "selectedCpu", remap = false)
    ICraftingCPU ae2craftingtime_test_driver$selectedCpu();
}
