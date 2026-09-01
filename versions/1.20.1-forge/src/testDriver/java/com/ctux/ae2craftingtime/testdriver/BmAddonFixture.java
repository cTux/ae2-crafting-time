package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import com.edgemq.bmaddon.ae2.BloodAltarPatternDetails;
import com.edgemq.bmaddon.blockentity.BloodAltarAssemblerBlockEntity;
import com.edgemq.bmaddon.item.BloodAltarPatternItem;
import com.edgemq.bmaddon.registry.BMAddonItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.common.recipe.BloodMagicRecipeType;
import wayoftime.bloodmagic.recipe.RecipeBloodAltar;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

final class BmAddonFixture extends AddonCpuFixture<BmAddonFixture.Placement> {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var assemblerBlock = ForgeRegistries.BLOCKS.getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild("bmaddon", "blood_altar_assembler")));
        if (assemblerBlock == null) {
            throw new IllegalStateException("BM Addon Blood Assembler is unavailable");
        }
        for (var direction : Direction.Plane.HORIZONTAL) {
            var assembler = terminal.relative(direction, 3);
            if (level.getBlockState(assembler).isAir()) {
                level.setBlockAndUpdate(assembler, assemblerBlock.defaultBlockState());
                return new Placement(assembler, terminal);
            }
        }
        throw new IllegalStateException("no empty space beside the fixture AE2 grid for BM Addon");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var level = player.serverLevel();
        if (!(level.getBlockEntity(placement.assembler) instanceof BloodAltarAssemblerBlockEntity assembler)) {
            throw new IllegalStateException("BM Addon Blood Assembler was not placed");
        }
        if (!assembler.getMainNode().isReady()) {
            assembler.onReady();
        }
        if (!(level.getBlockEntity(placement.terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("BM Addon fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("BM Addon fixture terminal node is unavailable"));
        var assemblerNode = assembler.getMainNode().getNode();
        if (assemblerNode == null) {
            throw new IllegalStateException("BM Addon Blood Assembler node is unavailable");
        }
        if (terminalNode.getGrid() != assemblerNode.getGrid()) {
            GridHelper.createConnection(terminalNode, assemblerNode);
        }
        if (!assembler.getMainNode().isActive()) {
            return false;
        }
        if (placement.outputId == null) {
            installPattern(terminalNode.getGrid(), level, assembler, placement);
        }
        return true;
    }

    @Override
    protected String outputId(Placement placement, FixtureMarker marker) {
        return placement.outputId;
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return grid.getCraftingService().getCpus().stream()
                .filter(candidate -> !candidate.isBusy())
                .findFirst().orElse(null);
    }

    private static void installPattern(IGrid grid, ServerLevel level, BloodAltarAssemblerBlockEntity assembler,
            Placement placement) {
        var recipe = recipe(level, assembler);
        var input = recipe.getInput().getItems()[0].copy();
        input.setCount(1);
        var pattern = new ItemStack(BMAddonItems.BLOOD_ALTAR_PATTERN.get());
        BloodAltarPatternItem.encode(pattern, recipe, input);
        var details = BloodAltarPatternDetails.create(level, pattern, recipe)
                .orElseThrow(() -> new IllegalStateException("BM Addon pattern could not be decoded"));
        assembler.getPatternInventory().setItemDirect(0, pattern);
        assembler.onChangeInventory(assembler.getPatternInventory(), 0);
        var storage = grid.getStorageService().getInventory();
        for (var requirement : details.getInputs()) {
            var stack = requirement.getPossibleInputs()[0];
            var amount = stack.amount() * requirement.getMultiplier();
            if (storage.insert(stack.what(), amount, Actionable.MODULATE, IActionSource.empty()) != amount) {
                throw new IllegalStateException("BM Addon fixture input could not be inserted");
            }
        }
        placement.outputId = details.getPrimaryOutput().what().getId().toString();
    }

    private static RecipeBloodAltar recipe(ServerLevel level, BloodAltarAssemblerBlockEntity assembler) {
        return level.getRecipeManager().getAllRecipesFor(BloodMagicRecipeType.ALTAR.get()).stream()
                .filter(candidate -> candidate.getMinimumTier() <= assembler.getBloodMagicRecipeTierLimit())
                .filter(candidate -> candidate.getInput().getItems().length > 0 && !candidate.getOutput().isEmpty())
                .min(Comparator.comparing(candidate -> candidate.getId().toString()))
                .orElseThrow(() -> new IllegalStateException("BM Addon has no usable tier-1 Blood Magic recipe"));
    }

    private static final class Placement {
        private final BlockPos assembler;
        private final BlockPos terminal;
        private String outputId;

        private Placement(BlockPos assembler, BlockPos terminal) {
            this.assembler = assembler;
            this.terminal = terminal;
        }
    }
}
