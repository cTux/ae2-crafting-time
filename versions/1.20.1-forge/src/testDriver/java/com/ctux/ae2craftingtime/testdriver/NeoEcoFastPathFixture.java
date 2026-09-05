package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.crafting.CraftAmountScreen;
import cn.dancingsnow.neoecoae.all.NEMultiBlocks;
import cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity;
import cn.dancingsnow.neoecoae.blocks.entity.NEBlockEntity;
import com.ctux.ae2craftingtime.testdriver.mixin.NeoEcoAmountAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;

/** A real ECO worker/bus crafts 64 planks; cold-cache fallback must lead to a verified batch. */
final class NeoEcoFastPathFixture extends NeoEcoFixture {
    private List<BlockPos> crafting;
    private BlockPos terminal;
    private boolean supplied;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var cpu = super.place(player, marker);
        terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        crafting = placeBlueprint(player.serverLevel(), terminal, NEMultiBlocks.CRAFTING_SYSTEM_L9);
        return cpu;
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!super.finish(player, placement)) return false;
        var level = player.serverLevel();
        var controller = crafting.stream().map(level::getBlockEntity)
                .filter(NEBlockEntity.class::isInstance).map(entity -> (NEBlockEntity<?, ?>) entity)
                .filter(entity -> entity.getClass().getSimpleName().equals("ECOCraftingSystemBlockEntity"))
                .findFirst().orElseThrow();
        controller.rebuildMultiblock();
        if (!controller.isFormed()) throw new IllegalStateException("ECO crafting worker did not form");
        var bus = crafting.stream().map(level::getBlockEntity).filter(ECOCraftingPatternBusBlockEntity.class::isInstance)
                .map(ECOCraftingPatternBusBlockEntity.class::cast).findFirst().orElseThrow();
        if (bus.getGridNode() == null) return false;
        var host = (IInWorldGridNodeHost) level.getBlockEntity(terminal);
        IGrid grid = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).findFirst().orElseThrow();
        if (bus.getGrid() != grid) throw new IllegalStateException("ECO crafting worker is on another grid");
        if (!supplied) {
            grid.getStorageService().getInventory().extract(AEItemKey.of(Items.BIRCH_PLANKS), Long.MAX_VALUE,
                    Actionable.MODULATE, IActionSource.empty());
            var input = new ItemStack[9];
            Arrays.fill(input, ItemStack.EMPTY);
            input[0] = new ItemStack(Items.BIRCH_LOG);
            var recipe = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.tryParse("minecraft:birch_planks"))
                    .orElseThrow();
            var pattern = PatternDetailsHelper.encodeCraftingPattern(recipe, input, new ItemStack(Items.BIRCH_PLANKS, 4),
                    false, false);
            bus.getTerminalPatternInventory().setItemDirect(0, pattern);
            var inserted = grid.getStorageService().getInventory().insert(AEItemKey.of(Items.BIRCH_LOG), 64,
                    Actionable.MODULATE, IActionSource.empty());
            if (inserted != 64) throw new IllegalStateException("ECO fixture could not store its inputs");
            grid.getEnergyService().injectPower(1_000_000, Actionable.MODULATE);
            supplied = true;
            return false; // Let the real provider registration reach AE2 before opening the plan.
        }
        return !bus.getAvailablePatterns().isEmpty();
    }

    @Override protected String outputId(Placement placement, FixtureMarker marker) { return "minecraft:birch_planks"; }

    @Override void configureAmount(CraftAmountScreen screen) {
        ((NeoEcoAmountAccessor) screen).ae2craftingtime_test_driver$amount().setLongValue(64);
    }

    @Override void verifyDispatch(DispatchObservation.Snapshot snapshot) {
        super.verifyDispatch(snapshot);
        if (snapshot.fastPathCrafts() == 0) throw new IllegalStateException("ECO job completed without exercising FastPath");
    }
}
