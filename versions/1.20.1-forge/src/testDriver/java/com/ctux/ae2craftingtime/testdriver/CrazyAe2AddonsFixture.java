package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Objects;

final class CrazyAe2AddonsFixture extends AddonCpuFixture<CrazyAe2AddonsFixture.Placement> {
    private static final int TEST_PRIORITY = 42;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var level = player.serverLevel();
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Crazy AE2 Addons fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Crazy AE2 Addons fixture grid is unavailable"));
        var storage = ForgeRegistries.BLOCKS.getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild("ae2", "1k_crafting_storage")));
        if (storage == null) {
            throw new IllegalStateException("AE2 1k crafting storage is unavailable");
        }
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : new Direction[] { Direction.UP, Direction.DOWN }) {
                var node = host.getGridNode(direction);
                var position = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(position).isAir()) {
                    level.setBlockAndUpdate(position, storage.defaultBlockState());
                    return new Placement(position, terminal);
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture AE2 grid for native CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.storage()) instanceof CraftingBlockEntity storage)) {
            throw new IllegalStateException("AE2 crafting storage was not placed");
        }
        if (!storage.getMainNode().isReady()) {
            storage.onReady();
        }
        if (!storage.isFormed()) {
            var calculator = new CraftingCPUCalculator(storage);
            calculator.updateBlockEntities(calculator.createCluster(level, placement.storage(), placement.storage()),
                    level, placement.storage(), placement.storage());
        }
        if (!(level.getBlockEntity(placement.terminal()) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Crazy AE2 Addons fixture terminal is unavailable");
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
        setPriority(cpu, TEST_PRIORITY);
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return grid.getCraftingService().getCpus().stream()
                .filter(CrazyAe2AddonsFixture::isNativeCpu)
                .filter(candidate -> !candidate.isBusy() && priority(candidate) == TEST_PRIORITY)
                .findFirst().orElse(null);
    }

    private static boolean isNativeCpu(ICraftingCPU cpu) {
        return cpu.getClass().getName().equals("appeng.me.cluster.implementations.CraftingCPUCluster");
    }

    private static void setPriority(ICraftingCPU cpu, int priority) {
        try {
            cpu.getClass().getMethod("setPrio", int.class).invoke(cpu, priority);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Crazy AE2 Addons priority setter is unavailable", error);
        }
    }

    private static int priority(ICraftingCPU cpu) {
        try {
            return (int) cpu.getClass().getMethod("getPrio").invoke(cpu);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Crazy AE2 Addons priority getter is unavailable", error);
        }
    }

    record Placement(BlockPos storage, BlockPos terminal) {
    }
}
