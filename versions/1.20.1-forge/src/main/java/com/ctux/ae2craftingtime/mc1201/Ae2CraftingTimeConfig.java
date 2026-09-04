package com.ctux.ae2craftingtime.mc1201;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Ae2CraftingTimeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHOW_IN_TREE;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHAT_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_DELAYED;
    public static final ForgeConfigSpec.IntValue MAX_SAMPLES;
    public static final ForgeConfigSpec.DoubleValue OUTLIER_MULTIPLIER;

    static {
        var builder = new ForgeConfigSpec.Builder();

        ENABLED = builder
                .comment("Track and show real AE2 crafting performance.")
                .define("enabled", true);

        SHOW_IN_TREE = builder
                .comment("Show TTC stats in AE2: Crafting Tree when it is installed.")
                .define("showInTree", true);

        SHOW_CHAT_MESSAGES = builder
                .comment("Post Ctrl-click TTC details and reset notices in chat.")
                .define("showChatMessages", true);

        NOTIFY_ON_DELAYED = builder
                .comment("Privately notify the craft owner when an output becomes delayed.")
                .define("notifyOnDelayed", true);

        MAX_SAMPLES = builder
                .comment("How many recent samples to keep for each crafted output.")
                .defineInRange("maxSamples", 10, 1, 100);

        OUTLIER_MULTIPLIER = builder
                .comment("Ignore samples this many times slower or faster than the median.")
                .defineInRange("outlierMultiplier", 4.0, 1.0, 1000.0);

        SPEC = builder.build();
    }

    private Ae2CraftingTimeConfig() {
    }
}
