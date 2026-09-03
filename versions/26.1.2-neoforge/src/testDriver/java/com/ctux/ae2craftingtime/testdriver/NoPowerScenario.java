package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class NoPowerScenario {
    static final String SCENARIO = "no-power-status";
    static final String KEY = "text.ae2craftingtime.no_power";
    static final List<String> CHECKS = List.of("screen", "real-job", "external-unpowered", "active-network",
            "mixed-row", "tooltip", "layout", "ukrainian", "power-restored", "cancelled", "inactive-cpu");
    private final DispatchStatusFixture fixture = new DispatchStatusFixture(64);
    private final StableFrames<Integer> frames = new StableFrames<>(3);
    private final StableFrames<Integer> tooltipFrames = new StableFrames<>(3);
    private int phase;
    private int loggedPhase = -1;
    private long loggedTick = -1;
    private int menuId;
    private long changedAt;
    private CompletableFuture<Boolean> operation;
    private CompletableFuture<Void> reload;
    private CompletableFuture<Void> powerPulse;

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) {
        if (loggedPhase != phase) {
            System.out.println("AE2CT power fixture phase " + phase);
            loggedPhase = phase;
        }
        if (phase < 3) {
            if (serverStep(minecraft, player -> {
                if (phase == 1) {
                    var grid = fixture.cpu(player).getMainNode().getGrid();
                    if (grid != null) grid.getEnergyService().injectPower(100_000, Actionable.MODULATE);
                }
                var ready = fixture.prepare(phase, player, marker);
                if (ready && phase == 0) {
                    DispatchStatusFixture.place(player, fixture.cpuPosition.east(2), "energy_cell");
                    player.level().setBlockAndUpdate(fixture.cpuPosition.east(6).north(), Blocks.AIR.defaultBlockState());
                    player.level().setBlockAndUpdate(fixture.cpuPosition.east(6).below(),
                            Blocks.FURNACE.defaultBlockState());
                }
                return ready;
            })) {
                phase++;
                changedAt = System.nanoTime();
            }
            return false;
        }
        // Real grid energy only: keep idle demand supplied while the 64-AE dispatch cannot fit.
        if (phase >= 5 && phase <= 7 && (powerPulse == null || powerPulse.isDone())) {
            if (powerPulse != null) powerPulse.join();
            var server = minecraft.getSingleplayerServer();
            var playerId = minecraft.player.getUUID();
            powerPulse = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                var cpu = fixture.cpu(player).getCluster();
                var energy = cpu.getGrid().getEnergyService();
                var tick = player.level().getGameTime();
                if (tick - loggedTick >= 20) {
                    System.out.println("AE2CT power fixture: idle=" + energy.getIdlePowerUsage()
                            + ", simulated=" + energy.extractAEPower(64, Actionable.SIMULATE, PowerMultiplier.CONFIG)
                            + ", reasons=" + ProfilerBridge.blockReasons(cpu, cpu.getGrid(), tick)
                            + ", menuCpu=" + (com.ctux.ae2craftingtime.mc1201.StatsRequestContext.current(player).craftingCpu() == cpu)
                            + ", active=" + cpu.isActive() + ", providerBusy=" + fixture.provider(player, 6).getLogic().isBusy());
                    loggedTick = tick;
                }
                energy.extractAEPower(Double.MAX_VALUE, Actionable.MODULATE, PowerMultiplier.ONE);
                energy.injectPower(PowerMultiplier.CONFIG.multiply(48), Actionable.MODULATE);
                if (energy.getIdlePowerUsage() >= 48) {
                    throw new IllegalStateException("fixture idle demand exceeds low-power budget: " + energy.getIdlePowerUsage());
                }
            });
        }
        var snapshot = UiObservationStore.latest();
        if (!(minecraft.screen instanceof CraftingCPUScreen<?> screen) || snapshot == null
                || !snapshot.screen().equals(screen.getClass().getName()) || !frames.observe(phase)) return false;
        if (phase == 3) {
            if (hasWarning(snapshot)) throw new IllegalStateException("unfuelled furnace triggered NO POWER");
            if (System.nanoTime() - changedAt < 2_000_000_000L) return false;
            checks.put("screen", true);
            checks.put("real-job", true);
            checks.put("external-unpowered", true);
            screenshot.accept("no-power-external-unpowered.png");
            menuId = screen.getMenu().containerId;
            phase++;
        } else if (phase == 4) {
            if (serverStep(minecraft, player -> {
                var energy = fixture.cpu(player).getMainNode().getGrid().getEnergyService();
                energy.extractAEPower(Double.MAX_VALUE, Actionable.MODULATE, PowerMultiplier.ONE);
                energy.injectPower(PowerMultiplier.CONFIG.multiply(48), Actionable.MODULATE);
                if (energy.getIdlePowerUsage() >= 48) {
                    throw new IllegalStateException("fixture idle demand exceeds low-power budget: " + energy.getIdlePowerUsage());
                }
                ((Container) player.level().getBlockEntity(fixture.cpuPosition.east(6).below())).clearContent();
                return true;
            })) phase++;
        } else if (phase == 5 || phase == 6) {
            if (reload != null && !reload.isDone()) return false;
            if (reload != null) reload.join();
            if (!hasWarning(snapshot)) return false;
            moveMouse.accept(snapshot.gui().x() + 40, snapshot.gui().y() + 30);
            if (!tooltipReady(snapshot.tooltip()) || !tooltipFrames.observe(phase)) return false;
            var warning = snapshot.text().stream().filter(text -> text.key().equals(KEY)).findFirst().orElseThrow();
            if (!warning.bounds().inside(snapshot.gui()) || snapshot.badges().stream()
                    .noneMatch(badge -> warning.bounds().inside(badge))) {
                throw new IllegalStateException("NO POWER has no contained rendered badge");
            }
            if (!serverStep(minecraft, player -> {
                var cpu = fixture.cpu(player).getCluster();
                System.out.println("AE2CT power fixture: idle=" + cpu.getGrid().getEnergyService().getIdlePowerUsage()
                        + ", simulated=" + cpu.getGrid().getEnergyService().extractAEPower(64, Actionable.SIMULATE, PowerMultiplier.CONFIG));
                if (!cpu.isActive() || cpu.craftingLogic.getWaitingFor(AEItemKey.of(Items.DIAMOND)) != 1) {
                    throw new IllegalStateException("expected active CPU with one active and 63 pending outputs");
                }
                return true;
            })) return false;
            checks.put("active-network", true);
            checks.put("mixed-row", true);
            checks.put("tooltip", true);
            checks.put("layout", true);
            if (phase == 5) {
                screenshot.accept("no-power-en-us.png");
                minecraft.getLanguageManager().setSelected("uk_ua");
                minecraft.options.languageCode = "uk_ua";
                reload = minecraft.reloadResourcePacks();
                phase++;
            } else if (warning.rendered().equals("Немає енергії")) {
                checks.put("ukrainian", true);
                screenshot.accept("no-power-uk-ua.png");
                minecraft.getLanguageManager().setSelected("en_us");
                minecraft.options.languageCode = "en_us";
                reload = minecraft.reloadResourcePacks();
                phase++;
            }
        } else if (phase == 7) {
            if (!reload.isDone()) return false;
            reload.join();
            phase++;
        } else if (phase == 8) {
            if (powerPulse != null && !powerPulse.isDone()) return false;
            if (serverStep(minecraft, player -> {
                var energy = fixture.cpu(player).getMainNode().getGrid().getEnergyService();
                energy.injectPower(100_000, Actionable.MODULATE);
                return true;
            })) {
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 9) {
            if (hasWarning(snapshot)) {
                if (System.nanoTime() - changedAt > 2_000_000_000L) throw new IllegalStateException("power recovery missed refresh");
                return false;
            }
            if (screen.getMenu().containerId != menuId) throw new IllegalStateException("recovery reopened menu");
            if (!serverStep(minecraft, player -> fixture.cpu(player).getCluster().craftingLogic
                    .getWaitingFor(AEItemKey.of(Items.DIAMOND)) > 1)) return false;
            checks.put("power-restored", true);
            screenshot.accept("no-power-restored.png");
            phase++;
        } else if (phase == 10) {
            if (serverStep(minecraft, player -> {
                var cpu = fixture.cpu(player);
                cpu.getCluster().craftingLogic.cancel();
                var clear = ProfilerBridge.blockReasons(cpu.getCluster(), cpu.getMainNode().getGrid(),
                        player.level().getGameTime()).isEmpty();
                cpu.getMainNode().getGrid().getEnergyService().extractAEPower(Double.MAX_VALUE,
                        Actionable.MODULATE, PowerMultiplier.ONE);
                return clear;
            })) {
                checks.put("cancelled", true);
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 11) {
            if (hasWarning(snapshot)) throw new IllegalStateException("inactive CPU triggered NO POWER");
            if (System.nanoTime() - changedAt < 2_000_000_000L) return false;
            if (!serverStep(minecraft, player -> !fixture.cpu(player).getCluster().isActive())) return false;
            checks.put("inactive-cpu", true);
            screenshot.accept("no-power-inactive.png");
            return true;
        }
        return false;
    }

    static boolean tooltipReady(List<UiSnapshot.ObservedText> tooltip) {
        return List.of(KEY, KEY + ".explanation", KEY + ".suggestion").stream()
                .allMatch(key -> tooltip.stream().anyMatch(text -> text.key().equals(key)));
    }

    private static boolean hasWarning(UiSnapshot snapshot) {
        return snapshot.text().stream().anyMatch(text -> text.key().equals(KEY));
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
