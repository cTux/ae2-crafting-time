package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingCPUMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CraftingCPUMenu.class)
public interface CraftingCPUMenuAccessor {
    @Invoker(value = "setCPU", remap = false)
    void ae2craftingtime_test_driver$setCpu(ICraftingCPU cpu);
}
