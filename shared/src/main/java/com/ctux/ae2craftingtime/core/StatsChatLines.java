package com.ctux.ae2craftingtime.core;

import java.util.List;
import java.util.Locale;

public final class StatsChatLines {
    public record Line(String label, String value) {
    }

    public static List<Line> lines(String name, long amount, ProfileStats stats) {
        var lines = new java.util.ArrayList<Line>();
        lines.add(new Line("Item", name));
        lines.add(new Line("Amount", amount + " " + unitName(stats)));
        lines.add(new Line("Samples", Integer.toString(stats.sampleCount())));
        lines.add(new Line("Average", duration(stats.averageDurationTicks())));
        lines.add(new Line("Latest", duration(stats.lastDurationTicks())));
        lines.add(new Line("Throughput", rate(stats.amountPerTick()) + " " + unitName(stats) + "/t, "
                + rate(stats.amountPerSecond()) + " " + unitName(stats) + "/s"));
        if (stats.usedSampleCount() != stats.sampleCount()) {
            lines.add(new Line("Used Samples", stats.usedSampleCount() + "/" + stats.sampleCount()));
        }
        if (!stats.sampleDurationTicks().isEmpty()) {
            lines.add(new Line("Outlier Filter", rate(stats.outlierMultiplier()) + "x"));
            lines.add(new Line("Durations", durations(stats.sampleDurationTicks())));
        }
        if (!stats.reliableEstimate()) {
            lines.add(new Line("Confidence", confidence(stats)));
        }
        lines.add(new Line("TTC", TimeEstimate.format(amount, stats).orElse("unknown")));
        return List.copyOf(lines);
    }

    public static List<String> compactMessages(String name, long amount, ProfileStats stats) {
        var unit = unitName(stats);
        var messages = new java.util.ArrayList<String>();
        messages.add(name + " x" + amount + ": "
                + TimeEstimate.format(amount, stats).orElse("unknown"));

        var details = stats.sampleCount() + " samples, avg " + seconds(stats.averageDurationTicks())
                + ", latest " + seconds(stats.lastDurationTicks())
                + ", " + rate(stats.amountPerSecond()) + " " + unit + "/s";
        if (stats.usedSampleCount() != stats.sampleCount()) {
            details += ", used " + stats.usedSampleCount() + "/" + stats.sampleCount();
        }
        if (!stats.reliableEstimate()) {
            details += ", low confidence";
        }
        messages.add(details);
        return List.copyOf(messages);
    }

    private static String unitName(ProfileStats stats) {
        return stats.unit() == ProfileUnit.MILLIBUCKET ? "mB" : "items";
    }

    private static String duration(double ticks) {
        var tickText = ticks == Math.rint(ticks)
                ? String.format(Locale.ROOT, "%.0f", ticks)
                : String.format(Locale.ROOT, "%.2f", ticks);
        return String.format(Locale.ROOT, "%s ticks (%.2fs)", tickText, ticks / 20.0);
    }

    private static String seconds(double ticks) {
        return String.format(Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    private static String rate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String durations(List<Long> ticks) {
        var values = new java.util.ArrayList<String>();
        for (var tick : ticks) {
            values.add(Long.toString(tick));
        }
        return String.join(", ", values) + " ticks";
    }

    private static String confidence(ProfileStats stats) {
        if (stats.sampleCount() < 3) {
            return "low (<3 samples)";
        }
        if (stats.usedSampleCount() != stats.sampleCount()) {
            return "low (outliers filtered)";
        }
        return "low";
    }

    private StatsChatLines() {
    }
}
