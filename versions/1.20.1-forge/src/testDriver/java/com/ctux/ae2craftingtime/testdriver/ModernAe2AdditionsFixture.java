package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import stone.mae2.bootstrap.MAE2Blocks;

import java.util.Arrays;
import java.util.Objects;

final class ModernAe2AdditionsFixture
        extends AddonCpuFixture<ModernAe2AdditionsFixture.Placement> {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var level = player.serverLevel();
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Modern AE2 Additions fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Modern AE2 Additions fixture grid is unavailable"));
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : Direction.values()) {
                var node = host.getGridNode(direction);
                var storage = anchor.relative(direction).immutable();
                var accelerator = storage.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(storage).isAir()
                        && level.getBlockState(accelerator).isAir()) {
                    level.setBlockAndUpdate(storage, AEBlocks.CRAFTING_STORAGE_1K.block().defaultBlockState());
                    level.setBlockAndUpdate(accelerator,
                            MAE2Blocks.ACCELERATOR_4x.get().defaultBlockState());
                    return new Placement(storage, accelerator, terminal);
                }
            }
        }
        throw new IllegalStateException(
                "no two-block connection beside the fixture AE2 grid for Modern AE2 Additions CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.storage()) instanceof CraftingBlockEntity storage)
                || !(level.getBlockEntity(placement.accelerator()) instanceof CraftingBlockEntity accelerator)) {
            throw new IllegalStateException("Modern AE2 Additions crafting CPU was not placed");
        }
        if (!storage.getMainNode().isReady()) {
            storage.onReady();
        }
        if (!accelerator.getMainNode().isReady()) {
            accelerator.onReady();
        }
        if (!storage.isFormed()) {
            var min = new BlockPos(Math.min(placement.storage().getX(), placement.accelerator().getX()),
                    Math.min(placement.storage().getY(), placement.accelerator().getY()),
                    Math.min(placement.storage().getZ(), placement.accelerator().getZ()));
            var max = new BlockPos(Math.max(placement.storage().getX(), placement.accelerator().getX()),
                    Math.max(placement.storage().getY(), placement.accelerator().getY()),
                    Math.max(placement.storage().getZ(), placement.accelerator().getZ()));
            var calculator = new CraftingCPUCalculator(storage);
            calculator.updateBlockEntities(calculator.createCluster(level, min, max), level, min, max);
        }
        if (!(level.getBlockEntity(placement.terminal()) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Modern AE2 Additions fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Modern AE2 Additions fixture terminal node is unavailable"));
        var storageNode = storage.getMainNode().getNode();
        if (storageNode == null) {
            return false;
        }
        if (terminalNode.getGrid() != storageNode.getGrid()) {
            GridHelper.createConnection(terminalNode, storageNode);
        }
        return storage.getCluster() != null && !storage.getCluster().isBusy();
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (player == null
                || !(player.serverLevel().getBlockEntity(placement.storage()) instanceof CraftingBlockEntity storage)
                || storage.getCluster() == null) {
            return null;
        }
        var cpu = storage.getCluster();
        if (!cpu.isActive() || cpu.getGrid() != grid || cpu.getCoProcessors() != 4) {
            throw new IllegalStateException("Modern AE2 Additions CPU is not selectable; active=" + cpu.isActive()
                    + " sameGrid=" + (cpu.getGrid() == grid) + " coProcessors=" + cpu.getCoProcessors());
        }
        return cpu;
    }

    record Placement(BlockPos storage, BlockPos accelerator, BlockPos terminal) {
    }
}
