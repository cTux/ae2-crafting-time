package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.almostreliable.merequester.requester.RequesterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class MeRequesterFixture {
    static final String SCENARIO = "merequester-screen";
    static final String SCREEN = "com.almostreliable.merequester.client.RequesterScreen";

    private BlockPos position;
    private IGridNode connectionNode;

    boolean setup(ServerPlayer player, FixtureMarker marker) {
        if (!ModList.get().isLoaded("merequester")) {
            throw new IllegalStateException("ME Requester is unavailable");
        }
        if (player == null) {
            return false;
        }
        if (position == null) {
            position = place(player, marker);
            return false;
        }
        return finish(player);
    }

    BlockPos position() {
        return position;
    }

    private BlockPos place(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild("merequester", "requester"));
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
            requester.onReady();
        }
        var requesterNode = requester.getMainNode().getNode();
        if (requesterNode == null) {
            return false;
        }
        if (requesterNode.getGrid() != connectionNode.getGrid()) {
            GridHelper.createConnection(connectionNode, requesterNode);
        }
        requester.getRequests().setStack(0, new GenericStack(AEItemKey.of(Items.FURNACE), 64));
        requester.getRequests().get(0).updateState(false);
        requester.requestChanged(0);
        return requester.getMainNode().isActive();
    }
}
