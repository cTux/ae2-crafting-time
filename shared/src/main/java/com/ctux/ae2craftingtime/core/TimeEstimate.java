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

        var text = formatSeconds(estimate.getAsLong());
        return Optional.of(stats.reliableEstimate() ? text : text + "?");
    }

    public static OptionalLong progressSeconds(long elapsedNanos, long startAmount, long remainingAmount) {
        var completedAmount = startAmount - remainingAmount;
        if (elapsedNanos <= 0 || remainingAmount <= 0 || completedAmount <= 0) {
            return OptionalLong.empty();
        }

        return OptionalLong.of((long) Math.ceil(
                elapsedNanos / 1_000_000_000.0 * remainingAmount / completedAmount));
    }

    public static Optional<String> formatTotal(Iterable<OptionalLong> estimates) {
        long totalSeconds = 0;
        for (var estimate : estimates) {
            if (estimate.isPresent()) {
                totalSeconds += estimate.getAsLong();
            }
        }
        return totalSeconds == 0 ? Optional.empty() : Optional.of(formatSeconds(totalSeconds));
    }

    private static String formatSeconds(long seconds) {
        var hours = seconds / 3600;
        var minutes = (seconds % 3600) / 60;
        var remainingSeconds = seconds % 60;
        if (hours > 0) {
            return String.format("~%d:%02d:%02d", hours, minutes, remainingSeconds);
        }
        if (minutes > 0) {
            return String.format("~%d:%02d", minutes, remainingSeconds);
        }
        return "~" + seconds + "s";
    }

    private TimeEstimate() {
    }
}
