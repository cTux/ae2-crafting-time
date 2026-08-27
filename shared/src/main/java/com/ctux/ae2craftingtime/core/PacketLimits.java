package com.ctux.ae2craftingtime.core;

import java.util.List;
import java.util.regex.Pattern;

public final class PacketLimits {
    public static final int MAX_KEYS = 256;
    public static final int MAX_OUTPUT_ID_LENGTH = 128;
    public static final int MAX_SAMPLES = 100;
    private static final Pattern OUTPUT_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public static int checkedSize(int size, int maximum, String field) {
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException(field + " size must be between 0 and " + maximum);
        }
        return size;
    }

    public static List<String> checkedKeys(List<String> keys) {
        checkedSize(keys.size(), MAX_KEYS, "keys");
        if (keys.stream().anyMatch(key -> key == null || key.length() > MAX_OUTPUT_ID_LENGTH
                || !OUTPUT_ID.matcher(key).matches())) {
            throw new IllegalArgumentException("invalid output id");
        }
        return List.copyOf(keys);
    }

    public static String checkedOutputId(String outputId) {
        return checkedKeys(List.of(outputId)).get(0);
    }

    private PacketLimits() {
    }
}
