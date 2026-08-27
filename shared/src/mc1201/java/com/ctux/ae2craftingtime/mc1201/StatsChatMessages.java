package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsChatAction;
import com.ctux.ae2craftingtime.mc1201.net.StatsChatC2S;
import net.minecraft.client.Minecraft;

public final class StatsChatMessages {
    public static void show(ProfileKey key, String name, long amount) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        StatsNetwork.sendToServer(new StatsChatC2S(key.outputId(), amount, StatsChatAction.SHOW));
    }

    public static void reset(ProfileKey key, String name) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ClientStats.CACHE.remove(key);
        StatsNetwork.sendToServer(new StatsChatC2S(key.outputId(), 0, StatsChatAction.RESET));
    }

    private StatsChatMessages() {
    }
}
