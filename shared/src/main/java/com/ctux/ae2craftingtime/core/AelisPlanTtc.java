package com.ctux.ae2craftingtime.core;

import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalLong;

/** Exact addon quantities stay exact until they fit the existing estimate boundary. */
public final class AelisPlanTtc {
    public static final int COLUMN_WIDTH = 70;
    private static final BigInteger MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static volatile boolean screenReady;
    private static volatile boolean layoutReady;

    public static void screenReady(boolean ready) { screenReady = ready; }
    public static void layoutReady(boolean ready) { layoutReady = ready; }
    public static boolean ready() { return screenReady && layoutReady; }

    public static int panelCap(int screenWidth, boolean enabled) {
        return enabled && screenWidth >= 300 + COLUMN_WIDTH + 12 ? 360 + COLUMN_WIDTH : 360;
    }

    public static int reservation(int panelWidth) { return panelWidth > 360 ? COLUMN_WIDTH : 0; }

    public static OptionalLong normalized(String outputId, int amountPerUnit, BigInteger amount) {
        if (amount.signum() <= 0) return OptionalLong.empty();
        var normalized = ProfileAmounts.unit(outputId, amountPerUnit) == ProfileUnit.MILLIBUCKET
                ? amount.multiply(BigInteger.valueOf(1000)).divide(BigInteger.valueOf(amountPerUnit)) : amount;
        return normalized.signum() > 0 && normalized.compareTo(MAX) <= 0
                ? OptionalLong.of(normalized.longValueExact()) : OptionalLong.empty();
    }

    public static Optional<TtcEstimate> total(Iterable<Optional<TtcEstimate>> estimates, boolean incomplete) {
        return TimeEstimate.total(estimates, !incomplete);
    }

    private AelisPlanTtc() {}
}
