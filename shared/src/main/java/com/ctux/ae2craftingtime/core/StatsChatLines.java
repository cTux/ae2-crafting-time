package com.ctux.ae2craftingtime.core;

import java.util.List;
import java.util.Locale;

public final class StatsChatLines {
    public record Line(String label, String value) {
    }

    public static List<Line> lines(ProfileKey key, long amount, ProfileStats stats) {
        var lines = new java.util.ArrayList<Line>();
        lines.add(new Line("Item", key.outputId()));
        lines.add(new Line("Amount", amount + " " + unitName(stats)));
        lines.add(new Line("Samples", Integer.toString(stats.sampleCount())));
        lines.add(new Line("Average", duration(stats.averageDurationTicks())));
        lines.add(new Line("Latest", duration(stats.lastDurationTicks())));
        lines.add(new Line("Throughput", rate(stats.amountPerTick()) + " " + unitName(stats) + "/t, "
                + rate(stats.amountPerSecond()) + " " + unitName(stats) + "/s"));
        if (!stats.reliableEstimate()) {
            lines.add(new Line("Confidence", "low"));
        }
        lines.add(new Line("TTC", TimeEstimate.format(amount, stats).orElse("unknown")));
        return List.copyOf(lines);
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

    private static String rate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private StatsChatLines() {
    }
}
