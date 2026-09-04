package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side only. Answers a double-click locate request from the crafting
 * CPU screen: resolves the clicking player's open CPU scope and grid,
 * requires job ownership, resolves live provider positions, and hands the
 * highlight to the loader sender. Anything unresolvable answers the same
 * expiry notice as a stale chat link. No locate records are involved: unlike
 * the async chat link, this request is answered immediately.
 */
public final class ProviderLocateServer {
    public static void locate(ServerPlayer player, String outputId,
            BiConsumer<ServerPlayer, ProviderHighlightCodec.Highlight> sender) {
        if (player == null || sender == null) {
            return;
        }
        ProfileKey key;
        try {
            key = new ProfileKey(outputId);
        } catch (IllegalArgumentException ignored) {
            expired(player);
            return;
        }
        var context = StatsRequestContext.current(player);
        var scope = context.craftingCpu();
        var grid = context.grid();
        if (scope == null || grid == null) {
            expired(player);
            return;
        }
        var owner = ProfilerBridge.jobOwner(scope).orElse(null);
        if (owner == null || !owner.equals(player.getUUID())) {
            expired(player);
            return;
        }
        var scopedKey = new ProfileKey(ProfilerBridge.networkId(grid), key.outputId());
        var positions = ProfilerBridge.locatePositions(scope, grid, scopedKey);
        if (positions.isEmpty()) {
            expired(player);
            return;
        }
        var dimension = ProfilerBridge.dimensionId(grid);
        sender.accept(player, new ProviderHighlightCodec.Highlight(dimension, positions,
                key.outputId(), ProviderLocateCommand.HIGHLIGHT_SECONDS));
        player.sendSystemMessage(DelayedChatText.highlightingMessage(
                ProviderLocateCommand.providerName(player.level(), positions), positions, dimension));
    }

    private static void expired(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("text.ae2craftingtime.chat.delayed.expired"));
    }

    private ProviderLocateServer() {
    }
}
