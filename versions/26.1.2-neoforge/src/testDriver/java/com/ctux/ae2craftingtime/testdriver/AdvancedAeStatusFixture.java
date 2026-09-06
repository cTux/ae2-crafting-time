package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftingCPUMenuAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;

final class AdvancedAeStatusFixture {
    private final AdvancedAeFixture delegate = new AdvancedAeFixture();
    private AdvancedAeFixture.Placement placement;
    private boolean submitted;

    boolean prepare(ServerPlayer player, FixtureMarker marker) {
        if (placement == null) placement = delegate.place(player, marker);
        return delegate.finish(player, placement);
    }

    boolean submit(ServerPlayer player, ICraftingPlan plan) {
        if (submitted) return true;
        var core = (AdvCraftingBlockEntity) player.level().getBlockEntity(placement.core());
        var result = core.getCluster().submitJob(core.getMainNode().getGrid(), plan,
                IActionSource.ofPlayer(player), null);
        if (!result.successful()) throw new IllegalStateException("AdvancedAE rejected the status smoke craft: " + result.errorCode());
        submitted = true;
        return true;
    }

    void open(ServerPlayer player, CraftingBlockEntity menuHost) {
        var core = (AdvCraftingBlockEntity) player.level().getBlockEntity(placement.core());
        MenuOpener.open(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(menuHost));
        var menu = (CraftingCPUMenuAccessor) player.containerMenu;
        menu.ae2craftingtime_test_driver$setCpu(null);
        menu.ae2craftingtime_test_driver$setCpu(core.getCluster().getActiveCPUs().get(0));
    }
}
