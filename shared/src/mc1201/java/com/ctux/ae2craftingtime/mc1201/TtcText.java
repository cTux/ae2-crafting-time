package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TtcText {
    public static MutableComponent ttc(String eta) {
        return Component.translatable("text.ae2craftingtime.ttc", eta);
    }

    public static MutableComponent totalTtc(String eta) {
        return Component.translatable("text.ae2craftingtime.total_ttc", eta);
    }

    public static MutableComponent requesterTtc(String eta) {
        return Component.translatable("text.ae2craftingtime.requester_ttc", eta);
    }

    public static MutableComponent noStats() {
        return Component.translatable("text.ae2craftingtime.no_stats");
    }

    public static MutableComponent detailsHint() {
        return Component.translatable("text.ae2craftingtime.details_hint");
    }

    public static MutableComponent resetHint() {
        return Component.translatable("text.ae2craftingtime.reset_hint");
    }

    public static MutableComponent sortTitle() {
        return Component.translatable("text.ae2craftingtime.sort.title");
    }

    public static MutableComponent sortMode(int mode) {
        return Component.translatable(switch (mode) {
            case 1 -> "text.ae2craftingtime.sort.shortest";
            case 2 -> "text.ae2craftingtime.sort.longest";
            default -> "text.ae2craftingtime.sort.ae2";
        });
    }

    public static List<Component> statsLines(String name, long amount, ProfileStats stats) {
        var lines = new ArrayList<Component>();
        lines.add(statsLine("text.ae2craftingtime.stats.item", name));
        lines.add(statsLine("text.ae2craftingtime.stats.amount",
                I18n.get("text.ae2craftingtime.value.amount", amount, unitName(stats))));
        lines.add(statsLine("text.ae2craftingtime.stats.samples", Integer.toString(stats.sampleCount())));
        lines.add(statsLine("text.ae2craftingtime.stats.average", duration(stats.averageDurationTicks())));
        lines.add(statsLine("text.ae2craftingtime.stats.latest", duration(stats.lastDurationTicks())));
        lines.add(statsLine("text.ae2craftingtime.stats.throughput",
                I18n.get("text.ae2craftingtime.value.throughput", rate(stats.amountPerTick()), unitName(stats),
                        rate(stats.amountPerSecond()), unitName(stats))));
        if (stats.usedSampleCount() != stats.sampleCount()) {
            lines.add(statsLine("text.ae2craftingtime.stats.used_samples",
                    I18n.get("text.ae2craftingtime.value.used_samples", stats.usedSampleCount(),
                            stats.sampleCount())));
        }
        if (!stats.sampleDurationTicks().isEmpty()) {
            lines.add(statsLine("text.ae2craftingtime.stats.outlier_filter", rate(stats.outlierMultiplier()) + "x"));
            lines.add(statsLine("text.ae2craftingtime.stats.durations",
                    I18n.get("text.ae2craftingtime.value.durations", durations(stats.sampleDurationTicks()))));
        }
        if (!stats.reliableEstimate()) {
            lines.add(statsLine("text.ae2craftingtime.stats.confidence", confidence(stats)));
        }
        lines.add(statsLine("text.ae2craftingtime.stats.ttc",
                TimeEstimate.format(amount, stats).orElse(I18n.get("text.ae2craftingtime.unknown"))));
        return List.copyOf(lines);
    }

    public static List<String> compactMessages(String name, long amount, ProfileStats stats) {
        var messages = new ArrayList<String>();
        messages.add(I18n.get("text.ae2craftingtime.chat.summary", name, amount,
                TimeEstimate.format(amount, stats).orElse(I18n.get("text.ae2craftingtime.unknown"))));

        var details = I18n.get("text.ae2craftingtime.chat.details", stats.sampleCount(),
                seconds(stats.averageDurationTicks()), seconds(stats.lastDurationTicks()),
                rate(stats.amountPerSecond()), unitName(stats));
        if (stats.usedSampleCount() != stats.sampleCount()) {
            details += I18n.get("text.ae2craftingtime.chat.details.used", stats.usedSampleCount(),
                    stats.sampleCount());
        }
        if (!stats.reliableEstimate()) {
            details += I18n.get("text.ae2craftingtime.chat.details.low_confidence");
        }
        messages.add(details);
        return List.copyOf(messages);
    }

    public static String noCachedStats(String name) {
        return I18n.get("text.ae2craftingtime.chat.no_cached", name);
    }

    public static String resetStats(String name) {
        return I18n.get("text.ae2craftingtime.chat.reset", name);
    }

    private static Component statsLine(String labelKey, String value) {
        return Component.translatable(labelKey).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.AQUA));
    }

    private static String unitName(ProfileStats stats) {
        return I18n.get(stats.unit() == ProfileUnit.MILLIBUCKET
                ? "text.ae2craftingtime.unit.millibucket"
                : "text.ae2craftingtime.unit.item");
    }

    private static String duration(double ticks) {
        var tickText = ticks == Math.rint(ticks)
                ? String.format(Locale.ROOT, "%.0f", ticks)
                : String.format(Locale.ROOT, "%.2f", ticks);
        return I18n.get("text.ae2craftingtime.value.duration", tickText, tickSeconds(ticks));
    }

    private static String seconds(double ticks) {
        return I18n.get("text.ae2craftingtime.value.seconds", tickSeconds(ticks));
    }

    private static String tickSeconds(double ticks) {
        return String.format(Locale.ROOT, "%.2f", ticks / 20.0);
    }

    private static String rate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String durations(List<Long> ticks) {
        var values = new ArrayList<String>();
        for (var tick : ticks) {
            values.add(Long.toString(tick));
        }
        return String.join(", ", values);
    }

    private static String confidence(ProfileStats stats) {
        if (stats.sampleCount() < 3) {
            return I18n.get("text.ae2craftingtime.confidence.low_samples");
        }
        if (stats.usedSampleCount() != stats.sampleCount()) {
            return I18n.get("text.ae2craftingtime.confidence.low_outliers");
        }
        return I18n.get("text.ae2craftingtime.confidence.low");
    }

    private TtcText() {
    }
}
