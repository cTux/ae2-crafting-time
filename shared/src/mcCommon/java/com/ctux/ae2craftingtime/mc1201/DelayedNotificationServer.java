package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.TimeEstimate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class DelayedNotificationServer {
    public static void maybeNotify(Object scope, long tick, MinecraftServer server) {
        if (scope == null || server == null) {
            return;
        }
        if (!Ae2CraftingTimeConfig.NOTIFY_ON_DELAYED.get()) {
            return;
        }
        var newlyDelayed = ProfilerBridge.pollNewlyDelayed(scope, tick);
        if (newlyDelayed.isEmpty()) {
            return;
        }
        var owner = ProfilerBridge.jobOwner(scope);
        if (owner.isEmpty()) {
            return;
        }
        var player = server.getPlayerList().getPlayer(owner.get());
        if (player == null) {
            return;
        }
        for (var event : newlyDelayed) {
            player.sendSystemMessage(delayedMessage(event.key().outputId(),
                    ProfilerBridge.displayName(event.key()),
                    event.diagnostic().idleTicks(),
                    event.diagnostic().typicalDurationTicks()));
        }
    }

    private static Component delayedMessage(String outputId, String outputName, long idleTicks,
            double typicalTicks) {
        var idleSeconds = (long) Math.ceil(Math.max(0, idleTicks) / 20.0);
        var typical = TimeEstimate.formatTicks(typicalTicks);
        var name = outputName != null && !outputName.isBlank() ? outputName : outputId;
        return Component.translatable("text.ae2craftingtime.chat.delayed",
                name,
                Component.translatable("text.ae2craftingtime.value.whole_seconds", idleSeconds),
                typical);
    }

    private DelayedNotificationServer() {
    }
}
