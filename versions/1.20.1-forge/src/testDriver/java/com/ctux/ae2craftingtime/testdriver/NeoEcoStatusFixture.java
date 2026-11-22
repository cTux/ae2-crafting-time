package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.stacks.AEKey;
import appeng.api.config.Actionable;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftConfirmMenu;
import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.ctux.ae2craftingtime.api.ExternalCpuProfiling;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.StatsRequestContext;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public final class NeoEcoStatusFixture extends NeoEcoFixture {
    private static volatile Object heldLogic;
    private static long renderedMillis = Long.MIN_VALUE;
    private CompletableFuture<Void> action;
    private CompletableFuture<Boolean> poll;
    private ECOCraftingCPU selected;
    private IGrid grid;
    private ICraftingPlan plan;
    private MenuLocator locator;
    private UUID savedJob;
    private int learnedSamples;
    private long exactStock;
    private int phase;
    private boolean reconnect;

    public static boolean holdsReturns(Object logic) {
        return heldLogic == logic;
    }

    public static void rendered(long millis) {
        if (heldLogic != null) renderedMillis = millis;
    }

    @Override
    boolean verifyAfterSample(Minecraft client, Consumer<String> capture) {
        if (action == null) {
            schedule(client, this::openRunningStatus);
            return false;
        }
        if (!action.isDone()) return false;
        action.join();
        if (phase == 0 && renderedMillis > 0) {
            capture.accept("neoeco-native-status-running.png");
            phase++;
            schedule(client, player -> {
                selected.getLogic().setJobSuspended(true);
                expectEstimate(false);
            });
        } else if (phase == 1 && renderedMillis == -1) {
            capture.accept("neoeco-native-status-paused.png");
            phase++;
            schedule(client, player -> {
                selected.getLogic().setJobSuspended(false);
                expectEstimate(true);
            });
        } else if (phase == 2 && renderedMillis > 0) {
            phase++;
            schedule(client, this::verifyCancellationAndDisable);
        } else if (phase == 3) {
            reconnect = true;
        } else if (phase == 4 && poll(client, this::restoreRunningStatus)) {
            phase++;
        } else if (phase == 5 && renderedMillis == -1) {
            capture.accept("neoeco-native-status-after-restart.png");
            phase++;
            schedule(client, this::submitExact);
        } else if (phase == 6 && poll(client, this::exactFinished)) {
            return true;
        }
        return false;
    }

    @Override
    boolean reconnectRequested() { return reconnect; }

    @Override
    void reconnected() {
        reconnect = false;
        phase = 4;
        renderedMillis = Long.MIN_VALUE;
    }

    private void schedule(Minecraft client, Consumer<ServerPlayer> operation) {
        var server = client.getSingleplayerServer();
        var playerId = client.player.getUUID();
        action = server.submit(() -> operation.accept(server.getPlayerList().getPlayer(playerId)));
    }

    private boolean poll(Minecraft client, Function<ServerPlayer, Boolean> operation) {
        if (poll == null) {
            var server = client.getSingleplayerServer();
            var playerId = client.player.getUUID();
            poll = server.submit(() -> operation.apply(server.getPlayerList().getPlayer(playerId)));
        }
        if (!poll.isDone()) return false;
        boolean ready = poll.join();
        poll = null;
        return ready;
    }

    private void openRunningStatus(ServerPlayer player) {
        if (!(player.containerMenu instanceof CraftConfirmMenu menu)) {
            throw new IllegalStateException("NeoEco follow-up needs the learned AE2 plan");
        }
        grid = StatsRequestContext.current(player).grid();
        plan = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result();
        selected = (ECOCraftingCPU) cpu(player, grid);
        if (selected == null || plan == null || plan.simulation()) {
            throw new IllegalStateException("NeoEco status fixture needs a real CPU and follow-up plan");
        }
        heldLogic = selected.getLogic();
        locator = menu.getLocator();
        submit(player);
        expectEstimate(true);
        openStatus(player);
    }

    private void openStatus(ServerPlayer player) {
        try {
            var status = Class.forName("cn.dancingsnow.neoecoae.gui.computation.ECOCraftingStatusUI");
            if (!Boolean.TRUE.equals(status.getMethod("tryOpen", ServerPlayer.class, MenuLocator.class)
                    .invoke(null, player, locator))) {
                throw new IllegalStateException("NeoEco declined the real terminal status request");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot open NeoEco's status page", exception);
        }
    }

    private void submit(ServerPlayer player) {
        if (!grid.getCraftingService().submitJob(plan, null, selected, false, IActionSource.ofPlayer(player))
                .successful() || !selected.isBusy()) {
            throw new IllegalStateException("NeoEco follow-up job was not accepted");
        }
    }

    private void verifyCancellationAndDisable(ServerPlayer player) {
        selected.cancelJob();
        if (ProfilerBridge.tracksExternalJob(selected)) {
            throw new IllegalStateException("Cancelled NeoEco job left live tracking");
        }
        expectEstimate(false);
        submit(player);
        expectEstimate(true);
        var config = ServerOptionsRuntime.current();
        boolean original = config.features().enabled(OptionFeature.NEO_ECO);
        var key = ProfilerBridge.key(ProfilerBridge.networkId(grid), plan.finalOutput().what());
        learnedSamples = ProfilerBridge.stats(key).orElseThrow().sampleCount();
        try {
            config.features().setEnabled(OptionFeature.NEO_ECO, false);
            ProfilerBridge.configure(config);
            expectEstimate(false);
        } finally {
            config.features().setEnabled(OptionFeature.NEO_ECO, original);
            ProfilerBridge.configure(config);
        }
        expectEstimate(false);
        if (ProfilerBridge.stats(key).orElseThrow().sampleCount() != learnedSamples) {
            throw new IllegalStateException("Disabling the NeoEco bridge changed learned history");
        }
        selected.cancelJob();
        submit(player);
        expectEstimate(true);
        selected.getLogic().setJobSuspended(true);
        savedJob = getJobId(selected);
        if (savedJob == null) throw new IllegalStateException("NeoEco job has no persisted identity");
    }

    private boolean restoreRunningStatus(ServerPlayer player) {
        var host = locator.locate(player, ITerminalHost.class);
        if (!(host instanceof IActionHost actionHost) || actionHost.getActionableNode() == null) return false;
        grid = actionHost.getActionableNode().getGrid();
        if (grid == null) return false;
        var restored = (ECOCraftingCPU) cpu(player, grid);
        if (restored == null || !restored.isBusy()) return false;
        if (selected == restored || !savedJob.equals(getJobId(restored)) || !restored.getLogic().isJobSuspended()) {
            throw new IllegalStateException("Restart did not restore the same suspended NeoEco job on a new CPU object");
        }
        selected = restored;
        heldLogic = restored.getLogic();
        restored.getLogic().setJobSuspended(false);
        expectEstimate(false);
        if (ProfilerBridge.stats(ProfilerBridge.key(ProfilerBridge.networkId(grid), plan.finalOutput().what()))
                .orElseThrow().sampleCount() != learnedSamples) {
            throw new IllegalStateException("Restart did not retain learned NeoEco history");
        }
        openStatus(player);
        return true;
    }

    private static UUID getJobId(ECOCraftingCPU cpu) {
        try {
            var logic = cpu.getLogic();
            return (UUID) logic.getClass().getMethod("getProcessingJobId").invoke(logic);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read the real NeoEco crafting ID", exception);
        }
    }

    private void submitExact(ServerPlayer player) {
        selected.cancelJob();
        heldLogic = selected.getLogic();
        var output = plan.finalOutput().what();
        exactStock = stock(output);
        try {
            var options = Class.forName("cn.dancingsnow.neoecoae.api.me.planning.ECOPlannerOptions");
            var defaults = options.getConstructor(boolean.class, boolean.class, Set.class)
                    .newInstance(true, false, Set.of());
            var request = Class.forName("cn.dancingsnow.neoecoae.api.me.bigorder.ECOBigOrderRequest");
            var exact = request.getConstructor(AEKey.class, BigInteger.class, boolean.class, options)
                    .newInstance(output, BigInteger.ONE, true, defaults);
            Function<ICraftingPlan, ICraftingSubmitResult> submit = exactPlan ->
                    grid.getCraftingService().submitJob(exactPlan, null, selected, false, IActionSource.ofPlayer(player));
            var result = (ICraftingSubmitResult) request.getMethod("submit", Function.class).invoke(exact, submit);
            if (!result.successful() || !selected.isBusy()) {
                throw new IllegalStateException("NeoEco did not accept the native exact order: " + result);
            }
            expectEstimate(false);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot submit the native exact order", exception);
        } finally {
            heldLogic = null;
        }
    }

    private boolean exactFinished(ServerPlayer player) {
        expectEstimate(false);
        if (selected.isBusy()) return false;
        var output = plan.finalOutput().what();
        if (stock(output) != exactStock + 1) throw new IllegalStateException("Exact order did not return one output");
        if (ProfilerBridge.stats(ProfilerBridge.key(ProfilerBridge.networkId(grid), output))
                .orElseThrow().sampleCount() != learnedSamples) {
            throw new IllegalStateException("Exact order added a Crafting Time sample");
        }
        return true;
    }

    private long stock(AEKey output) {
        return grid.getStorageService().getInventory().extract(output, Long.MAX_VALUE, Actionable.SIMULATE,
                IActionSource.empty());
    }

    private void expectEstimate(boolean known) {
        var value = ExternalCpuProfiling.remainingMillis(selected);
        if (known ? value.orElse(0) <= 0 : value.isPresent()) {
            throw new IllegalStateException("Unexpected NeoEco active estimate: " + value);
        }
    }

    @Override
    protected void cleanup(ServerPlayer player, Placement placement) {
        if (selected != null && selected.isBusy()) selected.cancelJob();
        heldLogic = null;
        renderedMillis = Long.MIN_VALUE;
    }
}
