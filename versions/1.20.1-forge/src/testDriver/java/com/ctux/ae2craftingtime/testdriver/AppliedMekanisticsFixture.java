package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.storage.DriveBlockEntity;
import me.ramidzkh.mekae2.AMItems;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

final class AppliedMekanisticsFixture extends AddonCpuFixture<AppliedMekanisticsFixture.Placement> {
    private static final long CHEMICAL_AMOUNT = 1_000_000;

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
        var oxygen = MekanismAPI.gasRegistry().getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild("mekanism", "oxygen")));
        if (oxygen == null || oxygen.isEmptyType()) {
            throw new IllegalStateException("Mekanism oxygen is unavailable");
        }
        inventory.setItemDirect(slot, new ItemStack(AMItems.CHEMICAL_CELL_1K.get()));
        drive.onChangeInventory(inventory, slot);
        return new Placement(drive, slot, grid, IActionSource.ofPlayer(player),
                MekanismKey.of(new GasStack(oxygen, 1)));
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
        return placement.grid().getStorageService().getInventory().extract(placement.oxygen(), CHEMICAL_AMOUNT,
                Actionable.SIMULATE, placement.source()) == CHEMICAL_AMOUNT;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return grid.getCraftingService().getCpus().stream().filter(cpu -> !cpu.isBusy()).findFirst().orElse(null);
    }

    record Placement(DriveBlockEntity drive, int slot, IGrid grid, IActionSource source, MekanismKey oxygen) {
    }
}
