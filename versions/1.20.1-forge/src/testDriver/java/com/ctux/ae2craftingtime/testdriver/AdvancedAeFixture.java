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

final class AdvancedAeFixture extends AddonCpuFixture<AdvancedAeFixture.Placement> {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
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
                var position = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(position).isAir()) {
                    level.setBlockAndUpdate(position, core.defaultBlockState());
                    if (!(level.getBlockEntity(position) instanceof AdvCraftingBlockEntity)) {
                        throw new IllegalStateException("AdvancedAE quantum core placement produced "
                                + ForgeRegistries.BLOCKS.getKey(level.getBlockState(position).getBlock()));
                    }
                    return new Placement(position, terminal);
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture AE2 grid for AdvancedAE CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
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
        if (!core.isFormed()) {
            var calculator = new AdvCraftingCPUCalculator(core);
            calculator.updateBlockEntities(
                    calculator.createCluster(level, placement.core(), placement.core()),
                    level, placement.core(), placement.core());
        }
        if (!core.isFormed()) {
            throw new IllegalStateException("AdvancedAE quantum core did not form after rescan; node ready="
                    + core.getMainNode().isReady());
        }
        if (!(level.getBlockEntity(placement.terminal()) instanceof IInWorldGridNodeHost host)) {
            throw new IllegalStateException("AdvancedAE fixture terminal is unavailable");
        }
        var hostNode = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException("AdvancedAE fixture terminal node is unavailable"));
        var coreNode = core.getMainNode().getNode();
        if (coreNode == null) {
            throw new IllegalStateException("AdvancedAE core node is unavailable");
        }
        if (hostNode.getGrid() != coreNode.getGrid()) {
            GridHelper.createConnection(hostNode, coreNode);
        }
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (player == null
                || !(player.serverLevel().getBlockEntity(placement.core()) instanceof AdvCraftingBlockEntity core)
                || core.getCluster() == null) {
            return null;
        }
        var cpu = core.getCluster().getRemainingCapacityCPU();
        if (!cpu.isActive() || cpu.getGrid() != grid) {
            throw new IllegalStateException("AdvancedAE CPU is not selectable; active=" + cpu.isActive()
                    + " sameGrid=" + (cpu.getGrid() == grid)
                    + " nodeActive=" + core.getMainNode().isActive()
                    + " nodeOnline=" + core.getMainNode().isOnline()
                    + " nodePowered=" + core.getMainNode().isPowered());
        }
        return cpu;
    }

    record Placement(BlockPos core, BlockPos terminal) {
    }
}
