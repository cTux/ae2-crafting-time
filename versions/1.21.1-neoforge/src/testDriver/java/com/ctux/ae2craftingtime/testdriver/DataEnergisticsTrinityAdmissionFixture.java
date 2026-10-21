package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingSubmitResult;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityDataCoreBlockEntity;
import com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.Future;

/** A real oversized AE2 calculation must be rejected by native CPU storage admission. */
final class DataEnergisticsTrinityAdmissionFixture extends DataEnergisticsTrinityFixture {
    private Future<ICraftingPlan> oversized;
    private boolean rejected;

    @Override boolean admissionRejected() { return rejected; }

    @Override
    protected boolean finishVariant(ServerPlayer player, IGrid grid, Placement placement) {
        if (rejected) return true;
        var host = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(placement.origin());
        var coordinator = host.getCpuPartitions().stream().filter(cpu -> cpu.number() == 0)
                .findFirst().orElseThrow();
        long capacity = coordinator.getAvailableStorage();
        if (capacity <= 0 || capacity >= 1_000_000)
            throw new IllegalStateException("formed Trinity CPU has no bounded finite storage admission case: " + capacity);
        if (oversized == null) {
            oversized = grid.getCraftingService().beginCraftingCalculation(player.serverLevel(),
                    () -> IActionSource.ofPlayer(player), outputKey(), capacity + 1,
                    CalculationStrategy.REPORT_MISSING_ITEMS);
            return false;
        }
        if (!oversized.isDone()) return false;
        ICraftingPlan plan;
        try { plan = oversized.get(); }
        catch (Exception error) { throw new IllegalStateException("native oversized AE2 plan failed", error); }
        if (plan.bytes() <= capacity)
            throw new IllegalStateException("oversized native plan did not exceed CPU storage: "
                    + plan.bytes() + " <= " + capacity);
        var result = coordinator.submitJob(grid, plan, IActionSource.ofPlayer(player), null);
        if (result != CraftingSubmitResult.CPU_TOO_SMALL
                || host.getCpuPartitions().stream().anyMatch(TrinityDataCoreVirtualCpu::isBusy)
                || ProfilerBridge.hasPending(new ProfileKey(ProfilerBridge.networkId(grid), outputId(placement, null)))
                || ProfilerBridge.stats(new ProfileKey(ProfilerBridge.networkId(grid), outputId(placement, null)))
                        .map(stats -> stats.sampleCount()).orElse(0) != 0)
            throw new IllegalStateException("oversized native plan was not rejected without profiling: " + result);
        rejected = true;
        return true;
    }
}
