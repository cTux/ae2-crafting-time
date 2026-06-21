package com.ctux.ae2craftingtime.mc1201;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Ae2CraftingTime.MOD_ID)
public final class Ae2CraftingTime {
    public static final String MOD_ID = "ae2craftingtime";

    public Ae2CraftingTime() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Ae2CraftingTimeConfig.SPEC);
        StatsNetwork.register();
    }
}
