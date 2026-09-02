package com.ctux.ae2craftingtime.testdriver;

import appbot.ae2.ManaKey;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Arrays;
import java.util.Objects;

final class AppliedBotanicsFixture extends AddonCpuFixture<AppliedBotanicsFixture.Placement> {
    private final NativeCpuFixture nativeCpu = new NativeCpuFixture();

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var cpu = nativeCpu.place(player, marker);
        var host = (IInWorldGridNodeHost) player.serverLevel().getBlockEntity(cpu.terminal());
        var grid = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst().orElseThrow();
        var drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive()).findFirst().orElseThrow();
        var manaCell = BuiltInRegistries.ITEM.getOptional(
                Objects.requireNonNull(ResourceLocation.tryBuild("appbot", "mana_storage_cell_1k"))).orElse(null);
        if (manaCell == null || manaCell == Items.AIR) {
            throw new IllegalStateException("Applied Botanics mana cell is unavailable");
        }
        var inventory = drive.getInternalInventory();
        var cells = new ItemStack[] { new ItemStack(manaCell), AEItems.ITEM_CELL_1K.stack() };
        var mounted = 0;
        var manaSlot = -1;
        for (var slot = 0; slot < inventory.size() && mounted < cells.length; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                if (mounted == 0) {
                    manaSlot = slot;
                }
                inventory.setItemDirect(slot, cells[mounted++]);
                drive.onChangeInventory(inventory, slot);
            }
        }
        if (mounted != cells.length) {
            throw new IllegalStateException("Applied Botanics fixture needs two empty drive slots");
        }
        return new Placement(cpu, drive, manaSlot, grid);
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var cell = placement.drive().getOriginalCellInventory(placement.manaSlot());
        if (cell == null) {
            return false;
        }
        var source = IActionSource.ofPlayer(player);
        var amount = 999L;
        var missing = amount - cell.extract(ManaKey.KEY, amount, Actionable.SIMULATE, source);
        if (missing > 0 && cell.insert(ManaKey.KEY, missing, Actionable.MODULATE, source) != missing) {
            throw new IllegalStateException("Applied Botanics cell rejected mana");
        }
        placement.grid().getStorageService().invalidateCache();
        var storage = placement.grid().getStorageService().getInventory();
        if (storage.extract(ManaKey.KEY, amount, Actionable.SIMULATE, source) != amount
                || AeKeyAmounts.normalize(ManaKey.KEY, amount) != amount
                || AeKeyAmounts.unit(ManaKey.KEY) != ProfileUnit.MANA) {
            throw new IllegalStateException("Applied Botanics raw mana contract failed");
        }
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var needed = 64 - storage.extract(cobblestone, 64, Actionable.SIMULATE, source);
        if (needed > 0 && storage.insert(cobblestone, needed, Actionable.MODULATE, source) != needed) {
            throw new IllegalStateException("Applied Botanics fixture rejected crafting ingredients");
        }
        return nativeCpu.finish(player, placement.cpu());
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return nativeCpu.cpu(player, placement.cpu(), grid);
    }

    record Placement(NativeCpuFixture.Placement cpu, DriveBlockEntity drive, int manaSlot, IGrid grid) {
    }
}
