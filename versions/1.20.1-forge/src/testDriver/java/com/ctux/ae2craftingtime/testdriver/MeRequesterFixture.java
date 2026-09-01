package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
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
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-4, -4, -4), terminal.offset(4, 4, 4))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : Direction.values()) {
                var node = host.getGridNode(direction);
                var candidate = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() != null && level.getBlockState(candidate).isAir()) {
                    level.setBlockAndUpdate(candidate, block.defaultBlockState());
                    connectionNode = node;
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("no empty connection beside the fixture AE2 grid for ME Requester");
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
