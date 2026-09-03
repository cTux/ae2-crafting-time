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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
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
    private int sort;
    private long lastFrame = -1;
    private java.awt.Robot keyboard;
    private int clickPhase;
    private boolean clicked;
    private int chatCount;

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) throws Exception {
        if (phase == 0) {
            if (server(minecraft, player -> fixture.prepare(player, marker))) phase++;
            return false;
        }
        if (phase == 1) {
            if (minecraft.screen == null) {
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(fixture.terminal), Direction.NORTH, fixture.terminal, false));
            } else if (minecraft.screen instanceof MEStorageScreen<?> screen) {
                var entry = ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$repo().getAllEntries().stream()
                        .filter(row -> row.getWhat().getId().toString().equals("minecraft:smooth_stone") && row.isCraftable())
                        .findFirst().orElse(null);
                if (entry != null) {
                    ((MEStorageScreenAccessor) screen).ae2craftingtime_test_driver$click(entry, 2, ContainerInput.CLONE);
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
        if (phase == 8) {
            if (!server(minecraft, player -> fixture.cpu(player).getCluster().isBusy())) return false;
            checks.put("submitted", true);
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
                screenshot.accept(prefix + "-default.png");
            }
            if (sort == 2 && !rows.subList(0, 2).equals(List.of("minecraft:smooth_stone", "minecraft:stone"))) {
                throw new IllegalStateException(prefix + " ascending row order is " + rows);
            }
            if ((sort == 0 || sort == 3) && !rows.subList(0, 2).equals(List.of("minecraft:stone", "minecraft:smooth_stone"))) {
                throw new IllegalStateException(prefix + " descending row order is " + rows);
            }
            if (sort > 0) screenshot.accept(prefix + "-sort-" + sort + ".png");
            if (sort++ < 3) {
                var button = minecraft.screen.children().stream().filter(TtcSortButton.class::isInstance)
                        .map(TtcSortButton.class::cast).findFirst().orElseThrow();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
            } else {
                checks.put(prefix + "-sort", true);
                sort = 0;
                phase++;
            }
        } else if (phase == 4 || phase == 11) {
            var row = snapshot.rows().stream().filter(r -> r.outputId().equals("minecraft:stone")).findFirst().orElseThrow();
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
                    appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.STONE))).isEmpty())) return false;
            checks.put(prefix + (reset ? "-reset" : "-details"), true);
            screenshot.accept(prefix + (reset ? "-reset.png" : "-details.png"));
            phase++;
            clicked = false;
        } else if (phase == 7) {
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
            if (server(minecraft, player -> { fixture.seed(player); return true; })) phase++;
        } else if (phase == 15) {
            if (snapshot.text().stream().noneMatch(t -> t.key().equals("text.ae2craftingtime.ttc_delayed"))) return false;
            checks.put("delayed", true);
            screenshot.accept("status-delayed.png");
            phase++;
        } else if (phase == 16) {
            boolean complete = server(minecraft, player -> fixture.pump(player, true) == 1 && !fixture.cpu(player).getCluster().isBusy());
            var header = snapshot.text().stream().filter(t -> t.bounds() != null && t.bounds().y() < snapshot.gui().y() + 19
                    && t.key().equals("text.ae2craftingtime.ttc")).findFirst();
            if (header.isPresent() && !checks.get("header")) {
                if (!header.get().bounds().inside(snapshot.gui()) || !LayoutValidator.validateBadges(snapshot).isEmpty()) {
                    throw new IllegalStateException("status TTC header or badge layout is invalid");
                }
                checks.put("header", true);
                checks.put("layout", true);
                screenshot.accept("status-progress.png");
            }
            if (complete) {
                checks.put("output", true);
                phase++;
            }
        } else if (phase == 17 && snapshot.rows().stream().noneMatch(row -> row.craftAmount() > 0)) {
            checks.put("completed", true);
            screenshot.accept("status-completed.png");
            return true;
        }
        return false;
    }

    private boolean clickStats(Minecraft minecraft, UiSnapshot snapshot, boolean reset) throws Exception {
        var chat = ((ChatComponentAccessor) minecraft.gui.getChat()).ae2craftingtime_test_driver$messages();
        if (!clicked) {
            if (keyboard == null) keyboard = new java.awt.Robot();
            if (clickPhase++ == 0) {
                chatCount = chat.size();
                keyboard.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
                if (reset) keyboard.keyPress(java.awt.event.KeyEvent.VK_ALT);
                return false;
            }
            if (clickPhase < 3) return false;
            var row = snapshot.rows().stream().filter(r -> r.outputId().equals("minecraft:stone")).findFirst().orElseThrow();
            DriverPlatform.click(minecraft, row.cell().centerX(), row.cell().centerY());
            keyboard.keyRelease(java.awt.event.KeyEvent.VK_ALT);
            keyboard.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
            clicked = true;
            clickPhase = 0;
        }
        String expected = reset ? "Cleared TTC stats for Stone" : "Stone x";
        return chat.size() > chatCount && chat.subList(0, chat.size() - chatCount).stream()
                .anyMatch(message -> message.content().getString().contains(expected));
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
