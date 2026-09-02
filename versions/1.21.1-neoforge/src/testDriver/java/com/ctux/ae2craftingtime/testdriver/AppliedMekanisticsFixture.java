package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import me.ramidzkh.mekae2.AMItems;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Objects;

final class AppliedMekanisticsFixture extends AddonCpuFixture<AppliedMekanisticsFixture.Placement> {
    private static final long CHEMICAL_AMOUNT = 1_000_000;
    private static final long COBBLESTONE_AMOUNT = 64;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(player.serverLevel().getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Applied Mekanistics fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Applied Mekanistics fixture grid is unavailable"));
        var drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive())
                .findFirst().orElseThrow(() -> new IllegalStateException("Applied Mekanistics fixture drive is unavailable"));
        var inventory = drive.getInternalInventory();
        var slot = -1;
        for (var index = 0; index < inventory.size(); index++) {
            if (inventory.getStackInSlot(index).isEmpty()) {
                slot = index;
                break;
            }
        }
        if (slot < 0) {
            throw new IllegalStateException("Applied Mekanistics fixture drive has no empty slot");
        }
        var oxygen = MekanismAPI.CHEMICAL_REGISTRY.get(
                Objects.requireNonNull(ResourceLocation.tryBuild("mekanism", "oxygen")));
        if (oxygen == null || new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(oxygen), 1).isEmpty()) {
            throw new IllegalStateException("Mekanism oxygen is unavailable");
        }
        inventory.setItemDirect(slot, new ItemStack(AMItems.CHEMICAL_CELL_1K.get()));
        var itemSlot = -1;
        for (var index = 0; index < inventory.size(); index++) {
            if (inventory.getStackInSlot(index).isEmpty()) {
                itemSlot = index;
                break;
            }
        }
        if (itemSlot < 0) {
            throw new IllegalStateException("Applied Mekanistics fixture drive has no second empty slot");
        }
        inventory.setItemDirect(itemSlot, AEItems.ITEM_CELL_1K.stack());
        for (var anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(player.serverLevel().getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : new Direction[] { Direction.UP, Direction.DOWN }) {
                var node = host.getGridNode(direction);
                var position = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && player.serverLevel().getBlockState(position).isAir()) {
                    player.serverLevel().setBlockAndUpdate(position,
                            AEBlocks.CRAFTING_STORAGE_256K.block().defaultBlockState());
                    return new Placement(drive, slot, grid, position, terminal, IActionSource.ofPlayer(player),
                            MekanismKey.of(new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(oxygen), 1)));
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture grid for AppMek CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var storage = placement.drive().getOriginalCellInventory(placement.slot());
        if (storage == null) {
            return false;
        }
        var available = storage.extract(placement.oxygen(), CHEMICAL_AMOUNT, Actionable.SIMULATE,
                placement.source());
        var missing = CHEMICAL_AMOUNT - available;
        if (missing > 0 && storage.insert(placement.oxygen(), missing, Actionable.MODULATE,
                placement.source()) != missing) {
            throw new IllegalStateException("Applied Mekanistics chemical cell rejected oxygen");
        }
        placement.grid().getStorageService().invalidateCache();
        var gridStorage = placement.grid().getStorageService().getInventory();
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var availableCobblestone = gridStorage.extract(cobblestone, COBBLESTONE_AMOUNT, Actionable.SIMULATE,
                placement.source());
        var missingCobblestone = COBBLESTONE_AMOUNT - availableCobblestone;
        if (missingCobblestone > 0 && gridStorage.insert(cobblestone, missingCobblestone, Actionable.MODULATE,
                placement.source()) != missingCobblestone) {
            throw new IllegalStateException("Applied Mekanistics fixture grid rejected cobblestone");
        }
        var chemicalReady = gridStorage.extract(placement.oxygen(),
                CHEMICAL_AMOUNT, Actionable.SIMULATE, placement.source()) == CHEMICAL_AMOUNT;
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.cpu()) instanceof CraftingBlockEntity cpuStorage)) {
            throw new IllegalStateException("Applied Mekanistics fixture CPU was not placed");
        }
        if (!cpuStorage.getMainNode().isReady()) {
            return false;
        }
        if (!cpuStorage.isFormed()) {
            var calculator = new CraftingCPUCalculator(cpuStorage);
            calculator.updateBlockEntities(calculator.createCluster(level, placement.cpu(), placement.cpu()),
                    level, placement.cpu(), placement.cpu());
        }
        if (!(level.getBlockEntity(placement.terminal()) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Applied Mekanistics fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Applied Mekanistics fixture terminal node is unavailable"));
        var storageNode = cpuStorage.getMainNode().getNode();
        if (storageNode == null) {
            return false;
        }
        if (terminalNode.getGrid() != storageNode.getGrid()) {
            GridHelper.createConnection(terminalNode, storageNode);
        }
        return chemicalReady && cpuStorage.getCluster() != null && !cpuStorage.getCluster().isBusy();
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (!(player.serverLevel().getBlockEntity(placement.cpu()) instanceof CraftingBlockEntity storage)
                || storage.getCluster() == null) {
            return null;
        }
        var cpu = storage.getCluster();
        if (!cpu.isActive() || cpu.getGrid() != grid) {
            throw new IllegalStateException("Applied Mekanistics CPU is not selectable; active=" + cpu.isActive()
                    + " sameGrid=" + (cpu.getGrid() == grid));
        }
        return cpu;
    }

    record Placement(DriveBlockEntity drive, int slot, IGrid grid, BlockPos cpu, BlockPos terminal,
            IActionSource source, MekanismKey oxygen) {
    }
}
