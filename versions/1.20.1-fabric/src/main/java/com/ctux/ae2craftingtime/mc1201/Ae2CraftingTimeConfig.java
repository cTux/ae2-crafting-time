package com.ctux.ae2craftingtime.mc1201;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Ae2CraftingTimeConfig {
    public static final BooleanValue ENABLED = new BooleanValue(true);
    public static final BooleanValue SHOW_IN_TREE = new BooleanValue(true);
    public static final BooleanValue SHOW_CHAT_MESSAGES = new BooleanValue(true);
    public static final IntValue MAX_SAMPLES = new IntValue(10);
    public static final DoubleValue OUTLIER_MULTIPLIER = new DoubleValue(4.0);

    public static void load(Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            for (var line : Files.readAllLines(path)) {
                var parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                set(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException ignored) {
            // ponytail: keep defaults if Fabric config cannot be read.
        }
    }

    private static void set(String key, String value) {
        switch (key) {
            case "enabled" -> ENABLED.set(parseBoolean(value, ENABLED.get()));
            case "showInTree" -> SHOW_IN_TREE.set(parseBoolean(value, SHOW_IN_TREE.get()));
            case "showChatMessages" -> SHOW_CHAT_MESSAGES.set(parseBoolean(value, SHOW_CHAT_MESSAGES.get()));
            case "maxSamples" -> MAX_SAMPLES.set(parseInt(value, MAX_SAMPLES.get(), 1, 100));
            case "outlierMultiplier" -> OUTLIER_MULTIPLIER.set(parseDouble(value, OUTLIER_MULTIPLIER.get(), 1.0, 1000.0));
            default -> {
            }
        }
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }

    private static double parseDouble(String value, double fallback, double min, double max) {
        try {
            return Math.max(min, Math.min(max, Double.parseDouble(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static final class BooleanValue {
        private boolean value;

        private BooleanValue(boolean value) {
            this.value = value;
        }

        public boolean get() {
            return value;
        }

        private void set(boolean value) {
            this.value = value;
        }
    }

    public static final class IntValue {
        private int value;

        private IntValue(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        private void set(int value) {
            this.value = value;
        }
    }

    public static final class DoubleValue {
        private double value;

        private DoubleValue(double value) {
            this.value = value;
        }

        public double get() {
            return value;
        }

        private void set(double value) {
            this.value = value;
        }
    }

    private Ae2CraftingTimeConfig() {
    }
}
