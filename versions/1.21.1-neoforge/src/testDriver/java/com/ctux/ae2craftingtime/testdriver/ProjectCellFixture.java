package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import com.gmr.projectcell.ProjectCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Objects;

final class ProjectCellFixture extends AddonCpuFixture<ProjectCellFixture.Placement> {
    private static final long COBBLESTONE_COUNT = 64;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(player.serverLevel().getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("ProjectCell fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("ProjectCell fixture grid is unavailable"));
        var drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive())
                .findFirst().orElseThrow(() -> new IllegalStateException("ProjectCell fixture drive is unavailable"));
        var inventory = drive.getInternalInventory();
        var slot = -1;
        for (var index = 0; index < inventory.size(); index++) {
            if (inventory.getStackInSlot(index).isEmpty()) {
                slot = index;
                break;
            }
        }
        if (slot < 0) {
            throw new IllegalStateException("ProjectCell fixture drive has no empty slot");
        }

        var source = IActionSource.ofPlayer(player);
        var cobblestone = AEItemKey.of(Blocks.COBBLESTONE);
        grid.getStorageService().getInventory().extract(cobblestone, Long.MAX_VALUE, Actionable.MODULATE, source);
        var cell = new ItemStack(ProjectCell.EMC_STORAGE_CELL.get());
        cell.set(ProjectCell.OWNER_UUID.get(), player.getUUID());
        inventory.setItemDirect(slot, cell);
        drive.onChangeInventory((appeng.util.inv.AppEngInternalInventory) inventory, slot);
        return new Placement(drive, slot, grid, source, cobblestone);
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var storage = placement.drive().getOriginalCellInventory(placement.slot());
        if (storage == null) {
            return false;
        }
        var available = storage.extract(placement.cobblestone(), COBBLESTONE_COUNT, Actionable.SIMULATE,
                placement.source());
        if (available < COBBLESTONE_COUNT) {
            var required = COBBLESTONE_COUNT - available;
            if (storage.insert(placement.cobblestone(), required, Actionable.MODULATE,
                    placement.source()) != required) {
                throw new IllegalStateException("ProjectCell EMC Storage Cell rejected cobblestone");
            }
            placement.grid().getStorageService().invalidateCache();
        }
        return placement.grid().getStorageService().getInventory().extract(placement.cobblestone(),
                COBBLESTONE_COUNT, Actionable.SIMULATE, placement.source()) == COBBLESTONE_COUNT;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return grid.getCraftingService().getCpus().stream().filter(cpu -> !cpu.isBusy()).findFirst().orElse(null);
    }

    record Placement(DriveBlockEntity drive, int slot, IGrid grid, IActionSource source, AEItemKey cobblestone) {
    }
}
