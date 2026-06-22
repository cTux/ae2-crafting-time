package com.ctux.ae2craftingtime.mc1201.net;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.menu.AEBaseMenu;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record StatsRequestC2S(List<String> keys) {
    public static void encode(StatsRequestC2S packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.keys.size());
        for (var key : packet.keys) {
            buffer.writeUtf(key);
        }
    }

    public static StatsRequestC2S decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var keys = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            keys.add(buffer.readUtf());
        }
        return new StatsRequestC2S(keys);
    }

    public void handle(ServerPlayer player) {
        var entries = new ArrayList<StatsEntry>();
        var networkId = ProfilerBridge.networkId(currentGrid(player));
        for (var key : keys) {
            ProfilerBridge.stats(new ProfileKey(networkId, key))
                    .ifPresent(stats -> entries.add(new StatsEntry(new ProfileKey(key), stats)));
        }
        StatsNetwork.sendTo(player, new StatsSnapshotS2C(entries));
    }

    private static IGrid currentGrid(ServerPlayer player) {
        if (player.containerMenu instanceof AEBaseMenu menu && menu.getTarget() instanceof IActionHost host) {
            var node = host.getActionableNode();
            return node == null ? null : node.getGrid();
        }
        return null;
    }
}
