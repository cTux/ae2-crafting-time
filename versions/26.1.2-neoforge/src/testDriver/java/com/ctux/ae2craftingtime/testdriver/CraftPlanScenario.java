package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.RepoSlot;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

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
    private final NoSpaceScenario noSpace;
    private final StandardAe2Scenario standard;
    private final NoProviderScenario noProvider;
    private final NoPowerScenario noPower;
    private final DriverOptions options;
    private final String driverFile;
    private final AddonCpuFixture<?> baseFixture;
    private final AddonCpuFixture<?> addonFixture;
    private final StableFrames<List<String>> stableRows = new StableFrames<>(3);
    private final LinkedHashMap<String, Boolean> checks = new LinkedHashMap<>();
    private final List<String> screenshots = new ArrayList<>();
    private CompletableFuture<Void> screenshotWrite = CompletableFuture.completedFuture(null);
    private final List<List<String>> orders = new ArrayList<>();
    private final List<List<String>> knownOrders = new ArrayList<>();
    private boolean pendingPass;
    private ScenarioState state = ScenarioState.STARTING;
    private long stateStarted;
    private FixtureMarker marker;
    private int sortStage;
    private long lastFrame = -1;
    private DriverResult.Failure failure;
    private String networkId;
    private String outputId;
    private CompletableFuture<String> cpuCheck;
    private CompletableFuture<Boolean> submitCheck;
    private CompletableFuture<Integer> sampleCheck;
    private final WirelessTerminalFixture wirelessFixture;
    private final RequesterFixture requesterFixture;
    private final Ae2NetworkAnalyserFixture networkAnalyserFixture;
    private CompletableFuture<ItemStack> wirelessSetup;
    private CompletableFuture<ItemStack> networkAnalyserSetup;
    private boolean wirelessHoverStarted;
    private boolean wirelessOpenRequested;
    private boolean treeHoverStarted;
    private final StatsInteraction treeStats = new StatsInteraction();

    public CraftPlanScenario(Minecraft minecraft, DriverOptions options, String driverFile) {
        DispatchObservation.watch(null, null);
        this.minecraft = minecraft;
        standard = StandardAe2Scenario.supports(options.scenario()) ? new StandardAe2Scenario(options.scenario()) : null;
        noSpace = NoSpaceScenario.SCENARIO.equals(options.scenario()) ? new NoSpaceScenario() : null;
        noPower = NoPowerScenario.SCENARIO.equals(options.scenario()) ? new NoPowerScenario() : null;
        noProvider = NoProviderScenario.SCENARIO.equals(options.scenario()) ? new NoProviderScenario() : null;
        this.options = options;
        this.driverFile = driverFile;
        baseFixture = standard == null && noSpace == null && noProvider == null && noPower == null ? DriverPlatform.baseFixture(options.scenario()) : null;
        addonFixture = AddonCpuFixture.create(options.scenario());
        wirelessFixture = WirelessTerminalFixture.create(options.scenario());
        requesterFixture = RequesterFixture.SCENARIO.equals(options.scenario()) ? RequesterFixture.create() : null;
        networkAnalyserFixture = Ae2NetworkAnalyserFixture.SCENARIO.equals(options.scenario())
                ? new Ae2NetworkAnalyserFixture() : null;
        DriverResult.requiredChecks(options.scenario()).forEach(key -> checks.put(key, false));
    }

    public void tick() {
        if (!screenshotWrite.isDone()) return;
        screenshotWrite.join();
        if (pendingPass) {
            try { writePass(); } catch (IOException error) { fail("result_write", "atomic result", ReportText.failure(error)); }
            return;
        }
        if (state == ScenarioState.FAILED && !options.interactive()) minecraft.stop();
        if (state == ScenarioState.FAILED || state == ScenarioState.QUIT_REQUESTED) {
            return;
        }
        if (elapsed().compareTo(state == ScenarioState.STARTING || standard != null || noSpace != null || noProvider != null || noPower != null ? START_TIMEOUT : STEP_TIMEOUT) > 0) {
            fail("timeout", state.name(), currentScreen());
            return;
        }
        try {
            // Reload futures can complete while the loading overlay still covers the rendered screen.
            if (minecraft.getOverlay() != null) return;
            switch (state) {
                case STARTING -> start();
                case WORLD_READY -> openTerminal();
                case TERMINAL_OPEN -> selectTarget();
                case PLAN_OPEN -> openPlan();
                case PLAN_STABLE -> stabilizePlan();
                case ADDON_CPU_SELECTED -> submitAddonCraft();
                case ADDON_CRAFT_SUBMITTED -> awaitAddonSample();
                case ADDON_SAMPLE_RECORDED -> selectTarget(ScenarioState.ADDON_PLAN_OPEN);
                case ADDON_PLAN_OPEN -> verifyAddonTtc();
                case BASE_CHECKED -> cycleSorts();
                case SORTS_CHECKED -> checkTooltip();
                case TOOLTIP_CHECKED -> writePass();
                case RESULT_WRITTEN -> requestQuit();
                default -> {
                }
            }
        } catch (Exception | LinkageError error) {
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
        if (!minecraft.getLanguageManager().getSelected().equals("en_us")) {
            minecraft.getLanguageManager().setSelected("en_us");
            minecraft.options.languageCode = "en_us";
            minecraft.reloadResourcePacks();
            return;
        }
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || minecraft.getSingleplayerServer() == null || minecraft.getCurrentServer() != null) {
            return;
        }
        var world = minecraft.gameDirectory.toPath().resolve("saves").resolve(options.world()).normalize();
        var saves = minecraft.gameDirectory.toPath().resolve("saves").normalize();
        if (!world.getParent().equals(saves)) {
            throw new IllegalArgumentException("world escapes saves directory");
        }
        if (!minecraft.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .toRealPath().equals(world.toRealPath())) {
            throw new IllegalArgumentException("running world is not the requested disposable fixture");
        }
        AdapterSmokePolicy.verify(DriverPlatform.TARGET, options.scenario(),
                minecraft.getLanguageManager().getSelected(),
                com.ctux.ae2craftingtime.integration.IntegrationMixinPlugin.snapshot());
        marker = FixtureMarker.read(world);
        if (!marker.disposableWorldId().equals(options.world())) {
            throw new IllegalArgumentException("fixture world ID mismatch");
        }
        if (standard != null || noSpace != null || noProvider != null || noPower != null) {
            advance(ScenarioState.WORLD_READY);
            return;
        }
        if (baseFixture != null && !baseFixture.setup(minecraft, marker)) {
            return;
        }
        if (addonFixture != null && !addonFixture.setup(minecraft, marker)) {
            return;
        }
        if (wirelessFixture != null && !setupWirelessTerminal()) {
            return;
        }
        if (networkAnalyserFixture != null && !setupNetworkAnalyser()) {
            return;
        }
        if (requesterFixture != null && !requesterFixture.setup(
                minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID()), marker)) {
            return;
        }
        outputId = addonFixture == null ? marker.outputId() : addonFixture.outputId(marker);
        advance(ScenarioState.WORLD_READY);
    }

    private void openTerminal() throws IOException {
        if (standard != null) {
            try {
                if (standard.tick(minecraft, marker, checks, this::screenshotUnchecked, this::moveMouse)) {
                    advance(ScenarioState.TERMINAL_OPEN);
                    writePass();
                }
            } catch (Exception error) { throw new IllegalStateException("standard AE2 flow failed", error); }
            return;
        }
        if (noPower != null) {
            if (noPower.tick(minecraft, marker, checks, this::screenshotUnchecked, this::moveMouse)) {
                advance(ScenarioState.TERMINAL_OPEN);
                writePass();
            }
            return;
        }
        if (noProvider != null) {
            if (noProvider.tick(minecraft, marker, checks, this::screenshotUnchecked, this::moveMouse)) {
                advance(ScenarioState.TERMINAL_OPEN);
                writePass();
            }
            return;
        }
        if (noSpace != null) {
            if (noSpace.tick(minecraft, marker, checks, this::screenshotUnchecked, this::moveMouse)) {
                advance(ScenarioState.TERMINAL_OPEN);
                writePass();
            }
            return;
        }
        if (networkAnalyserFixture != null) {
            if (minecraft.screen != null) {
                if (minecraft.screen.getClass().getName().equals(Ae2NetworkAnalyserFixture.SCREEN)) {
                    advance(ScenarioState.TERMINAL_OPEN);
                }
                return;
            }
            if (BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem()).toString()
                    .equals(Ae2NetworkAnalyserFixture.ITEM)) {
                minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
            }
            return;
        }
        if (requesterFixture != null) {
            if (minecraft.screen != null) {
                if (minecraft.screen.getClass().getName().equals(RequesterFixture.SCREEN)) {
                    advance(ScenarioState.TERMINAL_OPEN);
                }
                return;
            }
            var position = requesterFixture.position();
            minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
            return;
        }
        if (wirelessFixture != null) {
            if (minecraft.screen != null) {
                if (minecraft.screen.getClass().getName().equals(wirelessFixture.screenClass())) {
                    checks.put("screen", true);
                    advance(ScenarioState.TERMINAL_OPEN);
                }
                return;
            }
            if (!wirelessOpenRequested && BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem())
                    .toString().equals(wirelessFixture.itemId())) {
                minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
                wirelessOpenRequested = true;
            }
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        var position = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var face = Direction.valueOf(marker.terminal().face());
        var hit = minecraft.hitResult instanceof BlockHitResult current && current.getBlockPos().equals(position)
                ? current
                : new BlockHitResult(Vec3.atCenterOf(position).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5)),
                        face, position, false);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
        advance(ScenarioState.TERMINAL_OPEN);
    }

    private void selectTarget() {
        selectTarget(ScenarioState.PLAN_OPEN);
    }

    private void selectTarget(ScenarioState next) {
        if (networkAnalyserFixture != null) {
            verifyNetworkAnalyser();
            return;
        }
        if (requesterFixture != null) {
            verifyRequester();
            return;
        }
        if (!(minecraft.screen instanceof MEStorageScreen<?> screen)) {
            return;
        }
        var repo = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$repo();
        var entry = repo.getAllEntries().stream()
                .filter(candidate -> candidate.getWhat().getId().toString().equals(outputId))
                .filter(candidate -> candidate.isCraftable())
                .findFirst().orElse(null);
        if (entry == null) {
            return;
        }
        if (wirelessFixture != null) {
            var slot = screen.getMenu().slots.stream().filter(RepoSlot.class::isInstance).map(RepoSlot.class::cast)
                    .filter(candidate -> candidate.getEntry() == entry).findFirst().orElse(null);
            if (slot == null) {
                return;
            }
            if (!wirelessHoverStarted) {
                UiObservationStore.clearWirelessTooltip();
                moveMouse(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
                wirelessHoverStarted = true;
                return;
            }
            if (!wirelessTooltipReady(UiObservationStore.wirelessTooltip(), checks.containsKey("ttc-tooltip"))) {
                return;
            }
            if (checks.containsKey("ttc-tooltip")) {
                checks.put("ttc-tooltip", true);
            }
        }
        if (wirelessFixture != null) {
            screenshotUnchecked(wirelessFixture.screenshotPrefix() + "-terminal.png");
        }
        ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$click(entry, 2, ContainerInput.CLONE);
        advance(next);
    }

    private void verifyRequester() {
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || !snapshot.screen().equals(RequesterFixture.SCREEN)) {
            return;
        }
        checks.put("screen", true);
        checks.put("ttc-row", snapshot.text().stream().anyMatch(CraftPlanScenario::isResolvedTtc));
        checks.put("total-ttc", snapshot.text().stream()
                .anyMatch(text -> text.key().equals("text.ae2craftingtime.total_ttc")));
        checks.put("layout", !snapshot.badges().isEmpty() && LayoutValidator.validateBadges(snapshot).isEmpty());
        if (checks.values().stream().allMatch(Boolean::booleanValue)) {
            screenshotUnchecked("merequester-screen.png");
            try {
                writePass();
            } catch (IOException error) {
                throw new IllegalStateException("cannot write ME Requester result", error);
            }
        }
    }

    private void verifyNetworkAnalyser() {
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || !snapshot.screen().equals(Ae2NetworkAnalyserFixture.SCREEN)) {
            return;
        }
        checks.put("screen", snapshot.menu().equals(Ae2NetworkAnalyserFixture.MENU));
        checks.put("layout", snapshot.gui().x() >= 0 && snapshot.gui().y() >= 0
                && snapshot.gui().x() + snapshot.gui().width() <= snapshot.screenWidth()
                && snapshot.gui().y() + snapshot.gui().height() <= snapshot.screenHeight());
        if (checks.values().stream().allMatch(Boolean::booleanValue)) {
            screenshotUnchecked("ae2networkanalyser-screen.png");
            try {
                writePass();
            } catch (IOException error) {
                throw new IllegalStateException("cannot write AE2 Network Analyser result", error);
            }
        }
    }

    private void openPlan() {
        if (minecraft.screen instanceof CraftAmountScreen amount) {
            ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next().onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
            return;
        }
        if (minecraft.screen instanceof CraftConfirmScreen) {
            advance(ScenarioState.PLAN_STABLE);
        }
    }

    private void stabilizePlan() throws IOException {
        if (CraftingTreeScenario.SCENARIO.equals(options.scenario())) {
            verifyCraftingTree();
            return;
        }
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.rows().stream().noneMatch(row -> row.outputId().equals(outputId))) {
            stableRows.reset();
            return;
        }
        if (addonFixture != null) {
            lastFrame = snapshot.frame();
            selectAddonCpu();
            return;
        }
        if (!stable(snapshot)) {
            return;
        }
        if (!options.scenario().equals("craft-plan")) {
            if (wirelessFixture != null) {
                checks.put("plan-ttc", snapshot.rows().stream().filter(row -> row.outputId().equals(outputId))
                        .flatMap(row -> row.description().stream())
                        .anyMatch(CraftPlanScenario::isResolvedTtc));
                screenshot(wirelessFixture.screenshotPrefix() + "-plan.png");
                writePass();
                return;
            }
            selectAddonCpu();
            return;
        }
        orders.add(ids(snapshot));
        knownOrders.add(knownIds(snapshot));
        checks.put("screen", snapshot.screen().equals(CraftConfirmScreen.class.getName()));
        checks.put("ttc-row", snapshot.rows().stream().filter(row -> row.outputId().equals(outputId))
                .flatMap(row -> row.description().stream()).anyMatch(text -> text.key().equals("text.ae2craftingtime.ttc")));
        checks.put("layout", !snapshot.badges().isEmpty() && LayoutValidator.validate(snapshot).isEmpty());
        screenshot("craft-plan.png");
        clickSort(snapshot);
        advance(ScenarioState.BASE_CHECKED);
    }

    private void verifyCraftingTree() throws IOException {
        var snapshot = UiObservationStore.latest();
        if (minecraft.screen instanceof CraftConfirmScreen screen) {
            if (!stable(snapshot)) {
                return;
            }
            var button = screen.children().stream()
                    .filter(child -> child.getClass().getName().endsWith(".ae2ct.gui.ChangeButton"))
                    .map(net.minecraft.client.gui.components.Button.class::cast).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Crafting Tree toolbar button is missing"));
            button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
            stableRows.reset();
            return;
        }
        if (snapshot == null || !CraftingTreeScenario.isScreen(snapshot.screen()) || snapshot.frame() == lastFrame) {
            return;
        }
        lastFrame = snapshot.frame();
        var target = snapshot.rows().stream().filter(row -> row.outputId().equals(outputId)).findFirst();
        if (target.isEmpty() || snapshot.badges().isEmpty() || !stableRows.observe(ids(snapshot))) {
            return;
        }
        checks.put("screen", true);
        checks.put("node-ttc", true);
        checks.put("layout", LayoutValidator.validateBadges(snapshot).isEmpty());
        if (!treeHoverStarted) {
            screenshot("crafting-tree-screen.png");
            moveMouse(target.get().cell().centerX(), target.get().cell().centerY());
            treeHoverStarted = true;
            stableRows.reset();
            return;
        }
        if (!checks.get("tooltip") && !CraftingTreeScenario.tooltipReady(snapshot)) {
            return;
        }
        if (!checks.get("tooltip")) {
            checks.put("tooltip", true);
            screenshot("crafting-tree-tooltip.png");
        }
        boolean reset = checks.get("details");
        if (!treeStats.click(minecraft, snapshot, outputId, reset)) return;
        checks.put(reset ? "reset" : "details", true);
        screenshot(reset ? "crafting-tree-reset.png" : "crafting-tree-details.png");
        treeStats.next();
        if (reset) writePass();
    }

    private void selectAddonCpu() {
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
                var cpu = addonFixture.cpu(player, context.grid());
                if (cpu != null) {
                    accessor.ae2craftingtime_test_driver$selectedCpu(cpu);
                    var id = ProfilerBridge.networkId(context.grid());
                    ProfilerBridge.clearStats(new ProfileKey(id, outputId));
                    return id;
                }
                return null;
            });
        }
        if (!cpuCheck.isDone()) {
            return;
        }
        networkId = cpuCheck.join();
        if (networkId == null) {
            cpuCheck = null;
            return;
        }
        checks.put("cpu-selected", true);
        advance(ScenarioState.ADDON_CPU_SELECTED);
    }

    private void submitAddonCraft() {
        if (submitCheck == null) {
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            submitCheck = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null || !(player.containerMenu instanceof appeng.menu.me.crafting.CraftConfirmMenu menu)) {
                    throw new IllegalStateException("server Crafting Plan menu is unavailable");
                }
                var plan = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result();
                if (plan == null) {
                    throw new IllegalStateException("server Crafting Plan result is unavailable");
                }
                if (plan.simulation()) {
                    var missing = new ArrayList<String>();
                    for (var entry : plan.missingItems()) {
                        missing.add(entry.getKey().getId() + "=" + entry.getLongValue());
                    }
                    throw new IllegalStateException("server Crafting Plan is a simulation; missing=" + missing);
                }
                DispatchObservation.watch(networkId, outputId);
                addonFixture.startCraft(player, menu);
                return true;
            });
        }
        if (submitCheck.isDone()) {
            submitCheck.join();
            advance(ScenarioState.ADDON_CRAFT_SUBMITTED);
        }
    }

    private void awaitAddonSample() {
        if (sampleCheck == null) {
            var server = minecraft.getSingleplayerServer();
            sampleCheck = server.submit(() -> ProfilerBridge.stats(new ProfileKey(networkId, outputId))
                    .map(stats -> stats.sampleCount()).orElse(0));
        }
        if (!sampleCheck.isDone()) {
            return;
        }
        if (sampleCheck.join() == 0) {
            sampleCheck = null;
            return;
        }
        var observed = DispatchObservation.snapshot();
        if (observed.finishes() == 0) {
            sampleCheck = null;
            return;
        }
        addonFixture.verifyDispatch(observed);
        AdapterSmokePolicy.verifyCpu(DriverPlatform.TARGET, options.scenario(), observed);
        checks.put("job-accepted", true);
        checks.put("dispatch-amount", true);
        checks.put("returned-amount", true);
        checks.put("job-finished", true);
        checks.put("profile-sample", true);
        stableRows.reset();
        advance(ScenarioState.ADDON_SAMPLE_RECORDED);
    }

    private void verifyAddonTtc() throws IOException {
        if (minecraft.screen instanceof CraftAmountScreen amount) {
            ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next().onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
            return;
        }
        var snapshot = UiObservationStore.latest();
        if (!(minecraft.screen instanceof CraftConfirmScreen) || snapshot == null || snapshot.frame() == lastFrame) {
            return;
        }
        lastFrame = snapshot.frame();
        var resolved = snapshot.rows().stream()
                .filter(row -> row.outputId().equals(outputId))
                .flatMap(row -> row.description().stream())
                .anyMatch(CraftPlanScenario::isResolvedTtc);
        if (!resolved) {
            return;
        }
        checks.put("ttc-after-sample", true);
        screenshot(options.scenario().replace("-cpu", "") + "-profiled-plan.png");
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
        screenshotUnchecked("craft-plan-sort-" + sortStage + ".png");
        if (sortStage < 3) {
            clickSort(snapshot);
            stableRows.reset();
            return;
        }
        var ascending = knownOrders.get(2);
        var descending = knownOrders.get(3);
        checks.put("sort-cycle", SortObservation.valid(orders.get(1), orders.get(2), orders.get(3),
                ascending, descending));
        var target = snapshot.rows().stream().filter(row -> row.outputId().equals(outputId)).findFirst()
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
        checks.put("total-ttc", snapshot.text().stream()
                .anyMatch(text -> text.key().equals("text.ae2craftingtime.total_ttc")));
        screenshot("craft-plan-tooltip.png");
        advance(ScenarioState.TOOLTIP_CHECKED);
    }

    private void writePass() throws IOException {
        AdapterSmokePolicy.verify(DriverPlatform.TARGET, options.scenario(),
                minecraft.getLanguageManager().getSelected(),
                com.ctux.ae2craftingtime.integration.IntegrationMixinPlugin.snapshot());
        if (!screenshotWrite.isDone()) { pendingPass = true; return; }
        screenshotWrite.join();
        pendingPass = false;
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
        if (standard != null) standard.releaseKeys();
        treeStats.releaseKeys();
        if (!options.interactive()) {
            minecraft.stop();
        }
        advance(ScenarioState.QUIT_REQUESTED);
    }

    private void fail(String code, String expected, String observed) {
        if (standard != null) standard.releaseKeys();
        treeStats.releaseKeys();
        failure = new DriverResult.Failure(state.name(), code, ReportText.safe(expected), ReportText.safe(observed));
        advance(ScenarioState.FAILED);
        try {
            screenshot("failure.png");
        } catch (IOException ignored) {
        }
        try {
            AtomicResultWriter.write(options.output(), result(false, "FAIL", failure));
        } catch (IOException ignored) {
        }
    }

    boolean evidenceReady() { return screenshotWrite.isDone(); }

    private DriverResult result(boolean complete, String value, DriverResult.Failure resultFailure) {
        return new DriverResult(1, complete, driverFile, DriverPlatform.TARGET, options.profile(), options.scenario(), value,
                minecraft.getLanguageManager().getSelected(),
                com.ctux.ae2craftingtime.integration.IntegrationMixinPlugin.snapshot(),
                addonFixture == null ? null : DispatchObservation.snapshot(), checks, screenshots, resultFailure);
    }

    static boolean wirelessTooltipReady(List<UiSnapshot.ObservedText> tooltip, boolean requireTtc) {
        return !tooltip.isEmpty() && (!requireTtc || tooltip.stream().anyMatch(CraftPlanScenario::isResolvedTtc));
    }

    static boolean renderedPlan(UiSnapshot snapshot) {
        return snapshot != null && !snapshot.badges().isEmpty() && snapshot.text().stream()
                .anyMatch(text -> text.key().equals("text.ae2craftingtime.total_ttc"));
    }

    private boolean stable(UiSnapshot snapshot) {
        if (!renderedPlan(snapshot)) {
            stableRows.reset();
            return false;
        }
        if (snapshot.frame() == lastFrame) {
            return false;
        }
        lastFrame = snapshot.frame();
        return stableRows.observe(snapshot.rows().stream().map(UiSnapshot.Row::outputId).toList());
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
        button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
        moveMouse(snapshot.gui().x() - 8, snapshot.gui().y() - 8);
        stableRows.reset();
    }

    private void moveMouse(int guiX, int guiY) {
        var window = minecraft.getWindow();
        var rawX = guiX * (double) window.getScreenWidth() / window.getGuiScaledWidth();
        var rawY = guiY * (double) window.getScreenHeight() / window.getGuiScaledHeight();
        ((MouseHandlerAccessor) minecraft.mouseHandler).ae2craftingtime_test_driver$move(
                window.handle(), rawX, rawY);
    }

    private void screenshot(String name) throws IOException {
        Files.createDirectories(options.output());
        if (screenshots.contains(name)) return;
        screenshotWrite = DriverScreenshots.capture(minecraft, options.output().resolve(name));
        Files.writeString(options.output().resolve(name.replace(".png", ".json")),
                new com.google.gson.Gson().toJson(UiObservationStore.latest()));
        screenshots.add(name);
    }

    private void screenshotUnchecked(String name) {
        try {
            screenshot(name);
        } catch (IOException error) {
            throw new IllegalStateException("cannot save test-driver screenshot", error);
        }
    }

    private boolean setupWirelessTerminal() {
        if (wirelessSetup == null) {
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            wirelessSetup = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    throw new IllegalStateException("fixture player is unavailable");
                }
                return wirelessFixture.setup(player, marker);
            });
        }
        if (!wirelessSetup.isDone()) {
            return false;
        }
        var stack = wirelessSetup.join();
        if (stack == null) {
            wirelessSetup = null;
            return false;
        }
        minecraft.player.getInventory().setSelectedSlot(0);
        minecraft.player.getInventory().setItem(0, stack.copy());
        return true;
    }

    private boolean setupNetworkAnalyser() {
        if (networkAnalyserSetup == null) {
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            networkAnalyserSetup = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    throw new IllegalStateException("fixture player is unavailable");
                }
                return networkAnalyserFixture.setup(player);
            });
        }
        if (!networkAnalyserSetup.isDone()) {
            return false;
        }
        var stack = networkAnalyserSetup.join();
        minecraft.player.getInventory().setSelectedSlot(0);
        minecraft.player.getInventory().setItem(0, stack.copy());
        return true;
    }

    static boolean isResolvedTtc(UiSnapshot.ObservedText text) {
        return text.key().equals("text.ae2craftingtime.ttc")
                && !text.arguments().contains("text.ae2craftingtime.collecting_data");
    }

    private void advance(ScenarioState next) {
        if (!ScenarioFlow.allows(state, next)) {
            throw new IllegalStateException("invalid scenario transition " + state + " -> " + next);
        }
        state = next;
        stateStarted = System.nanoTime();
    }

    private Duration elapsed() {
        var now = System.nanoTime();
        stateStarted = startTime(stateStarted, now);
        return Duration.ofNanos(now - stateStarted);
    }

    static long startTime(long started, long now) {
        return started == 0 ? now : started;
    }

    private String currentScreen() {
        if (standard != null) return standard.checkpoint() + " screen=" + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
        if (minecraft.screen == null) {
            return "none";
        }
        var screen = minecraft.screen.getClass().getName();
        var snapshot = UiObservationStore.latest();
        return state == ScenarioState.PLAN_STABLE && snapshot != null ? screen + " rows=" + ids(snapshot) : screen;
    }

}
