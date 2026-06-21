package com.ctux.ae2craftingtime.core;

import java.util.Optional;
import java.util.OptionalLong;

public final class TimeEstimate {
    public static OptionalLong seconds(long amount, ProfileStats stats) {
        if (amount <= 0 || stats.amountPerSecond() <= 0) {
            return OptionalLong.empty();
        }

        return OptionalLong.of((long) Math.ceil(amount / stats.amountPerSecond()));
    }

    public static Optional<String> format(long amount, ProfileStats stats) {
        var estimate = seconds(amount, stats);
        if (estimate.isEmpty()) {
            return Optional.empty();
        }

        var seconds = estimate.getAsLong();
        var hours = seconds / 3600;
        var minutes = (seconds % 3600) / 60;
        var remainingSeconds = seconds % 60;
        if (hours > 0) {
            return Optional.of(String.format("~%d:%02d:%02d", hours, minutes, remainingSeconds));
        }
        if (minutes > 0) {
            return Optional.of(String.format("~%d:%02d", minutes, remainingSeconds));
        }
        return Optional.of("~" + seconds + "s");
    }

    private TimeEstimate() {
    }
}
