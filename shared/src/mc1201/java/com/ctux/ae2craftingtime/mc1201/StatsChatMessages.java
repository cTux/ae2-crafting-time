package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsChatLines;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public final class StatsChatMessages {
    public static void show(ProfileKey key, String name, long amount) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            player.connection.sendChat(ChatFormatting.YELLOW + "No cached TTC stats for " + name + " yet");
            return;
        }

        for (var message : StatsChatLines.compactMessages(name, amount, stats.get())) {
            player.connection.sendChat(color(message));
        }
    }

    public static void reset(ProfileKey key, String name) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ClientStats.CACHE.remove(key);
        ClientStatsRequests.reset(key);
        player.connection.sendChat(ChatFormatting.YELLOW + "Reset TTC stats for " + name);
    }

    private static String color(String message) {
        var colon = message.indexOf(": ");
        if (colon >= 0) {
            return ChatFormatting.GOLD + message.substring(0, colon)
                    + ChatFormatting.GRAY + ": "
                    + ChatFormatting.AQUA + message.substring(colon + 2);
        }
        return ChatFormatting.GRAY + message;
    }

    private StatsChatMessages() {
    }
}
