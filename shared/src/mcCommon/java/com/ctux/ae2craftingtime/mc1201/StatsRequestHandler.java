package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.PlayerRequestRateLimit;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StatsRequestHandler {
    private static final PlayerRequestRateLimit RATE_LIMIT = new PlayerRequestRateLimit();

    private StatsRequestHandler() {
    }

    public static Response collect(ServerPlayer player, List<String> keys) {
        if (!RATE_LIMIT.allow(player.getUUID(), keys.size(), System.currentTimeMillis())) {
            return null;
        }
        var entries = new ArrayList<StatsEntry>();
        var waitingTicks = new HashMap<String, Long>();
        var context = StatsRequestContext.current(player);
        var networkId = ProfilerBridge.networkId(context.grid());
        var gameTick = player.level().getGameTime();
        var missing = ProfilerBridge.missingProviders(context.craftingCpu(), context.grid());
        var missingProviders = new HashSet<String>();
        for (var key : keys) {
            var profileKey = new ProfileKey(networkId, key);
            if (missing.contains(profileKey)) {
                missingProviders.add(key);
            }
            ProfilerBridge.entry(profileKey, new ProfileKey(key), context.craftingCpu(), gameTick)
                    .ifPresent(entries::add);
            ProfilerBridge.waitingTicks(profileKey, context.craftingCpu(), gameTick)
                    .ifPresent(value -> waitingTicks.put(key, value));
        }
        return new Response(entries, networkAmounts(context.grid(), keys), waitingTicks, missingProviders,
                StatsRequestContext.cpuContext(player.containerMenu));
    }

    private static Map<String, Long> networkAmounts(IGrid grid, List<String> keys) {
        var amounts = new HashMap<String, Long>();
        if (grid == null) {
            return amounts;
        }
        var requested = new HashSet<>(keys);
        keys.forEach(key -> amounts.put(key, 0L));
        for (var entry : grid.getStorageService().getInventory().getAvailableStacks()) {
            var id = entry.getKey().getId().toString();
            if (requested.contains(id)) {
                amounts.merge(id, entry.getLongValue(), Long::sum);
            }
        }
        return amounts;
    }

    public record Response(List<StatsEntry> entries, Map<String, Long> networkAmounts,
            Map<String, Long> waitingTicks, Set<String> missingProviders, long cpuContext) {
    }
}
