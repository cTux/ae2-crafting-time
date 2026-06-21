package com.ctux.ae2cpd.mc1201;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Ae2CraftPerformanceDebugger.MOD_ID)
public final class Ae2CraftPerformanceDebugger {
    public static final String MOD_ID = "ae2cpd";

    public Ae2CraftPerformanceDebugger() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Ae2CpdConfig.SPEC);
    }
}
