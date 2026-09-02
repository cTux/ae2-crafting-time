package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCalculator;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;

import java.util.Arrays;
import java.util.Objects;

final class AdvancedAeFixture extends AddonCpuFixture<AdvancedAeFixture.Placement> {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
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
        var core = BuiltInRegistries.BLOCK.get(ResourceLocation.tryBuild("advanced_ae", "quantum_core"));
        if (core == null) {
            throw new IllegalStateException("AdvancedAE quantum core is unavailable");
        }
        for (BlockPos anchor : BlockPos.betweenClosed(terminal.offset(-12, -4, -12), terminal.offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost host)) {
                continue;
            }
            for (var direction : new Direction[] { Direction.UP, Direction.DOWN }) {
                var node = host.getGridNode(direction);
                var position = anchor.relative(direction).immutable();
                if (node != null && node.getGrid() == grid && level.getBlockState(position).isAir()) {
                    // The three upgrade blocks must be inside a complete structural shell.
                    var height = position.getY() + 8;
                    for (var column : BlockPos.betweenClosed(position, position.offset(2, 0, 4))) {
                        height = Math.max(height, level.getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                                column.getX(), column.getZ()) + 1);
                    }
                    var min = new BlockPos(position.getX(), height, position.getZ());
                    var max = min.offset(2, 2, 4);
                    for (var part : BlockPos.betweenClosed(min, max)) {
                        if (!level.getBlockState(part).isAir()) {
                            throw new IllegalStateException("AdvancedAE enclosure would overwrite a fixture block");
                        }
                    }
                    for (var part : BlockPos.betweenClosed(min, max)) {
                        var id = part.equals(min.offset(1, 1, 1)) ? "quantum_core"
                                : part.equals(min.offset(1, 1, 2)) ? "quantum_accelerator"
                                : part.equals(min.offset(1, 1, 3)) ? "data_entangler" : "quantum_structure";
                        var block = Objects.requireNonNull(BuiltInRegistries.BLOCK.get(
                                ResourceLocation.tryBuild("advanced_ae", id)), "AdvancedAE block " + id);
                        level.setBlockAndUpdate(part, block.defaultBlockState());
                    }
                    position = min.offset(1, 1, 1);
                    if (!(level.getBlockEntity(position) instanceof AdvCraftingBlockEntity)) {
                        throw new IllegalStateException("AdvancedAE quantum core placement produced "
                                + BuiltInRegistries.BLOCK.getKey(level.getBlockState(position).getBlock()));
                    }
                    return new Placement(position, terminal, min, max);
                }
            }
        }
        throw new IllegalStateException("no empty vertical connection beside the fixture AE2 grid for AdvancedAE CPU");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.core()) instanceof AdvCraftingBlockEntity core)) {
            throw new IllegalStateException("AdvancedAE quantum core was not placed");
        }
        for (var part : BlockPos.betweenClosed(placement.min(), placement.max())) {
            if (!((AdvCraftingBlockEntity) level.getBlockEntity(part)).getMainNode().isReady()) {
                return false;
            }
        }
        var calculator = new AdvCraftingCPUCalculator(core);
        if (!calculator.verifyInternalStructure(level, placement.min(), placement.max())) {
            throw new IllegalStateException("AdvancedAE enclosure is invalid");
        }
        if (!core.isFormed()) {
            calculator.updateBlockEntities(
                    calculator.createCluster(level, placement.min(), placement.max()),
                    level, placement.min(), placement.max());
        }
        if (!core.isFormed()) {
            throw new IllegalStateException("AdvancedAE quantum core did not form after rescan; node ready="
                    + core.getMainNode().isReady());
        }
        var accelerator = (AdvCraftingBlockEntity) level.getBlockEntity(placement.min().offset(1, 1, 2));
        var entangler = (AdvCraftingBlockEntity) level.getBlockEntity(placement.min().offset(1, 1, 3));
        var cluster = core.getCluster();
        if (cluster.numBlockEntities() != 45
                || cluster.getCoProcessors() != core.getAcceleratorThreads() + accelerator.getAcceleratorThreads()
                || cluster.getCoProcessors() <= core.getAcceleratorThreads()
                || cluster.getAvailableStorage() != core.getStorageBytes() * entangler.getStorageMultiplier()
                || cluster.getAvailableStorage() <= core.getStorageBytes()) {
            throw new IllegalStateException("AdvancedAE Accelerator or Data Entangler capacity was not applied");
        }
        if (!(level.getBlockEntity(placement.terminal()) instanceof IInWorldGridNodeHost host)) {
            throw new IllegalStateException("AdvancedAE fixture terminal is unavailable");
        }
        var hostNode = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException("AdvancedAE fixture terminal node is unavailable"));
        var coreNode = core.getMainNode().getNode();
        if (coreNode == null) {
            throw new IllegalStateException("AdvancedAE core node is unavailable");
        }
        if (hostNode.getGrid() != coreNode.getGrid()) {
            GridHelper.createConnection(hostNode, coreNode);
        }
        return true;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        if (player == null
                || !(player.serverLevel().getBlockEntity(placement.core()) instanceof AdvCraftingBlockEntity core)
                || core.getCluster() == null) {
            return null;
        }
        var cpu = core.getCluster().getRemainingCapacityCPU();
        if (!cpu.isActive() || cpu.getGrid() != grid) {
            throw new IllegalStateException("AdvancedAE CPU is not selectable; active=" + cpu.isActive()
                    + " sameGrid=" + (cpu.getGrid() == grid)
                    + " nodeActive=" + core.getMainNode().isActive()
                    + " nodeOnline=" + core.getMainNode().isOnline()
                    + " nodePowered=" + core.getMainNode().isPowered());
        }
        return cpu;
    }

    @Override
    protected void startCraft(ServerPlayer player, Placement placement, CraftConfirmMenu menu) {
        var core = (AdvCraftingBlockEntity) player.serverLevel().getBlockEntity(placement.core());
        var cluster = core.getCluster();
        // The released service hook auto-selects a cluster even when given an AdvCraftingCPU wrapper.
        var plan = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result();
        var result = cluster.submitJob(core.getMainNode().getGrid(), plan,
                appeng.api.networking.security.IActionSource.ofPlayer(player), null);
        if (!result.successful()) {
            throw new IllegalStateException("AdvancedAE rejected the smoke craft: " + result.errorCode());
        }
        if (cluster.getActiveCPUs().size() != 1) {
            throw new IllegalStateException("AdvancedAE did not receive the smoke crafting job");
        }
        menu.getHost().returnToMainMenu(player, menu);
    }

    record Placement(BlockPos core, BlockPos terminal, BlockPos min, BlockPos max) {
    }
}
