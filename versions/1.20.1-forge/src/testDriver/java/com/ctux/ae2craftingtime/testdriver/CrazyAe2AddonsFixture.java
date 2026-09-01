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

final class CrazyAe2AddonsFixture extends AddonCpuFixture<CraftingCPUCluster> {
    private static final int TEST_PRIORITY = 42;

    @Override
    protected CraftingCPUCluster place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(player.serverLevel().getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Crazy AE2 Addons fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Crazy AE2 Addons fixture grid is unavailable"));
        var cpu = grid.getCraftingService().getCpus().stream()
                .filter(CraftingCPUCluster.class::isInstance).map(CraftingCPUCluster.class::cast)
                .filter(candidate -> !candidate.isBusy())
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Crazy AE2 Addons fixture has no idle AE2 CPU"));
        setPriority(cpu, TEST_PRIORITY);
        return cpu;
    }

    @Override
    protected boolean finish(ServerPlayer player, CraftingCPUCluster cpu) {
        return priority(cpu) == TEST_PRIORITY;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, CraftingCPUCluster cpu, IGrid grid) {
        return !cpu.isBusy() && cpu.isActive() && cpu.getGrid() == grid
                && priority(cpu) == TEST_PRIORITY ? cpu : null;
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
