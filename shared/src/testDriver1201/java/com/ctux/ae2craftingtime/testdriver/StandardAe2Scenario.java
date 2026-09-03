package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.crafting.CraftAmountScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.me.crafting.CraftingStatusScreen;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import com.ctux.ae2craftingtime.testdriver.mixin.ChatComponentAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
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
    static final String SCENARIO = "standard-ae2";
    static final List<String> CHECKS = List.of("plan", "plan-sort", "plan-tooltip", "plan-details", "plan-reset",
            "submitted", "status", "status-sort", "status-tooltip", "status-details", "status-reset",
            "waiting", "running", "delayed", "header", "layout", "completed", "output");
    private final StandardCraftFixture fixture = new StandardCraftFixture();
    private final StableFrames<Integer> frames = new StableFrames<>(8);
    private CompletableFuture<Boolean> operation;
    private int phase;
    private int reportedPhase = -1;
    private int sort;
    private long lastFrame = -1;
    private boolean keyboardUsed;
    private int clickPhase;
    private boolean clicked;
    private int chatCount;
    private long nextStatsClick;

    String checkpoint() { return "phase=" + phase + " fixture=" + fixture.checkpoint; }

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) throws Exception {
        if (reportedPhase != phase) {
            System.out.println("AE2CT standard checkpoint " + java.time.Instant.now() + " " + checkpoint());
            reportedPhase = phase;
        }
        if (phase == 0) {
            if (server(minecraft, player -> fixture.prepare(player, marker))) phase++;
            return false;
        }
        if (phase == 1) {
            if (minecraft.screen == null) {
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(fixture.terminal).add(0, 0, -0.5), Direction.NORTH, fixture.terminal, false));
            } else if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var entry = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$repo().getAllEntries().stream()
                        .filter(row -> row.getWhat().getId().toString().equals("minecraft:smooth_stone") && row.isCraftable())
                        .findFirst().orElse(null);
                if (entry != null) {
                    DriverPlatform.cloneEntry(screen, entry);
                    phase++;
                }
            }
            return false;
        }
        if (phase == 2) {
            if (minecraft.screen instanceof CraftAmountScreen amount) {
                var button = ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            } else if (minecraft.screen instanceof CraftConfirmScreen) phase++;
            return false;
        }
        if (phase == 8 || phase == 18) {
            if (phase == 8) {
                if (!server(minecraft, player -> fixture.cpu(player).getCluster().isBusy())) return false;
                checks.put("submitted", true);
            }
            if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var button = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$statusButton();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
                phase++;
            }

            return false;
        }
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.frame() == lastFrame) return false;
        lastFrame = snapshot.frame();
        if (!frames.observe(phase * 10 + sort)) return false;
        boolean plan = phase < 8;
        String prefix = plan ? "plan" : "status";
        if (phase == 3 || phase == 10) {
            var rows = snapshot.rows().stream().filter(row -> row.craftAmount() > 0).map(UiSnapshot.Row::outputId).toList();
            if (!rows.containsAll(List.of("minecraft:stone", "minecraft:smooth_stone"))) return false;
            if (sort == 0) {
                checks.put(prefix, true);
                if (plan && !snapshot.text().stream().anyMatch(t -> t.key().equals("text.ae2craftingtime.total_ttc"))) return false;
                if (plan && (snapshot.badges().isEmpty() || !LayoutValidator.validateBadges(snapshot).isEmpty())) {
                    throw new IllegalStateException("plan badge layout: " + LayoutValidator.validateBadges(snapshot));
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
                checks.put(prefix + "-sort", true);
                sort = 0;
                phase++;
            }
        } else if (phase == 4 || phase == 11) {
            var row = snapshot.rows().stream().filter(r -> r.outputId().equals(statsOutput())).findFirst().orElseThrow();
            moveMouse.accept(row.cell().centerX(), row.cell().centerY());
            if (snapshot.tooltip().stream().noneMatch(t -> t.key().equals("text.ae2craftingtime.details_hint"))) return false;
            checks.put(prefix + "-tooltip", true);
            screenshot.accept(prefix + "-tooltip.png");
            phase++;
        } else if (phase == 5 || phase == 6 || phase == 12 || phase == 13) {
            boolean reset = phase == 6 || phase == 13;
            if (!clickStats(minecraft, snapshot, reset)) return false;
            if (reset && !server(minecraft, player -> ProfilerBridge.stats(ProfilerBridge.key(
                    ProfilerBridge.networkId(fixture.cpu(player).getMainNode().getGrid()),
                    appeng.api.stacks.AEItemKey.of(plan ? net.minecraft.world.item.Items.STONE : net.minecraft.world.item.Items.SMOOTH_STONE))).isEmpty())) return false;
            checks.put(prefix + (reset ? "-reset" : "-details"), true);
            screenshot.accept(prefix + (reset ? "-reset.png" : "-details.png"));
            phase++;
            clicked = false;
        } else if (phase == 7) {
            moveMouse.accept(0, 0);
            if (!server(minecraft, player -> { fixture.seed(player); return true; })) return false;
            var start = minecraft.screen.children().stream().filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast).filter(w -> w.active && w.getMessage().getString().equals("Start"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Crafting Plan Start button is missing"));
            DriverPlatform.click(minecraft, start.getX() + 4, start.getY() + 4);
            phase++;
        } else if (phase == 9) {
            if (!(minecraft.screen instanceof CraftingStatusScreen)) return false;
            checks.put("waiting", snapshot.text().stream().anyMatch(t -> t.key().equals("text.ae2craftingtime.waiting")));
            checks.put("running", snapshot.text().stream().anyMatch(t -> t.key().equals("text.ae2craftingtime.ttc")));
            if (checks.get("waiting") && checks.get("running")) {
                screenshot.accept("status-waiting-running.png");
                phase++;
            }
        } else if (phase == 14) {
            moveMouse.accept(0, 0);
            if (server(minecraft, player -> { fixture.seed(player); return true; })) phase++;
        } else if (phase == 15) {
            if (snapshot.text().stream().noneMatch(t -> t.key().equals("text.ae2craftingtime.ttc_delayed"))) return false;
            checks.put("delayed", true);
            screenshot.accept("status-delayed.png");
            phase++;
        } else if (phase == 16) {
            boolean complete = server(minecraft, player -> fixture.pump(player, true) == 1 && !fixture.cpu(player).getCluster().isBusy() && fixture.observedNewSamples(player));
            var header = snapshot.text().stream().filter(t -> t.bounds() != null && t.bounds().y() < snapshot.gui().y() + 19
                    && t.key().equals("text.ae2craftingtime.ttc")).findFirst();
            if (header.isPresent() && !checks.get("header")) {
                if (!header.get().bounds().inside(snapshot.gui()) || !LayoutValidator.validateBadges(snapshot).isEmpty()) {
                    throw new IllegalStateException("status header " + header.get().bounds() + " GUI " + snapshot.gui()
                            + " badge layout: " + LayoutValidator.validateBadges(snapshot));
                }
                checks.put("header", true);
                checks.put("layout", true);
                screenshot.accept("status-progress.png");
            }
            if (complete) {
                checks.put("output", true);
                phase++;
            }
        } else if (phase == 17) {
            // Older AE2 can retain its last incremental row after the CPU becomes idle.
            // Preserve that view, then reopen through the actual return/status buttons.
            screenshot.accept("status-finished-job.png");
            var button = minecraft.screen.children().stream().filter(appeng.client.gui.widgets.TabButton.class::isInstance)
                    .map(appeng.client.gui.widgets.TabButton.class::cast).filter(w -> w.visible).findFirst().orElseThrow();
            DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            phase++;
        } else if (phase == 19 && minecraft.screen instanceof CraftingStatusScreen
                && snapshot.rows().stream().noneMatch(row -> row.craftAmount() > 0)) {
            checks.put("completed", true);
            screenshot.accept("status-completed.png");
            return true;
        }
        return false;
    }

    private boolean clickStats(Minecraft minecraft, UiSnapshot snapshot, boolean reset) throws Exception {
        var chat = ((ChatComponentAccessor) minecraft.gui.getChat()).ae2craftingtime_test_driver$messages();
        if (!clicked) {
            if (System.nanoTime() < nextStatsClick) return false;
            if (!DriverPlatform.focus(minecraft)) {
                releaseKeys();
                clickPhase = 0;
                return false;
            }
            if (clickPhase++ == 0) {
                chatCount = chat.size();
                key(0x11, false);
                if (reset) key(0x12, false);
                return false;
            }
            if (!DriverPlatform.modifiers(minecraft, reset)) return false;
            var row = snapshot.rows().stream().filter(r -> r.outputId().equals(statsOutput())).findFirst().orElseThrow();
            DriverPlatform.click(minecraft, row.cell().centerX(), row.cell().centerY());
            releaseKeys();
            clicked = true;
            clickPhase = 0;
        }
        String expected = reset ? "Cleared TTC stats for " + statsOutput() : statsOutput() + " x1:";
        boolean received = chat.size() > chatCount && chat.subList(0, chat.size() - chatCount).stream()
                .anyMatch(message -> message.content().getString().contains(expected));
        if (received) nextStatsClick = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(
                com.ctux.ae2craftingtime.core.PlayerMessageRateLimit.COOLDOWN_MILLIS);
        return received;
    }

    void releaseKeys() {
        if (keyboardUsed) {
            key(0x12, true);
            key(0x11, true);
        }
    }

    private void key(int code, boolean release) {
        // CodexVM is Windows; Minecraft's existing JNA dependency avoids AWT's cached headless state.
        var input = new INPUT();
        input.type = new DWORD(INPUT.INPUT_KEYBOARD);
        input.input.setType(KEYBDINPUT.class);
        input.input.ki.wVk = new WORD(0);
        input.input.ki.wScan = new WORD(code == 0x11 ? 0x1d : 0x38);
        input.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_SCANCODE | (release ? KEYBDINPUT.KEYEVENTF_KEYUP : 0));
        keyboardUsed = true;
        if (User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {input}, input.size()).intValue() != 1) {
            throw new IllegalStateException("Native modifier input was rejected");
        }
    }

    private String statsOutput() {
        // Reset the waiting status row so the running furnace keeps its real in-flight sample.
        return phase < 8 ? "minecraft:stone" : "minecraft:smooth_stone";
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
