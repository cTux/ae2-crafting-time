package com.ctux.ae2craftingtime.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

/** Display stored rates without changing profiling arithmetic or resource units. */
public final class ThroughputNumbers {
    private static final String[] SUFFIXES = {"k", "M", "B", "T", "P", "E"};

    public static String hover(double value, boolean compact) {
        if (!Double.isFinite(value) || value <= 0) return "?";
        return compact ? compact(value) : String.format(Locale.ROOT, "%.2f", value);
    }

    public static String compact(double value) {
        if (!Double.isFinite(value) || value <= 0) return "?";
        if (value < 0.005) return "<0.01";
        var decimal = BigDecimal.valueOf(value);
        if (value < 1000) return decimal.setScale(2, RoundingMode.HALF_UP).toPlainString();
        for (int tier = 0; tier < SUFFIXES.length; tier++) {
            var scaled = decimal.movePointLeft(3 * (tier + 1)).setScale(2, RoundingMode.HALF_UP);
            if (scaled.compareTo(BigDecimal.valueOf(1000)) < 0) {
                return "~" + scaled.stripTrailingZeros().toPlainString() + SUFFIXES[tier];
            }
        }
        var rounded = decimal.round(new MathContext(3, RoundingMode.HALF_UP)).stripTrailingZeros();
        int exponent = rounded.precision() - rounded.scale() - 1;
        return "~" + rounded.movePointLeft(exponent).toPlainString() + "e" + exponent;
    }

    public static String full(double value) {
        if (!Double.isFinite(value) || value <= 0) return "?";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private ThroughputNumbers() {}
}
