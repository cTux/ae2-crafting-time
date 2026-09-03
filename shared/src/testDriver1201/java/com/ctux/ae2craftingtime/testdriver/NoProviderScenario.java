package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class NoProviderScenario {
    static final String SCENARIO = "no-provider-status";
    static final String KEY = "text.ae2craftingtime.no_provider";
    static final List<String> CHECKS = List.of("screen", "real-job", "mixed-row", "pattern-removed", "tooltip",
            "layout", "ukrainian", "pattern-restored", "second-provider", "provider-removed",
            "provider-restored", "cancelled");
    private BlockPos cpuPosition;
    private int phase;
    private int menuId;
    private long changedAt;
    private CompletableFuture<Boolean> operation;
    private CompletableFuture<Void> reload;
    private Future<ICraftingPlan> calculation;
    private final StableFrames<Integer> frames = new StableFrames<>(3);
    private final StableFrames<Integer> tooltipFrames = new StableFrames<>(3);

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> screenshot, BiConsumer<Integer, Integer> moveMouse) {
        if (phase < 3) {
            if (serverStep(minecraft, player -> prepare(player, marker))) {
                phase++;
            }
            return false;
        }
        var snapshot = UiObservationStore.latest();
        if (!(minecraft.screen instanceof CraftingCPUScreen<?> screen) || snapshot == null
                || !snapshot.screen().equals(screen.getClass().getName()) || !frames.observe(phase)) {
            return false;
        }
        if (phase == 3) {
            checks.put("screen", true);
            checks.put("real-job", true);
            checks.put("mixed-row", true);
            if (hasWarning(snapshot)) {
                throw new IllegalStateException("connected provider triggered NO PROVIDER");
            }
            screenshot.accept("no-provider-before.png");
            menuId = screen.getMenu().containerId;
            phase++;
        } else if (phase == 4) {
            if (serverStep(minecraft, player -> {
                provider(player, 6).getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                return true;
            })) {
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 5 || phase == 6) {
            if (reload != null && !reload.isDone()) {
                return false;
            }
            if (reload != null) {
                reload.join();
            }
            if (!hasWarning(snapshot)) {
                return false;
            }
            moveMouse.accept(snapshot.gui().x() + 40, snapshot.gui().y() + 30);
            if (!tooltipReady(snapshot.tooltip()) || !tooltipFrames.observe(phase)) {
                return false;
            }
            var warning = snapshot.text().stream().filter(text -> text.key().equals(KEY)).findFirst().orElseThrow();
            if (!warning.bounds().inside(snapshot.gui()) || snapshot.badges().stream()
                    .noneMatch(badge -> warning.bounds().inside(badge))) {
                throw new IllegalStateException("NO PROVIDER has no contained rendered badge");
            }
            checks.put("pattern-removed", true);
            checks.put("tooltip", true);
            checks.put("layout", true);
            if (phase == 5) {
                screenshot.accept("no-provider-en-us.png");
                minecraft.getLanguageManager().setSelected("uk_ua");
                minecraft.options.languageCode = "uk_ua";
                reload = minecraft.reloadResourcePacks();
                phase++;
            } else if (warning.rendered().equals("Без провайдера")) {
                checks.put("ukrainian", true);
                screenshot.accept("no-provider-uk-ua.png");
                minecraft.getLanguageManager().setSelected("en_us");
                minecraft.options.languageCode = "en_us";
                reload = minecraft.reloadResourcePacks();
                phase++;
            }
        } else if (phase == 7) {
            if (!reload.isDone()) {
                return false;
            }
            reload.join();
            if (serverStep(minecraft, player -> {
                provider(player, 6).getLogic().getPatternInv().setItemDirect(0, pattern());
                return true;
            })) {
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 8 || phase == 12) {
            if (hasWarning(snapshot)) {
                if (System.nanoTime() - changedAt > 2_000_000_000L) {
                    throw new IllegalStateException("provider recovery did not reach the next status refresh");
                }
                return false;
            }
            if (screen.getMenu().containerId != menuId) {
                throw new IllegalStateException("recovery reopened the menu");
            }
            checks.put(phase == 8 ? "pattern-restored" : "provider-restored", true);
            screenshot.accept(phase == 8 ? "no-provider-pattern-restored.png" : "no-provider-block-restored.png");
            phase++;
        } else if (phase == 9) {
            if (serverStep(minecraft, player -> {
                provider(player, 8).getLogic().getPatternInv().setItemDirect(0, pattern());
                provider(player, 6).getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                return true;
            })) {
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 10) {
            if (hasWarning(snapshot)) {
                throw new IllegalStateException("second connected provider did not prevent warning");
            }
            if (System.nanoTime() - changedAt < 2_000_000_000L) {
                return false;
            }
            if (!checks.get("second-provider")) {
                screenshot.accept("no-provider-redundant.png");
                checks.put("second-provider", true);
            }
            if (serverStep(minecraft, player -> {
                player.serverLevel().setBlockAndUpdate(cpuPosition.east(8), Blocks.AIR.defaultBlockState());
                return true;
            })) {
                phase++;
            }
        } else if (phase == 11) {
            if (!hasWarning(snapshot)) {
                return false;
            }
            if (!checks.get("provider-removed")) {
                screenshot.accept("no-provider-block-removed.png");
                checks.put("provider-removed", true);
            }
            if (serverStep(minecraft, player -> restoreProvider(player))) {
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 13) {
            if (serverStep(minecraft, player -> {
                var cpu = cpu(player);
                cpu.getCluster().craftingLogic.cancel();
                return ProfilerBridge.missingProviders(cpu.getCluster(), cpu.getMainNode().getGrid()).isEmpty();
            })) {
                changedAt = System.nanoTime();
                phase++;
            }
        } else if (phase == 14 && !hasWarning(snapshot) && System.nanoTime() - changedAt > 1_000_000_000L) {
            checks.put("cancelled", true);
            screenshot.accept("no-provider-cancelled.png");
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
        if (!operation.isDone()) {
            return false;
        }
        var done = operation.join();
        operation = null;
        return done;
    }

    private boolean prepare(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        if (phase == 0) {
            cpuPosition = new BlockPos(marker.terminal().x() + 40, marker.terminal().y(), marker.terminal().z());
            for (var pos : BlockPos.betweenClosed(cpuPosition.offset(-2, -1, -2), cpuPosition.offset(10, 3, 2))) {
                level.setBlockAndUpdate(pos, pos.getY() < cpuPosition.getY()
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            place(player, cpuPosition, "16k_crafting_storage");
            place(player, cpuPosition.east(2), "creative_energy_cell");
            place(player, cpuPosition.east(4), "drive");
            place(player, cpuPosition.east(6), "pattern_provider");
            place(player, cpuPosition.east(8), "pattern_provider");
            level.setBlockAndUpdate(cpuPosition.east(6).north(), Blocks.CHEST.defaultBlockState());
            player.teleportTo(cpuPosition.getX() + 0.5, cpuPosition.getY(), cpuPosition.getZ() + 2.5);
            return true;
        }
        var cpu = cpu(player);
        if (phase == 1) {
            if (!cpu.getMainNode().isReady()) {
                return false;
            }
            if (!cpu.isFormed()) {
                var calculator = new CraftingCPUCalculator(cpu);
                calculator.updateBlockEntities(calculator.createCluster(level, cpuPosition, cpuPosition),
                        level, cpuPosition, cpuPosition);
            }
            for (var offset : List.of(2, 4, 6, 8)) {
                if (!connect(player, offset)) {
                    return false;
                }
            }
            if (!cpu.getCluster().isActive()) {
                return false;
            }
            var drive = (DriveBlockEntity) level.getBlockEntity(cpuPosition.east(4));
            drive.getInternalInventory().setItemDirect(0,
                    new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("ae2:item_storage_cell_1k"))));
            drive.getCellInventory(0).insert(AEItemKey.of(Items.COBBLESTONE), 64, Actionable.MODULATE, IActionSource.empty());
            provider(player, 6).getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.YES);
            provider(player, 6).getLogic().getPatternInv().setItemDirect(0, pattern());
            calculation = cpu.getMainNode().getGrid().getCraftingService().beginCraftingCalculation(level,
                    () -> IActionSource.ofMachine(cpu), AEItemKey.of(Items.DIAMOND), 64, CalculationStrategy.REPORT_MISSING_ITEMS);
            return true;
        }
        if (!calculation.isDone()) {
            return false;
        }
        try {
            if (!cpu.getCluster().isBusy()) {
                var result = cpu.getMainNode().getGrid().getCraftingService().submitJob(calculation.get(), null,
                        cpu.getCluster(), false, IActionSource.ofMachine(cpu));
                if (!result.successful()) {
                    throw new IllegalStateException("fixture crafting submission failed: " + result);
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("fixture calculation/submission failed", error);
        }
        var active = cpu.getCluster().craftingLogic.getWaitingFor(AEItemKey.of(Items.DIAMOND));
        if (active <= 0) {
            return false;
        }
        if (active >= 64) {
            throw new IllegalStateException("fixture has no remaining scheduled batches");
        }
        MenuOpener.open(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(cpu));
        return true;
    }

    private boolean restoreProvider(ServerPlayer player) {
        if (!(player.serverLevel().getBlockEntity(cpuPosition.east(8)) instanceof PatternProviderBlockEntity)) {
            place(player, cpuPosition.east(8), "pattern_provider");
            return false;
        }
        if (!connect(player, 8)) {
            return false;
        }
        provider(player, 8).getLogic().getPatternInv().setItemDirect(0, pattern());
        return true;
    }

    private boolean connect(ServerPlayer player, int offset) {
        var host = (IInWorldGridNodeHost) player.serverLevel().getBlockEntity(cpuPosition.east(offset));
        var node = host.getGridNode(Direction.UP);
        if (node == null) {
            return false;
        }
        var cpuNode = cpu(player).getMainNode().getNode();
        if (node.getGrid() != cpuNode.getGrid()) {
            GridHelper.createConnection(cpuNode, node);
        }
        return true;
    }

    private CraftingBlockEntity cpu(ServerPlayer player) {
        return (CraftingBlockEntity) player.serverLevel().getBlockEntity(cpuPosition);
    }

    private PatternProviderBlockEntity provider(ServerPlayer player, int offset) {
        return (PatternProviderBlockEntity) player.serverLevel().getBlockEntity(cpuPosition.east(offset));
    }

    private static ItemStack pattern() {
        return DriverPlatform.processingPattern(new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1),
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
    }

    private static void place(ServerPlayer player, BlockPos pos, String id) {
        player.serverLevel().setBlockAndUpdate(pos,
                BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("ae2:" + id)).defaultBlockState());
    }
}
