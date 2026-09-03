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

/** Gives the copied Forge world a native CPU and supply before optional fixtures run. */
final class FabricBaseFixture extends NativeCpuFixture {
    private final boolean addCpu;
    private DriveBlockEntity drive;
    private IGrid grid;
    private int cellSlot;

    FabricBaseFixture(boolean addCpu) {
        this.addCpu = addCpu;
    }

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        // CPU-specific scenarios place their own block after the supply is ready.
        var placement = addCpu ? super.place(player, marker)
                : new Placement(null, new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z()));
        // Fabric level metadata has a different saved player position from the copied block layout.
        var face = Direction.valueOf(marker.terminal().face());
        player.teleportTo(placement.terminal().getX() + 0.5 + face.getStepX() * 2,
                placement.terminal().getY() - 1, placement.terminal().getZ() + 0.5 + face.getStepZ() * 2);
        var host = (IInWorldGridNodeHost) player.serverLevel().getBlockEntity(placement.terminal());
        grid = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst().orElseThrow();
        drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive()).findFirst().orElseThrow();
        var inventory = drive.getInternalInventory();
        for (var slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                inventory.setItemDirect(slot, AEItems.ITEM_CELL_1K.stack());
                drive.onChangeInventory(inventory, slot);
                cellSlot = slot;
                return placement;
            }
        }
        throw new IllegalStateException("Fabric fixture needs an empty drive slot");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (addCpu && !super.finish(player, placement)) return false;
        var cell = drive.getOriginalCellInventory(cellSlot);
        if (cell == null) return false;
        var source = IActionSource.ofPlayer(player);
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var missing = 64 - cell.extract(cobblestone, 64, Actionable.SIMULATE, source);
        if (missing > 0 && cell.insert(cobblestone, missing, Actionable.MODULATE, source) != missing) {
            throw new IllegalStateException("Fabric fixture rejected crafting ingredients");
        }
        grid.getStorageService().invalidateCache();
        return true;
    }
}
