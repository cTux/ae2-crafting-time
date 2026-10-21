package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.math.BigInteger;
import java.time.Duration;
import java.util.UUID;

/** Real worker cancellation after one accepted output, then a fresh job on that worker. */
class DataEnergisticsTrinityCancelFixture extends DataEnergisticsTrinityFixture {
    private TrinityDataCoreVirtualCpu cancelledWorker;
    private UUID cancelledJob;
    private String cancelledIdentity;
    private int cancelledNumber;
    private boolean optionChecked;
    private long optionTick;

    protected OptionFeature optionFeature() { return OptionFeature.DATA_ENERGISTICS_TRINITY; }

    @Override boolean supportsCancelFollowUp() { return true; }

    @Override protected boolean usesPhysicalSuffix() { return cancelledJob == null; }

    @Override java.time.Duration submittedCraftTimeout() {
        return cancelledJob == null ? Duration.ofSeconds(90) : super.submittedCraftTimeout();
    }

    @Override protected boolean requiresLearnedActiveStatus() { return cancelledJob != null; }

    @Override
    void configureAmount(appeng.client.gui.me.crafting.CraftAmountScreen screen) {
        ((CraftAmountScreenAccessor) screen).ae2craftingtime_test_driver$amount()
                .setLongValue(cancelledJob == null ? 2 : 1);
    }

    @Override
    boolean cancelForFreshJob(ServerPlayer player, String networkId, String outputId) {
        if (worker == null || !worker.isBusy() || jobId == null || requested != 2
                || !"minecraft:glass".equals(outputId) || !sawAcceptedOutput
                || worker.getWaitingFor(outputKey()).signum() <= 0) return false;
        var key = new ProfileKey(networkId, outputId);
        var history = ProfilerBridge.stats(key).orElseThrow(
                () -> new IllegalStateException("cancellation checkpoint lacks a genuine accepted sample"));
        if (history.sampleCount() != 1)
            throw new IllegalStateException("cancellation checkpoint lacks one genuine accepted sample");
        if (!optionChecked) {
            var config = ServerOptionsRuntime.current();
            var feature = optionFeature();
            if (!config.features().enabled(feature) || !ProfilerBridge.hasPending(key))
                throw new IllegalStateException("native option checkpoint has no live pending work");
            var prior = ProfilerBridge.recoveryStamp(worker);
            ProfilerBridge.disabled(feature);
            config.features().setEnabled(feature, false);
            try {
                ProfilerBridge.configure(config);
                ServerOptionsRuntime.sendTo(player);
                if (ProfilerBridge.hasPending(key))
                    throw new IllegalStateException("turning profiling off retained pending work");
            } finally {
                config.features().setEnabled(feature, true);
                ProfilerBridge.configure(config);
                ServerOptionsRuntime.sendTo(player);
            }
            if (ProfilerBridge.hasPending(key) || !history.equals(ProfilerBridge.stats(key).orElse(null)))
                throw new IllegalStateException("turning profiling back on resumed pending work or changed history");
            if (prior.equals(ProfilerBridge.recoveryStamp(worker)))
                throw new IllegalStateException("option cycle did not invalidate the native job generation");
            optionChecked = true;
            optionTick = player.server.getTickCount();
            return false;
        }
        if (player.server.getTickCount() <= optionTick) return false;
        if (ProfilerBridge.hasPending(key) || ProfilerBridge.stats(key)
                .map(stats -> stats.sampleCount()).orElse(0) != 1)
            throw new IllegalStateException("stale native job resumed after the option cycle");
        cancelledWorker = worker;
        cancelledJob = jobId;
        cancelledIdentity = worker.getStableDispatchIdentity();
        cancelledNumber = worker.number();
        worker.cancelJob();
        if (worker.isBusy() || !link.isCanceled()
                || ProfilerBridge.stats(key).map(stats -> stats.sampleCount()).orElse(0) != 1)
            throw new IllegalStateException("native cancellation fabricated a sample or left its job active");
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
        if (!TrinityPhysicalSuffix.isolated(furnace.getItem(0).getCount(),
                furnace.getItem(2).getCount(), suffixBuffer(player).getItem(0).getCount()))
            throw new IllegalStateException("cancelled Trinity suffix was not physically isolated");
        discardCancelledPhysicalSuffix(player);
        return true;
    }

    boolean optionChecked() { return optionChecked; }

    @Override boolean sameWorkerNewJob() {
        return cancelledWorker != null && worker != cancelledWorker
                && !cancelledJob.equals(jobId)
                && cancelledNumber == worker.number()
                && cancelledIdentity.equals(worker.getStableDispatchIdentity());
    }

    @Override
    protected void verifyVariant(IGrid grid, boolean complete) {
        if (cancelledJob != null && worker != null && !worker.isBusy() && complete && !sameWorkerNewJob())
            throw new IllegalStateException("fresh native craft reused cancelled job identity or a different worker");
    }
}
