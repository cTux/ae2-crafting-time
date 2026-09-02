package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.almostreliable.merequester.requester.RequesterBlockEntity;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.concurrent.CompletableFuture;

final class MeRequesterFixture {
    static final String SCENARIO = "merequester-screen";
    static final String SCREEN = "com.almostreliable.merequester.client.RequesterScreen";
    private static final long REQUEST_AMOUNT = 64;

    private BlockPos position;
    private IGridNode connectionNode;
    private CompletableFuture<BlockPos> placementFuture;
    private CompletableFuture<Boolean> setupFuture;

    boolean setup(ServerPlayer player, FixtureMarker marker) {
        if (!DriverPlatform.isModLoaded("merequester")) {
            throw new IllegalStateException("ME Requester is unavailable");
        }
        if (player == null) {
            return false;
        }
        var server = player.server;
        var playerId = player.getUUID();
        if (placementFuture == null) {
            placementFuture = server.submit(() -> place(server.getPlayerList().getPlayer(playerId), marker));
        }
        if (!placementFuture.isDone()) {
            return false;
        }
        position = placementFuture.join();
        if (setupFuture == null) {
            setupFuture = server.submit(() -> finish(server.getPlayerList().getPlayer(playerId)));
        }
        if (!setupFuture.isDone()) {
            return false;
        }
        if (!setupFuture.join()) {
            setupFuture = null;
            return false;
        }
        return true;
    }

    BlockPos position() {
        return position;
    }

    private BlockPos place(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryBuild("merequester", "requester")).orElse(null);
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
            throw new IllegalStateException("ME Requester block is unavailable");
        }
        findGrid:
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            for (var direction : Direction.values()) {
                var node = GridHelper.getExposedNode(level, anchor, direction);
                if (node != null && node.getGrid() != null) {
                    connectionNode = node;
                    break findGrid;
                }
            }
        }
        if (connectionNode == null) {
            throw new IllegalStateException("fixture AE2 grid is unavailable for ME Requester");
        }
        var playerPosition = player.blockPosition();
        for (var candidate : BlockPos.betweenClosed(
                playerPosition.offset(-2, 0, -2), playerPosition.offset(2, 2, 2))) {
            if (level.getBlockState(candidate).isAir()) {
                var position = candidate.immutable();
                level.setBlockAndUpdate(position, block.defaultBlockState());
                return position;
            }
        }
        throw new IllegalStateException("no empty placement near the fixture player for ME Requester");
    }

    private boolean finish(ServerPlayer player) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(position) instanceof RequesterBlockEntity requester)) {
            throw new IllegalStateException("ME Requester block entity was not placed");
        }
        if (!requester.getMainNode().isReady()) {
            return false;
        }
        var requesterNode = requester.getMainNode().getNode();
        if (requesterNode == null) {
            return false;
        }
        if (requesterNode.getGrid() != connectionNode.getGrid()) {
            GridHelper.createConnection(connectionNode, requesterNode);
        }
        var key = AEItemKey.of(Items.DIAMOND);
        var tick = level.getGameTime();
        var networkId = ProfilerBridge.networkId(connectionNode.getGrid());
        ProfilerBridge.start(networkId, requester, key, REQUEST_AMOUNT, tick);
        ProfilerBridge.complete(networkId, requester, key, REQUEST_AMOUNT, tick + 40);
        DriverPlatform.configureRequester(requester, new GenericStack(key, REQUEST_AMOUNT));
        requester.requestChanged(0);
        return requester.getMainNode().isActive();
    }
}
