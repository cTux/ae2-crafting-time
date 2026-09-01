package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
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

import java.util.Arrays;
import java.util.Objects;

final class MeRequesterFixture {
    static final String SCENARIO = "merequester-screen";
    static final String SCREEN = "com.almostreliable.merequester.client.RequesterScreen";

    private BlockPos position;

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
        return finish(player, marker);
    }

    BlockPos position() {
        return position;
    }

    private BlockPos place(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            return null;
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("ME Requester fixture grid is unavailable"));
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild("merequester", "requester"));
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
            throw new IllegalStateException("ME Requester block is unavailable");
        }
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : Direction.values()) {
                var node = host.getGridNode(direction);
                var candidate = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(candidate).isAir()) {
                    level.setBlockAndUpdate(candidate, block.defaultBlockState());
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("no empty connection beside the fixture AE2 grid for ME Requester");
    }

    private boolean finish(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(position) instanceof RequesterBlockEntity requester)) {
            throw new IllegalStateException("ME Requester block entity was not placed");
        }
        if (!requester.getMainNode().isReady()) {
            requester.onReady();
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var terminalHost = (IInWorldGridNodeHost) level.getBlockEntity(terminal);
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst().orElseThrow();
        var requesterNode = requester.getMainNode().getNode();
        if (requesterNode == null) {
            return false;
        }
        if (requesterNode.getGrid() != terminalNode.getGrid()) {
            GridHelper.createConnection(terminalNode, requesterNode);
        }
        requester.getRequests().setStack(0, new GenericStack(AEItemKey.of(Items.FURNACE), 64));
        requester.getRequests().get(0).updateState(false);
        requester.requestChanged(0);
        return requester.getMainNode().isActive();
    }
}
