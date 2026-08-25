package com.ctux.ae2craftingtime.core;

public final class TtcColor {
    public static final int GREEN = 0x55FF55;
    public static final int YELLOW = 0xFFFF55;
    public static final int RED = 0xFF5555;

    public static int forSeconds(long seconds, long minSeconds, long maxSeconds) {
        if (maxSeconds <= minSeconds) {
            return GREEN;
        }
        var ratio = Math.max(0, Math.min(1, (double) (seconds - minSeconds) / (maxSeconds - minSeconds)));
        return ratio <= 0.5
                ? interpolate(GREEN, YELLOW, ratio * 2)
                : interpolate(YELLOW, RED, (ratio - 0.5) * 2);
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
