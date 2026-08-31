package com.ctux.ae2craftingtime.testdriver;

import cn.dancingsnow.neoecoae.all.NEMultiBlocks;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationDriveBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.definition.MultiBlockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;

final class NeoEcoFixture {
    private static final BlockPos INTERFACE_OFFSET = new BlockPos(2, 1, 1);

    private NeoEcoFixture() {
    }

    static void prepare(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var origins = List.of(
                terminal.west().subtract(INTERFACE_OFFSET),
                terminal.north().subtract(INTERFACE_OFFSET));
        var context = origins.stream()
                .map(origin -> blueprint(level, origin))
                .filter(candidate -> candidate.blocks.keySet().stream()
                        .allMatch(pos -> level.getBlockState(pos).isAir()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no empty space beside fixture terminal for NeoEco CPU"));

        context.blocks.forEach((pos, state) -> level.setBlockAndUpdate(pos, state));
        var cell = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("neoecoae", "eco_computation_cell_l9")));
        context.blocks.keySet().stream()
                .map(level::getBlockEntity)
                .filter(ECOComputationDriveBlockEntity.class::isInstance)
                .map(ECOComputationDriveBlockEntity.class::cast)
                .forEach(drive -> drive.setCellStack(cell));
    }

    private static BlueprintContext blueprint(Level level, BlockPos origin) {
        var context = new BlueprintContext(level, origin);
        NEMultiBlocks.COMPUTATION_SYSTEM_L9.createLevel(context);
        return context;
    }

    private static final class BlueprintContext extends MultiBlockContext {
        private final Level level;
        private final BlockPos origin;
        private final LinkedHashMap<BlockPos, BlockState> blocks = new LinkedHashMap<>();

        private BlueprintContext(Level level, BlockPos origin) {
            this.level = level;
            this.origin = origin;
            repeats = 1;
        }

        @Override
        public void setBlock(BlockPos pos, BlockState blockState) {
            blocks.put(origin.offset(pos), blockState);
        }

        @Override
        public void setBlockEntity(BlockPos pos, BiFunction<BlockPos, BlockState, BlockEntity> factory) {
            throw new UnsupportedOperationException("NeoEco computation blueprint unexpectedly supplied a block entity");
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public List<BlockPos> allBlocks() {
            return List.copyOf(blocks.keySet());
        }

        @Override
        public boolean isFormed() {
            return false;
        }
    }
}
