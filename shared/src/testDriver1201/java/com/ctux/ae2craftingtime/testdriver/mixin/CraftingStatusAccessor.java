package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftingCPUScreen.class, remap = false)
public interface CraftingStatusAccessor {
    @Accessor("status") CraftingStatus ae2craftingtime_test_driver$status();
    @Accessor("scrollbar") Scrollbar ae2craftingtime_test_driver$scrollbar();
}
