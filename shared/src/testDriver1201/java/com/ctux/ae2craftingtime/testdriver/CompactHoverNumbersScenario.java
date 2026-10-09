package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.crafting.CraftAmountScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.me.crafting.CraftingStatusScreen;
import appeng.api.stacks.AEItemKey;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real server-owned samples, native tooltips and modifier-click chat in one disposable world. */
final class CompactHoverNumbersScenario implements AutoCloseable {
    static final List<String> CHECKS = List.of("seeded-server-rate", "plan-throughput", "status-throughput",
            "full-chat-rates", "legacy-hover", "fixture-restored");
    record RateCase(String name, long amount, long ticks, String tick, String second,
            String compactTick, String compactSecond) {}
    static final List<RateCase> CASES = List.of(
            new RateCase("ordinary", 1, 20, "0.05", "1", "0.05", "1.00"),
            new RateCase("fractional", 1, 1000, "0.001", "0.02", "<0.01", "0.02"),
            new RateCase("thousand", 1000, 1, "1000", "20000", "~1k", "~20k"),
            new RateCase("petascale", 1_000_000_000_000_000L, 1, "1000000000000000", "20000000000000000", "~1P", "~20P"),
            new RateCase("exascale", 1_000_000_000_000_000_000L, 1, "1000000000000000000", "20000000000000000000", "~1E", "~20E"));
    private enum Stage { PREPARE, SEED, TERMINAL, AMOUNT, PLAN, PLAN_CHAT, LEGACY,
        SUBMIT, OPEN_STATUS, STATUS, STATUS_CHAT, CHAT_CAPTURE, CLEANUP, DONE }
    private StandardCraftFixture fixture;
    private SuiteFixture pristine;
    private final StatsInteraction interaction = new StatsInteraction();
    private final StableFrames<Object> frames = new StableFrames<>(8);
    private final boolean original = ClientOptionsRuntime.current().features().enabled(OptionFeature.COMPACT_HOVER_NUMBERS);
    private boolean closed;
    private int index;
    private Stage stage = Stage.PREPARE;
    private CompletableFuture<Boolean> operation;
    private boolean hovered;
    private boolean clicked;
    private long lastFrame = -1;
    private long seededAt;

    String checkpoint() { return "compact-hover=" + index + ":" + stage; }

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) {
        var rate = CASES.get(index);
        if (stage == Stage.DONE) return true;
        if (stage == Stage.PREPARE) {
            if (fixture == null) {
                fixture = new StandardCraftFixture();
                fixture.unprofiledPlan = true;
                fixture.holdFinalOutput = true;
                setCompact(true);
            }
            if (server(minecraft, player -> {
                if (pristine == null) pristine = new SuiteFixture(((net.minecraft.server.level.ServerLevel) player.level()), player, marker);
                return fixture.prepare(player, marker);
            })) next(Stage.SEED);
            return false;
        }
        if (stage == Stage.SEED) {
            if (server(minecraft, player -> {
                if (seededAt == 0) {
                    fixture.seed(player, rate.amount(), rate.ticks());
                    seededAt = ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime();
                    return false;
                }
                if (((net.minecraft.server.level.ServerLevel) player.level()).getGameTime() <= seededAt) return false;
                var stats = ProfilerBridge.stats(ProfilerBridge.key(
                        ProfilerBridge.networkId(fixture.cpu(player).getMainNode().getGrid()), AEItemKey.of(Items.STONE)));
                if (stats.isEmpty()) return false;
                if (stats.get().sampleCount() != 1 || stats.get().amountPerTick() != Double.parseDouble(rate.tick())
                        || stats.get().amountPerSecond() != Double.parseDouble(rate.second()))
                    throw new IllegalStateException("Unexpected server seed: " + stats.get());
                return true;
            })) next(Stage.TERMINAL);
            return false;
        }
        if (stage == Stage.TERMINAL) {
            if (minecraft.screen == null) {
                if (!clicked) {
                    minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(fixture.terminal).add(0, 0, -0.5),
                                    Direction.NORTH, fixture.terminal, false));
                    clicked = true;
                }
            } else if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var entry = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$repo().getAllEntries().stream()
                        .filter(row -> row.getWhat().getId().toString().equals("minecraft:smooth_stone") && row.isCraftable())
                        .findFirst().orElse(null);
                if (entry != null && frames.observe(screen.getMenu().containerId)) {
                    DriverPlatform.cloneEntry(screen, entry);
                    next(Stage.AMOUNT);
                }
            }
            return false;
        }
        if (stage == Stage.AMOUNT) {
            if (minecraft.screen instanceof CraftAmountScreen amount && !clicked) {
                var button = ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                clicked = true;
            } else if (minecraft.screen instanceof CraftConfirmScreen) next(Stage.PLAN);
            return false;
        }
        if (stage == Stage.SUBMIT) {
            if (minecraft.screen instanceof CraftConfirmScreen screen && !clicked) {
                var start = widgets(screen).stream().filter(button -> button.getMessage().getString().equals("Start"))
                        .findFirst().orElseThrow();
                DriverPlatform.click(minecraft, start.getX() + 4, start.getY() + 4);
                clicked = true;
            } else if (minecraft.screen instanceof MEStorageScreen<?>) next(Stage.OPEN_STATUS);
            return false;
        }
        if (stage == Stage.OPEN_STATUS) {
            if (minecraft.screen instanceof MEStorageScreen<?> screen && !clicked) {
                var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                clicked = true;
            } else if (minecraft.screen instanceof CraftingStatusScreen) next(Stage.STATUS);
            return false;
        }
        if (stage == Stage.CHAT_CAPTURE) {
            if (!frames.observe(true)) return false;
            screenshot.accept(image(rate, "chat"));
            minecraft.player.closeContainer();
            next(Stage.CLEANUP);
            return false;
        }
        if (stage == Stage.CLEANUP) {
            if (server(minecraft, player -> {
                fixture.cpu(player).getCluster().craftingLogic.cancel();
                if (fixture.cpu(player).getCluster().isBusy()) return false;
                pristine.restore(player);
                if (!pristine.samplesRestored()) throw new IllegalStateException("Retained samples were not restored");
                return true;
            })) {
                if (++index == CASES.size()) {
                    index--;
                    close();
                    if (ClientOptionsRuntime.current().features().enabled(OptionFeature.COMPACT_HOVER_NUMBERS) != original)
                        throw new IllegalStateException("Compact hover option was not restored");
                    CHECKS.forEach(check -> checks.put(check, true));
                    next(Stage.DONE);
                    return true;
                }
                fixture = null;
                seededAt = 0;
                next(Stage.PREPARE);
            }
            return false;
        }
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.frame() == lastFrame || !(minecraft.screen instanceof CraftConfirmScreen
                || minecraft.screen instanceof CraftingStatusScreen)) return false;
        lastFrame = snapshot.frame();
        var row = snapshot.rows().stream().filter(value -> value.outputId().equals("minecraft:stone")).findFirst().orElse(null);
        if (row == null) return false;
        moveMouse.accept(row.cell().centerX(), row.cell().centerY());
        if (!hovered) {
            hovered = true;
            frames.reset();
            return false;
        }
        if (stage == Stage.PLAN || stage == Stage.STATUS || stage == Stage.LEGACY) {
            var value = snapshot.tooltip().stream().filter(text -> text.key().equals("text.ae2craftingtime.stats.throughput"))
                    .map(UiSnapshot.ObservedText::rendered).findFirst().orElse("");
            var tick = stage == Stage.LEGACY ? String.format(java.util.Locale.ROOT, "%.2f", Double.parseDouble(rate.tick())) : rate.compactTick();
            var second = stage == Stage.LEGACY ? String.format(java.util.Locale.ROOT, "%.2f", Double.parseDouble(rate.second())) : rate.compactSecond();
            if (!value.contains(tick + " items/t") || !value.contains(second + " items/s")) return false;
            if (!frames.observe(value)) return false;
            screenshot.accept(image(rate, stage == Stage.LEGACY ? "legacy" : stage == Stage.PLAN ? "plan" : "status"));
            if (stage == Stage.LEGACY) { setCompact(true); next(Stage.SUBMIT); }
            else next(stage == Stage.PLAN ? Stage.PLAN_CHAT : Stage.STATUS_CHAT);
            return false;
        }
        if (stage == Stage.PLAN_CHAT || stage == Stage.STATUS_CHAT) {
            long amount = minecraft.screen instanceof CraftConfirmScreen ? row.craftAmount() : row.activeAmount() + row.pendingAmount();
            if (!interaction.click(minecraft, snapshot, "minecraft:stone", false, amount)) return false;
            if (!CraftingTreeScenario.fullChatRates(interaction.received(), "minecraft:stone", amount, rate.tick(), rate.second()))
                throw new IllegalStateException("Received chat did not retain the selected amount and both full rates");
            interaction.next();
            if (stage == Stage.PLAN_CHAT) { setCompact(false); next(Stage.LEGACY); }
            else {
                DriverPlatform.openChat(minecraft);
                next(Stage.CHAT_CAPTURE);
            }
        }
        return false;
    }

    private static List<net.minecraft.client.gui.components.AbstractWidget> widgets(net.minecraft.client.gui.screens.Screen screen) {
        return screen.children().stream().filter(net.minecraft.client.gui.components.AbstractWidget.class::isInstance)
                .map(net.minecraft.client.gui.components.AbstractWidget.class::cast).toList();
    }

    private static String image(RateCase rate, String part) { return "compact-hover-" + rate.name() + "-" + part + ".png"; }
    private void next(Stage value) { stage = value; clicked = false; hovered = false; frames.reset(); }
    private static void setCompact(boolean value) {
        ClientOptionsRuntime.current().features().setEnabled(OptionFeature.COMPACT_HOVER_NUMBERS, value);
    }
    private boolean server(Minecraft minecraft, Function<ServerPlayer, Boolean> action) {
        if (operation == null) {
            var id = minecraft.player.getUUID();
            operation = minecraft.getSingleplayerServer().submit(() -> action.apply(
                    minecraft.getSingleplayerServer().getPlayerList().getPlayer(id)));
        }
        if (!operation.isDone()) return false;
        boolean done = operation.join();
        operation = null;
        return done;
    }
    @Override public void close() {
        try { interaction.releaseKeys(); }
        finally { if (!closed) { setCompact(original); closed = true; } }
    }
}
