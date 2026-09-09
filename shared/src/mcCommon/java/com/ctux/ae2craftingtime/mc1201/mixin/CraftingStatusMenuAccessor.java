package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingStatusMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.WeakHashMap;
import com.google.common.collect.ImmutableSet;

@Mixin(CraftingStatusMenu.class)
public interface CraftingStatusMenuAccessor {
    @Accessor(value = "cpuSerialMap", remap = false)
    WeakHashMap<ICraftingCPU, Integer> ae2craftingtime$getCpuSerialMap();

    @Accessor(value = "lastCpuSet", remap = false)
    ImmutableSet<ICraftingCPU> ae2craftingtime$getLastCpuSet();
}
