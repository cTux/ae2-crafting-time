package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsChatLines;
import net.minecraft.client.Minecraft;

public final class StatsChatMessages {
    public static void show(ProfileKey key, long amount) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            player.connection.sendChat("AE2 TTC: no cached stats for " + key.outputId() + " yet");
            return;
        }

        for (var message : StatsChatLines.compactMessages(key, amount, stats.get())) {
            player.connection.sendChat(message);
        }
    }

    public static void reset(ProfileKey key) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ClientStats.CACHE.remove(key);
        ClientStatsRequests.reset(key);
        player.connection.sendChat("AE2 TTC reset: " + key.outputId());
    }

    private StatsChatMessages() {
    }
}
