package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;


/** Prepares the native 1.21.1 disposable world through current AE2 APIs. */
final class NeoForgeBaseFixture extends NativeCpuFixture {
    private DriveBlockEntity drive;
    private IGrid grid;
    private int cellSlot;
    private boolean initialized;


    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var level = player.serverLevel();
        var destination = new BlockPos(marker.terminal().x() - 2, marker.terminal().y() - 2, marker.terminal().z());
        var template = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
        template.fillFromWorld(level, new BlockPos(0, 83, 44), new net.minecraft.core.Vec3i(5, 3, 2), false, null);
        for (var position : BlockPos.betweenClosed(destination, destination.offset(4, 2, 1))) {
            if (!level.isEmptyBlock(position)) throw new IllegalStateException("Native smoke grid needs open space");
        }
        if (!template.placeInWorld(level, destination, destination,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                level.random, 3)) throw new IllegalStateException("Cannot copy the native smoke grid");
        for (var position : BlockPos.betweenClosed(destination.offset(-1, -1, -4), destination.offset(5, -1, 2))) {
            level.setBlockAndUpdate(position, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
        }
        player.teleportTo(destination.getX() + 2.5, destination.getY(), destination.getZ() - 1.5);
        var placement = new Placement(destination.offset(0, 1, 1), destination.offset(2, 2, 0));
        drive = (DriveBlockEntity) level.getBlockEntity(destination.above());
        var inventory = drive.getInternalInventory();
        for (var slot = 0; slot < inventory.size(); slot++) {
            if (slot + 1 < inventory.size() && inventory.getStackInSlot(slot).isEmpty()
                    && inventory.getStackInSlot(slot + 1).isEmpty()) {
                inventory.setItemDirect(slot, AEItems.ITEM_CELL_1K.stack());
                inventory.setItemDirect(slot + 1, AEItems.FLUID_CELL_1K.stack());
                cellSlot = slot;
                return placement;
            }
        }
        throw new IllegalStateException("NeoForge fixture needs an empty drive slot");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!super.finish(player, placement)) return false;
        grid = drive.getMainNode().getGrid();
        if (grid == null || !drive.getMainNode().isActive()) return false;
        var cell = drive.getOriginalCellInventory(cellSlot);
        if (cell == null) return false;
        var source = IActionSource.ofPlayer(player);
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var missing = 64 - cell.extract(cobblestone, 64, Actionable.SIMULATE, source);
        if (missing > 0 && cell.insert(cobblestone, missing, Actionable.MODULATE, source) != missing) {
            throw new IllegalStateException("NeoForge fixture rejected crafting ingredients");
        }
        grid.getStorageService().invalidateCache();
        if (!initialized) {
            var level = player.serverLevel();
            var position = placement.terminal().east(2);
            var provider = (appeng.blockentity.crafting.PatternProviderBlockEntity) level.getBlockEntity(position);
            level.setBlockAndUpdate(position.above(), appeng.core.definitions.AEBlocks.MOLECULAR_ASSEMBLER.block().defaultBlockState());
            var recipe = level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)
                    .stream().filter(holder -> holder.id().toString().equals("minecraft:furnace"))
                    .findFirst().orElseThrow();
            var inputs = new net.minecraft.world.item.ItemStack[9];
            java.util.Arrays.setAll(inputs, index -> index == 4 ? net.minecraft.world.item.ItemStack.EMPTY
                    : new net.minecraft.world.item.ItemStack(Items.COBBLESTONE));
            provider.getLogic().getPatternInv().setItemDirect(0, appeng.api.crafting.PatternDetailsHelper.encodeCraftingPattern(
                    recipe, inputs, new net.minecraft.world.item.ItemStack(Items.FURNACE), false, false));
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
