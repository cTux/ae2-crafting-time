package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.Direction;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ExtendedAeFixture extends AddonCpuFixture<List<BlockPos>> {
    @Override
    protected List<BlockPos> place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var ae2Assembler = BuiltInRegistries.BLOCK.getOptional(
                Objects.requireNonNull(ResourceLocation.tryBuild("ae2", "molecular_assembler"))).orElse(null);
        var extendedAssembler = BuiltInRegistries.BLOCK.getOptional(
                Objects.requireNonNull(ResourceLocation.tryBuild(DriverPlatform.EXTENDED_AE_ID, "ex_molecular_assembler"))).orElse(null);
        if (ae2Assembler == null || extendedAssembler == null) {
            throw new IllegalStateException("ExtendedAE assembler blocks are unavailable");
        }
        var replaced = new ArrayList<BlockPos>();
        for (var position : BlockPos.betweenClosed(terminal.offset(-24, -8, -24), terminal.offset(24, 8, 24))) {
            if (level.getBlockState(position).is(ae2Assembler)) {
                var immutable = position.immutable();
                level.setBlockAndUpdate(immutable, extendedAssembler.defaultBlockState());
                replaced.add(immutable);
            }
        }
        if (replaced.isEmpty()) {
            throw new IllegalStateException("fixture has no AE2 molecular assemblers to replace");
        }
        return List.copyOf(replaced);
    }

    @Override
    protected boolean finish(ServerPlayer player, List<BlockPos> positions) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        for (var position : positions) {
            if (!(level.getBlockEntity(position) instanceof IInWorldGridNodeHost assembler)) {
                throw new IllegalStateException("ExtendedAE assembler was not placed at " + position);
            }
            if (Arrays.stream(Direction.values()).map(assembler::getGridNode)
                    .filter(Objects::nonNull).noneMatch(node -> node.isActive())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, List<BlockPos> positions, IGrid grid) {
        return grid.getCraftingService().getCpus().stream()
                .filter(candidate -> !candidate.isBusy())
                .findFirst().orElse(null);
    }
}
