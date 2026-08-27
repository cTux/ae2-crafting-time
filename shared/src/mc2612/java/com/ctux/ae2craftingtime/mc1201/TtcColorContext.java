package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class TtcColorContext {
    private static final Map<ProfileKey, Integer> COLORS = new HashMap<>();

    public static void set(Map<ProfileKey, Integer> colors) {
        COLORS.clear();
        COLORS.putAll(colors);
    }

    public static OptionalInt get(ProfileKey key) {
        var color = COLORS.get(key);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }

    public static void clear() {
        COLORS.clear();
    }

    private TtcColorContext() {
    }
}
