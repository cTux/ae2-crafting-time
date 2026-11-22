package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerPlayer;

/** Uses a real AE2 requester link so the legacy CPU can report accepted final output. */
final class NeoEcoPendingFixture extends NeoEcoFixture {
    @Override
    protected void startCraft(ServerPlayer player, Placement placement, CraftConfirmMenu menu) {
        var grid = placement.grid();
        var plan = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result();
        var selected = cpu(player, placement, grid);
        if (selected == null || !(plan.finalOutput().what() instanceof AEItemKey output)) {
            throw new IllegalStateException("Legacy NeoEco craft requires an ECO CPU and an item output");
        }
        var destination = new PlayerRequester(player, grid.getPivot(), output);
        var result = grid.getCraftingService().submitJob(plan, destination, selected, false, IActionSource.ofPlayer(player));
        if (!result.successful()) {
            throw new IllegalStateException("Legacy NeoEco requester craft was declined: " + result);
        }
    }

    private record PlayerRequester(ServerPlayer player, IGridNode node, AEItemKey output) implements ICraftingRequester {
        @Override
        public IGridNode getActionableNode() {
            return node;
        }

        @Override
        public ImmutableSet<ICraftingLink> getRequestedJobs() {
            return ImmutableSet.of();
        }

        @Override
        public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
            if (!output.equals(what) || amount != 1 || player.getInventory().getFreeSlot() < 0) return 0;
            return mode == Actionable.SIMULATE || player.getInventory().add(output.toStack(1)) ? 1 : 0;
        }

        @Override
        public void jobStateChange(ICraftingLink link) {}
    }
}
