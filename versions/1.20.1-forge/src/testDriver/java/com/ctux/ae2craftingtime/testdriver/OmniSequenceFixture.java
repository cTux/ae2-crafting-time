package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import com.atir.molecularmanipulator.blockentity.OmniComputationCoreBlockEntity;
import com.atir.molecularmanipulator.blockentity.OmniComputationStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

final class OmniSequenceFixture extends AddonCpuFixture<OmniSequenceFixture.Placement> {
    private static final List<Integer> DISTANCES = List.of(40, 56, 72);

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var placement = findSpace(level, terminal);
        placement.blocks.forEach((pos, state) -> level.setBlock(pos, state, 2));
        return placement;
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.controller) instanceof OmniComputationCoreBlockEntity core)) {
            throw new IllegalStateException("OmniSequence computation controller was not placed");
        }
        if (!core.getMainNode().isReady()) {
            return false;
        }
        core.refreshStructureNow();
        if (!core.isStructureFormed()) {
            var inspection = OmniComputationStructure.inspect(level, placement.controller, placement.facing);
            throw new IllegalStateException("OmniSequence computation fixture did not form: correct="
                    + inspection.correct() + ", missing=" + inspection.missing()
                    + ", conflicts=" + inspection.conflicts());
        }
        if (!(level.getBlockEntity(placement.terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("OmniSequence fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OmniSequence fixture terminal node is unavailable"));
        var coreNode = core.getMainNode().getNode();
        if (coreNode == null) {
            throw new IllegalStateException("OmniSequence computation node is unavailable");
        }
        if (terminalNode.getGrid() != coreNode.getGrid()) {
            GridHelper.createConnection(terminalNode, coreNode);
        }
        core.serverTick();
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (player == null
                || !(player.serverLevel().getBlockEntity(placement.controller)
                        instanceof OmniComputationCoreBlockEntity core)) {
            return null;
        }
        core.serverTick();
        return core.allCpus().stream()
                .filter(candidate -> candidate.isActive() && candidate.getGrid() == grid)
                .findFirst().orElse(null);
    }

    private static Placement findSpace(net.minecraft.server.level.ServerLevel level, BlockPos terminal) {
        for (int distance : DISTANCES) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var controller = terminal.relative(facing, distance).above(2);
                var blocks = new LinkedHashMap<BlockPos, BlockState>();
                boolean clear = true;
                for (var part : OmniComputationStructure.parts()) {
                    var pos = OmniComputationStructure.worldPos(controller, facing, part);
                    if (!level.getBlockState(pos).isAir()) {
                        clear = false;
                        break;
                    }
                    if (part.type() != OmniComputationStructure.PartType.AIR) {
                        var state = OmniComputationStructure.block(part.type()).defaultBlockState();
                        if (part.type() == OmniComputationStructure.PartType.CONTROLLER) {
                            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
                        }
                        blocks.put(pos.immutable(), state);
                    }
                }
                if (clear) {
                    return new Placement(controller, terminal, facing, blocks);
                }
            }
        }
        throw new IllegalStateException("no empty space near the fixture for OmniSequence CPU");
    }

    record Placement(BlockPos controller, BlockPos terminal, Direction facing,
            LinkedHashMap<BlockPos, BlockState> blocks) {
    }
}
