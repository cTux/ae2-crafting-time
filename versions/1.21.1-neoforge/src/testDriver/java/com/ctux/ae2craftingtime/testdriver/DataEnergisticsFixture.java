package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.fish_dan_.data_energistics.blockentity.patternprovider.AdaptivePatternProviderBlockEntity;
import com.fish_dan_.data_energistics.registry.DEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Exercises a native CPU through Data Energistics' installed AE2 provider. */
final class DataEnergisticsFixture extends NativeCpuFixture {
    private BlockPos providerPosition;
    private boolean inputStored;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var placement = super.place(player, marker);
        var level = player.serverLevel();
        for (int offset : List.of(12, 16, 20, 24)) {
            var candidate = placement.terminal().east(offset);
            if (!Arrays.stream(new int[] {0, 1, 2, 3})
                    .allMatch(depth -> level.getBlockState(candidate.below(depth)).isAir())) continue;
            providerPosition = candidate;
            level.setBlockAndUpdate(candidate, DEBlocks.ADAPTIVE_PATTERN_PROVIDER.get().defaultBlockState()
                    .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.DOWN));
            level.setBlockAndUpdate(candidate.below(), Blocks.FURNACE.defaultBlockState());
            level.setBlockAndUpdate(candidate.below(2), Blocks.HOPPER.defaultBlockState());
            level.setBlockAndUpdate(candidate.below(3), AEBlocks.INTERFACE.block().defaultBlockState());
            ((FurnaceBlockEntity) level.getBlockEntity(candidate.below())).setItem(1, new ItemStack(Items.COAL, 64));
            var provider = (AdaptivePatternProviderBlockEntity) level.getBlockEntity(candidate);
            provider.getProviderInventory().setItemDirect(0, new ItemStack(AEBlocks.PATTERN_PROVIDER.block()));
            provider.getLogic().getPatternInv().setItemDirect(0, PatternDetailsHelper.encodeProcessingPattern(
                    List.of(new GenericStack(AEItemKey.of(Items.SAND), 1)),
                    List.of(new GenericStack(AEItemKey.of(Items.GLASS), 1))));
            provider.getLogic().updatePatterns();
            return placement;
        }
        throw new IllegalStateException("no empty space beside the fixture for an adaptive provider");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!super.finish(player, placement)) return false;
        var level = player.serverLevel();
        var provider = (AdaptivePatternProviderBlockEntity) level.getBlockEntity(providerPosition);
        var receiver = (AENetworkedBlockEntity) level.getBlockEntity(providerPosition.below(3));
        if (!provider.getMainNode().isReady() || !receiver.getMainNode().isReady()) return false;
        if (!provider.getProviderStack().is(AEBlocks.PATTERN_PROVIDER.block().asItem())) {
            throw new IllegalStateException("adaptive provider rejected its AE2 provider stack");
        }
        var terminal = (IInWorldGridNodeHost) level.getBlockEntity(placement.terminal());
        var node = Arrays.stream(Direction.values()).map(terminal::getGridNode).filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException("fixture terminal node is unavailable"));
        for (var machine : List.of(provider, receiver)) {
            var machineNode = Objects.requireNonNull(machine.getMainNode().getNode());
            if (machineNode.getGrid() != node.getGrid()) GridHelper.createConnection(node, machineNode);
        }
        if (!provider.getMainNode().isActive() || !receiver.getMainNode().isActive()) return false;
        if (!inputStored) {
            var stored = node.getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.SAND), 64,
                    Actionable.MODULATE, IActionSource.ofPlayer(player));
            if (stored != 64) throw new IllegalStateException("adaptive fixture could not store sand input: " + stored);
            inputStored = true;
        }
        var available = provider.getLogic().getAvailablePatterns();
        var registered = node.getGrid().getCraftingService().getCraftingFor(AEItemKey.of(Items.GLASS));
        if (registered.isEmpty()) return false;
        if (available.size() != 1 || registered.size() != 1 || registered.iterator().next() != available.getFirst()) {
            throw new IllegalStateException("adaptive provider is not the only registered glass pattern source");
        }
        return true;
    }

    @Override
    protected String outputId(Placement placement, FixtureMarker marker) {
        return "minecraft:glass";
    }

    @Override
    protected void cleanup(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        if (providerPosition != null) {
            for (int depth = 0; depth < 4; depth++)
                level.setBlockAndUpdate(providerPosition.below(depth), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(placement.storage(), Blocks.AIR.defaultBlockState());
    }
}
