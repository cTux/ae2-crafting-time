package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.CraftingBlockReason;
import com.ctux.ae2craftingtime.core.StallDiagnostic;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TtcText {
    public static MutableComponent ttc(String eta) {
        return Component.translatable("text.ae2craftingtime.ttc", eta);
    }

    public static MutableComponent tooltipTtc(Component value) {
        return Component.translatable("text.ae2craftingtime.stats.ttc")
                .append(": ").append(value).setStyle(value.getStyle());
    }

    public static MutableComponent ttcDelayed() {
        return Component.translatable("text.ae2craftingtime.ttc_delayed");
    }

    public static MutableComponent waiting() {
        return Component.translatable("text.ae2craftingtime.waiting");
    }

    public static MutableComponent noSpace() {
        return Component.translatable("text.ae2craftingtime.no_space")
                .withStyle(ChatFormatting.RED);
    }

    public static List<Component> noSpaceTooltip() {
        return List.of(noSpace(), Component.translatable("text.ae2craftingtime.no_space.explanation"),
                Component.translatable("text.ae2craftingtime.no_space.suggestion"));
    }

    public static MutableComponent blockReason(CraftingBlockReason reason) {
        return Component.translatable("text.ae2craftingtime." + reason.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.RED);
    }

    public static List<Component> blockReasonTooltip(CraftingBlockReason reason) {
        var key = "text.ae2craftingtime." + reason.name().toLowerCase(Locale.ROOT);
        return List.of(blockReason(reason), Component.translatable(key + ".explanation"),
                Component.translatable(key + ".suggestion"));
    }

    public static MutableComponent ttcCollectingData() {
        return Component.translatable("text.ae2craftingtime.ttc",
                Component.translatable("text.ae2craftingtime.collecting_data"))
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent totalTtc(String eta) {
        return Component.translatable("text.ae2craftingtime.total_ttc", eta);
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

    public static MutableComponent locateHint() {
        return Component.translatable("text.ae2craftingtime.locate_hint");
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

    public static List<Component> statsLines(ProfileStats stats, Optional<TtcAccuracyStats> accuracy) {
        var lines = new ArrayList<Component>();
        lines.add(statsLine("text.ae2craftingtime.stats.throughput",
                I18n.get("text.ae2craftingtime.value.throughput", rate(stats.amountPerTick()), unitName(stats),
                        rate(stats.amountPerSecond()), unitName(stats))));
        if (stats.usedSampleCount() != stats.sampleCount()) {
            lines.add(statsLine("text.ae2craftingtime.stats.used_samples",
                    I18n.get("text.ae2craftingtime.value.used_samples", stats.usedSampleCount(),
                            stats.sampleCount())));
        }
        if (!stats.sampleDurationTicks().isEmpty()) {
            var windows = windows(stats);
            if (!windows.isEmpty()) {
                lines.add(statsLine("text.ae2craftingtime.stats.samples",
                        windows + " (" + stats.sampleCount() + ")"));
                lines.add(Component.translatable("text.ae2craftingtime.stats.samples.explanation")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (!stats.reliableEstimate()) {
            lines.add(statsLine("text.ae2craftingtime.stats.confidence", confidence(stats)));
        }
        accuracy.ifPresent(value -> {
            lines.add(statsLine("text.ae2craftingtime.stats.accuracy", accuracy(value)));
            lines.add(statsLine("text.ae2craftingtime.stats.latest_accuracy", latestAccuracy(value)));
        });
        return List.copyOf(lines);
    }

    public static List<Component> stallLines(long amount, long scheduledAmount, ProfileStats stats, StallDiagnostic stall) {
        var lines = new ArrayList<Component>();
        var eta = TimeEstimate.format(amount, stats).orElse(I18n.get("text.ae2craftingtime.unknown"));
        lines.add(Component.translatable("text.ae2craftingtime.stats.ttc").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": " + eta + ", ").withStyle(ChatFormatting.AQUA))
                .append(Component.translatable("text.ae2craftingtime.stall.delayed").withStyle(ChatFormatting.RED))
                .append(Component.literal(": " + I18n.get("text.ae2craftingtime.value.whole_seconds",
                        secondsRounded(stall.idleTicks())) + ", ").withStyle(ChatFormatting.AQUA))
                .append(Component.translatable("text.ae2craftingtime.stall.typical").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": " + TimeEstimate.formatTicks(stall.typicalDurationTicks()))
                        .withStyle(ChatFormatting.AQUA)));
        lines.add(Component.empty());
        lines.add(Component.translatable("text.ae2craftingtime.stall.improvements")
                .withStyle(ChatFormatting.GOLD));
        for (var hint : stall.hints(scheduledAmount)) {
            appendHint(lines, hint, stall);
        }
        return List.copyOf(lines);
    }

    public static List<String> compactMessages(String name, long amount, ProfileStats stats) {
        return compactMessages(name, amount, stats, Optional.empty());
    }

    public static List<String> compactMessages(String name, long amount, ProfileStats stats,
            Optional<TtcAccuracyStats> accuracy) {
        var messages = new ArrayList<String>();
        messages.add(I18n.get("text.ae2craftingtime.chat.summary", name, amount,
                TimeEstimate.format(amount, stats).orElse(I18n.get("text.ae2craftingtime.unknown"))));

        var details = normalizedDetails(stats);
        if (stats.usedSampleCount() != stats.sampleCount()) {
            details += I18n.get("text.ae2craftingtime.chat.details.used", stats.usedSampleCount(),
                    stats.sampleCount());
        }
        if (!stats.reliableEstimate()) {
            details += I18n.get("text.ae2craftingtime.chat.details.low_confidence");
        }
        if (accuracy.isPresent()) {
            details += " | " + accuracy(accuracy.get()) + "; " + latestAccuracy(accuracy.get());
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
        return I18n.get(stats.unit().translationKey());
    }

    private static String singularUnitName(ProfileStats stats) {
        return I18n.get(stats.unit() == com.ctux.ae2craftingtime.core.ProfileUnit.ITEM
                ? "text.ae2craftingtime.unit.item.singular"
                : stats.unit().translationKey());
    }

    private static long secondsRounded(long ticks) {
        return (long) Math.ceil(ticks / 20.0);
    }

    private static void appendHint(List<Component> lines, StallDiagnostic.Hint hint, StallDiagnostic stall) {
        switch (hint) {
            case ADD_PARALLEL_PROVIDERS -> {
                lines.add(Component.translatable("text.ae2craftingtime.stall.hint.parallel")
                        .withStyle(ChatFormatting.YELLOW));
                var available = Math.max(0, stall.totalParallelSlots() - stall.usedParallelSlots());
                var reasonKey = stall.activeBatches() == 1
                        ? "text.ae2craftingtime.stall.reason.parallel_one"
                        : "text.ae2craftingtime.stall.reason.parallel_many";
                lines.add(Component.translatable(reasonKey, available, stall.activeBatches())
                        .withStyle(ChatFormatting.GRAY));
            }
            case SPEED_UP_MACHINE -> {
                lines.add(Component.translatable("text.ae2craftingtime.stall.hint.speed")
                        .withStyle(ChatFormatting.YELLOW));
                var slowdown = stall.typicalDurationTicks() <= 0 ? 1.0
                        : stall.idleTicks() / stall.typicalDurationTicks();
                lines.add(Component.translatable("text.ae2craftingtime.stall.reason.speed", rate(slowdown))
                        .withStyle(ChatFormatting.GRAY));
            }
            case ADD_CRAFTING_CO_PROCESSORS -> {
                lines.add(Component.translatable("text.ae2craftingtime.stall.hint.co_processors")
                        .withStyle(ChatFormatting.YELLOW));
                lines.add(Component.translatable("text.ae2craftingtime.stall.reason.co_processors",
                        stall.totalParallelSlots()).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static String rate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String windows(ProfileStats stats) {
        var values = new ArrayList<String>();
        for (var i = 0; i < stats.sampleDurationTicks().size(); i++) {
            var ticks = stats.sampleTicksPerUnit(i);
            if (ticks.isPresent()) {
                TimeEstimate.formatSampleTicks(ticks.getAsDouble()).ifPresent(value -> values.add(
                        I18n.get("text.ae2craftingtime.value.window", 1, singularUnitName(stats), value)));
            }
        }
        return String.join(", ", values);
    }

    private static String normalizedDetails(ProfileStats stats) {
        var average = stats.averageTicksPerUnit();
        var latest = stats.latestTicksPerUnit();
        if (average.isEmpty() || latest.isEmpty()) {
            return I18n.get("text.ae2craftingtime.chat.details.rate", stats.sampleCount(),
                    rate(stats.amountPerSecond()), unitName(stats));
        }
        return I18n.get("text.ae2craftingtime.chat.details", stats.sampleCount(), singularUnitName(stats),
                TimeEstimate.formatSampleTicks(average.getAsDouble()).orElse("?"), singularUnitName(stats),
                TimeEstimate.formatSampleTicks(latest.getAsDouble()).orElse("?"),
                rate(stats.amountPerSecond()), unitName(stats));
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

    private static String accuracy(TtcAccuracyStats stats) {
        if (stats.fullyCoveredSampleCount() == 0) {
            return I18n.get("text.ae2craftingtime.value.accuracy_pending", stats.sampleCount(),
                    percent(stats.averageCoverage()));
        }
        return I18n.get("text.ae2craftingtime.value.accuracy", stats.fullyCoveredSampleCount(), stats.sampleCount(),
                rate(stats.meanAbsolutePercentageError()), rate(stats.meanActualToPredictedRatio()),
                String.format(Locale.ROOT, "%+.2f", stats.meanSignedErrorSeconds()), percent(stats.averageCoverage()));
    }

    private static String latestAccuracy(TtcAccuracyStats stats) {
        return I18n.get("text.ae2craftingtime.value.latest_accuracy", stats.lastPredictedSeconds(),
                rate(stats.lastActualWallSeconds()), rate(stats.lastActualTickSeconds()), stats.lastKnownRows(),
                stats.lastTotalRows());
    }

    private static String percent(double ratio) {
        return rate(ratio * 100.0);
    }

    private TtcText() {
    }
}
