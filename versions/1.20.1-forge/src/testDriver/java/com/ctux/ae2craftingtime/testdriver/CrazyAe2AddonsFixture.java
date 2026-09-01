package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Objects;

final class CrazyAe2AddonsFixture extends AddonCpuFixture<IGrid> {
    private static final int TEST_PRIORITY = 42;

    @Override
    protected IGrid place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(player.serverLevel().getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Crazy AE2 Addons fixture terminal is unavailable");
        }
        return Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Crazy AE2 Addons fixture grid is unavailable"));
    }

    @Override
    protected boolean finish(ServerPlayer player, IGrid grid) {
        var cpu = idleCpu(grid);
        if (cpu == null) {
            return false;
        }
        setPriority(cpu, TEST_PRIORITY);
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, IGrid placement, IGrid grid) {
        var cpu = idleCpu(grid);
        return cpu != null && priority(cpu) == TEST_PRIORITY ? cpu : null;
    }

    private static CraftingCPUCluster idleCpu(IGrid grid) {
        return grid.getCraftingService().getCpus().stream()
                .filter(CraftingCPUCluster.class::isInstance).map(CraftingCPUCluster.class::cast)
                .filter(candidate -> !candidate.isBusy()).findFirst().orElse(null);
    }

    private static void setPriority(CraftingCPUCluster cpu, int priority) {
        try {
            cpu.getClass().getMethod("setPrio", int.class).invoke(cpu, priority);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Crazy AE2 Addons priority setter is unavailable", error);
        }
    }

    private static int priority(CraftingCPUCluster cpu) {
        try {
            return (int) cpu.getClass().getMethod("getPrio").invoke(cpu);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Crazy AE2 Addons priority getter is unavailable", error);
        }
    }
}
