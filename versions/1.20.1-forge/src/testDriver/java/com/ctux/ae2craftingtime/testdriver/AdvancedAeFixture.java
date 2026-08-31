package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCalculator;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;

import java.util.Arrays;
import java.util.Objects;

final class AdvancedAeFixture {
    private AdvancedAeFixture() {
    }

    static Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("fixture terminal is not connected to an AE2 grid");
        }
        IGrid grid = Arrays.stream(Direction.values())
                .map(terminalHost::getGridNode)
                .filter(Objects::nonNull)
                .map(node -> node.getGrid())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture terminal grid is unavailable"));
        var core = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild("advanced_ae", "quantum_core"));
        if (core == null) {
            throw new IllegalStateException("AdvancedAE quantum core is unavailable");
        }
        for (BlockPos anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : new Direction[] { Direction.UP, Direction.DOWN }) {
                var node = host.getGridNode(direction);
                var position = anchor.relative(direction);
                if (node != null && node.getGrid() == grid && level.getBlockState(position).isAir()) {
                    level.setBlockAndUpdate(position, core.defaultBlockState());
                    return new Placement(position, anchor.immutable(), direction);
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture AE2 grid for AdvancedAE CPU");
    }

    static boolean finish(ServerPlayer player, Placement placement) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.core()) instanceof AdvCraftingBlockEntity core)) {
            throw new IllegalStateException("AdvancedAE quantum core was not placed");
        }
        if (!core.getMainNode().isReady()) {
            core.onReady();
        }
        if (!core.isFormed() && core.getMainNode().isReady()) {
            new AdvCraftingCPUCalculator(core).calculateMultiblock(level, placement.core());
        }
        if (!core.isFormed()) {
            return false;
        }
        if (!(level.getBlockEntity(placement.anchor()) instanceof IInWorldGridNodeHost host)) {
            throw new IllegalStateException("AdvancedAE quantum core connection is unavailable");
        }
        var hostNode = host.getGridNode(placement.direction());
        var coreNode = core.getGridNode(placement.direction().getOpposite());
        if (hostNode == null || coreNode == null) {
            return false;
        }
        if (hostNode.getGrid() != coreNode.getGrid()) {
            GridHelper.createConnection(hostNode, coreNode);
        }
        return true;
    }

    static ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (player == null
                || !(player.serverLevel().getBlockEntity(placement.core()) instanceof AdvCraftingBlockEntity core)
                || core.getCluster() == null) {
            return null;
        }
        var cpu = core.getCluster().getRemainingCapacityCPU();
        return cpu.isActive() && cpu.getGrid() == grid ? cpu : null;
    }

    record Placement(BlockPos core, BlockPos anchor, Direction direction) {
    }
}
