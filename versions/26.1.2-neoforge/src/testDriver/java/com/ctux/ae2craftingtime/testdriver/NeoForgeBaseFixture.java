package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Builds the same native grid in every verified disposable 26.1.2 world. */
final class NeoForgeBaseFixture extends NativeCpuFixture {
    private boolean initialized;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var level = player.level();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        for (var position : BlockPos.betweenClosed(terminal.offset(-3, -2, -3), terminal.offset(5, 3, 3))) {
            level.setBlockAndUpdate(position, position.getY() == terminal.getY() - 2
                    ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(terminal.below(), AEBlocks.CONTROLLER.block().defaultBlockState());
        level.setBlockAndUpdate(terminal.below().west(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(terminal.west(2), AEBlocks.DRIVE.block().defaultBlockState());
        level.setBlockAndUpdate(terminal.east(2), AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
        level.setBlockAndUpdate(terminal.east(2).above(), AEBlocks.MOLECULAR_ASSEMBLER.block().defaultBlockState());
        level.setBlockAndUpdate(terminal.west(), AEBlocks.CRAFTING_STORAGE_1K.block().defaultBlockState());
        PartHelper.setPart(level, terminal, null, player, AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
        PartHelper.setPart(level, terminal, Direction.NORTH, player, AEParts.CRAFTING_TERMINAL.get());
        var drive = (DriveBlockEntity) level.getBlockEntity(terminal.west(2));
        drive.getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_1K.stack());
        drive.getInternalInventory().setItemDirect(1, AEItems.FLUID_CELL_1K.stack());
        player.teleportTo(terminal.getX() + 0.5, terminal.getY() - 1, terminal.getZ() - 2.5);
        return new Placement(terminal.west(), terminal);
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.level();
        var terminal = placement.terminal();
        var terminalNode = ((IInWorldGridNodeHost) level.getBlockEntity(terminal)).getGridNode(Direction.NORTH);
        if (terminalNode == null) return false;
        for (var position : java.util.List.of(terminal.below(), terminal.below().west(), terminal.west(2),
                terminal.east(2), terminal.west())) {
            var node = ((IInWorldGridNodeHost) level.getBlockEntity(position)).getGridNode(Direction.UP);
            if (node == null) return false;
            if (node.getGrid() != terminalNode.getGrid()) GridHelper.createConnection(terminalNode, node);
        }
        if (!super.finish(player, placement)) return false;
        var grid = terminalNode.getGrid();
        var drive = (DriveBlockEntity) level.getBlockEntity(terminal.west(2));
        if (!drive.getMainNode().isActive()) return false;
        var cell = drive.getOriginalCellInventory(0);
        if (cell == null) return false;
        var source = IActionSource.ofPlayer(player);
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var missing = 64 - cell.extract(cobblestone, 64, Actionable.SIMULATE, source);
        if (missing > 0 && cell.insert(cobblestone, missing, Actionable.MODULATE, source) != missing) {
            throw new IllegalStateException("Native fixture rejected crafting ingredients");
        }
        grid.getStorageService().invalidateCache();
        if (!initialized) {
            var provider = (appeng.blockentity.crafting.PatternProviderBlockEntity) level.getBlockEntity(terminal.east(2));
            var recipe = level.getServer().getRecipeManager().getRecipes()
                    .stream().filter(holder -> holder.id().identifier().toString().equals("minecraft:furnace"))
                    .findFirst().orElseThrow();
            var inputs = new net.minecraft.world.item.ItemStack[9];
            java.util.Arrays.setAll(inputs, index -> index == 4 ? net.minecraft.world.item.ItemStack.EMPTY
                    : new net.minecraft.world.item.ItemStack(Items.COBBLESTONE));
            provider.getLogic().getPatternInv().setItemDirect(0, appeng.api.crafting.PatternDetailsHelper.encodeCraftingPattern(
                    new net.minecraft.world.item.crafting.RecipeHolder<>(recipe.id(), (net.minecraft.world.item.crafting.CraftingRecipe) recipe.value()), inputs, new net.minecraft.world.item.ItemStack(Items.FURNACE), false, false));
            provider.getLogic().updatePatterns();
            var key = AEItemKey.of(Items.FURNACE);
            var tick = level.getGameTime();
            var network = com.ctux.ae2craftingtime.mc1201.ProfilerBridge.networkId(grid);
            com.ctux.ae2craftingtime.mc1201.ProfilerBridge.start(network, this, key, 1, tick);
            com.ctux.ae2craftingtime.mc1201.ProfilerBridge.complete(network, this, key, 1, tick + 40);
            initialized = true;
        }
        return grid.getCraftingService().isCraftable(AEItemKey.of(Items.FURNACE));
    }
}
