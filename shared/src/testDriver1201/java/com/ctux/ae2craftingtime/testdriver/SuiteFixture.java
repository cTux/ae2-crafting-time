package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeSavedData;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

/** Pristine bounded fixture state, accessed only on the integrated server thread. */
final class SuiteFixture {
    private final ServerLevel level;
    private final BlockPos min;
    private final BlockPos max;
    private final StructureTemplate blocks = new StructureTemplate();
    private final boolean emptyBlocks;
    private final Ae2CraftingTimeSavedData data;
    private final List<com.ctux.ae2craftingtime.core.PersistedOutputSamples> samples;
    private final List<ItemStack> inventory = new ArrayList<>();
    private final double x, y, z;
    private final float yaw, pitch;

    SuiteFixture(ServerLevel level, ServerPlayer player, FixtureMarker marker) {
        this.level = level;
        var terminal = marker.terminal();
        // Covers the base/addon grid, dispatch grid (+40), and standard grid (+60).
        min = new BlockPos(terminal.x() - 32, Math.max(-64, terminal.y() - 12), terminal.z() - 32);
        max = new BlockPos(terminal.x() + 80, terminal.y() + 12, terminal.z() + 32);
        DriverPlatform.captureSuite(blocks, level, min, new Vec3i(max.getX() - min.getX() + 1,
                max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1));
        emptyBlocks = BlockPos.betweenClosedStream(min, max)
                .allMatch(position -> level.getBlockState(position).is(Blocks.AIR));
        System.out.println("AE2CT suite fixture-captured emptyBlocks=" + emptyBlocks + " utc=" + java.time.Instant.now());
        data = DriverPlatform.suiteSavedData(level);
        samples = List.copyOf(data.samples());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            inventory.add(player.getInventory().getItem(slot).copy());
        }
        x = player.getX(); y = player.getY(); z = player.getZ();
        yaw = player.getYRot(); pitch = player.getXRot();
    }

    void restore(ServerPlayer player) {
        player.closeContainer();
        // Removing every node destroys old CPU jobs before their replacement grid is placed.
        for (var position : BlockPos.betweenClosed(min, max)) {
            if (!level.isEmptyBlock(position)) {
                level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            }
        }
        for (var entity : level.getEntities(player, new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1))) {
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) entity.discard();
        }
        // Native templates reject an empty palette; clearing already restores an all-air fixture.
        if (!emptyBlocks && !blocks.placeInWorld(level, min, min, new StructurePlaceSettings(), level.getRandom(), 3)) {
            throw new IllegalStateException("Cannot restore suite fixture");
        }
        data.replaceFrom(samples);
        data.replaceProviderStarts(List.of());
        data.replaceProviderRecords(List.of());
        data.replaceStatuses(List.of());
        ProfilerBridge.load(data);
        player.getInventory().clearContent();
        for (int slot = 0; slot < inventory.size(); slot++) {
            player.getInventory().setItem(slot, inventory.get(slot).copy());
        }
        player.teleportTo(x, y, z);
        player.setYRot(yaw); player.setXRot(pitch);
        player.inventoryMenu.broadcastChanges();
    }
}
