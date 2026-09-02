package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import com.moakiee.ae2lt.block.TianshuSupercomputerControllerBlock;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.logic.tianshu.TianshuAutoBuildPlan;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockComponent;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanner;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

final class LightningTechFixture extends AddonCpuFixture<LightningTechFixture.Placement> {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        for (int distance : List.of(40, 56, 72)) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var controller = terminal.relative(facing, distance).above(2);
                var blocks = new LinkedHashMap<BlockPos, BlockState>();
                blocks.put(controller, ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get().defaultBlockState()
                        .setValue(TianshuSupercomputerControllerBlock.FACING, facing));
                for (var part : TianshuAutoBuildPlan.create(pos -> TianshuMultiblockComponent.AIR).placements()) {
                    var block = switch (part.target()) {
                        case CASING -> ModBlocks.TIANSHU_SUPERCOMPUTER_CASING.get();
                        case COOLING -> ModBlocks.PHASE_CHANGE_COOLING_UNIT.get();
                        case GLASS -> ModBlocks.TIANSHU_SUPERCOMPUTER_GLASS.get();
                        case PORT -> ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get();
                    };
                    blocks.put(TianshuMultiblockScanner.worldPos(controller, part.localPos(), facing),
                            block.defaultBlockState());
                }
                for (var pos : BlockPos.betweenClosed(2, 2, 2, 4, 4, 4)) {
                    var block = pos.equals(new BlockPos(3, 3, 3))
                            ? ModBlocks.MULTIDIMENSIONAL_SUPERCOMPUTING_UNIT.get()
                            : ModBlocks.TIANSHU_BLANK_UNIT.get();
                    blocks.put(TianshuMultiblockScanner.worldPos(controller, pos, facing), block.defaultBlockState());
                }
                var provider = controller.above(10);
                blocks.put(provider, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
                blocks.put(provider.below(), Blocks.FURNACE.defaultBlockState());
                blocks.put(provider.below(2), Blocks.HOPPER.defaultBlockState());
                blocks.put(provider.below(3), AEBlocks.INTERFACE.block().defaultBlockState());
                if (blocks.keySet().stream().allMatch(pos -> player.serverLevel().getBlockState(pos).isAir())) {
                    blocks.forEach((pos, state) -> player.serverLevel().setBlock(pos, state, 2));
                    ((FurnaceBlockEntity) player.serverLevel().getBlockEntity(provider.below()))
                            .setItem(1, new ItemStack(Items.COAL, 64));
                    ((PatternProviderBlockEntity) player.serverLevel().getBlockEntity(provider)).getLogic()
                            .getPatternInv().setItemDirect(0, PatternDetailsHelper.encodeProcessingPattern(
                                    new GenericStack[]{new GenericStack(AEItemKey.of(Items.STONE), 1)},
                                    new GenericStack[]{new GenericStack(AEItemKey.of(Items.SMOOTH_STONE), 1)}));
                    return new Placement(controller, terminal);
                }
            }
        }
        throw new IllegalStateException("no empty space near the fixture for LightningTech CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        var controller = (TianshuSupercomputerControllerBlockEntity) level.getBlockEntity(placement.controller);
        controller.scanNow();
        if (!controller.isFormed()) {
            throw new IllegalStateException("LightningTech Tianshu fixture did not form: " + controller.issueText());
        }
        var port = (TianshuSupercomputerPortBlockEntity) level.getBlockEntity(controller.getPortPos());
        if (!port.getMainNode().isReady()) {
            return false;
        }
        var terminal = (IInWorldGridNodeHost) level.getBlockEntity(placement.terminal);
        var terminalNode = Arrays.stream(Direction.values()).map(terminal::getGridNode).filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException("fixture terminal node is unavailable"));
        var portNode = Objects.requireNonNull(port.getMainNode().getNode(), "LightningTech port node is unavailable");
        if (terminalNode.getGrid() != portNode.getGrid()) {
            GridHelper.createConnection(terminalNode, portNode);
        }
        for (var pos : List.of(placement.controller.above(10), placement.controller.above(7))) {
            var machine = (AENetworkBlockEntity) level.getBlockEntity(pos);
            if (!machine.getMainNode().isReady()) {
                return false;
            }
            var node = Objects.requireNonNull(machine.getMainNode().getNode());
            if (node.getGrid() != terminalNode.getGrid()) {
                GridHelper.createConnection(terminalNode, node);
            }
        }
        terminalNode.getGrid().getStorageService().getInventory()
                .insert(AEItemKey.of(Items.STONE), 64, Actionable.MODULATE, IActionSource.ofPlayer(player));
        return true;
    }

    @Override
    protected String outputId(Placement placement, FixtureMarker marker) {
        return "minecraft:smooth_stone";
    }

    @Override
    protected void startCraft(ServerPlayer player, Placement placement, CraftConfirmMenu menu) {
        var controller = (TianshuSupercomputerControllerBlockEntity) player.serverLevel()
                .getBlockEntity(placement.controller);
        var pool = controller.getTimeWheelCraftingCpuPool();
        // Select at submission too: AE2's CPU-list refresh can reset the menu between driver steps.
        ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$selectedCpu(pool);
        menu.startJob();
        if (pool.getActiveCpus().size() != 1) {
            throw new IllegalStateException("LightningTech did not receive the smoke crafting job");
        }
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        var controller = (TianshuSupercomputerControllerBlockEntity) player.serverLevel()
                .getBlockEntity(placement.controller);
        return controller.isCpuActive() && controller.getGrid() == grid
                ? controller.getTimeWheelCraftingCpuPool() : null;
    }

    record Placement(BlockPos controller, BlockPos terminal) {
    }
}
