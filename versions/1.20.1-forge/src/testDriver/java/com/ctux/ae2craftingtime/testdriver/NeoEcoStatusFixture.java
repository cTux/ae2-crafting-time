package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingPlan;
import appeng.api.config.Actionable;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftConfirmMenu;
import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import com.ctux.ae2craftingtime.api.ExternalCpuProfiling;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.StatsRequestContext;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusText;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MouseHandlerAccessor;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public class NeoEcoStatusFixture extends NeoEcoFixture {
    private static volatile Object heldLogic;
    private static long renderedMillis = Long.MIN_VALUE;
    private static int renderedSerial = -1;
    private static final Map<AEKey, StatusCell> frame = new HashMap<>();
    private static Map<AEKey, StatusCell> renderedRows = Map.of();
    private static AEKey renderedKey;
    private static int cellX;
    private static int cellY;
    private static AEKey hoveredKey;
    private static String hoveredEstimate;
    private static volatile boolean observeRows;
    private static volatile boolean omitRowPayload;
    private static volatile boolean widenGoldEstimate;
    public static final double WIDE_MILLIS = 1e22;
    private CompletableFuture<Void> action;
    private CompletableFuture<Boolean> poll;
    private ECOCraftingCPU selected;
    private IGrid grid;
    private ICraftingPlan plan;
    private MenuLocator locator;
    private UUID savedJob;
    private int learnedSamples;
    private long exactStock;
    private long baselineStock;
    private int phase;
    private boolean reconnect;
    private int rowPhase;
    private boolean originalRows;

    private record StatusCell(List<String> quantities, String estimate, int x, int y) {}

    public static boolean holdsDispatch(Object logic) {
        return heldLogic == logic;
    }

    public static void rendered(long millis, int serial) {
        if (heldLogic != null) {
            renderedMillis = millis;
            renderedSerial = serial;
            renderedRows = Map.copyOf(frame);
        }
    }

    public static void beginFrame() {
        frame.clear();
        renderedKey = null;
    }

    public static boolean observeRows() {
        return observeRows;
    }

    public static boolean omitRowPayload() {
        return omitRowPayload;
    }

    public static boolean widenGoldEstimate() {
        return widenGoldEstimate;
    }

    public static void drawKey(AEKey key, int x, int y) {
        renderedKey = key;
        cellX = x;
        cellY = y;
    }

    public static void drawLines(List<Component> lines) {
        if (renderedKey == null) return;
        var quantities = lines.stream().filter(line -> !NeoEcoStatusText.isEstimate(line))
                .map(Component::getString).toList();
        var estimate = lines.stream().filter(NeoEcoStatusText::isEstimate).map(Component::getString)
                .findFirst().orElse(null);
        frame.put(renderedKey, new StatusCell(quantities, estimate, cellX, cellY));
        renderedKey = null;
    }

    public static void hover(AEKey key, List<Component> lines) {
        hoveredKey = key;
        hoveredEstimate = lines.stream().filter(NeoEcoStatusText::isEstimate).map(Component::getString)
                .findFirst().orElse(null);
    }

    void startBaseline(ServerPlayer player) {
        try {
            Class.forName("com.ctux.ae2craftingtime.api.ExternalCpuProfiling");
            throw new IllegalStateException("The baseline unexpectedly contains the Forge API");
        } catch (ClassNotFoundException expected) {
            openRunningStatus(player, false);
            baselineStock = stock(plan.finalOutput().what());
        }
    }

    boolean verifyBaseline(Minecraft client, Consumer<String> capture) {
        if (phase == 0) {
            if (renderedSerial < 0 || renderedMillis != -1) return false;
            capture.accept("neoeco-native-status-baseline.png");
            heldLogic = null;
            phase++;
        }
        return poll(client, player -> {
            if (selected.isBusy()) return false;
            var output = plan.finalOutput().what();
            if (stock(output) != baselineStock + 1
                    || ProfilerBridge.stats(ProfilerBridge.key(ProfilerBridge.networkId(grid), output)).isPresent()) {
                throw new IllegalStateException("Baseline expected one real output with no Crafting Time sample");
            }
            return true;
        });
    }

    @Override
    boolean verifyAfterSample(Minecraft client, Consumer<String> capture) {
        if (action == null) {
            var features = ClientOptionsRuntime.current().features();
            originalRows = features.enabled(OptionFeature.STATUS_ROWS);
            features.setEnabled(OptionFeature.STATUS_ROWS, true);
            observeRows = true;
            schedule(client, player -> openRunningStatus(player, true));
            return false;
        }
        if (!action.isDone()) return false;
        action.join();
        if (phase == 0 && renderedMillis > 0) {
            if (!verifyRowViews(client, capture)) return false;
            phase++;
            schedule(client, player -> {
                setSuspended(selected, true);
                expectEstimate(false);
            });
        } else if (phase == 1 && renderedSerial >= 0 && renderedMillis == -1) {
            if (hasRowEstimate()) throw new IllegalStateException("Paused NeoEco job still shows per-item TTC");
            capture.accept("neoeco-native-status-paused.png");
            phase++;
            schedule(client, player -> {
                setSuspended(selected, false);
                expectEstimate(true);
            });
        } else if (phase == 2 && renderedMillis > 0) {
            verifyMixedRows(true);
            phase++;
            schedule(client, this::verifyCancellationAndDisable);
        } else if (phase == 3) {
            reconnect = true;
        } else if (phase == 4 && poll(client, this::restoreRunningStatus)) {
            phase++;
        } else if (phase == 5 && renderedSerial >= 0 && renderedMillis == -1) {
            if (hasRowEstimate() || renderedRows.containsKey(AEItemKey.of(Items.GOLD_INGOT))) {
                throw new IllegalStateException("NeoEco shows rows from the preceding job after restart");
            }
            capture.accept("neoeco-native-status-after-restart.png");
            phase++;
            schedule(client, this::submitExact);
        } else if (phase == 6 && poll(client, this::exactFinished)) {
            ClientOptionsRuntime.current().features().setEnabled(OptionFeature.STATUS_ROWS, originalRows);
            observeRows = false;
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
        renderedSerial = -1;
        renderedRows = Map.of();
    }

    private boolean hasRowEstimate() {
        return renderedRows.values().stream().anyMatch(cell -> cell.estimate() != null);
    }

    private boolean verifyRowViews(Minecraft client, Consumer<String> capture) {
        var features = ClientOptionsRuntime.current().features();
        var gold = AEItemKey.of(Items.GOLD_INGOT);
        switch (rowPhase) {
            case 0 -> {
                verifyMixedRows(true);
                capture.accept("neoeco-native-status-running.png");
                moveMouse(client, renderedRows.get(gold).x(), renderedRows.get(gold).y());
            }
            case 1 -> {
                if (!gold.equals(hoveredKey) || hoveredEstimate == null) return false;
                if (!hoveredEstimate.equals(renderedRows.get(gold).estimate())) {
                    throw new IllegalStateException("NeoEco hover TTC did not match the selected item");
                }
                capture.accept("neoeco-native-status-hover.png");
                widenGoldEstimate = true;
            }
            case 2 -> {
                var cell = renderedRows.get(gold);
                if (cell.estimate() != null || !gold.equals(hoveredKey) || hoveredEstimate == null
                        || !hoveredEstimate.contains(TimeEstimate.formatDuration(WIDE_MILLIS))) return false;
                if (cell.quantities().isEmpty() || !hasRowEstimate()) {
                    throw new IllegalStateException("An oversized NeoEco estimate hid native quantities or another estimate");
                }
                capture.accept("neoeco-native-status-crowded.png");
                features.setEnabled(OptionFeature.STATUS_ROWS, false);
            }
            case 3 -> {
                if (hasRowEstimate() || hoveredEstimate != null) return false;
                verifyMixedRows(false);
                capture.accept("neoeco-native-status-rows-off.png");
                moveMouse(client, 0, 0);
                omitRowPayload = true;
                widenGoldEstimate = false;
                features.setEnabled(OptionFeature.STATUS_ROWS, true);
            }
            case 4 -> {
                if (hasRowEstimate()) return false;
                verifyMixedRows(false);
                capture.accept("neoeco-native-status-no-row-payload.png");
                omitRowPayload = false;
            }
            default -> {
                if (!hasRowEstimate()) return false;
                verifyMixedRows(true);
                return true;
            }
        }
        rowPhase++;
        return false;
    }

    private static void moveMouse(Minecraft client, int x, int y) {
        var window = client.getWindow();
        double rawX = x * (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double rawY = y * (double) window.getScreenHeight() / window.getGuiScaledHeight();
        ((MouseHandlerAccessor) client.mouseHandler).ae2craftingtime_test_driver$move(window.getWindow(), rawX, rawY);
    }

    private void verifyMixedRows(boolean enabled) {
        var output = renderedRows.get(plan.finalOutput().what());
        var gold = requiredCell(Items.GOLD_INGOT);
        var diamond = requiredCell(Items.DIAMOND);
        var stored = requiredCell(Items.COBBLESTONE);
        if (output == null || output.quantities().isEmpty() || gold.quantities().isEmpty()
                || diamond.quantities().isEmpty() || stored.quantities().isEmpty()
                || diamond.estimate() != null || stored.estimate() != null
                || (output.estimate() != null) != enabled || (gold.estimate() != null) != enabled
                || enabled && output.estimate().equals(gold.estimate())) {
            throw new IllegalStateException("Unexpected NeoEco item rows (enabled=" + enabled + "): " + renderedRows);
        }
    }

    private static StatusCell requiredCell(net.minecraft.world.item.Item item) {
        var cell = renderedRows.get(AEItemKey.of(item));
        if (cell == null) throw new IllegalStateException("Missing NeoEco item row for " + item + ": " + renderedRows);
        return cell;
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

    private void openRunningStatus(ServerPlayer player, boolean profiled) {
        if (!(player.containerMenu instanceof CraftConfirmMenu menu)) {
            throw new IllegalStateException("NeoEco follow-up needs the learned AE2 plan");
        }
        grid = StatsRequestContext.current(player).grid();
        plan = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result();
        selected = (ECOCraftingCPU) cpu(player, grid);
        if (selected == null || plan == null || plan.simulation()) {
            throw new IllegalStateException("NeoEco status fixture needs a real CPU and follow-up plan");
        }
        locator = menu.getLocator();
        submit(player, profiled ? mixedPlan(player) : plan);
        if (profiled) expectEstimate(true);
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
        submit(player, plan);
    }

    private ICraftingPlan mixedPlan(ServerPlayer player) {
        var gold = AEItemKey.of(Items.GOLD_INGOT);
        var tick = player.serverLevel().getGameTime();
        var network = ProfilerBridge.networkId(grid);
        ProfilerBridge.start(network, this, gold, 1, tick - 120);
        ProfilerBridge.complete(network, this, gold, 1, tick);
        ProfilerBridge.flushCompletedSamples();
        if (ProfilerBridge.stats(ProfilerBridge.key(network, gold)).isEmpty()) {
            throw new IllegalStateException("NeoEco mixed job needs a learned gold timing");
        }
        // Add two externally supplied outputs to the real furnace plan.
        // The held native job schedules both while leaving its input stored.
        var emitted = new KeyCounter();
        emitted.addAll(plan.emittedItems());
        emitted.add(gold, 3);
        emitted.add(AEItemKey.of(Items.DIAMOND), 1);
        return new CraftingPlan(plan.finalOutput(), plan.bytes(), plan.simulation(), plan.multiplePaths(),
                plan.usedItems(), emitted, plan.missingItems(), plan.patternTimes());
    }

    private void submit(ServerPlayer player, ICraftingPlan submittedPlan) {
        var previousJobs = activeJobs();
        var result = grid.getCraftingService().submitJob(submittedPlan, null, selected, true,
                IActionSource.ofPlayer(player));
        selected = acceptedCpu(result, previousJobs);
        heldLogic = selected.getLogic();
    }

    private ECOCraftingCPU acceptedCpu(ICraftingSubmitResult result, Set<UUID> previousJobs) {
        if (!result.successful()) {
            throw new IllegalStateException("NeoEco follow-up job was not accepted: " + result);
        }
        // Standalone AE2 submissions return no requester link. NeoEco still
        // creates a separately registered CPU with a real crafting ID.
        var newCpus = activeCpus().filter(cpu -> !previousJobs.contains(getJobId(cpu))).toList();
        if (newCpus.size() != 1) throw new IllegalStateException("Expected one new active NeoEco CPU: " + newCpus);
        return newCpus.get(0);
    }

    private Set<UUID> activeJobs() {
        return activeCpus().map(NeoEcoStatusFixture::getJobId).collect(java.util.stream.Collectors.toSet());
    }

    private java.util.stream.Stream<ECOCraftingCPU> activeCpus() {
        return grid.getCraftingService().getCpus().stream().filter(ECOCraftingCPU.class::isInstance)
                .map(ECOCraftingCPU.class::cast).filter(ECOCraftingCPU::isBusy);
    }

    private ECOCraftingCPU activeCpu(UUID jobId) {
        return activeCpus().filter(cpu -> jobId.equals(getJobId(cpu))).findFirst().orElse(null);
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
        setSuspended(selected, true);
        savedJob = getJobId(selected);
        if (savedJob == null) throw new IllegalStateException("NeoEco job has no persisted identity");
    }

    private boolean restoreRunningStatus(ServerPlayer player) {
        var host = locator.locate(player, ITerminalHost.class);
        if (!(host instanceof IActionHost actionHost) || actionHost.getActionableNode() == null) return false;
        grid = actionHost.getActionableNode().getGrid();
        if (grid == null) return false;
        var restored = activeCpu(savedJob);
        if (restored == null) return false;
        if (selected == restored
                || !Boolean.TRUE.equals(callLogic(restored, "isJobSuspended", new Class<?>[0]))) {
            throw new IllegalStateException("Restart did not restore the same suspended NeoEco job on a new CPU object");
        }
        selected = restored;
        heldLogic = restored.getLogic();
        setSuspended(restored, false);
        expectEstimate(false);
        if (ProfilerBridge.stats(ProfilerBridge.key(ProfilerBridge.networkId(grid), plan.finalOutput().what()))
                .orElseThrow().sampleCount() != learnedSamples) {
            throw new IllegalStateException("Restart did not retain learned NeoEco history");
        }
        openStatus(player);
        return true;
    }

    private static UUID getJobId(ECOCraftingCPU cpu) {
        return (UUID) callLogic(cpu, "getProcessingJobId", new Class<?>[0]);
    }

    private static void setSuspended(ECOCraftingCPU cpu, boolean suspended) {
        callLogic(cpu, "setJobSuspended", new Class<?>[] { boolean.class }, suspended);
    }

    private static Object callLogic(ECOCraftingCPU cpu, String method, Class<?>[] parameters, Object... arguments) {
        // The driver compiles against the retained 20.3 API. In 20.5 these methods
        // are declared on the concrete execution class instead of the API base class.
        try {
            var logic = cpu.getLogic();
            return logic.getClass().getMethod(method, parameters).invoke(logic, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot call the NeoEco crafting logic: " + method, exception);
        }
    }

    private void submitExact(ServerPlayer player) {
        selected.cancelJob();
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
            var previousJobs = activeJobs();
            var result = (ICraftingSubmitResult) request.getMethod("submit", Function.class).invoke(exact, submit);
            selected = acceptedCpu(result, previousJobs);
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
        observeRows = false;
        omitRowPayload = false;
        widenGoldEstimate = false;
        renderedMillis = Long.MIN_VALUE;
        renderedSerial = -1;
    }
}
