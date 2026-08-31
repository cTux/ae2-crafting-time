package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.crafting.CraftAmountScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsRequestContext;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MouseHandlerAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CraftPlanScenario {
    private static final Duration STEP_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration START_TIMEOUT = Duration.ofMinutes(2);
    private final Minecraft minecraft;
    private final DriverOptions options;
    private final String driverFile;
    private final StableFrames<List<String>> stableRows = new StableFrames<>(3);
    private final LinkedHashMap<String, Boolean> checks = new LinkedHashMap<>();
    private final List<String> screenshots = new ArrayList<>();
    private final List<List<String>> orders = new ArrayList<>();
    private final List<List<String>> knownOrders = new ArrayList<>();
    private ScenarioState state = ScenarioState.STARTING;
    private long stateStarted = System.nanoTime();
    private FixtureMarker marker;
    private int sortStage;
    private long lastFrame = -1;
    private DriverResult.Failure failure;
    private String networkId;
    private CompletableFuture<String> cpuCheck;
    private CompletableFuture<Integer> sampleCheck;
    private CompletableFuture<NeoEcoFixture.Placement> fixturePlacement;
    private CompletableFuture<Void> fixtureSetup;

    public CraftPlanScenario(Minecraft minecraft, DriverOptions options, String driverFile) {
        this.minecraft = minecraft;
        this.options = options;
        this.driverFile = driverFile;
        DriverResult.requiredChecks(options.scenario()).forEach(key -> checks.put(key, false));
    }

    public void tick() {
        if (state == ScenarioState.FAILED || state == ScenarioState.QUIT_REQUESTED) {
            return;
        }
        if (elapsed().compareTo(state == ScenarioState.STARTING ? START_TIMEOUT : STEP_TIMEOUT) > 0) {
            fail("timeout", state.name(), currentScreen());
            return;
        }
        try {
            switch (state) {
                case STARTING -> start();
                case WORLD_READY -> openTerminal();
                case TERMINAL_OPEN -> selectTarget();
                case PLAN_OPEN -> openPlan();
                case PLAN_STABLE -> stabilizePlan();
                case NEOECO_CPU_SELECTED -> submitNeoEcoCraft();
                case NEOECO_CRAFT_SUBMITTED -> awaitNeoEcoSample();
                case NEOECO_SAMPLE_RECORDED -> selectTarget(ScenarioState.NEOECO_PLAN_OPEN);
                case NEOECO_PLAN_OPEN -> verifyNeoEcoTtc();
                case BASE_CHECKED -> cycleSorts();
                case SORTS_CHECKED -> checkTooltip();
                case TOOLTIP_CHECKED -> writePass();
                case RESULT_WRITTEN -> requestQuit();
                default -> {
                }
            }
        } catch (Exception error) {
            fail("exception", state.name(), ReportText.failure(error));
        }
    }

    public ScenarioState state() {
        return state;
    }

    public DriverResult.Failure failure() {
        return failure;
    }

    public long elapsedMillis() {
        return Duration.ofNanos(System.nanoTime() - stateStarted).toMillis();
    }

    private void start() throws IOException {
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || minecraft.getSingleplayerServer() == null || minecraft.getCurrentServer() != null) {
            return;
        }
        var world = minecraft.gameDirectory.toPath().resolve("saves").resolve(options.world()).normalize();
        var saves = minecraft.gameDirectory.toPath().resolve("saves").normalize();
        if (!world.getParent().equals(saves)) {
            throw new IllegalArgumentException("world escapes saves directory");
        }
        marker = FixtureMarker.read(world);
        if (!marker.disposableWorldId().equals(options.world())) {
            throw new IllegalArgumentException("fixture world ID mismatch");
        }
        if (options.scenario().equals("neoeco-cpu")) {
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            if (fixturePlacement == null) {
                fixturePlacement = server.submit(() -> NeoEcoFixture.place(
                        server.getPlayerList().getPlayer(playerId), marker));
            }
            if (!fixturePlacement.isDone()) {
                return;
            }
            if (fixtureSetup == null) {
                fixtureSetup = server.submit(() -> NeoEcoFixture.finish(
                        server.getPlayerList().getPlayer(playerId), fixturePlacement.join()));
            }
            if (!fixtureSetup.isDone()) {
                return;
            }
            fixtureSetup.join();
        }
        advance(ScenarioState.WORLD_READY);
    }

    private void openTerminal() {
        if (minecraft.screen != null) {
            return;
        }
        var position = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var hit = minecraft.hitResult instanceof BlockHitResult current && current.getBlockPos().equals(position)
                ? current
                : new BlockHitResult(Vec3.atCenterOf(position).add(0, 0, 0.5),
                        Direction.valueOf(marker.terminal().face()), position, false);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
        advance(ScenarioState.TERMINAL_OPEN);
    }

    private void selectTarget() {
        selectTarget(ScenarioState.PLAN_OPEN);
    }

    private void selectTarget(ScenarioState next) {
        if (!(minecraft.screen instanceof MEStorageScreen<?> screen)) {
            return;
        }
        var repo = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$repo();
        var entry = repo.getAllEntries().stream()
                .filter(candidate -> candidate.getWhat().getId().toString().equals(marker.outputId()))
                .filter(candidate -> candidate.isCraftable())
                .findFirst().orElse(null);
        if (entry == null) {
            return;
        }
        ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$click(entry, 2, ClickType.CLONE);
        advance(next);
    }

    private void openPlan() {
        if (minecraft.screen instanceof CraftAmountScreen amount) {
            ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next().onPress();
            return;
        }
        if (minecraft.screen instanceof CraftConfirmScreen) {
            advance(ScenarioState.PLAN_STABLE);
        }
    }

    private void stabilizePlan() throws IOException {
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.rows().stream().noneMatch(row -> row.outputId().equals(marker.outputId()))) {
            stableRows.reset();
            return;
        }
        if (!stable(snapshot)) {
            return;
        }
        if (options.scenario().equals("neoeco-cpu")) {
            selectNeoEcoCpu();
            return;
        }
        orders.add(ids(snapshot));
        knownOrders.add(knownIds(snapshot));
        checks.put("screen", snapshot.screen().equals(CraftConfirmScreen.class.getName()));
        checks.put("ttc-row", snapshot.rows().stream().filter(row -> row.outputId().equals(marker.outputId()))
                .flatMap(row -> row.description().stream()).anyMatch(text -> text.key().equals("text.ae2craftingtime.ttc")));
        checks.put("total-ttc", snapshot.text().stream()
                .anyMatch(text -> text.key().equals("text.ae2craftingtime.total_ttc")));
        checks.put("layout", !snapshot.badges().isEmpty() && LayoutValidator.validate(snapshot).isEmpty());
        screenshot("craft-plan.png");
        clickSort(snapshot);
        advance(ScenarioState.BASE_CHECKED);
    }

    private void selectNeoEcoCpu() {
        if (!(minecraft.screen instanceof CraftConfirmScreen)) {
            return;
        }
        if (cpuCheck == null) {
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            cpuCheck = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null || !(player.containerMenu instanceof appeng.menu.me.crafting.CraftConfirmMenu menu)) {
                    throw new IllegalStateException("server Crafting Plan menu is unavailable");
                }
                var context = StatsRequestContext.current(player);
                var accessor = (CraftConfirmMenuAccessor) menu;
                var attempts = context.grid().getCraftingService().getCpus().size() + 1;
                for (int i = 0; i < attempts; i++) {
                    var cpu = accessor.ae2craftingtime_test_driver$selectedCpu();
                    if (cpu != null
                            && cpu.getClass().getName().equals("cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU")) {
                        var id = ProfilerBridge.networkId(context.grid());
                        ProfilerBridge.clearStats(new ProfileKey(id, marker.outputId()));
                        return id;
                    }
                    menu.cycleSelectedCPU(true);
                }
                throw new IllegalStateException("NeoEco CPU is not available in the fixture network");
            });
        }
        if (!cpuCheck.isDone()) {
            return;
        }
        networkId = cpuCheck.join();
        checks.put("cpu-selected", true);
        advance(ScenarioState.NEOECO_CPU_SELECTED);
    }

    private void submitNeoEcoCraft() {
        if (minecraft.screen instanceof CraftConfirmScreen screen) {
            screen.getMenu().startJob();
            advance(ScenarioState.NEOECO_CRAFT_SUBMITTED);
        }
    }

    private void awaitNeoEcoSample() {
        if (sampleCheck == null) {
            var server = minecraft.getSingleplayerServer();
            sampleCheck = server.submit(() -> ProfilerBridge.stats(new ProfileKey(networkId, marker.outputId()))
                    .map(stats -> stats.sampleCount()).orElse(0));
        }
        if (!sampleCheck.isDone()) {
            return;
        }
        if (sampleCheck.join() == 0) {
            sampleCheck = null;
            return;
        }
        checks.put("profile-sample", true);
        stableRows.reset();
        advance(ScenarioState.NEOECO_SAMPLE_RECORDED);
    }

    private void verifyNeoEcoTtc() throws IOException {
        if (minecraft.screen instanceof CraftAmountScreen amount) {
            ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next().onPress();
            return;
        }
        var snapshot = UiObservationStore.latest();
        if (!(minecraft.screen instanceof CraftConfirmScreen) || snapshot == null || !stable(snapshot)) {
            return;
        }
        checks.put("ttc-after-sample", snapshot.rows().stream()
                .filter(row -> row.outputId().equals(marker.outputId()))
                .flatMap(row -> row.description().stream())
                .anyMatch(text -> text.key().equals("text.ae2craftingtime.ttc")));
        screenshot("neoeco-profiled-plan.png");
        writePass();
    }

    private void cycleSorts() {
        var snapshot = UiObservationStore.latest();
        if (!stable(snapshot)) {
            return;
        }
        orders.add(ids(snapshot));
        knownOrders.add(knownIds(snapshot));
        sortStage++;
        if (sortStage < 3) {
            clickSort(snapshot);
            stableRows.reset();
            return;
        }
        var ascending = knownOrders.get(2);
        var descending = knownOrders.get(3);
        checks.put("sort-cycle", SortObservation.valid(orders.get(1), orders.get(2), orders.get(3),
                ascending, descending));
        var target = snapshot.rows().stream().filter(row -> row.outputId().equals(marker.outputId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("target row is not visible"));
        moveMouse(target.cell().centerX(), target.cell().centerY());
        stableRows.reset();
        advance(ScenarioState.SORTS_CHECKED);
    }

    private void checkTooltip() throws IOException {
        var snapshot = UiObservationStore.latest();
        if (!stable(snapshot)) {
            return;
        }
        checks.put("tooltip", snapshot.tooltip().stream().anyMatch(text -> text.key().equals("text.ae2craftingtime.details_hint"))
                && snapshot.tooltip().stream().anyMatch(text -> text.key().equals("text.ae2craftingtime.reset_hint")));
        screenshot("craft-plan-tooltip.png");
        advance(ScenarioState.TOOLTIP_CHECKED);
    }

    private void writePass() throws IOException {
        var failed = checks.entrySet().stream().filter(entry -> !entry.getValue()).map(java.util.Map.Entry::getKey)
                .toList();
        if (!failed.isEmpty()) {
            fail("check_failed", "all checks true", String.join(",", failed));
            return;
        }
        AtomicResultWriter.write(options.output(), result(true, "PASS", null));
        advance(ScenarioState.RESULT_WRITTEN);
    }

    private void requestQuit() {
        if (!options.interactive()) {
            minecraft.stop();
        }
        advance(ScenarioState.QUIT_REQUESTED);
    }

    private void fail(String code, String expected, String observed) {
        failure = new DriverResult.Failure(state.name(), code, ReportText.safe(expected), ReportText.safe(observed));
        advance(ScenarioState.FAILED);
        try {
            AtomicResultWriter.write(options.output(), result(false, "FAIL", failure));
        } catch (IOException ignored) {
        }
        if (!options.interactive()) {
            minecraft.stop();
        }
    }

    private DriverResult result(boolean complete, String value, DriverResult.Failure resultFailure) {
        return new DriverResult(1, complete, driverFile, "1.20.1-forge", options.profile(), options.scenario(), value,
                checks, screenshots, resultFailure);
    }

    private boolean stable(UiSnapshot snapshot) {
        if (snapshot == null || snapshot.frame() == lastFrame) {
            return false;
        }
        lastFrame = snapshot.frame();
        return stableRows.observe(snapshot.rows().stream().map(row -> row.outputId() + ":"
                + row.description().stream().map(UiSnapshot.ObservedText::key).toList()).toList());
    }

    private static List<String> ids(UiSnapshot snapshot) {
        return snapshot.rows().stream().map(UiSnapshot.Row::outputId).toList();
    }

    private static List<String> knownIds(UiSnapshot snapshot) {
        return snapshot.rows().stream()
                .filter(row -> row.description().stream().anyMatch(text -> text.key().equals("text.ae2craftingtime.ttc")))
                .map(UiSnapshot.Row::outputId).toList();
    }

    private void clickSort(UiSnapshot snapshot) {
        var button = minecraft.screen.children().stream().filter(TtcSortButton.class::isInstance)
                .map(TtcSortButton.class::cast).findFirst()
                .orElseThrow(() -> new IllegalStateException("TTC sort button is missing"));
        button.onPress();
        moveMouse(snapshot.gui().x() - 8, snapshot.gui().y() - 8);
        stableRows.reset();
    }

    private void moveMouse(int guiX, int guiY) {
        var window = minecraft.getWindow();
        var rawX = guiX * (double) window.getScreenWidth() / window.getGuiScaledWidth();
        var rawY = guiY * (double) window.getScreenHeight() / window.getGuiScaledHeight();
        ((MouseHandlerAccessor) minecraft.mouseHandler).ae2craftingtime_test_driver$move(
                window.getWindow(), rawX, rawY);
    }

    private void screenshot(String name) throws IOException {
        Files.createDirectories(options.output());
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(options.output().resolve(name));
        }
        screenshots.add(name);
    }

    private void advance(ScenarioState next) {
        if (!ScenarioFlow.allows(state, next)) {
            throw new IllegalStateException("invalid scenario transition " + state + " -> " + next);
        }
        state = next;
        stateStarted = System.nanoTime();
    }

    private Duration elapsed() {
        return Duration.ofNanos(System.nanoTime() - stateStarted);
    }

    private String currentScreen() {
        return minecraft.screen == null ? "none" : minecraft.screen.getClass().getName();
    }

}
