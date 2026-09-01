package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.menu.me.crafting.CraftConfirmMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftConfirmMenu.class)
public interface CraftConfirmMenuAccessor {
    @Accessor(value = "selectedCpu", remap = false)
    void ae2craftingtime_test_driver$selectedCpu(ICraftingCPU cpu);

    @Accessor(value = "result", remap = false)
    ICraftingPlan ae2craftingtime_test_driver$result();
}
