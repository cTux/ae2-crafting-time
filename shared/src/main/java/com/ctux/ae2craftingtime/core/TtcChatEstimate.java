package com.ctux.ae2craftingtime.core;

import java.util.Optional;

/** Metadata survives vanilla's string conversion of chat translation arguments. */
public final class TtcChatEstimate {
    public static final String RELIABLE = "text.ae2craftingtime.estimate.reliable";
    public static final String UNRELIABLE = "text.ae2craftingtime.estimate.unreliable";

    public static String key(TtcEstimate estimate) {
        return estimate.reliable() ? RELIABLE : UNRELIABLE;
    }

    public static Optional<TtcEstimate> read(String key, Object[] arguments) {
        if ((!RELIABLE.equals(key) && !UNRELIABLE.equals(key)) || arguments.length != 2
                || !(arguments[1] instanceof String value) || value.length() > 32) return Optional.empty();
        try {
            var duration = Double.parseDouble(value);
            return Double.isFinite(duration) && duration >= 0
                    ? Optional.of(new TtcEstimate(duration, RELIABLE.equals(key))) : Optional.empty();
        } catch (NumberFormatException invalid) {
            return Optional.empty();
        }
    }

    private TtcChatEstimate() { }
}
