package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.crafting.CraftAmountScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.me.crafting.CraftingStatusScreen;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient;
import com.ctux.ae2craftingtime.mc1201.ProviderHighlightShapes;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** Bounded, real plan -> dispatch -> vanilla processing -> completed output flow. */
final class StandardAe2Scenario {
    static final Map<String, List<String>> CHECKS = Map.ofEntries(
            Map.entry("standard-plan-controls", List.of("plan", "plan-sort", "missing-first", "plan-tooltip",
                    "plan-details", "plan-reset", "total-ttc", "layout", "item-resolution")),
            Map.entry("standard-status-controls", List.of("submitted", "status", "status-sort", "status-tooltip", "status-details", "status-reset", "header", "layout")),
            Map.entry("waiting-status", List.of("submitted", "waiting", "first-dispatch", "recovered", "layout")),
            Map.entry("running-status", List.of("submitted", "running", "progress", "header", "layout")),
            Map.entry("cpu-list-total-ttc", CpuListTtcScenario.CHECKS),
            Map.entry("delayed-status", List.of("submitted", "delayed", "row", "style", "tooltip", "layout", "recovered",
                    "plate-recovered", "final-plate", "completed", "output", "profile-sample", "plate-cleared")),
            Map.entry("craft-lifecycle", List.of("plan", "submitted", "status", "profile-sample", "total-cleared", "completed", "output",
                    "plan-no-data", "plan-partial", "accuracy-full", "accuracy-partial", "details-chat")));
    private enum Stage { PREPARE, TERMINAL, AMOUNT, PLAN_SORT, PLAN_TOOLTIP, PLAN_DETAILS, PLAN_RESET,
        SUBMIT, OPEN_STATUS, ACTIVE, STATUS_SORT, STATUS_TOOLTIP, STATUS_DETAILS, STATUS_RESET,
        RESTORE, DELAYED, PUMP, FINISHED, REOPEN, EMPTY, WORLD_POSITION, WORLD_HIGHLIGHT, WORLD_RELEASE, WORLD_FINISHED,
        GALLERY_PARTIAL_PLAN, GALLERY_PROFILED_PLAN, GALLERY_DETAILS, GALLERY_CHAT, GALLERY_NEXT_JOB,
        CPU_LIST_REOPEN, CPU_LIST_REOPENED }
    private final String leaf;
    private final StandardCraftFixture fixture = new StandardCraftFixture();
    private final CpuListTtcScenario cpuList;
    private final boolean connectedDedicated;
    StandardAe2Scenario(String leaf, String world, java.nio.file.Path output, boolean connectedDedicated) {
        if (!CHECKS.containsKey(leaf)) throw new IllegalArgumentException("Unknown standard leaf: " + leaf);
        this.leaf = leaf;
        this.connectedDedicated = connectedDedicated;
        cpuList = leaf.equals("cpu-list-total-ttc") ? new CpuListTtcScenario(fixture, world, output, connectedDedicated) : null;
        if (cpuList != null && cpuList.resumed()) phase = Stage.ACTIVE;
    }
    static boolean supports(String scenario) { return CHECKS.containsKey(scenario); }
    private final StableFrames<Object> frames = new StableFrames<>(8);
    private CompletableFuture<Boolean> operation;
    private Stage phase = Stage.PREPARE;
    private Stage reportedPhase;
    private String reportedCheckpoint;
    private int sort;
    private boolean partialJob;
    private boolean reviewJob;
    private boolean chatCleared;
    private long lastFrame = -1;
    private volatile boolean dispatched;
    private volatile boolean progressed;
    private volatile boolean finalOutputReady;
    private boolean stonePlateObserved;
    private final StableFrames<Boolean> worldFrames = new StableFrames<>(8);
    private final StatsInteraction stats = new StatsInteraction();

    String checkpoint() { return "phase=" + phase + " fixture=" + fixture.checkpoint
            + (cpuList == null ? "" : " " + cpuList.checkpoint()); }

    boolean reconnectRequested() { return cpuList != null && cpuList.reconnectRequested(); }
    void reconnected() { cpuList.reconnected(); }

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) throws Exception {
        var currentCheckpoint = checkpoint();
        if (reportedPhase != phase || !currentCheckpoint.equals(reportedCheckpoint)) {
            System.out.println("AE2CT standard checkpoint " + java.time.Instant.now() + " " + currentCheckpoint);
            reportedPhase = phase;
            reportedCheckpoint = currentCheckpoint;
        }
        // Menu-free close/reopen and reconnect transitions are owned by this state machine.
        if (phase == Stage.ACTIVE && cpuList != null) {
            var complete = cpuList.tick(minecraft, marker, checks, screenshot, moveMouse);
            return complete;
        }
        if (phase == Stage.PREPARE) {
            fixture.cpuListScenario = leaf.equals("cpu-list-total-ttc");
            fixture.holdFinalOutput = leaf.equals("delayed-status");
            fixture.missingPlanInput = leaf.equals("standard-plan-controls");
            fixture.unprofiledPlan = leaf.equals("craft-lifecycle");
            if (connectedDedicated && fixture.cpuListScenario) {
                var state = CpuListTtcControl.state();
                if (!state.ready()) return false;
                fixture.bindTerminal(new net.minecraft.core.BlockPos(state.x(), state.y(), state.z()));
                phase = Stage.TERMINAL;
                return false;
            }
            if (server(minecraft, player -> fixture.prepare(player, marker)
                    && (!fixture.cpuListScenario || fixture.prepareCpuListJobs(player)))) {
                if (leaf.equals("standard-plan-controls")) {
                    mark(checks, "item-resolution", ProviderHighlightShapes.resolveItem(null).isEmpty()
                            && ProviderHighlightShapes.resolveItem("not an id!!").isEmpty()
                            && ProviderHighlightShapes.resolveItem("minecraft:not_a_real_item_xyz").isEmpty()
                            && ProviderHighlightShapes.resolveItem("minecraft:stone").is(net.minecraft.world.item.Items.STONE));
                }
                phase = Stage.values()[phase.ordinal() + 1];
            }
            return false;
        }
        if (phase == Stage.TERMINAL) {
            if (minecraft.screen == null) {
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(fixture.terminal).add(0, 0, -0.5), Direction.NORTH, fixture.terminal, false));
            } else if (minecraft.screen instanceof MEStorageScreen<?> screen && fixture.cpuListScenario) {
                var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                phase = Stage.ACTIVE;
            } else if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var entry = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$repo().getAllEntries().stream()
                        .filter(row -> row.getWhat().getId().toString().equals("minecraft:smooth_stone") && row.isCraftable())
                        .findFirst().orElse(null);
                if (entry != null && frames.observe(screen.getMenu().containerId)) {
                    DriverPlatform.cloneEntry(screen, entry);
                    frames.reset();
                }
            } else if (minecraft.screen instanceof CraftAmountScreen) {
                phase = Stage.AMOUNT;
            }
            return false;
        }
        if (phase == Stage.AMOUNT) {
            if (minecraft.screen instanceof CraftAmountScreen amount) {
                var button = ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            } else if (minecraft.screen instanceof CraftConfirmScreen) phase = Stage.values()[phase.ordinal() + 1];
            return false;
        }
        if (phase == Stage.OPEN_STATUS || phase == Stage.REOPEN) {
            if (phase == Stage.OPEN_STATUS) {
                if (!server(minecraft, player -> fixture.cpu(player).getCluster().isBusy())) return false;
                mark(checks, "submitted", true);
            }
            if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                phase = Stage.values()[phase.ordinal() + 1];
            }

            return false;
        }
        if (phase == Stage.WORLD_POSITION) {
            if (server(minecraft, player -> { fixture.viewFinalProvider(player); return true; })) {
                phase = Stage.WORLD_HIGHLIGHT;
            }
            return false;
        }
        if (phase == Stage.WORLD_HIGHLIGHT) {
            minecraft.player.setYRot(9.462f);
            minecraft.player.setXRot(2);
            if (minecraft.screen != null || !worldFrames.observe(hasPlate("minecraft:smooth_stone", 8))) return false;
            if (!hasPlate("minecraft:smooth_stone", 8)) return false;
            screenshot.accept("delayed-world-highlight.png");
            worldFrames.reset();
            phase = Stage.WORLD_RELEASE;
            return false;
        }
        if (phase == Stage.WORLD_RELEASE) {
            if (server(minecraft, player -> {
                if (!fixture.finalOutputReady(player) || !fixture.cpu(player).getCluster().isBusy()) {
                    throw new IllegalStateException("Final delayed output was not held in an active craft");
                }
                fixture.holdFinalOutput = false;
                fixture.pump(player, false);
                return true;
            })) phase = Stage.WORLD_FINISHED;
            return false;
        }
        if (phase == Stage.WORLD_FINISHED) {
            if (!server(minecraft, player -> fixture.pump(player, false) == 1
                    && !fixture.cpu(player).getCluster().isBusy() && fixture.observedNewSamples(player))) return false;
            boolean cleared = !hasPlate("minecraft:stone", 4) && !hasPlate("minecraft:smooth_stone", 8);
            if (!worldFrames.observe(cleared) || !cleared) return false;
            mark(checks, "completed", true);
            mark(checks, "output", true);
            mark(checks, "profile-sample", true);
            mark(checks, "plate-cleared", true);
            screenshot.accept("delayed-world-finished.png");
            minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(fixture.terminal.east(8).north()).add(0, 0, -0.5),
                            Direction.NORTH, fixture.terminal.east(8).north(), false));
            phase = Stage.REOPEN;
            return false;
        }
        if (phase == Stage.GALLERY_CHAT) {
            if (!(minecraft.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                    || !worldFrames.observe(true)) return false;
            screenshot.accept(partialJob ? "job-accuracy-partial.png" : "job-accuracy-full.png");
            mark(checks, partialJob ? "accuracy-partial" : "accuracy-full", true);
            if (!partialJob) {
                screenshot.accept("details-chat.png");
                mark(checks, "details-chat", true);
            }
            minecraft.setScreen(null);
            worldFrames.reset();
            if (partialJob) return true;
            phase = Stage.GALLERY_NEXT_JOB;
            return false;
        }
        if (phase == Stage.GALLERY_NEXT_JOB) {
            if (server(minecraft, player -> { fixture.preparePartialJob(player); return true; })) {
                partialJob = true;
                reviewJob = false;
                phase = Stage.TERMINAL;
            }
            return false;
        }
        if (phase == Stage.CPU_LIST_REOPEN) {
            if (minecraft.screen == null) {
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(fixture.terminal).add(0, 0, -0.5), Direction.NORTH,
                                fixture.terminal, false));
            } else if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                phase = Stage.CPU_LIST_REOPENED;
            }
            return false;
        }
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.frame() == lastFrame) return false;
        lastFrame = snapshot.frame();
        if (phase == Stage.PLAN_SORT && leaf.equals("standard-plan-controls") && !planEstimatesReady(snapshot.rows())) {
            frames.reset();
            return false;
        }
        var planDescriptions = leaf.equals("craft-lifecycle") && minecraft.screen instanceof CraftConfirmScreen
                ? snapshot.rows().stream().map(UiSnapshot.Row::description).toList() : List.of();
        if (!frames.observe(List.of(phase, sort, CaptureEvidence.readiness(snapshot), planDescriptions))) return false;
        boolean plan = phase.ordinal() < Stage.OPEN_STATUS.ordinal();
        String prefix = plan ? "plan" : "status";
        if (phase == Stage.PLAN_SORT && !leaf.equals("standard-plan-controls")) {
            if (!(minecraft.screen instanceof CraftConfirmScreen) || snapshot.rows().stream()
                    .filter(row -> row.craftAmount() > 0).count() < 2) return false;
            if (leaf.equals("craft-lifecycle")) {
                if (reviewJob) {
                    phase = Stage.GALLERY_DETAILS;
                    return false;
                }
                if (!galleryPlanReady(snapshot.rows(), partialJob ? 1 : 0)) return false;
                moveMouse.accept(0, 0);
                if (!snapshot.tooltip().isEmpty()) return false;
                if (partialJob) {
                    screenshot.accept("plan-partial-job.png");
                    phase = Stage.SUBMIT;
                } else {
                    screenshot.accept("plan-no-data.png");
                    mark(checks, "plan-no-data", true);
                    phase = Stage.GALLERY_PARTIAL_PLAN;
                }
            } else {
                mark(checks, "plan", true);
                phase = Stage.SUBMIT;
            }
        } else if (phase == Stage.GALLERY_PARTIAL_PLAN) {
            if (!server(minecraft, player -> { fixture.seed(player, net.minecraft.world.item.Items.STONE); return true; })) return false;
            phase = Stage.GALLERY_PROFILED_PLAN;
        } else if (phase == Stage.GALLERY_PROFILED_PLAN) {
            if (!Boolean.TRUE.equals(checks.get("plan-partial"))) {
                if (!galleryPlanReady(snapshot.rows(), 1) || !snapshot.tooltip().isEmpty()) return false;
                screenshot.accept("plan-partial.png");
                mark(checks, "plan-partial", true);
            }
            if (!Boolean.TRUE.equals(checks.get("plan"))) {
                if (!server(minecraft, player -> { fixture.seed(player, net.minecraft.world.item.Items.SMOOTH_STONE); return true; })) return false;
                mark(checks, "plan", true);
                return false;
            }
            if (!planEstimatesReady(snapshot.rows())) return false;
            screenshot.accept("plan-default.png");
            phase = Stage.SUBMIT;
        } else if (phase == Stage.GALLERY_DETAILS) {
            if (!server(minecraft, player -> {
                var key = ProfilerBridge.key(ProfilerBridge.networkId(fixture.cpu(player).getMainNode().getGrid()),
                        appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.SMOOTH_STONE));
                var accuracy = ProfilerBridge.accuracy(key).orElseThrow(() -> new IllegalStateException("Completed job has no accuracy sample"));
                if (!galleryAccuracyReady(accuracy, partialJob)) throw new IllegalStateException("Unexpected real job coverage: " + accuracy);
                return true;
            })) return false;
            if (!chatCleared) {
                minecraft.gui.getChat().clearMessages(false);
                chatCleared = true;
                stats.next();
            }
            if (!stats.click(minecraft, snapshot, "minecraft:smooth_stone", false)) return false;
            var messages = ((com.ctux.ae2craftingtime.testdriver.mixin.ChatComponentAccessor) minecraft.gui.getChat())
                    .ae2craftingtime_test_driver$messages();
            if (messages.stream().noneMatch(message -> message.content().getString()
                    .endsWith("coverage " + (partialJob ? "1/2" : "2/2")))) {
                throw new IllegalStateException("Stats chat did not report the completed job coverage");
            }
            minecraft.player.closeContainer();
            DriverPlatform.openChat(minecraft);
            chatCleared = false;
            phase = Stage.GALLERY_CHAT;
        } else if (phase == Stage.PLAN_SORT || phase == Stage.STATUS_SORT) {
            var rows = snapshot.rows().stream().filter(row -> row.craftAmount() > 0).map(UiSnapshot.Row::outputId).toList();
            if (!rows.containsAll(List.of("minecraft:stone", "minecraft:smooth_stone"))) return false;
            if (plan && !missingFirst(snapshot.rows())) {
                throw new IllegalStateException("Crafting Plan missing rows are not first: " + snapshot.rows());
            }
            if (sort == 0) {
                mark(checks, prefix, true);
                if (plan && !snapshot.text().stream().anyMatch(t -> t.key().equals("text.ae2craftingtime.total_ttc"))) return false;
                if (plan && (snapshot.badges().isEmpty() || !LayoutValidator.validateBadges(snapshot).isEmpty())) {
                    throw new IllegalStateException("plan badge layout: " + LayoutValidator.validateBadges(snapshot));
                }
                if (plan) {
                    mark(checks, "total-ttc", true);
                    mark(checks, "layout", true);
                }
                moveMouse.accept(snapshot.gui().x() - 8, snapshot.gui().y() - 8);
                screenshot.accept(prefix + "-default.png");
            }
            if (sort != 1) {
                var expected = plan && sort == 2 ? List.of("minecraft:smooth_stone", "minecraft:stone")
                        : List.of("minecraft:stone", "minecraft:smooth_stone");
                if (!rows.subList(0, 2).equals(expected)) {
                    throw new IllegalStateException(prefix + " sort " + sort + " row order is " + rows + ", expected " + expected);
                }
            }
            if (sort > 0) screenshot.accept(prefix + "-sort-" + sort + ".png");
            if (sort++ < 3) {
                AbstractWidget button = minecraft.screen.children().stream().filter(TtcSortButton.class::isInstance)
                        .map(TtcSortButton.class::cast).findFirst().orElseThrow();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            } else {
                mark(checks, prefix + "-sort", true);
                if (plan) mark(checks, "missing-first", true);
                sort = 0;
                phase = Stage.values()[phase.ordinal() + 1];
            }
        } else if (phase == Stage.PLAN_TOOLTIP || phase == Stage.STATUS_TOOLTIP) {
            var row = snapshot.rows().stream().filter(r -> r.outputId().equals(statsOutput())).findFirst().orElseThrow();
            moveMouse.accept(row.cell().centerX(), row.cell().centerY());
            if (snapshot.tooltip().stream().noneMatch(t -> t.key().equals("text.ae2craftingtime.details_hint"))) return false;
            mark(checks, prefix + "-tooltip", true);
            screenshot.accept(prefix + "-tooltip.png");
            phase = Stage.values()[phase.ordinal() + 1];
        } else if (phase == Stage.PLAN_DETAILS || phase == Stage.PLAN_RESET || phase == Stage.STATUS_DETAILS || phase == Stage.STATUS_RESET) {
            boolean reset = phase == Stage.PLAN_RESET || phase == Stage.STATUS_RESET;
            if (!stats.click(minecraft, snapshot, statsOutput(), reset)) return false;
            if (reset && !server(minecraft, player -> ProfilerBridge.stats(ProfilerBridge.key(
                    ProfilerBridge.networkId(fixture.cpu(player).getMainNode().getGrid()),
                    appeng.api.stacks.AEItemKey.of(plan ? net.minecraft.world.item.Items.STONE : net.minecraft.world.item.Items.SMOOTH_STONE))).isEmpty())) return false;
            mark(checks, prefix + (reset ? "-reset" : "-details"), true);
            screenshot.accept(prefix + (reset ? "-reset.png" : "-details.png"));
            phase = Stage.values()[phase.ordinal() + 1];
            stats.next();
            if (reset && plan) return true;
        } else if (phase == Stage.SUBMIT) {
            moveMouse.accept(0, 0);
            if (!leaf.equals("craft-lifecycle") && !server(minecraft, player -> { fixture.seed(player); return true; })) return false;
            var start = minecraft.screen.children().stream().filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast).filter(w -> w.active && w.getMessage().getString().equals("Start"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Crafting Plan Start button is missing"));
            DriverPlatform.click(minecraft, start.getX() + 4, start.getY() + 4);
            phase = Stage.values()[phase.ordinal() + 1];
        } else if (phase == Stage.ACTIVE) {
            if (!(minecraft.screen instanceof CraftingStatusScreen)) return false;
            var waiting = rowText(snapshot, "minecraft:smooth_stone", "text.ae2craftingtime.waiting");
            var running = rowText(snapshot, "minecraft:stone", "text.ae2craftingtime.ttc");
            if (leaf.equals("standard-status-controls")) { phase = Stage.STATUS_SORT; return false; }
            if (leaf.equals("craft-lifecycle")) {
                mark(checks, "status", true);
                screenshot.accept(partialJob ? "status-partial-job.png" : "status-default.png");
                phase = Stage.PUMP;
            } else if (leaf.equals("delayed-status")) { phase = Stage.DELAYED; }
            else if (waiting != null && running != null) {
                validateLayout(snapshot);
                mark(checks, "waiting", true);
                mark(checks, "running", true);
                mark(checks, "layout", true);
                screenshot.accept("status-waiting-running.png");
                phase = Stage.PUMP;
            }
        } else if (phase == Stage.RESTORE) {
            moveMouse.accept(0, 0);
            if (server(minecraft, player -> { fixture.seed(player); return true; })) phase = Stage.PUMP;
        } else if (phase == Stage.DELAYED) {
            stonePlateObserved |= hasPlate("minecraft:stone", 4);
            var warning = rowText(snapshot, "minecraft:stone", "text.ae2craftingtime.ttc_delayed");
            if (warning == null) return false;
            if (!warning.bold() || !Integer.valueOf(0xFF5555).equals(warning.color())) {
                throw new IllegalStateException("DELAYED must be bold red on the active stone row");
            }
            validateLayout(snapshot);
            if (!checks.get("delayed")) {
                screenshot.accept("status-delayed.png");
                mark(checks, "delayed", true);
            }
            mark(checks, "row", true);
            mark(checks, "style", true);
            mark(checks, "layout", true);
            if (!checks.get("tooltip")) {
                var row = snapshot.rows().stream().filter(r -> r.outputId().equals("minecraft:stone")).findFirst().orElseThrow();
                moveMouse.accept(row.cell().centerX(), row.cell().centerY());
                if (!delayedTooltip(snapshot)) return false;
                screenshot.accept("delayed-tooltip.png");
                mark(checks, "tooltip", true);
            }
            if (!stonePlateObserved) return false;
            moveMouse.accept(0, 0);
            phase = Stage.PUMP;
        } else if (phase == Stage.PUMP) {
            boolean complete = server(minecraft, player -> {
                long output = fixture.pump(player, true);
                dispatched = fixture.cpu(player).getCluster().craftingLogic.getWaitingFor(
                        appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.SMOOTH_STONE)) > 0;
                progressed = fixture.returnedStone;
                finalOutputReady = fixture.finalOutputReady(player);
                return output == 1 && !fixture.cpu(player).getCluster().isBusy() && fixture.observedNewSamples(player);
            });
            if (leaf.equals("delayed-status") && progressed && dispatched) {
                if (stonePlateObserved && !hasPlate("minecraft:stone", 4)
                        && !Boolean.TRUE.equals(checks.get("plate-recovered"))) {
                    mark(checks, "plate-recovered", true);
                    screenshot.accept("delayed-plate-recovered.png");
                }
                if (operation == null && Boolean.TRUE.equals(checks.get("plate-recovered")) && finalOutputReady
                        && rowText(snapshot, "minecraft:smooth_stone", "text.ae2craftingtime.ttc_delayed") != null
                        && hasPlate("minecraft:smooth_stone", 8)) {
                    mark(checks, "final-plate", true);
                    screenshot.accept("delayed-final-held.png");
                    minecraft.player.closeContainer();
                    phase = Stage.WORLD_POSITION;
                    return false;
                }
            }
            if (leaf.equals("waiting-status") && dispatched && progressed
                    && rowText(snapshot, "minecraft:smooth_stone", "text.ae2craftingtime.ttc") != null
                    && rowText(snapshot, "minecraft:smooth_stone", "text.ae2craftingtime.waiting") == null) {
                mark(checks, "first-dispatch", true);
                mark(checks, "recovered", true);
                screenshot.accept("waiting-recovered.png");
                return true;
            }
            if (leaf.equals("running-status") && progressed && dispatched
                    && rowText(snapshot, "minecraft:smooth_stone", "text.ae2craftingtime.ttc") != null
                    && snapshot.text().stream().anyMatch(t -> t.key().equals("text.ae2craftingtime.ttc")
                            && t.bounds() != null && t.bounds().y() < snapshot.gui().y() + 19 && t.bounds().inside(snapshot.gui()))) {
                validateLayout(snapshot);
                mark(checks, "progress", true);
                mark(checks, "header", true);
                screenshot.accept("running-progress.png");
                return true;
            }
            var header = snapshot.text().stream().filter(t -> t.bounds() != null && t.bounds().y() < snapshot.gui().y() + 19
                    && t.key().equals("text.ae2craftingtime.ttc")).findFirst();
            if (leaf.equals("standard-status-controls") && header.isPresent() && !Boolean.TRUE.equals(checks.get("header"))) {
                if (!header.get().bounds().inside(snapshot.gui()) || !LayoutValidator.validateBadges(snapshot).isEmpty()) {
                    throw new IllegalStateException("status header " + header.get().bounds() + " GUI " + snapshot.gui()
                            + " badge layout: " + LayoutValidator.validateBadges(snapshot));
                }
                mark(checks, "header", true);
                mark(checks, "layout", true);
                screenshot.accept("status-progress.png");
                return true;
            }
            if (complete) {
                mark(checks, "output", true);
                mark(checks, "profile-sample", true);
                phase = Stage.values()[phase.ordinal() + 1];
            }
        } else if (phase == Stage.FINISHED) {
            // Older AE2 can retain its last incremental row after the CPU becomes idle.
            // Preserve that view, then reopen through the actual return/status buttons.
            if (leaf.equals("craft-lifecycle") && snapshot.text().stream()
                    .anyMatch(t -> t.key().equals("text.ae2craftingtime.ttc") && t.bounds() != null
                            && t.bounds().y() < snapshot.gui().y() + 19)) {
                throw new IllegalStateException("completed crafting status still shows total TTC");
            }
            if (leaf.equals("craft-lifecycle")) mark(checks, "total-cleared", true);
            if (leaf.equals("craft-lifecycle")) screenshot.accept(partialJob ? "status-partial-finished.png" : "status-finished-job.png");
            var button = minecraft.screen.children().stream().filter(appeng.client.gui.widgets.TabButton.class::isInstance)
                    .map(appeng.client.gui.widgets.TabButton.class::cast).filter(w -> w.visible).findFirst().orElseThrow();
            DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            phase = Stage.values()[phase.ordinal() + 1];
        } else if (phase == Stage.EMPTY && minecraft.screen instanceof CraftingStatusScreen
                && snapshot.rows().stream().noneMatch(row -> row.craftAmount() > 0)) {
            mark(checks, "completed", true);
            if (leaf.equals("delayed-status")) {
                if (snapshot.text().stream().anyMatch(t -> t.key().equals("text.ae2craftingtime.ttc_delayed"))) return false;
                mark(checks, "recovered", true);
                screenshot.accept("delayed-recovered.png");
            } else { screenshot.accept(partialJob ? "status-partial-completed.png" : "status-completed.png"); }
            if (leaf.equals("craft-lifecycle")) {
                minecraft.player.closeContainer();
                reviewJob = true;
                phase = Stage.TERMINAL;
                return false;
            }
            return true;
        }
        return false;
    }

    static boolean galleryPlanReady(List<UiSnapshot.Row> rows, int knownRows) {
        var ids = List.of("minecraft:stone", "minecraft:smooth_stone");
        for (int index = 0; index < ids.size(); index++) {
            String id = ids.get(index);
            var row = rows.stream().filter(value -> value.outputId().equals(id) && value.craftAmount() > 0).findFirst();
            if (row.isEmpty()) return false;
            boolean known = index < knownRows;
            if (row.get().description().stream().noneMatch(text -> known ? CraftPlanScenario.isResolvedTtc(text)
                    : text.key().equals("text.ae2craftingtime.ttc")
                            && text.arguments().contains("text.ae2craftingtime.collecting_data"))) return false;
        }
        return true;
    }

    static boolean galleryAccuracyReady(com.ctux.ae2craftingtime.core.TtcAccuracyStats accuracy, boolean partial) {
        return accuracy.sampleCount() == 1 && accuracy.fullyCoveredSampleCount() == (partial ? 0 : 1)
                && accuracy.lastKnownRows() == (partial ? 1 : 2) && accuracy.lastTotalRows() == 2
                && accuracy.lastPredictedSeconds() > 0 && accuracy.lastActualWallSeconds() > 0;
    }

    private static boolean missingFirst(List<UiSnapshot.Row> rows) {
        var foundMissing = false;
        var foundNonMissing = false;
        for (var row : rows) {
            if (row.missingAmount() > 0) {
                foundMissing = true;
                if (foundNonMissing) return false;
            } else {
                foundNonMissing = true;
            }
        }
        return foundMissing;
    }

    private static UiSnapshot.ObservedText rowText(UiSnapshot snapshot, String output, String key) {
        var row = snapshot.rows().stream().filter(r -> r.outputId().equals(output)).findFirst();
        if (row.isEmpty()) return null;
        return snapshot.text().stream().filter(t -> t.key().equals(key) && t.bounds() != null
                && t.bounds().inside(row.get().cell())).findFirst().orElse(null);
    }

    private boolean hasPlate(String output, int providerOffset) {
        return ProviderHighlightClient.plates().stream().anyMatch(plate -> plate.outputId().equals(output)
                && plate.positions().contains(fixture.terminal.east(providerOffset)));
    }

    private static void validateLayout(UiSnapshot snapshot) {
        if (!LayoutValidator.validateBadges(snapshot).isEmpty() || snapshot.badges().isEmpty()) {
            throw new IllegalStateException("Invalid standard status badge layout");
        }
        var header = snapshot.text().stream().filter(t -> t.key().equals("text.ae2craftingtime.ttc")
                && t.bounds() != null && t.bounds().y() < snapshot.gui().y() + 19).findFirst();
        if (header.isPresent() && !header.get().bounds().inside(snapshot.gui())) {
            throw new IllegalStateException("Standard status header escapes GUI");
        }
    }

    private static boolean delayedTooltip(UiSnapshot snapshot) {
        var key = new com.ctux.ae2craftingtime.core.ProfileKey("minecraft:stone");
        var stall = com.ctux.ae2craftingtime.mc1201.ClientStats.CACHE.stall(key);
        if (stall.isEmpty()) return false;
        var diagnostic = stall.get();
        // Compare rendered numbers to the synchronized diagnostic, not a seeded warning.
        var seconds = (long) Math.ceil(diagnostic.idleTicks() / 20.0);
        var expected = ", " + net.minecraft.client.resources.language.I18n.get("text.ae2craftingtime.stall.delayed") + ": "
                + net.minecraft.client.resources.language.I18n.get("text.ae2craftingtime.value.whole_seconds", seconds) + ", "
                + net.minecraft.client.resources.language.I18n.get("text.ae2craftingtime.stall.typical") + ": "
                + com.ctux.ae2craftingtime.core.TimeEstimate.formatTicks(diagnostic.typicalDurationTicks());
        return snapshot.tooltip().stream().anyMatch(text -> text.key().equals("text.ae2craftingtime.stats.ttc")
                && text.rendered().startsWith(net.minecraft.client.resources.language.I18n.get("text.ae2craftingtime.stats.ttc") + ": ")
                && text.rendered().endsWith(expected))
                && snapshot.tooltip().stream().anyMatch(text -> text.key().equals("text.ae2craftingtime.stall.improvements"));
    }

    static boolean planEstimatesReady(List<UiSnapshot.Row> rows) {
        return List.of("minecraft:stone", "minecraft:smooth_stone").stream().allMatch(id -> rows.stream()
                .anyMatch(row -> row.outputId().equals(id)
                        && row.description().stream().anyMatch(CraftPlanScenario::isResolvedTtc)));
    }

    static List<UiSnapshot.ObservedText> cpuCardTotals(UiSnapshot snapshot) {
        return snapshot.text().stream().filter(t -> t.key().equals("text.ae2craftingtime.ttc")
                && t.bounds() != null && t.bounds().y() >= snapshot.gui().y() + 19).toList();
    }

    private static void mark(Map<String, Boolean> checks, String key, boolean value) {
        if (checks.containsKey(key)) checks.put(key, value);
    }

    void releaseKeys() { stats.releaseKeys(); }

    static boolean focus(long window) {
        var user = User32.INSTANCE;
        var nativeWindow = new com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Pointer.createConstant(
                org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(window)));
        var foreground = user.GetForegroundWindow();
        if (nativeWindow.equals(foreground)) return true;
        // A scheduled client can be visible while another desktop window still owns input.
        var currentThread = new DWORD(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetCurrentThreadId());
        var foregroundThread = new DWORD(user.GetWindowThreadProcessId(foreground, null));
        boolean attached = !currentThread.equals(foregroundThread)
                && user.AttachThreadInput(currentThread, foregroundThread, true);
        try {
            org.lwjgl.glfw.GLFW.glfwFocusWindow(window);
        } finally {
            if (attached) user.AttachThreadInput(currentThread, foregroundThread, false);
        }
        return false;
    }

    private String statsOutput() {
        // Reset the waiting status row so the running furnace keeps its real in-flight sample.
        return phase.ordinal() < Stage.OPEN_STATUS.ordinal() ? "minecraft:stone" : "minecraft:smooth_stone";
    }

    private boolean server(Minecraft minecraft, Function<ServerPlayer, Boolean> action) {
        if (operation == null) {
            var server = minecraft.getSingleplayerServer();
            var id = minecraft.player.getUUID();
            operation = server.submit(() -> action.apply(server.getPlayerList().getPlayer(id)));
        }
        if (!operation.isDone()) return false;
        boolean done = operation.join();
        operation = null;
        return done;
    }
}
