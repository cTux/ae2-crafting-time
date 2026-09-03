package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Arrays;
import java.util.Objects;

class NativeCpuFixture extends AddonCpuFixture<NativeCpuFixture.Placement> {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var level = player.level();
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("native CPU fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("native CPU fixture grid is unavailable"));
        var storage = BuiltInRegistries.BLOCK.getOptional(
                Objects.requireNonNull(Identifier.tryBuild("ae2", "1k_crafting_storage"))).orElse(null);
        if (storage == null) {
            throw new IllegalStateException("AE2 1k crafting storage is unavailable");
        }
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : Direction.values()) {
                var node = host.getGridNode(direction);
                var position = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(position).isAir()) {
                    level.setBlockAndUpdate(position, storage.defaultBlockState());
                    return new Placement(position, terminal);
                }
            }
        }
        throw new IllegalStateException("no empty connection beside the fixture AE2 grid for native CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.level();
        if (!(level.getBlockEntity(placement.storage()) instanceof CraftingBlockEntity storage)) {
            throw new IllegalStateException("AE2 crafting storage was not placed");
        }
        if (!storage.getMainNode().isReady()) {
            return false;
        }
        if (!storage.isFormed()) {
            var calculator = new CraftingCPUCalculator(storage);
            calculator.updateBlockEntities(calculator.createCluster(level, placement.storage(), placement.storage()),
                    level, placement.storage(), placement.storage());
        }
        if (!(level.getBlockEntity(placement.terminal()) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("native CPU fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst().orElseThrow();
        var storageNode = storage.getMainNode().getNode();
        if (storageNode == null) {
            return false;
        }
        if (terminalNode.getGrid() != storageNode.getGrid()) {
            GridHelper.createConnection(terminalNode, storageNode);
        }
        var cpu = storage.getCluster();
        if (cpu == null || cpu.isBusy()) {
            return false;
        }
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (!(player.level().getBlockEntity(placement.storage()) instanceof CraftingBlockEntity storage)
                || storage.getCluster() == null) {
            return null;
        }
        var cpu = storage.getCluster();
        if (!cpu.isActive() || cpu.getGrid() != grid || cpu.isBusy()) {
            throw new IllegalStateException("native CPU is not selectable; active=" + cpu.isActive()
                    + " sameGrid=" + (cpu.getGrid() == grid) + " busy=" + cpu.isBusy());
        }
        return cpu;
    }

    record Placement(BlockPos storage, BlockPos terminal) {
    }
}
