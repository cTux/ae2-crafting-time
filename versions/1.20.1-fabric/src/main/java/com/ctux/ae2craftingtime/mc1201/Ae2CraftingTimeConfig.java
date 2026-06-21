package com.ctux.ae2craftingtime.mc1201;

public final class Ae2CraftingTimeConfig {
    public static final BooleanValue ENABLED = new BooleanValue(true);
    public static final BooleanValue SHOW_IN_TREE = new BooleanValue(true);

    public record BooleanValue(boolean value) {
        public boolean get() {
            return value;
        }
    }

    private Ae2CraftingTimeConfig() {
    }
}
