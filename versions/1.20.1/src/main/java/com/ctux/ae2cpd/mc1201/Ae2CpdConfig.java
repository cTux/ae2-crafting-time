package com.ctux.ae2cpd.mc1201;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Ae2CpdConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue SAMPLES;
    public static final ForgeConfigSpec.BooleanValue SHOW_IN_TREE;

    static {
        var builder = new ForgeConfigSpec.Builder();

        ENABLED = builder
                .comment("Collect and display AE2 craft performance samples.")
                .define("enabled", true);

        SAMPLES = builder
                .comment("Latest samples kept in RAM per output key.")
                .defineInRange("samples", 20, 1, 1000);

        SHOW_IN_TREE = builder
                .comment("Show stats in AE2: Crafting Tree when that mod is installed.")
                .define("showInTree", true);

        SPEC = builder.build();
    }

    private Ae2CpdConfig() {
    }
}
