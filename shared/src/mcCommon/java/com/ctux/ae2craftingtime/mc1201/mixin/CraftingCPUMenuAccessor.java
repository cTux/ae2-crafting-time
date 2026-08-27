package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.networking.IGrid;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.me.crafting.CraftingCPUMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftingCPUMenu.class)
public interface CraftingCPUMenuAccessor {
    @Accessor(value = "grid", remap = false)
    IGrid ae2craftingtime$getGrid();

    @Accessor(value = "cpu", remap = false)
    CraftingCPUCluster ae2craftingtime$getCpu();
}
