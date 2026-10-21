package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.CraftingJobEstimateCodec;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityDataCoreBlockEntity;
import com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu;
import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/** Two client processes share the same disposable world, native worker and job. */
final class DataEnergisticsTrinityReloadFixture extends DataEnergisticsTrinityFixture {
    private static final Gson JSON = new Gson();
    private RestartState resumed;

    @Override boolean supportsRestart() { return true; }

    @Override protected boolean usesPhysicalSuffix() { return resumed == null; }

    @Override Duration submittedCraftTimeout() {
        return resumed == null ? Duration.ofSeconds(90) : super.submittedCraftTimeout();
    }

    @Override protected boolean requiresLearnedActiveStatus() { return resumed != null; }

    @Override int restartSampleBaseline() { return 1; }

    @Override boolean restoredGraphAdvanced() {
        return TrinityGraphObservation.advanced(worker, new ProfileKey(resumed.networkId, resumed.outputId));
    }

    @Override
    void configureAmount(appeng.client.gui.me.crafting.CraftAmountScreen screen) {
        ((CraftAmountScreenAccessor) screen).ae2craftingtime_test_driver$amount()
                .setLongValue(resumed == null ? 2 : 1);
    }

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var options = DriverOptions.load();
        if (options.continuation() == null) return super.place(player, marker);
        resumed = read(options.continuation(), options);
        var origin = new BlockPos(resumed.x, resumed.y, resumed.z);
        var provider = new BlockPos(resumed.providerX, resumed.providerY, resumed.providerZ);
        var owned = rotatedUnion(origin);
        for (int depth = 0; depth < 4; depth++) owned.add(provider.below(depth));
        owned.add(outputHoldPower(provider));
        owned.add(outputSuffixBuffer(provider));
        placement = new Placement(origin,
                new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z()), provider, owned);
        expectedResumeJobId = UUID.fromString(resumed.jobId);
        retainBufferedPrefix(resumed.jobId, 2, resumed.initialHoldNanos);
        requested = resumed.requested;
        beforeInput = resumed.inputBefore;
        beforeOutput = resumed.outputBefore;
        maxWaiting = new BigInteger(resumed.waiting);
        lastWaiting = new BigInteger(resumed.currentWaiting);
        waitingDecrement = maxWaiting.subtract(lastWaiting);
        sawAcceptedOutput = waitingDecrement.signum() > 0;
        await("restart-checkpoint-bound");
        return placement;
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (resumed == null) {
            return super.finish(player, placement);
        }
        submittedHost = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(placement.origin());
        if (submittedHost == null) return await("restart-host-missing");
        if (!submittedHost.isStructureFormed()) return await("restart-main-unformed");
        if (!submittedHost.isCpuStructureFormed()) return await("restart-cpu-unformed");
        if (!submittedHost.isCraftingStructureFormed()) return await("restart-crafting-unformed");
        if (submittedHost.craftingExecutionRoute() == null) return await("restart-route-missing");
        submittedGrid = submittedHost.craftingExecutionRoute().serviceGrid();
        if (submittedGrid == null) return await("restart-grid-missing");
        var partitions = submittedHost.getCpuPartitions().stream().toList();
        var matching = partitions.stream()
                .filter(cpu -> cpu.number() == resumed.workerNumber).toList();
        var published = submittedGrid.getCraftingService().getCpus();
        worker = matching.stream()
                .filter(cpu -> cpu.isBusy() && published.contains(cpu))
                .findFirst().orElse(null);
        if (worker == null) return await("restart-worker-missing:matching=" + matching.size()
                + ":busy=" + matching.stream().filter(TrinityDataCoreVirtualCpu::isBusy).count()
                + ":published=" + matching.stream().filter(published::contains).count()
                + ":partitions=" + partitions.stream().map(cpu -> cpu.number() + "/"
                        + cpu.isBusy() + "/" + published.contains(cpu)).toList());
        if (!Objects.equals(resumed.workerIdentity, worker.getStableDispatchIdentity()))
            throw new IllegalStateException("Trinity worker identity changed across restart");
        link = jobLink(worker);
        jobId = link.getCraftingID();
        if (!expectedResumeJobId.equals(jobId))
            throw new IllegalStateException("Trinity job UUID changed across restart");
        return true;
    }

    @Override
    boolean writeRestartCheckpoint(Path path, DriverOptions options, String networkId) {
        if (worker == null || !worker.isBusy() || jobId == null || requested != 2
                || !maxWaiting.equals(BigInteger.TWO)
                || !worker.getWaitingFor(AEItemKey.of(Items.GLASS)).equals(BigInteger.ONE)
                || !waitingDecrement.equals(BigInteger.ONE) || !sawAcceptedOutput)
            return false;
        var inventory = submittedGrid.getStorageService().getInventory();
        // Trinity holds the accepted prefix in its busy CPU until the whole job completes.
        if (inventory.getAvailableStacks().get(AEItemKey.of(Items.GLASS)) - beforeOutput != 0)
            return false;
        var sampleCount = ProfilerBridge.stats(new ProfileKey(networkId, "minecraft:glass"))
                .map(stats -> stats.sampleCount()).orElse(0);
        if (sampleCount != 1) return false;
        var estimate = ProfilerBridge.saveEstimate(worker, ProfilerBridge.newRecovery(),
                playerServer(), jobId, AEItemKey.of(Items.GLASS), submittedGrid);
        if (estimate.isEmpty()) throw new IllegalStateException("active Trinity graph was not saved before restart");
        if (graphRemaining(estimate.orElseThrow(), jobId, networkId) != 1)
            throw new IllegalStateException("active Trinity graph did not retain exactly one output");
        var furnace = (FurnaceBlockEntity) submittedHost.getLevel().getBlockEntity(placement.provider().below());
        var level = (net.minecraft.server.level.ServerLevel) submittedHost.getLevel();
        int bufferedSand = bufferedSand(level);
        int bufferedGlass = bufferedGlass(level);
        if (!twoGlassBeforeStatus() || !physicalSuffixBuffered()
                || !TrinityPhysicalSuffix.isolated(
                        furnace.getItem(0).getCount(), furnace.getItem(2).getCount(),
                        bufferedSand + bufferedGlass)
                || bufferedGlass != 1)
            throw new IllegalStateException("Trinity restart lacks its real isolated glass suffix");
        armOutputHold(level, placement);
        var state = new RestartState(2, "relaunch-ready", options.world(), options.campaign(), networkId,
                "minecraft:glass", jobId.toString(), worker.number(), worker.getStableDispatchIdentity(),
                placement.origin().getX(), placement.origin().getY(), placement.origin().getZ(),
                placement.provider().getX(), placement.provider().getY(), placement.provider().getZ(),
                requested, beforeInput, beforeOutput, maxWaiting.toString(),
                worker.getWaitingFor(AEItemKey.of(Items.GLASS)).toString(),
                bufferedSand, bufferedGlass, sampleCount, twoGlassBeforeStatus(), bufferedPrefixHoldNanos());
        try {
            Files.createDirectories(path.getParent());
            var temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, JSON.toJson(state));
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException error) {
            throw new IllegalStateException("cannot save Trinity restart checkpoint", error);
        }
        return true;
    }

    @Override
    String resumeRestartCheckpoint(ServerPlayer player, DriverOptions options) {
        var grid = submittedGrid;
        if (resumed == null || grid == null ||
                !resumed.networkId.equals(ProfilerBridge.networkId(grid)))
            throw new IllegalStateException("Trinity restored job has a different service grid");
        nativeEvidence(player, grid);
        if (!worker.isBusy() || worker.getWaitingFor(AEItemKey.of(Items.GLASS)).signum() <= 0)
            throw new IllegalStateException("Trinity restored job has no live waiting output");
        var sampleCount = ProfilerBridge.stats(new ProfileKey(resumed.networkId, resumed.outputId))
                .map(stats -> stats.sampleCount()).orElse(0);
        if (sampleCount != resumed.sampleCount || sampleCount != 1)
            throw new IllegalStateException("pre-reload Trinity timing history was not preserved exactly");
        var estimate = ProfilerBridge.saveEstimate(worker, ProfilerBridge.newRecovery(), player.server,
                jobId, AEItemKey.of(Items.GLASS), grid);
        if (estimate.isEmpty()) throw new IllegalStateException("matching Trinity job estimate did not recover");
        if (graphRemaining(estimate.orElseThrow(), jobId, resumed.networkId) != 1)
            throw new IllegalStateException("restored Trinity graph did not retain exactly one output");
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
        if (!TrinityPhysicalSuffix.isolated(furnace.getItem(0).getCount(),
                furnace.getItem(2).getCount(), bufferedSand(player) + bufferedGlass(player)))
            throw new IllegalStateException("Trinity physical suffix changed across restart");
        adoptPhysicalSuffix(player, resumed.bufferedGlass == 1 ? "minecraft:glass" : "minecraft:sand");
        adoptOutputHold(player);
        return resumed.networkId;
    }

    @Override
    String nativeEvidence(ServerPlayer player, IGrid grid) {
        return super.nativeEvidence(player, resumed == null ? grid : submittedGrid);
    }

    private static long graphRemaining(byte[] encoded, UUID expectedJob, String networkId) {
        var saved = CraftingJobEstimateCodec.decode(encoded).orElseThrow(
                () -> new IllegalStateException("Trinity saved graph is invalid"));
        var root = new ProfileKey(networkId, "minecraft:glass");
        if (!expectedJob.equals(saved.jobId()) || !root.equals(saved.graph().root()))
            throw new IllegalStateException("Trinity saved graph identity changed");
        return saved.graph().remainingAmounts().getOrDefault(root, -1L);
    }

    private net.minecraft.server.MinecraftServer playerServer() {
        return Objects.requireNonNull(submittedHost.getLevel()).getServer();
    }

    private static RestartState read(Path path, DriverOptions options) {
        try {
            var state = JSON.fromJson(Files.readString(path), RestartState.class);
            if (state == null || state.schema != 2 || !"relaunch-ready".equals(state.phase)
                    || !options.world().equals(state.world) || !options.campaign().equals(state.campaign)
                    || !"minecraft:glass".equals(state.outputId) || state.requested != 2
                    || state.sampleCount != 1 || state.bufferedSand < 0 || state.bufferedGlass < 0
                    || state.bufferedSand != 0 || state.bufferedGlass != 1 || !state.twoGlassBeforeStatus
                    || state.initialHoldNanos <= 0
                    || state.workerNumber <= 0 || new BigInteger(state.currentWaiting).signum() <= 0)
                throw new IllegalArgumentException("invalid Trinity restart checkpoint");
            UUID.fromString(state.jobId);
            return state;
        } catch (IOException error) {
            throw new IllegalStateException("cannot read Trinity restart checkpoint", error);
        }
    }

    private record RestartState(int schema, String phase, String world, String campaign,
            String networkId, String outputId, String jobId, int workerNumber, String workerIdentity,
            int x, int y, int z, int providerX, int providerY, int providerZ,
            long requested, long inputBefore, long outputBefore, String waiting,
            String currentWaiting, int bufferedSand, int bufferedGlass,
            int sampleCount, boolean twoGlassBeforeStatus, long initialHoldNanos) { }
}
