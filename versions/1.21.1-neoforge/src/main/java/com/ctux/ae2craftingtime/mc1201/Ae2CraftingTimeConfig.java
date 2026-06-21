package com.ctux.ae2craftingtime.mc1201;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Ae2CraftingTimeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_IN_TREE;

    static {
        var builder = new ModConfigSpec.Builder();

        ENABLED = builder
                .comment("Collect and display AE2 craft performance samples.")
                .define("enabled", true);

        SHOW_IN_TREE = builder
                .comment("Show stats in AE2: Crafting Tree when that mod is installed.")
                .define("showInTree", true);

        SPEC = builder.build();
    }

    private Ae2CraftingTimeConfig() {
    }
}
