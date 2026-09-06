package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
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
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.Future;

final class DispatchStatusFixture {
    BlockPos cpuPosition;
    private Future<ICraftingPlan> calculation;
    private final int inputAmount;
    private final LockCraftingMode initialLock;
    private final boolean initialBlocking;
    private Object advancedCpu;

    DispatchStatusFixture(int inputAmount) {
        this(inputAmount, LockCraftingMode.NONE, true);
    }

    DispatchStatusFixture(int inputAmount, LockCraftingMode initialLock, boolean initialBlocking) {
        this.inputAmount = inputAmount;
        this.initialLock = initialLock;
        this.initialBlocking = initialBlocking;
    }

    boolean prepare(int phase, ServerPlayer player, FixtureMarker marker) {
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
            if (!prepareAdvancedCpu(player, marker)) return false;
            var drive = (DriveBlockEntity) level.getBlockEntity(cpuPosition.east(4));
            drive.getInternalInventory().setItemDirect(0,
                    new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("ae2:item_storage_cell_1k"))));
            drive.getCellInventory(0).insert(AEItemKey.of(Items.COBBLESTONE), 64L * inputAmount, Actionable.MODULATE, IActionSource.empty());
            provider(player, 6).getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE,
                    initialBlocking ? YesNo.YES : YesNo.NO);
            provider(player, 6).getLogic().getConfigManager().putSetting(Settings.LOCK_CRAFTING_MODE,
                    advancedCpu == null ? LockCraftingMode.NONE : initialLock);
            provider(player, 6).getLogic().getPatternInv().setItemDirect(0, pattern());
            calculation = cpu.getMainNode().getGrid().getCraftingService().beginCraftingCalculation(level,
                    () -> IActionSource.ofMachine(cpu), AEItemKey.of(Items.DIAMOND), 64, CalculationStrategy.REPORT_MISSING_ITEMS);
            return true;
        }
        if (!calculation.isDone()) {
            return false;
        }
        try {
            if (advancedCpu != null) {
                invokeAdvanced("submit", new Class<?>[] { ServerPlayer.class, ICraftingPlan.class }, player, calculation.get());
            } else if (!cpu.getCluster().isBusy()) {
                var result = cpu.getMainNode().getGrid().getCraftingService().submitJob(calculation.get(), null,
                        cpu.getCluster(), false, IActionSource.ofMachine(cpu));
                if (!result.successful()) {
                    throw new IllegalStateException("fixture crafting submission failed: " + result);
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("fixture calculation/submission failed", error);
        }
        if (advancedCpu != null) {
            invokeAdvanced("open", new Class<?>[] { ServerPlayer.class, CraftingBlockEntity.class }, player, cpu);
            return true;
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

    boolean connect(ServerPlayer player, int offset) {
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

    CraftingBlockEntity cpu(ServerPlayer player) {
        return (CraftingBlockEntity) player.serverLevel().getBlockEntity(cpuPosition);
    }

    PatternProviderBlockEntity provider(ServerPlayer player, int offset) {
        return (PatternProviderBlockEntity) player.serverLevel().getBlockEntity(cpuPosition.east(offset));
    }

    ItemStack pattern() {
        return DriverPlatform.processingPattern(new GenericStack(AEItemKey.of(Items.COBBLESTONE), inputAmount),
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
    }

    static void place(ServerPlayer player, BlockPos pos, String id) {
        player.serverLevel().setBlockAndUpdate(pos,
                BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("ae2:" + id)).defaultBlockState());
    }

    private boolean prepareAdvancedCpu(ServerPlayer player, FixtureMarker marker) {
        if (!Boolean.getBoolean("ae2craftingtime.test.advancedStatus")) return true;
        if (advancedCpu == null) {
            try {
                Class.forName("net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity");
                advancedCpu = Class.forName("com.ctux.ae2craftingtime.testdriver.AdvancedAeStatusFixture")
                        .getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                return true;
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("cannot create AdvancedAE status fixture", error);
            }
        }
        var provider = cpuPosition.east(6);
        var advancedMarker = new FixtureMarker(1, "craft-plan", "ae2-crafting-time", marker.disposableWorldId(),
                new FixtureMarker.Position(provider.getX(), provider.getY(), provider.getZ(), "UP"), "minecraft:diamond");
        return (boolean) invokeAdvanced("prepare", new Class<?>[] { ServerPlayer.class, FixtureMarker.class },
                player, advancedMarker);
    }

    private Object invokeAdvanced(String method, Class<?>[] parameters, Object... arguments) {
        try {
            var declared = advancedCpu.getClass().getDeclaredMethod(method, parameters);
            declared.setAccessible(true);
            return declared.invoke(advancedCpu, arguments);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("AdvancedAE status fixture failed: " + method, error);
        }
    }
}
