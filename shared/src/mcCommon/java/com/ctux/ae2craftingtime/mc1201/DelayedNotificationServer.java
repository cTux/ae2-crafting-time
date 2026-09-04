package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class DelayedNotificationServer {
    public static void maybeNotify(Object scope, IGrid grid, long tick, MinecraftServer server) {
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
        var owner = ownerOf(scope, newlyDelayed.stream().map(event -> event.key()).toList());
        if (owner == null) {
            return;
        }
        var player = server.getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        var dimension = ProfilerBridge.dimensionId(grid);
        for (var event : newlyDelayed) {
            notify(player, scope, grid, dimension, owner, event.key(),
                    event.diagnostic().idleTicks(), event.diagnostic().typicalDurationTicks());
        }
        ProfilerBridge.persistProviderState();
    }

    static UUID ownerOf(Object scope, List<ProfileKey> keys) {
        var live = ProfilerBridge.jobOwner(scope);
        if (live.isPresent()) {
            return live.get();
        }
        for (var key : keys) {
            var remembered = ProviderLocateRecords.startFor(key)
                    .map(ProviderLocateRecords.ProviderStartInfo::owner)
                    .orElse(null);
            if (remembered != null) {
                return remembered;
            }
        }
        return null;
    }

    private static void notify(ServerPlayer player, Object scope, IGrid grid, String dimension, UUID owner,
            ProfileKey key, long idleTicks, double typicalTicks) {
        var positions = ProfilerBridge.locatePositions(scope, grid, key);
        var name = ProfilerBridge.displayName(key);
        UUID recordId = null;
        if (!positions.isEmpty()) {
            recordId = ProviderLocateRecords.create(owner, dimension, positions, name,
                    player.level().getGameTime()).id();
        }
        ProfilerBridge.replaceProviderStart(key, owner, positions, name);
        player.sendSystemMessage(DelayedChatText.delayedMessage(name, recordId, idleTicks, typicalTicks));
    }

    private DelayedNotificationServer() {
    }
}
