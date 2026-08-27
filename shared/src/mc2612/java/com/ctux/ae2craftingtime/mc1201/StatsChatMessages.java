package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.net.StatsChatC2S;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class StatsChatMessages {
    public static void show(ProfileKey key, String name, long amount) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            StatsNetwork.sendToServer(new StatsChatC2S(List.of(TtcText.noCachedStats(name))));
            return;
        }

        StatsNetwork.sendToServer(new StatsChatC2S(TtcText.compactMessages(name, amount, stats.get(),
                ClientStats.CACHE.accuracy(key))));
    }

    public static void reset(ProfileKey key, String name) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ClientStats.CACHE.remove(key);
        ClientStatsRequests.reset(key);
        StatsNetwork.sendToServer(new StatsChatC2S(List.of(TtcText.resetStats(name))));
    }

    private StatsChatMessages() {
    }
}
