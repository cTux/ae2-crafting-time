package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.PlayerMessageRateLimit;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.StatsChatAction;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import java.util.Locale;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class StatsChatServer {
    private static final PlayerMessageRateLimit RATE_LIMIT = new PlayerMessageRateLimit();

    public static void handle(ServerPlayer player, String outputId, long amount, StatsChatAction action) {
        var requestContext = StatsRequestContext.current(player);
        if (requestContext.grid() == null || !RATE_LIMIT.allow(player.getUUID(), System.currentTimeMillis())) {
            return;
        }
        var key = new ProfileKey(ProfilerBridge.networkId(requestContext.grid()),
                PacketLimits.checkedOutputId(outputId));
        if (action == StatsChatAction.RESET) {
            if (ProfilerBridge.stats(key).isEmpty()) {
                return;
            }
            if (ProfilerBridge.clearStats(key) && Ae2CraftingTimeConfig.SHOW_CHAT_MESSAGES.get()) {
                notifyReset(player, Component.translatable("text.ae2craftingtime.chat.reset", outputId));
            }
            return;
        }
        if (amount <= 0 || !Ae2CraftingTimeConfig.SHOW_CHAT_MESSAGES.get()) {
            return;
        }
        var stats = ProfilerBridge.stats(key);
        if (stats.isEmpty()) {
            broadcast(player, Component.translatable("text.ae2craftingtime.chat.no_cached", outputId));
            return;
        }
        var summary = Component.translatable("text.ae2craftingtime.chat.summary", outputId, amount,
                TimeEstimate.format(amount, stats.get()).orElse("?"));
        summary.append(Component.literal(" | ")).append(details(stats.get()));
        ProfilerBridge.accuracy(key).ifPresent(accuracy -> summary.append(Component.literal(" | "))
                .append(accuracy(accuracy)).append(Component.literal("; ")).append(latestAccuracy(accuracy)));
        broadcast(player, summary);
    }

    static Component details(ProfileStats stats) {
        var average = stats.averageTicksPerUnit();
        var latest = stats.latestTicksPerUnit();
        var unit = Component.translatable(stats.unit() == com.ctux.ae2craftingtime.core.ProfileUnit.ITEM
                ? "text.ae2craftingtime.unit.item.singular"
                : stats.unit().translationKey());
        var component = average.isPresent() && latest.isPresent()
                ? Component.translatable("text.ae2craftingtime.chat.details", stats.sampleCount(), unit,
                        TimeEstimate.formatSampleTicks(average.getAsDouble()).orElse("?"), unit,
                        TimeEstimate.formatSampleTicks(latest.getAsDouble()).orElse("?"),
                        decimal(stats.amountPerSecond()), Component.translatable(stats.unit().translationKey()))
                : Component.translatable("text.ae2craftingtime.chat.details.rate", stats.sampleCount(),
                        decimal(stats.amountPerSecond()), Component.translatable(stats.unit().translationKey()));
        if (stats.usedSampleCount() != stats.sampleCount()) {
            component.append(Component.translatable("text.ae2craftingtime.chat.details.used",
                    stats.usedSampleCount(), stats.sampleCount()));
        }
        if (!stats.reliableEstimate()) {
            component.append(Component.translatable("text.ae2craftingtime.chat.details.low_confidence"));
        }
        return component;
    }

    private static Component accuracy(TtcAccuracyStats stats) {
        if (stats.fullyCoveredSampleCount() == 0) {
            return Component.translatable("text.ae2craftingtime.value.accuracy_pending", stats.sampleCount(),
                    percent(stats.averageCoverage()));
        }
        return Component.translatable("text.ae2craftingtime.value.accuracy", stats.fullyCoveredSampleCount(),
                stats.sampleCount(), decimal(stats.meanAbsolutePercentageError()),
                decimal(stats.meanActualToPredictedRatio()),
                String.format(Locale.ROOT, "%+.2f", stats.meanSignedErrorSeconds()),
                percent(stats.averageCoverage()));
    }

    private static Component latestAccuracy(TtcAccuracyStats stats) {
        return Component.translatable("text.ae2craftingtime.value.latest_accuracy", stats.lastPredictedSeconds(),
                decimal(stats.lastActualWallSeconds()), decimal(stats.lastActualTickSeconds()), stats.lastKnownRows(),
                stats.lastTotalRows());
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String percent(double ratio) {
        return decimal(ratio * 100.0);
    }

    private static void broadcast(ServerPlayer player, Component message) {
        var server = player.level().getServer();
        var chatType = ChatType.bind(ChatType.CHAT, player);
        for (var recipient : server.getPlayerList().getPlayers()) {
            recipient.connection.sendDisguisedChatMessage(message, chatType);
        }
    }

    private static void notifyReset(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }

    private StatsChatServer() {
    }
}
