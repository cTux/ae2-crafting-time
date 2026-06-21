package com.ctux.ae2craftingtime.core;

public final class TtcColor {
    public static final int DARK_GREEN = 0x006400;
    public static final int DARK_YELLOW = 0x8B8000;
    public static final int DARK_RED = 0x8B0000;

    public static int forSeconds(long seconds, long minSeconds, long maxSeconds) {
        if (maxSeconds <= minSeconds) {
            return DARK_GREEN;
        }
        var ratio = Math.max(0, Math.min(1, (double) (seconds - minSeconds) / (maxSeconds - minSeconds)));
        return ratio <= 0.5
                ? interpolate(DARK_GREEN, DARK_YELLOW, ratio * 2)
                : interpolate(DARK_YELLOW, DARK_RED, (ratio - 0.5) * 2);
    }

    private static int interpolate(int from, int to, double ratio) {
        var red = channel(from, 16) + (int) Math.round((channel(to, 16) - channel(from, 16)) * ratio);
        var green = channel(from, 8) + (int) Math.round((channel(to, 8) - channel(from, 8)) * ratio);
        var blue = channel(from, 0) + (int) Math.round((channel(to, 0) - channel(from, 0)) * ratio);
        return red << 16 | green << 8 | blue;
    }

    private static int channel(int color, int shift) {
        return color >> shift & 0xFF;
    }

    private TtcColor() {
    }
}
