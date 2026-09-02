package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import io.github.projectet.ae2things.storage.DISKCellInventory;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Objects;

final class Ae2ThingsFixture extends AddonCpuFixture<Ae2ThingsFixture.Placement> {
    private final NativeCpuFixture nativeCpu = new NativeCpuFixture();

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var cpu = nativeCpu.place(player, marker);
        var host = (IInWorldGridNodeHost) player.serverLevel().getBlockEntity(cpu.terminal());
        var grid = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst().orElseThrow();
        var drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive()).findFirst().orElseThrow();
        var source = IActionSource.ofPlayer(player);
        grid.getStorageService().getInventory().extract(AEItemKey.of(Items.COBBLESTONE),
                Long.MAX_VALUE, Actionable.MODULATE, source);
        var inventory = drive.getInternalInventory();
        for (var slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                inventory.setItemDirect(slot, new ItemStack(BuiltInRegistries.ITEM.getOptional(new ResourceLocation("ae2things", "disk_drive_1k")).orElseThrow()));
                drive.onChangeInventory(inventory, slot);
                return new Placement(cpu, drive, slot, grid);
            }
        }
        throw new IllegalStateException("AE2 Things fixture needs an empty drive slot");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!nativeCpu.finish(player, placement.cpu())) {
            return false;
        }
        var cell = placement.drive().getOriginalCellInventory(placement.slot());
        if (cell == null) {
            return false;
        }
        if (!(cell instanceof DISKCellInventory)) {
            throw new IllegalStateException("AE2 Things DISK inventory was not mounted");
        }
        var source = IActionSource.ofPlayer(player);
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var missing = 64 - cell.extract(cobblestone, 64, Actionable.SIMULATE, source);
        if (missing > 0 && cell.insert(cobblestone, missing, Actionable.MODULATE, source) != missing) {
            throw new IllegalStateException("AE2 Things DISK rejected crafting ingredients");
        }
        placement.grid().getStorageService().invalidateCache();
        return placement.grid().getStorageService().getInventory()
                .extract(cobblestone, 64, Actionable.SIMULATE, source) == 64;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return nativeCpu.cpu(player, placement.cpu(), grid);
    }

    record Placement(NativeCpuFixture.Placement cpu, DriveBlockEntity drive, int slot, IGrid grid) {
    }
}
