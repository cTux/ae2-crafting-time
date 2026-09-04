package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class DelayedNotificationServer {
    public static void maybeNotify(Object scope, IGrid grid, long tick, MinecraftServer server) {
        maybeNotify(scope, grid, tick, server, defaultHighlightSender());
    }

    public static void maybeNotify(Object scope, IGrid grid, long tick, MinecraftServer server,
            BiConsumer<ServerPlayer, ProviderHighlightCodec.Highlight> highlightSender) {
        if (scope == null || server == null) {
            return;
        }
        if (!Ae2CraftingTimeConfig.NOTIFY_ON_DELAYED.get()) {
            return;
        }
        var newlyDelayed = ProfilerBridge.pollNewlyDelayed(scope, tick);
        var resolved = ProfilerBridge.pollResolvedDelayed(scope);
        if (newlyDelayed.isEmpty() && resolved.isEmpty()) {
            return;
        }
        var keys = new ArrayList<>(resolved);
        keys.addAll(newlyDelayed.stream().map(event -> event.key()).toList());
        var owner = ownerOf(scope, keys);
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
                    event.diagnostic().idleTicks(), event.diagnostic().typicalDurationTicks(), highlightSender);
        }
        for (var key : resolved) {
            pushClearHighlight(player, key, highlightSender);
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
            ProfileKey key, long idleTicks, double typicalTicks,
            BiConsumer<ServerPlayer, ProviderHighlightCodec.Highlight> highlightSender) {
        var positions = ProfilerBridge.locatePositions(scope, grid, key);
        var name = ProfilerBridge.displayName(key);
        UUID recordId = null;
        if (!positions.isEmpty()) {
            recordId = ProviderLocateRecords.create(owner, dimension, positions, name, key.outputId(),
                    player.level().getGameTime()).id();
        }
        ProfilerBridge.replaceProviderStart(key, owner, positions, name);
        pushAutoHighlight(player, dimension, key, positions, highlightSender);
        player.sendSystemMessage(DelayedChatText.delayedMessage(name, recordId, idleTicks, typicalTicks));
    }

    /**
     * Loader-agnostic highlight sender: shared code builds the
     * {@link ProviderHighlightCodec.Highlight} from already-resolved notify
     * state, while each loader's {@code StatsNetwork} delivers its own
     * {@code ProviderHighlightS2C} packet. Never requires an open menu.
     */
    public static BiConsumer<ServerPlayer, ProviderHighlightCodec.Highlight> defaultHighlightSender() {
        return (player, highlight) -> StatsNetwork.sendTo(player, new ProviderHighlightS2C(
                highlight.dimensionId(), highlight.positions(), highlight.outputId(),
                highlight.durationSeconds(), highlight.plateOnly()));
    }

    static void pushAutoHighlight(ServerPlayer player, String dimension, ProfileKey key,
            List<BlockPos> positions, BiConsumer<ServerPlayer, ProviderHighlightCodec.Highlight> highlightSender) {
        if (player == null || key == null || positions == null || positions.isEmpty()
                || highlightSender == null) {
            return;
        }
        highlightSender.accept(player, new ProviderHighlightCodec.Highlight(dimension, positions,
                key.outputId(), ProviderLocateCommand.HIGHLIGHT_SECONDS, true));
    }

    /**
     * Tells one player to drop every trace of an output: the empty highlight
     * routes to {@code ProviderHighlightClient.clearFor}, so the plate and
     * the edge vanish even with a closed screen and no snapshot. Used when a
     * stall resolves while the craft still runs.
     */
    static void pushClearHighlight(ServerPlayer player, ProfileKey key,
            BiConsumer<ServerPlayer, ProviderHighlightCodec.Highlight> highlightSender) {
        if (player == null || key == null || highlightSender == null) {
            return;
        }
        highlightSender.accept(player,
                new ProviderHighlightCodec.Highlight("", List.of(), key.outputId(), 0));
    }

    private DelayedNotificationServer() {
    }
}
