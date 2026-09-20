package com.ctux.ae2craftingtime.core;

public final class ConfigNumbers {
    public static double parseDouble(String value, double fallback, double min, double max) {
        try {
            var parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? Math.max(min, Math.min(max, parsed)) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private ConfigNumbers() {
    }
}
