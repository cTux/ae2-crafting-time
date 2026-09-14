package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEParts;
import appeng.core.AEConfig;
import appeng.api.networking.pathing.ChannelMode;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
    private final long outputAmount;
    private final boolean channelScenario;
    private BlockPos providerPosition;
    private BlockPos healthyProviderPosition;
    private ChannelMode originalChannelMode;
    private SuiteFixture originalFixture;
    private long heldInputs;
    private long returnedOutput;
    private long pendingOutput;
    private long recoveredWaiting;
    private Object advancedCpu;

    DispatchStatusFixture(int inputAmount) {
        this(inputAmount, LockCraftingMode.NONE, true, 64);
    }

    DispatchStatusFixture(int inputAmount, LockCraftingMode initialLock, boolean initialBlocking, long outputAmount) {
        this(inputAmount, initialLock, initialBlocking, outputAmount, false);
    }

    DispatchStatusFixture(int inputAmount, LockCraftingMode initialLock, boolean initialBlocking, long outputAmount,
            boolean channelScenario) {
        this.inputAmount = inputAmount;
        this.initialLock = initialLock;
        this.initialBlocking = initialBlocking;
        this.outputAmount = outputAmount;
        this.channelScenario = channelScenario;
    }

    boolean prepare(int phase, ServerPlayer player, FixtureMarker marker) {
        var level = player.level();
        if (phase == 0) {
            if (channelScenario) originalFixture = new SuiteFixture(level, player, marker);
            cpuPosition = new BlockPos(marker.terminal().x() + 40, marker.terminal().y(), marker.terminal().z());
            for (var pos : BlockPos.betweenClosed(cpuPosition.offset(-2, -1, -2),
                    cpuPosition.offset(12, 3, channelScenario ? 30 : 2))) {
                level.setBlockAndUpdate(pos, pos.getY() < cpuPosition.getY()
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            place(player, cpuPosition, "64k_crafting_storage");
            if (channelScenario) {
                placeChannelNetwork(player);
            } else {
                place(player, cpuPosition.east(2), "creative_energy_cell");
                place(player, cpuPosition.east(4), "drive");
                place(player, cpuPosition.east(6), "pattern_provider");
                place(player, cpuPosition.east(8), "pattern_provider");
                providerPosition = cpuPosition.east(6);
                level.setBlockAndUpdate(providerPosition.north(), Blocks.CHEST.defaultBlockState());
            }
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
            if (channelScenario) {
                if (!selectStarvedProvider(player)) return false;
            } else {
                for (var offset : List.of(2, 4, 6, 8)) {
                    if (!connect(player, offset)) return false;
                }
            }
            if (!cpu.getCluster().isActive()) {
                return false;
            }
            if (!prepareAdvancedCpu(player, marker)) return false;
            var drive = (DriveBlockEntity) level.getBlockEntity(
                    channelScenario ? cpuPosition.east(2).south() : cpuPosition.east(4));
            drive.getInternalInventory().setItemDirect(0,
                    new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse("ae2:item_storage_cell_4k"))));
            drive.getCellInventory(0).insert(AEItemKey.of(Items.COBBLESTONE), outputAmount * inputAmount, Actionable.MODULATE, IActionSource.empty());
            provider(player).getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE,
                    initialBlocking ? YesNo.YES : YesNo.NO);
            provider(player).getLogic().getConfigManager().putSetting(Settings.LOCK_CRAFTING_MODE,
                    advancedCpu == null ? LockCraftingMode.NONE : initialLock);
            provider(player).getLogic().getPatternInv().setItemDirect(0, pattern());
            calculation = cpu.getMainNode().getGrid().getCraftingService().beginCraftingCalculation(level,
                    () -> IActionSource.ofMachine(cpu), AEItemKey.of(Items.DIAMOND), outputAmount, CalculationStrategy.REPORT_MISSING_ITEMS);
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
        if (!channelScenario && active <= 0) {
            return false;
        }
        if (!channelScenario && active >= outputAmount) {
            throw new IllegalStateException("fixture has no remaining scheduled batches");
        }
        MenuOpener.open(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(cpu));
        return true;
    }

    boolean connect(ServerPlayer player, int offset) {
        var host = (IInWorldGridNodeHost) player.level().getBlockEntity(cpuPosition.east(offset));
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
        return (CraftingBlockEntity) player.level().getBlockEntity(cpuPosition);
    }

    PatternProviderBlockEntity provider(ServerPlayer player, int offset) {
        return (PatternProviderBlockEntity) player.level().getBlockEntity(cpuPosition.east(offset));
    }

    PatternProviderBlockEntity provider(ServerPlayer player) {
        return (PatternProviderBlockEntity) player.level().getBlockEntity(providerPosition);
    }

    BlockPos targetPosition() {
        return providerPosition.north();
    }

    boolean restoreChannel(ServerPlayer player) {
        recoveredWaiting = waiting(player);
        provider(player).getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.NO);
        for (int x = 3; x <= 11; x++) {
            var candidate = cpuPosition.east(x).north();
            if (!candidate.equals(providerPosition)) {
                player.level().setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState());
                return true;
            }
        }
        return false;
    }

    boolean channelRestored(ServerPlayer player) {
        var node = ((IInWorldGridNodeHost) provider(player)).getGridNode(Direction.UP);
        return node != null && node.isPowered() && node.hasGridBooted() && node.meetsChannelRequirements();
    }

    boolean installHealthyAlternative(ServerPlayer player) {
        var alternative = (PatternProviderBlockEntity) player.level().getBlockEntity(healthyProviderPosition);
        if (!alternative.getMainNode().isActive()) return false;
        alternative.getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.YES);
        alternative.getLogic().getPatternInv().setItemDirect(0, pattern());
        return true;
    }

    boolean removeHealthyAlternative(ServerPlayer player) {
        if (healthyProviderPosition == null) return false;
        ((PatternProviderBlockEntity) player.level().getBlockEntity(healthyProviderPosition))
                .getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
        return true;
    }

    boolean setPower(ServerPlayer player, boolean powered) {
        var pos = cpuPosition.east(2).above();
        if (powered) place(player, pos, "creative_energy_cell");
        else player.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return true;
    }

    boolean providerPowered(ServerPlayer player, boolean expected) {
        var node = ((IInWorldGridNodeHost) provider(player)).getGridNode(Direction.UP);
        return node != null && node.isPowered() == expected;
    }

    boolean reboot(ServerPlayer player) {
        cpu(player).getMainNode().getGrid().getPathingService().repath();
        return true;
    }

    boolean providerRebooting(ServerPlayer player) {
        var node = ((IInWorldGridNodeHost) provider(player)).getGridNode(Direction.UP);
        return node != null && node.isPowered() && !node.hasGridBooted();
    }

    boolean setInputs(ServerPlayer player, boolean present) {
        var inventory = jobInventory(player);
        var key = AEItemKey.of(Items.COBBLESTONE);
        if (present) {
            inventory.insert(key, heldInputs, Actionable.MODULATE);
            heldInputs = 0;
        } else {
            heldInputs = inventory.extract(key, Long.MAX_VALUE, Actionable.MODULATE);
            if (heldInputs <= 0 || inventory.extract(key, 1, Actionable.SIMULATE) != 0) {
                throw new IllegalStateException("missing-input control did not remove reserved CPU ingredients");
            }
        }
        return true;
    }

    boolean setInfiniteChannels(ServerPlayer player, boolean infinite) {
        if (originalChannelMode == null) originalChannelMode = AEConfig.instance().getChannelMode();
        AEConfig.instance().setChannelModel(infinite ? ChannelMode.INFINITE : originalChannelMode);
        cpu(player).getMainNode().getGrid().getPathingService().repath();
        return true;
    }

    boolean channelMode(ServerPlayer player, boolean infinite) {
        var mode = cpu(player).getMainNode().getGrid().getPathingService().getChannelMode();
        return infinite ? mode == ChannelMode.INFINITE : mode == originalChannelMode;
    }

    void restoreChannelMode(ServerPlayer player) {
        if (originalChannelMode != null) {
            AEConfig.instance().setChannelModel(originalChannelMode);
            cpu(player).getMainNode().getGrid().getPathingService().repath();
            originalChannelMode = null;
        }
        if (originalFixture != null) {
            originalFixture.restore(player);
            originalFixture = null;
        }
    }

    boolean recoveredDispatch(ServerPlayer player) {
        return channelRestored(player) && waiting(player) > recoveredWaiting;
    }

    boolean healthyAlternativeDispatched(ServerPlayer player) {
        return waiting(player) > 0 && jobInventory(player).extract(AEItemKey.of(Items.COBBLESTONE),
                1, Actionable.SIMULATE) > 0;
    }

    boolean initialChannelJob(ServerPlayer player) {
        var node = ((IInWorldGridNodeHost) provider(player)).getGridNode(Direction.UP);
        var grid = cpu(player).getMainNode().getGrid();
        var key = com.ctux.ae2craftingtime.mc1201.ProfilerBridge.key(
                com.ctux.ae2craftingtime.mc1201.ProfilerBridge.networkId(grid), AEItemKey.of(Items.DIAMOND));
        return node != null && node.isPowered() && node.hasGridBooted() && !node.meetsChannelRequirements()
                && waiting(player) == 0 && jobInventory(player).extract(AEItemKey.of(Items.COBBLESTONE),
                        Long.MAX_VALUE, Actionable.SIMULATE) == outputAmount * inputAmount
                && com.ctux.ae2craftingtime.mc1201.ProfilerBridge.stats(key).isEmpty();
    }

    private appeng.crafting.inv.ListCraftingInventory jobInventory(ServerPlayer player) {
        return advancedCpu == null ? cpu(player).getCluster().craftingLogic.getInventory()
                : (appeng.crafting.inv.ListCraftingInventory) invokeAdvanced("inventory",
                        new Class<?>[] { ServerPlayer.class }, player);
    }

    private long waiting(ServerPlayer player) {
        return advancedCpu == null ? cpu(player).getCluster().craftingLogic.getWaitingFor(AEItemKey.of(Items.DIAMOND))
                : (long) invokeAdvanced("waiting", new Class<?>[] { ServerPlayer.class }, player);
    }

    boolean finishDispatchedOutput(ServerPlayer player) {
        var targets = new java.util.ArrayList<BlockPos>();
        for (int x = 3; x <= 11; x++) targets.add(cpuPosition.east(x).north(2));
        targets.add(healthyProviderPosition.south());
        for (var position : targets) {
            var target = (net.minecraft.world.Container) player.level().getBlockEntity(position);
            for (int slot = 0; slot < target.getContainerSize(); slot++) {
                var stack = target.getItem(slot);
                if (!stack.isEmpty()) {
                    if (!stack.is(Items.COBBLESTONE)) throw new IllegalStateException("unexpected channel target input");
                    pendingOutput += stack.getCount();
                    target.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        var accepted = provider(player).getLogic().getReturnInv().insert(AEItemKey.of(Items.DIAMOND), pendingOutput,
                Actionable.MODULATE, IActionSource.empty());
        pendingOutput -= accepted;
        returnedOutput += accepted;
        if (returnedOutput > outputAmount) throw new IllegalStateException("channel fixture returned excess output");
        return returnedOutput == outputAmount && pendingOutput == 0 && jobCompleted(player);
    }

    boolean jobCompleted(ServerPlayer player) {
        return advancedCpu == null ? !cpu(player).getCluster().isBusy()
                : (boolean) invokeAdvanced("finished", new Class<?>[] { ServerPlayer.class }, player);
    }

    ItemStack pattern() {
        return PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), inputAmount)),
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)));
    }

    static void place(ServerPlayer player, BlockPos pos, String id) {
        player.level().setBlockAndUpdate(pos,
                BuiltInRegistries.BLOCK.getValue(Identifier.tryParse("ae2:" + id)).defaultBlockState());
    }

    private boolean prepareAdvancedCpu(ServerPlayer player, FixtureMarker marker) {
        if (!Boolean.getBoolean("ae2craftingtime.test.advancedStatus")) return true;
        if (advancedCpu == null) {
            try {
                Class.forName("net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity");
                advancedCpu = Class.forName("com.ctux.ae2craftingtime.testdriver.AdvancedAeStatusFixture")
                        .getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | NoClassDefFoundError error) {
                throw new IllegalStateException("AdvancedAE status mode requires the AdvancedAE fixture", error);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("cannot create AdvancedAE status fixture", error);
            }
        }
        var provider = channelScenario ? cpuPosition : providerPosition;
        var advancedMarker = new FixtureMarker(1, "craft-plan", "ae2-crafting-time", marker.disposableWorldId(),
                new FixtureMarker.Position(provider.getX(), provider.getY(), provider.getZ(), "UP"), "minecraft:diamond");
        return (boolean) invokeAdvanced("prepare", new Class<?>[] { ServerPlayer.class, FixtureMarker.class },
                player, advancedMarker);
    }

    private void placeChannelNetwork(ServerPlayer player) {
        var level = player.level();
        PartHelper.setPart(level, cpuPosition.east(), null, player,
                AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
        place(player, cpuPosition.east(2), "controller");
        place(player, cpuPosition.east(2).above(), "creative_energy_cell");
        place(player, cpuPosition.east(2).south(), "drive");
        PartHelper.setPart(level, cpuPosition.east(2).south(2), null, player,
                AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
        healthyProviderPosition = cpuPosition.east(2).south(3);
        place(player, healthyProviderPosition, "pattern_provider");
        level.setBlockAndUpdate(healthyProviderPosition.south(), Blocks.CHEST.defaultBlockState());
        // A real path longer than the observation TTL leaves time to capture the booting checkpoint.
        for (int z = 1; z <= 28; z++) {
            PartHelper.setPart(level, cpuPosition.east(11).south(z), null, player,
                    AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
        }
        for (int x = 3; x <= 11; x++) {
            PartHelper.setPart(level, cpuPosition.east(x), null, player,
                    AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
            place(player, cpuPosition.east(x).north(), "pattern_provider");
            level.setBlockAndUpdate(cpuPosition.east(x).north(2), Blocks.CHEST.defaultBlockState());
        }
    }

    private boolean selectStarvedProvider(ServerPlayer player) {
        for (int x = 3; x <= 11; x++) {
            var pos = cpuPosition.east(x).north();
            var host = (IInWorldGridNodeHost) player.level().getBlockEntity(pos);
            var node = host.getGridNode(Direction.UP);
            if (node != null && node.isPowered() && node.hasGridBooted() && !node.meetsChannelRequirements()) {
                providerPosition = pos;
                return true;
            }
        }
        return false;
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
