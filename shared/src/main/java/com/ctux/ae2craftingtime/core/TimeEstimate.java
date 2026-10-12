package com.ctux.ae2craftingtime.core;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public final class TimeEstimate {
    public static boolean hasPendingAmount(long requested, OptionalLong available) {
        return requested > 0 && (available.isEmpty() || requested > available.getAsLong());
    }

    public static Optional<TtcEstimate> milliseconds(long amount, ProfileStats stats) {
        if (amount <= 0 || !Double.isFinite(stats.amountPerSecond()) || stats.amountPerSecond() <= 0) {
            return Optional.empty();
        }

        var duration = amount / stats.amountPerSecond() * 1000;
        return Double.isFinite(duration) ? Optional.of(new TtcEstimate(duration, stats.reliableEstimate()))
                : Optional.empty();
    }

    public static Optional<String> format(long amount, ProfileStats stats) {
        return milliseconds(amount, stats).map(TimeEstimate::format);
    }

    public static String format(TtcEstimate estimate) {
        var text = formatDuration(estimate.durationMs());
        return estimate.reliable() ? text : text + "?";
    }

    public static Optional<TtcEstimate> total(Iterable<Optional<TtcEstimate>> estimates) {
        double duration = 0;
        boolean reliable = true;
        for (var estimate : estimates) {
            if (estimate.isPresent()) {
                duration += estimate.get().durationMs();
                reliable &= estimate.get().reliable();
            } else {
                reliable = false;
            }
        }
        return duration > 0 && Double.isFinite(duration)
                ? Optional.of(new TtcEstimate(duration, reliable)) : Optional.empty();
    }

    public static Optional<String> formatTotal(Iterable<Optional<TtcEstimate>> estimates) {
        return total(estimates).map(TimeEstimate::format);
    }

    public static boolean hasMeasuredProgress(long startItems, long remainingItems) {
        return remainingItems < startItems;
    }

    public static String formatTicks(double ticks) {
        return formatDuration(Math.max(0, ticks) * 50);
    }

    public static Optional<String> formatSampleTicks(double ticks) {
        var duration = ticks * 50;
        return !Double.isFinite(duration) || duration <= 0 ? Optional.empty()
                : Optional.of(Long.toString((long) Math.ceil(Math.max(5, duration))));
    }

    public static String formatDuration(double durationMs) {
        if (!Double.isFinite(durationMs) || durationMs < 0) return "?";
        if (durationMs == 0) return "0ms";
        if (durationMs < 1000) return (long) Math.ceil(Math.max(5, durationMs)) + "ms";
        var seconds = (long) Math.ceil(durationMs / 1000);
        var hours = seconds / 3600;
        var minutes = (seconds % 3600) / 60;
        var remainingSeconds = seconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "~%d:%02d:%02d", hours, minutes, remainingSeconds);
        }
        if (minutes > 0) {
            return String.format(Locale.ROOT, "~%d:%02d", minutes, remainingSeconds);
        }
        return "~" + seconds + "s";
    }

    public static String formatSignedSeconds(double seconds) {
        return Math.abs(seconds) < 1 ? String.format(Locale.ROOT, "%+.0fms", seconds * 1000)
                : String.format(Locale.ROOT, "%+.2fs", seconds);
    }

    private TimeEstimate() {
    }
}
