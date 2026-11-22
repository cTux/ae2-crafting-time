package com.ctux.ae2craftingtime.core;

import java.math.BigInteger;

public final class NeoEcoStatusRows {
    private NeoEcoStatusRows() {}

    public static long remaining(BigInteger active, BigInteger scheduled) {
        if (active.signum() < 0 || scheduled.signum() < 0) return 0;
        var total = active.add(scheduled);
        return total.bitLength() <= Long.SIZE - 1 ? total.longValue() : 0;
    }

    public static TtcEstimate decode(double millis, boolean reliable) {
        return Double.isFinite(millis) && millis >= 0 ? new TtcEstimate(millis, reliable) : null;
    }

    public static boolean fits(int nativeLines, int fontHeight, int textWidth) {
        // NeoEco draws at half scale, inserts a pixel between lines, and reserves the right of the cell for the icon.
        return textWidth <= 88 && ((long) nativeLines + 1) * fontHeight + 2L * nativeLines <= 44;
    }
}
