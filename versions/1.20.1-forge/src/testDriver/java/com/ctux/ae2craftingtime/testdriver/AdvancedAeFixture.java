package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;

import java.util.Arrays;
import java.util.Objects;

final class AdvancedAeFixture {
    private AdvancedAeFixture() {
    }

    static BlockPos place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("fixture terminal is not connected to an AE2 grid");
        }
        IGrid grid = Arrays.stream(Direction.values())
                .map(terminalHost::getGridNode)
                .filter(Objects::nonNull)
                .map(node -> node.getGrid())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture terminal grid is unavailable"));
        var core = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild("advanced_ae", "quantum_core"));
        if (core == null) {
            throw new IllegalStateException("AdvancedAE quantum core is unavailable");
        }
        for (BlockPos anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : new Direction[] { Direction.UP, Direction.DOWN }) {
                var node = host.getGridNode(direction);
                var target = anchor.relative(direction);
                if (node != null && node.getGrid() == grid && level.getBlockState(target).isAir()) {
                    level.setBlockAndUpdate(target, core.defaultBlockState());
                    return target;
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture AE2 grid for AdvancedAE CPU");
    }

    static void finish(ServerPlayer player, BlockPos position) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        if (!(player.serverLevel().getBlockEntity(position) instanceof AdvCraftingBlockEntity blockEntity)) {
            throw new IllegalStateException("AdvancedAE quantum core block entity was not placed");
        }
        if (!blockEntity.isFormed()) {
            blockEntity.updateMultiBlock(position);
        }
        if (!blockEntity.isFormed()) {
            throw new IllegalStateException("AdvancedAE quantum core did not form");
        }
    }
}
