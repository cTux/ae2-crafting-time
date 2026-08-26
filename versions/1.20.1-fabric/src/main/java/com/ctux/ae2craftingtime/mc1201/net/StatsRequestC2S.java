package com.ctux.ae2craftingtime.mc1201.net;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import com.ctux.ae2craftingtime.mc1201.StatsRequestContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record StatsRequestC2S(List<String> keys, boolean reset) {
    public StatsRequestC2S(List<String> keys) {
        this(keys, false);
    }

    public static void encode(StatsRequestC2S packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.reset);
        buffer.writeVarInt(packet.keys.size());
        for (var key : packet.keys) {
            buffer.writeUtf(key);
        }
    }

    public static StatsRequestC2S decode(FriendlyByteBuf buffer) {
        var reset = buffer.readBoolean();
        var size = buffer.readVarInt();
        var keys = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            keys.add(buffer.readUtf());
        }
        return new StatsRequestC2S(keys, reset);
    }

    public void handle(ServerPlayer player) {
        var entries = new ArrayList<StatsEntry>();
        var requestContext = StatsRequestContext.current(player);
        var grid = requestContext.grid();
        var networkId = ProfilerBridge.networkId(grid);
        for (var key : keys) {
            var profileKey = new ProfileKey(networkId, key);
            if (reset) {
                ProfilerBridge.clearStats(profileKey);
                continue;
            }
            ProfilerBridge.entry(profileKey, new ProfileKey(key), requestContext.craftingCpu(),
                    player.level().getGameTime()).ifPresent(entries::add);
        }
        StatsNetwork.sendTo(player, new StatsSnapshotS2C(keys, entries, networkAmounts(grid, keys)));
    }

    private static Map<String, Long> networkAmounts(IGrid grid, List<String> keys) {
        var amounts = new HashMap<String, Long>();
        if (grid == null) {
            return amounts;
        }
        keys.forEach(key -> amounts.put(key, 0L));
        for (var entry : grid.getStorageService().getInventory().getAvailableStacks()) {
            var id = entry.getKey().getId().toString();
            if (keys.contains(id)) {
                amounts.merge(id, entry.getLongValue(), Long::sum);
            }
        }
        return amounts;
    }

}
