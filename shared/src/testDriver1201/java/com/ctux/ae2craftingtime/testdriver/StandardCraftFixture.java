package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEParts;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/** Two actual vanilla smelters; the fixture supplies fuel and imports their output. */
final class StandardCraftFixture {
    BlockPos terminal;
    private boolean initialized;

    boolean prepare(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        if (terminal == null) {
            terminal = new BlockPos(marker.terminal().x() + 60, marker.terminal().y(), marker.terminal().z());
            for (var pos : BlockPos.betweenClosed(terminal.offset(-3, -2, -3), terminal.offset(9, 3, 3))) {
                level.setBlockAndUpdate(pos, pos.getY() == terminal.getY() - 2
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            PartHelper.setPart(level, terminal, null, player, AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
            PartHelper.setPart(level, terminal, Direction.NORTH, player, AEParts.CRAFTING_TERMINAL.get());
            DispatchStatusFixture.place(player, terminal.west(2), "16k_crafting_storage");
            DispatchStatusFixture.place(player, terminal.east(2), "drive");
            DispatchStatusFixture.place(player, terminal.below(), "creative_energy_cell");
            for (int offset : new int[] {4, 8}) {
                DispatchStatusFixture.place(player, terminal.east(offset), "pattern_provider");
                level.setBlockAndUpdate(terminal.east(offset).below(), Blocks.FURNACE.defaultBlockState());
            }
            player.teleportTo(terminal.getX() + 0.5, terminal.getY() - 1, terminal.getZ() - 2.5);
            return false;
        }
        var node = ((IInWorldGridNodeHost) level.getBlockEntity(terminal)).getGridNode(Direction.NORTH);
        if (node == null) return false;
        for (var pos : java.util.List.of(terminal.west(2), terminal.east(2), terminal.below(), terminal.east(4), terminal.east(8))) {
            var other = ((IInWorldGridNodeHost) level.getBlockEntity(pos)).getGridNode(Direction.UP);
            if (other == null) return false;
            if (node.getGrid() != other.getGrid()) GridHelper.createConnection(node, other);
        }
        var cpu = cpu(player);
        if (!cpu.isFormed()) {
            var calculator = new CraftingCPUCalculator(cpu);
            var pos = terminal.west(2);
            calculator.updateBlockEntities(calculator.createCluster(level, pos, pos), level, pos, pos);
        }
        if (!cpu.getCluster().isActive()) return false;
        if (!initialized) {
            var drive = (DriveBlockEntity) level.getBlockEntity(terminal.east(2));
            drive.getInternalInventory().setItemDirect(0, appeng.core.definitions.AEItems.ITEM_CELL_1K.stack());
            drive.getCellInventory(0).insert(AEItemKey.of(Items.COBBLESTONE), 2, Actionable.MODULATE, IActionSource.empty());
            pattern(player, 4, Items.COBBLESTONE, Items.STONE);
            pattern(player, 8, Items.STONE, Items.SMOOTH_STONE);
            seed(player);
            initialized = true;
        }
        return node.getGrid().getCraftingService().isCraftable(AEItemKey.of(Items.SMOOTH_STONE));
    }

    void seed(ServerPlayer player) {
        var grid = cpu(player).getMainNode().getGrid();
        var network = ProfilerBridge.networkId(grid);
        var tick = player.serverLevel().getGameTime();
        for (var item : java.util.List.of(Items.STONE, Items.SMOOTH_STONE)) {
            var key = AEItemKey.of(item);
            ProfilerBridge.start(network, this, key, 1, tick);
            ProfilerBridge.complete(network, this, key, 1, tick + (item == Items.STONE ? 100 : 40));
        }
    }

    private void pattern(ServerPlayer player, int offset, net.minecraft.world.item.Item input, net.minecraft.world.item.Item output) {
        var provider = (PatternProviderBlockEntity) player.serverLevel().getBlockEntity(terminal.east(offset));
        provider.getLogic().getPatternInv().setItemDirect(0, DriverPlatform.processingPattern(
                new GenericStack(AEItemKey.of(input), 1), new GenericStack(AEItemKey.of(output), 1)));
        provider.getLogic().updatePatterns();
    }

    CraftingBlockEntity cpu(ServerPlayer player) {
        return (CraftingBlockEntity) player.serverLevel().getBlockEntity(terminal.west(2));
    }

    long pump(ServerPlayer player, boolean fuel) {
        var storage = cpu(player).getMainNode().getGrid().getStorageService().getInventory();
        for (int offset : new int[] {4, 8}) {
            var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(terminal.east(offset).below());
            if (fuel && furnace.getItem(1).isEmpty()) furnace.setItem(1, new ItemStack(Items.COAL));
            var output = furnace.getItem(2);
            if (!output.isEmpty()) {
                long inserted = storage.insert(AEItemKey.of(output), output.getCount(), Actionable.MODULATE, IActionSource.ofMachine(cpu(player)));
                furnace.removeItem(2, (int) inserted);
                furnace.setChanged();
            }
        }
        return storage.extract(AEItemKey.of(Items.SMOOTH_STONE), Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty());
    }
}
