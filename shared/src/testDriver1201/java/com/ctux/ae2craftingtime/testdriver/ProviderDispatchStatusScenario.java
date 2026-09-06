package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class ProviderDispatchStatusScenario {
    static final String NO_TARGET = "no-target-status";
    static final String INPUT_BLOCKED = "input-blocked-status";
    static final String LOCKED = "locked-status";
    private static final String MIXED = "text.ae2craftingtime.dispatch_status.scheduled_only";
    private static final List<String> BASE_CHECKS = List.of("screen", "real-job", "tooltip", "layout");
    private final String scenario;
    private final String key;
    private final DispatchStatusFixture fixture;
    private final StableFrames<Integer> frames = new StableFrames<>(3);
    private final StableFrames<Integer> tooltipFrames = new StableFrames<>(3);
    private CompletableFuture<Boolean> operation;
    private int phase;
    private long changedAt;

    ProviderDispatchStatusScenario(String scenario) {
        if (!supports(scenario)) throw new IllegalArgumentException("unsupported provider status scenario: " + scenario);
        this.scenario = scenario;
        key = "text.ae2craftingtime." + scenario.replace("-status", "").replace('-', '_');
        fixture = new DispatchStatusFixture(1,
                LOCKED.equals(scenario) ? LockCraftingMode.LOCK_WHILE_LOW : LockCraftingMode.NONE,
                INPUT_BLOCKED.equals(scenario));
    }

    static boolean supports(String scenario) {
        return NO_TARGET.equals(scenario) || INPUT_BLOCKED.equals(scenario) || LOCKED.equals(scenario);
    }

    static List<String> checks(String scenario) {
        var checks = new java.util.ArrayList<>(BASE_CHECKS);
        if (advancedFixture()) checks.add("advanced-cpu");
        else checks.add("mixed-row");
        if (NO_TARGET.equals(scenario)) checks.addAll(List.of("target-removed", "target-restored"));
        if (INPUT_BLOCKED.equals(scenario)) checks.addAll(List.of(
                "blocking-mode", "blocking-recovered", "zero-insertion", "partial-capacity"));
        if (LOCKED.equals(scenario)) {
            checks.addAll(List.of("lock-while-low", "low-recovered"));
            if (!advancedFixture()) checks.addAll(List.of("lock-while-high", "high-recovered",
                    "pulse-lock", "pulse-recovered", "result-lock", "result-returned"));
        }
        return List.copyOf(checks);
    }

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) {
        if (phase < 3) {
            if (serverStep(minecraft, player -> fixture.prepare(phase, player, marker))) phase++;
            return false;
        }
        var snapshot = UiObservationStore.latest();
        if (!(minecraft.screen instanceof CraftingCPUScreen<?> screen) || snapshot == null
                || !snapshot.screen().equals(screen.getClass().getName()) || !frames.observe(phase)) return false;
        if (phase == 3) {
            checks.put("screen", true);
            checks.put("real-job", true);
            if (advancedFixture()) checks.put("advanced-cpu", true);
            else checks.put("mixed-row", true);
            if (NO_TARGET.equals(scenario)) {
                if (hasWarning(snapshot)) throw new IllegalStateException("NO TARGET appeared before target removal");
                if (serverStep(minecraft, player -> {
                    player.level().setBlockAndUpdate(fixture.cpuPosition.east(6).north(), Blocks.AIR.defaultBlockState());
                    return true;
                })) phase++;
                return false;
            }
            if (LOCKED.equals(scenario)) {
                if (serverStep(minecraft, player -> {
                    fixture.provider(player, 6).getLogic().getConfigManager().putSetting(
                            Settings.LOCK_CRAFTING_MODE, LockCraftingMode.LOCK_WHILE_LOW);
                    return true;
                })) phase++;
                return false;
            }
            if (!observeWarning(snapshot, checks, screenshot, moveMouse, scenario + "-en-us.png")) return false;
            checks.put("blocking-mode", true);
            phase++;
        } else if (NO_TARGET.equals(scenario)) {
            return tickNoTarget(minecraft, snapshot, checks, screenshot, moveMouse);
        } else if (INPUT_BLOCKED.equals(scenario)) {
            return tickInputBlocked(minecraft, snapshot, checks, screenshot);
        } else {
            return tickLocked(minecraft, snapshot, checks, screenshot, moveMouse);
        }
        return false;
    }

    private boolean tickNoTarget(Minecraft minecraft, UiSnapshot snapshot, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) {
        if (phase == 4) {
            if (!observeWarning(snapshot, checks, screenshot, moveMouse, "no-target-en-us.png")) return false;
            checks.put("target-removed", true);
            if (serverStep(minecraft, player -> {
                player.level().setBlockAndUpdate(fixture.cpuPosition.east(6).north(), Blocks.CHEST.defaultBlockState());
                return true;
            })) { changedAt = System.nanoTime(); phase++; }
        } else if (phase == 5 && recovered(snapshot)) {
            checks.put("target-restored", true);
            screenshot.accept("no-target-restored.png");
            return true;
        }
        return false;
    }

    private boolean tickInputBlocked(Minecraft minecraft, UiSnapshot snapshot, Map<String, Boolean> checks,
            Consumer<String> screenshot) {
        if (phase == 4 && serverStep(minecraft, player -> {
            fixture.provider(player, 6).getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.NO);
            ((Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).north())).clearContent();
            return true;
        })) { changedAt = System.nanoTime(); phase++; }
        else if (phase == 5 && recovered(snapshot)) {
            checks.put("blocking-recovered", true);
            screenshot.accept("input-blocked-recovered.png");
            if (serverStep(minecraft, player -> {
                var target = (Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).north());
                for (int slot = 0; slot < target.getContainerSize(); slot++) target.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                return true;
            })) phase++;
        } else if (phase == 6 && hasWarning(snapshot)) {
            checks.put("zero-insertion", true);
            screenshot.accept("input-blocked-zero-insertion.png");
            if (serverStep(minecraft, player -> {
                ((Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).north())).setItem(0, ItemStack.EMPTY);
                return true;
            })) { changedAt = System.nanoTime(); phase++; }
        } else if (phase == 7 && recovered(snapshot)) {
            checks.put("partial-capacity", true);
            screenshot.accept("input-blocked-partial-capacity.png");
            return true;
        }
        return false;
    }

    private boolean tickLocked(Minecraft minecraft, UiSnapshot snapshot, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) {
        var power = fixture.cpuPosition.east(6).south();
        if (phase == 4) {
            if (!observeWarning(snapshot, checks, screenshot, moveMouse, "locked-en-us.png")) return false;
            checks.put("lock-while-low", true);
            if (serverStep(minecraft, player -> {
                player.level().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
                return true;
            })) { changedAt = System.nanoTime(); phase++; }
        }
        else if (phase == 5 && recovered(snapshot)) {
            checks.put("low-recovered", true);
            if (advancedFixture()) return true;
            if (serverStep(minecraft, player -> {
                fixture.provider(player, 6).getLogic().getConfigManager().putSetting(
                        Settings.LOCK_CRAFTING_MODE, LockCraftingMode.LOCK_WHILE_HIGH);
                return true;
            })) phase++;
        } else if (phase == 6 && hasWarning(snapshot)) {
            checks.put("lock-while-high", true);
            if (serverStep(minecraft, player -> {
                player.level().setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
                return true;
            })) { changedAt = System.nanoTime(); phase++; }
        } else if (phase == 7 && recovered(snapshot)) {
            checks.put("high-recovered", true);
            if (serverStep(minecraft, player -> {
                fixture.provider(player, 6).getLogic().getConfigManager().putSetting(
                        Settings.LOCK_CRAFTING_MODE, LockCraftingMode.LOCK_UNTIL_PULSE);
                return true;
            })) phase++;
        } else if (phase == 8 && hasWarning(snapshot)) {
            checks.put("pulse-lock", true);
            if (serverStep(minecraft, player -> {
                var target = (Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).north());
                for (int slot = 0; slot < target.getContainerSize(); slot++) {
                    target.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                }
                player.level().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
                return true;
            })) { changedAt = System.nanoTime(); phase++; }
        } else if (phase == 9 && recovered(snapshot)) {
            checks.put("pulse-recovered", true);
            if (serverStep(minecraft, player -> {
                player.level().setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
                var logic = fixture.provider(player, 6).getLogic();
                logic.getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.NO);
                ((Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).north())).clearContent();
                logic.getConfigManager().putSetting(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.LOCK_UNTIL_RESULT);
                return true;
            })) phase++;
        } else if (phase == 10 && hasWarning(snapshot)) {
            checks.put("result-lock", true);
            screenshot.accept("locked-result-wait.png");
            if (serverStep(minecraft, player -> {
                var target = (Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).north());
                for (int slot = 0; slot < target.getContainerSize(); slot++) {
                    target.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                }
                fixture.provider(player, 6).getLogic().getReturnInv().insert(AEItemKey.of(Items.DIAMOND), 1,
                        Actionable.MODULATE, IActionSource.empty());
                return true;
            })) { changedAt = System.nanoTime(); phase++; }
        } else if (phase == 11 && recovered(snapshot)) {
            checks.put("result-returned", true);
            screenshot.accept("locked-recovered.png");
            return true;
        }
        return false;
    }

    private boolean observeWarning(UiSnapshot snapshot, Map<String, Boolean> checks, Consumer<String> screenshot,
            BiConsumer<Integer, Integer> moveMouse, String screenshotName) {
        if (!hasWarning(snapshot)) return false;
        var warning = snapshot.text().stream().filter(text -> text.key().equals(key)).findFirst().orElseThrow();
        moveMouse.accept(warning.bounds().x() + warning.bounds().width() / 2,
                warning.bounds().y() + warning.bounds().height() / 2);
        if (!tooltipReady(snapshot.tooltip()) || !tooltipFrames.observe(phase)) return false;
        if (!warning.bounds().inside(snapshot.gui())
                || snapshot.badges().stream().noneMatch(badge -> warning.bounds().inside(badge))) {
            throw new IllegalStateException(key + " has no contained rendered badge");
        }
        checks.put("tooltip", true);
        checks.put("layout", true);
        screenshot.accept(screenshotName);
        return true;
    }

    private boolean recovered(UiSnapshot snapshot) {
        if (hasWarning(snapshot)) {
            if (changedAt != 0 && System.nanoTime() - changedAt > 5_000_000_000L) {
                throw new IllegalStateException(key + " recovery missed the next status refresh");
            }
            return false;
        }
        return true;
    }

    private boolean tooltipReady(List<UiSnapshot.ObservedText> tooltip) {
        var expected = new java.util.ArrayList<>(List.of(key, key + ".explanation", key + ".suggestion"));
        if (!advancedFixture()) expected.add(MIXED);
        return expected.stream()
                .allMatch(key -> tooltip.stream().anyMatch(text -> text.key().equals(key)));
    }

    private static boolean advancedFixture() {
        return Boolean.getBoolean("ae2craftingtime.test.advancedStatus");
    }

    private boolean hasWarning(UiSnapshot snapshot) {
        return snapshot.text().stream().anyMatch(text -> text.key().equals(key));
    }

    private boolean serverStep(Minecraft minecraft, Function<ServerPlayer, Boolean> action) {
        if (operation == null) {
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            operation = server.submit(() -> action.apply(server.getPlayerList().getPlayer(playerId)));
        }
        if (!operation.isDone()) return false;
        var done = operation.join();
        operation = null;
        return done;
    }

}
