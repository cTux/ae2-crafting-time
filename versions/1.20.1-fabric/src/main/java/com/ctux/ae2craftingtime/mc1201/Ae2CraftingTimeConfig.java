package com.ctux.ae2craftingtime.mc1201;

public final class Ae2CraftingTimeConfig {
    public static final BooleanValue ENABLED = new BooleanValue(true);
    public static final BooleanValue SHOW_IN_TREE = new BooleanValue(true);
    public static final IntValue MAX_SAMPLES = new IntValue(10);
    public static final DoubleValue OUTLIER_MULTIPLIER = new DoubleValue(4.0);

    public record BooleanValue(boolean value) {
        public boolean get() {
            return value;
        }
    }

    public record IntValue(int value) {
        public int get() {
            return value;
        }
    }

    public record DoubleValue(double value) {
        public double get() {
            return value;
        }
    }

    private Ae2CraftingTimeConfig() {
    }
}
