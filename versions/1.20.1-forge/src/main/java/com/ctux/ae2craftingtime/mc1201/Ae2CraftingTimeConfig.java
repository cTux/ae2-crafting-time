package com.ctux.ae2craftingtime.mc1201;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Ae2CraftingTimeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHOW_IN_TREE;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHAT_MESSAGES;
    public static final ForgeConfigSpec.IntValue MAX_SAMPLES;
    public static final ForgeConfigSpec.DoubleValue OUTLIER_MULTIPLIER;

    static {
        var builder = new ForgeConfigSpec.Builder();

        ENABLED = builder
                .comment("Collect and display AE2 craft performance samples.")
                .define("enabled", true);

        SHOW_IN_TREE = builder
                .comment("Show stats in AE2: Crafting Tree when that mod is installed.")
                .define("showInTree", true);

        SHOW_CHAT_MESSAGES = builder
                .comment("Broadcast Ctrl-click TTC details and reset notices to chat.")
                .define("showChatMessages", true);

        MAX_SAMPLES = builder
                .comment("Number of recent completed samples retained per crafted output.")
                .defineInRange("maxSamples", 10, 1, 100);

        OUTLIER_MULTIPLIER = builder
                .comment("Ignore throughput samples slower or faster than median by this multiplier.")
                .defineInRange("outlierMultiplier", 4.0, 1.0, 1000.0);

        SPEC = builder.build();
    }

    private Ae2CraftingTimeConfig() {
    }
}
