package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import com.wintercogs.ae2omnicells.common.blocks.entities.OmniCraftingBlockEntity;
import com.wintercogs.ae2omnicells.common.init.OCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Objects;

final class OmniCellsFixture extends AddonCpuFixture<OmniCellsFixture.Placement> {
    private static final long STORAGE_BYTES = 1024L * 1024L;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var level = player.serverLevel();
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("OMNI Cells fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("OMNI Cells fixture grid is unavailable"));
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : new Direction[] { Direction.UP, Direction.DOWN }) {
                var node = host.getGridNode(direction);
                var position = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(position).isAir()) {
                    level.setBlockAndUpdate(position, OCBlocks.OMNI_CRAFTING_STORAGE_1M_BLOCK.get().defaultBlockState());
                    return new Placement(position, terminal);
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture AE2 grid for OMNI CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.storage()) instanceof OmniCraftingBlockEntity storage)) {
            throw new IllegalStateException("OMNI crafting storage was not placed");
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
            throw new IllegalStateException("OMNI Cells fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("OMNI Cells fixture terminal node is unavailable"));
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
                || !(player.serverLevel().getBlockEntity(placement.storage()) instanceof OmniCraftingBlockEntity storage)
                || storage.getCluster() == null) {
            return null;
        }
        var cpu = storage.getCluster();
        if (!cpu.isActive() || cpu.getGrid() != grid || cpu.getAvailableStorage() != STORAGE_BYTES) {
            throw new IllegalStateException("OMNI CPU is not selectable; active=" + cpu.isActive()
                    + " sameGrid=" + (cpu.getGrid() == grid) + " storage=" + cpu.getAvailableStorage());
        }
        return cpu;
    }

    record Placement(BlockPos storage, BlockPos terminal) {
    }
}
