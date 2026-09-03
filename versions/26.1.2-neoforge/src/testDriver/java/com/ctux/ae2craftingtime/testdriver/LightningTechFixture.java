package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** The 26.1.2 release supplies an overloaded provider, without a Tianshu CPU. */
final class LightningTechFixture extends AddonCpuFixture<BlockPos> {
    @Override
    protected BlockPos place(ServerPlayer player, FixtureMarker marker) {
        var position = new BlockPos(marker.terminal().x() + 2, marker.terminal().y(), marker.terminal().z());
        var level = player.level();
        var provider = (PatternProviderBlockEntity) level.getBlockEntity(position);
        var pattern = provider.getLogic().getPatternInv().getStackInSlot(0).copy();
        provider.getLogic().getPatternInv().setItemDirect(0, net.minecraft.world.item.ItemStack.EMPTY);
        level.setBlockAndUpdate(position, ModBlocks.OVERLOADED_PATTERN_PROVIDER.get().defaultBlockState());
        var replacement = (OverloadedPatternProviderBlockEntity) level.getBlockEntity(position);
        replacement.getExposedPatternInventory().setItemDirect(0, pattern);
        return position;
    }

    @Override
    protected boolean finish(ServerPlayer player, BlockPos position) {
        var provider = (OverloadedPatternProviderBlockEntity) player.level().getBlockEntity(position);
        if (!provider.getMainNode().isReady()) return false;
        var terminal = (appeng.api.networking.IInWorldGridNodeHost) player.level().getBlockEntity(position.west(2));
        var source = terminal.getGridNode(net.minecraft.core.Direction.NORTH);
        var node = provider.getMainNode().getNode();
        if (node == null) return false;
        if (node.getGrid() != source.getGrid()) appeng.api.networking.GridHelper.createConnection(source, node);
        provider.getLogic().updatePatterns();
        return provider.getMainNode().isActive() && node.getGrid().getCraftingService()
                .isCraftable(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.FURNACE));
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, BlockPos position, IGrid grid) {
        return grid.getCraftingService().getCpus().stream().filter(cpu -> !cpu.isBusy()).findFirst().orElse(null);
    }
}
