package com.ctux.ae2cpd.core;

import java.util.Optional;

public final class TimeEstimate {
    public static Optional<String> format(long amount, ProfileStats stats) {
        if (amount <= 0 || stats.amountPerSecond() <= 0) {
            return Optional.empty();
        }

        var seconds = (long) Math.ceil(amount / stats.amountPerSecond());
        var hours = seconds / 3600;
        var minutes = (seconds % 3600) / 60;
        var remainingSeconds = seconds % 60;
        return Optional.of(String.format("~%03d:%02d:%02d", hours, minutes, remainingSeconds));
    }

    private TimeEstimate() {
    }
}
