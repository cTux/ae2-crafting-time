package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCalculator;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;

import java.util.Arrays;
import java.util.Objects;

final class AdvancedAeFixture {
    private AdvancedAeFixture() {
    }

    static Placement place(ServerPlayer player, FixtureMarker marker) {
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
        var structure = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild("advanced_ae", "quantum_structure"));
        if (core == null || structure == null) {
            throw new IllegalStateException("AdvancedAE quantum computer blocks are unavailable");
        }
        for (BlockPos anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : Direction.values()) {
                var node = host.getGridNode(direction);
                var min = minimum(anchor, direction);
                if (node != null && node.getGrid() == grid
                        && BlockPos.betweenClosedStream(min, min.offset(2, 2, 2))
                                .allMatch(pos -> level.getBlockState(pos).isAir())) {
                    for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(2, 2, 2))) {
                        if (!pos.equals(min.offset(1, 1, 1))) {
                            level.setBlockAndUpdate(pos, structure.defaultBlockState());
                        }
                    }
                    var corePosition = min.offset(1, 1, 1);
                    level.setBlockAndUpdate(corePosition, core.defaultBlockState());
                    return new Placement(min, min.offset(2, 2, 2), corePosition);
                }
            }
        }
        throw new IllegalStateException("no empty space beside the fixture AE2 grid for AdvancedAE CPU");
    }

    static void finish(ServerPlayer player, Placement placement) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var position = placement.core();
        var level = player.serverLevel();
        if (!(level.getBlockEntity(position) instanceof AdvCraftingBlockEntity blockEntity)) {
            throw new IllegalStateException("AdvancedAE quantum core block entity was not placed");
        }
        if (!blockEntity.isFormed()) {
            var calculator = new AdvCraftingCPUCalculator(blockEntity);
            if (!calculator.checkMultiblockScale(placement.min(), placement.max())
                    || !calculator.verifyInternalStructure(level, placement.min(), placement.max())) {
                throw new IllegalStateException("AdvancedAE quantum computer fixture is invalid");
            }
            calculator.updateBlockEntities(
                    calculator.createCluster(level, placement.min(), placement.max()),
                    level, placement.min(), placement.max());
        }
        if (!blockEntity.isFormed()) {
            throw new IllegalStateException("AdvancedAE quantum core did not form");
        }
    }

    static ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (player == null
                || !(player.serverLevel().getBlockEntity(placement.core()) instanceof AdvCraftingBlockEntity core)
                || core.getCluster() == null) {
            return null;
        }
        var cpu = core.getCluster().getRemainingCapacityCPU();
        return cpu.isActive() && cpu.getGrid() == grid ? cpu : null;
    }

    record Placement(BlockPos min, BlockPos max, BlockPos core) {
    }

    private static BlockPos minimum(BlockPos anchor, Direction direction) {
        return switch (direction) {
            case EAST -> anchor.offset(1, -1, -1);
            case WEST -> anchor.offset(-3, -1, -1);
            case UP -> anchor.offset(-1, 1, -1);
            case DOWN -> anchor.offset(-1, -3, -1);
            case SOUTH -> anchor.offset(-1, -1, 1);
            case NORTH -> anchor.offset(-1, -1, -3);
        };
    }
}
