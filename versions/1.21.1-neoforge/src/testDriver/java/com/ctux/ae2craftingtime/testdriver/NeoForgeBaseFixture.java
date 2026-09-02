package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Objects;

/** Prepares the native 1.21.1 disposable world through current AE2 APIs. */
final class NeoForgeBaseFixture extends NativeCpuFixture {
    private DriveBlockEntity drive;
    private IGrid grid;
    private int cellSlot;
    private boolean initialized;


    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        // CPU-specific scenarios place their own block after the supply is ready.
        var placement = super.place(player, marker);
        var host = (IInWorldGridNodeHost) player.serverLevel().getBlockEntity(placement.terminal());
        grid = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst().orElseThrow();
        drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive()).findFirst().orElseThrow();
        var inventory = drive.getInternalInventory();
        for (var slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                inventory.setItemDirect(slot, AEItems.ITEM_CELL_1K.stack());
                cellSlot = slot;
                return placement;
            }
        }
        throw new IllegalStateException("NeoForge fixture needs an empty drive slot");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!super.finish(player, placement)) return false;
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
