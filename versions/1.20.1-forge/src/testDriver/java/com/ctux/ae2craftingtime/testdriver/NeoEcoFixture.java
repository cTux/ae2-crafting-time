package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import cn.dancingsnow.neoecoae.all.NEMultiBlocks;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationDriveBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.computation.ECOComputationSystemBlockEntity;
import cn.dancingsnow.neoecoae.multiblock.definition.MultiBlockContext;
import cn.dancingsnow.neoecoae.multiblock.placement.MultiBlockRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

final class NeoEcoFixture {
    private static final BlockPos INTERFACE_OFFSET = new BlockPos(2, 1, 1);

    private NeoEcoFixture() {
    }

    static Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var context = findSpace(level, terminal);

        context.blocks.forEach((pos, state) -> level.setBlockAndUpdate(pos, state));
        return new Placement(List.copyOf(context.blocks.keySet()));
    }

    static void finish(ServerPlayer player, Placement placement) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var controller = placement.blocks.stream()
                .map(level::getBlockEntity)
                .filter(ECOComputationSystemBlockEntity.class::isInstance)
                .map(ECOComputationSystemBlockEntity.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("NeoEco computation controller was not placed"));
        var min = new BlockPos(
                placement.blocks.stream().mapToInt(BlockPos::getX).min().orElseThrow(),
                placement.blocks.stream().mapToInt(BlockPos::getY).min().orElseThrow(),
                placement.blocks.stream().mapToInt(BlockPos::getZ).min().orElseThrow());
        var max = new BlockPos(
                placement.blocks.stream().mapToInt(BlockPos::getX).max().orElseThrow(),
                placement.blocks.stream().mapToInt(BlockPos::getY).max().orElseThrow(),
                placement.blocks.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
        controller.rebuildMultiblock();
        if (!controller.isFormed()) {
            var calculator = controller.getCalculator();
            var invalid = placement.blocks.stream()
                    .filter(pos -> !calculator.isValidBlockEntity(level.getBlockEntity(pos)))
                    .findFirst()
                    .map(pos -> pos + "=" + level.getBlockState(pos).getBlock())
                    .orElse("none");
            throw new IllegalStateException("NeoEco computation fixture did not form: scale="
                    + calculator.checkMultiblockScale(min, max) + ", structure="
                    + calculator.verifyInternalStructure(level, min, max) + ", invalid=" + invalid);
        }
        var cellItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("neoecoae", "eco_computation_cell_l9"));
        if (cellItem == null) {
            throw new IllegalStateException("NeoEco L9 computation cell is unavailable");
        }
        var cell = new ItemStack(cellItem);
        placement.blocks.stream()
                .map(level::getBlockEntity)
                .filter(ECOComputationDriveBlockEntity.class::isInstance)
                .map(ECOComputationDriveBlockEntity.class::cast)
                .forEach(drive -> drive.setCellStack(cell));
    }

    record Placement(List<BlockPos> blocks) {
    }

    private static BlueprintContext findSpace(Level level, BlockPos terminal) {
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
        for (BlockPos anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                var node = host.getGridNode(direction);
                if (node == null || node.getGrid() != grid) {
                    continue;
                }
                for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                    var interfaceOffset = MultiBlockRotation.localToWorld(
                            INTERFACE_OFFSET, BlockPos.ZERO, facing);
                    var candidate = blueprint(
                            level, anchor.relative(direction).subtract(interfaceOffset), facing);
                    if (candidate.blocks.keySet().stream().allMatch(pos -> level.getBlockState(pos).isAir())) {
                        return candidate;
                    }
                }
            }
        }
        throw new IllegalStateException("no empty space beside the fixture AE2 grid for NeoEco CPU");
    }

    private static BlueprintContext blueprint(Level level, BlockPos controllerPos, Direction facing) {
        var context = new BlueprintContext(level, controllerPos, facing);
        NEMultiBlocks.COMPUTATION_SYSTEM_L9.createLevel(context);
        return context;
    }

    private static final class BlueprintContext extends MultiBlockContext {
        private final Level level;
        private final BlockPos controllerPos;
        private final Direction facing;
        private final LinkedHashMap<BlockPos, BlockState> blocks = new LinkedHashMap<>();

        private BlueprintContext(Level level, BlockPos controllerPos, Direction facing) {
            this.level = level;
            this.controllerPos = controllerPos;
            this.facing = facing;
            repeats = 1;
        }

        @Override
        public void setBlock(BlockPos pos, BlockState blockState) {
            var target = MultiBlockRotation.localToWorld(pos, controllerPos, facing);
            blocks.put(target, MultiBlockRotation.rotateState(blockState, facing));
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
