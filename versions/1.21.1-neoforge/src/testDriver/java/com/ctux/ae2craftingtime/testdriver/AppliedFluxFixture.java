package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.storage.DriveBlockEntity;
import com.glodblock.github.appflux.common.AFSingletons;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

final class AppliedFluxFixture extends AddonCpuFixture<AppliedFluxFixture.Placement> {
    private static final long ENERGY_AMOUNT = 1_000;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(player.serverLevel().getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("Applied Flux fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("Applied Flux fixture grid is unavailable"));
        var drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive())
                .findFirst().orElseThrow(() -> new IllegalStateException("Applied Flux fixture drive is unavailable"));
        var inventory = drive.getInternalInventory();
        var slot = -1;
        for (var index = 0; index < inventory.size(); index++) {
            if (inventory.getStackInSlot(index).isEmpty()) {
                slot = index;
                break;
            }
        }
        if (slot < 0) {
            throw new IllegalStateException("Applied Flux fixture drive has no empty slot");
        }
        inventory.setItemDirect(slot, new ItemStack(AFSingletons.FE_CELL_1k));
        return new Placement(drive, slot, grid, IActionSource.ofPlayer(player), FluxKey.of(EnergyType.FE));
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var storage = placement.drive().getOriginalCellInventory(placement.slot());
        if (storage == null) {
            return false;
        }
        var available = storage.extract(placement.energy(), ENERGY_AMOUNT, Actionable.SIMULATE, placement.source());
        var missing = ENERGY_AMOUNT - available;
        if (missing > 0 && storage.insert(placement.energy(), missing, Actionable.MODULATE,
                placement.source()) != missing) {
            throw new IllegalStateException("Applied Flux FE cell rejected energy");
        }
        placement.grid().getStorageService().invalidateCache();
        return placement.grid().getStorageService().getInventory().extract(placement.energy(), ENERGY_AMOUNT,
                Actionable.SIMULATE, placement.source()) == ENERGY_AMOUNT;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return grid.getCraftingService().getCpus().stream().filter(cpu -> !cpu.isBusy()).findFirst().orElse(null);
    }

    record Placement(DriveBlockEntity drive, int slot, IGrid grid, IActionSource source, FluxKey energy) {
    }
}
