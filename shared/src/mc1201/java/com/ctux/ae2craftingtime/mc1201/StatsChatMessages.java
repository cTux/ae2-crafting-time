package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsChatLines;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class StatsChatMessages {
    public static void show(ProfileKey key, long amount) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            player.displayClientMessage(Component.literal("No cached crafting stats yet for " + key.outputId())
                    .withStyle(ChatFormatting.YELLOW), false);
            return;
        }

        player.displayClientMessage(Component.literal("AE2 Crafting Time").withStyle(ChatFormatting.GOLD), false);
        for (var line : StatsChatLines.lines(key, amount, stats.get())) {
            player.displayClientMessage(Component.literal(line.label() + ": ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(line.value()).withStyle(ChatFormatting.AQUA)), false);
        }
    }

    public static void reset(ProfileKey key) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ClientStats.CACHE.remove(key);
        ClientStatsRequests.reset(key);
        player.displayClientMessage(Component.literal("Forgot TTC stats for " + key.outputId())
                .withStyle(ChatFormatting.YELLOW), false);
    }

    private StatsChatMessages() {
    }
}
