package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.oktawia.crazyae2addons.logic.interfaces.ICpuPrio;

import java.util.Arrays;
import java.util.Objects;

final class CrazyAe2AddonsFixture extends AddonCpuFixture<ICraftingCPU> {
    private static final int TEST_PRIORITY = 42;

    @Override
    protected ICraftingCPU place(ServerPlayer player, FixtureMarker marker) {
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
                .filter(candidate -> !candidate.isBusy() && candidate instanceof ICpuPrio)
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Crazy AE2 Addons priority mixin is unavailable on the fixture CPU"));
        ((ICpuPrio) cpu).setPrio(TEST_PRIORITY);
        return cpu;
    }

    @Override
    protected boolean finish(ServerPlayer player, ICraftingCPU cpu) {
        return cpu instanceof ICpuPrio priority && priority.getPrio() == TEST_PRIORITY;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, ICraftingCPU cpu, IGrid grid) {
        return !cpu.isBusy() && grid.getCraftingService().getCpus().contains(cpu)
                && ((ICpuPrio) cpu).getPrio() == TEST_PRIORITY ? cpu : null;
    }
}
