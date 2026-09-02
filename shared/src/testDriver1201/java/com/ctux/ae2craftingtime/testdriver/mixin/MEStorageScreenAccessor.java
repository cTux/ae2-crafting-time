package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.menu.me.common.GridInventoryEntry;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MEStorageScreen.class)
public interface MEStorageScreenAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor(value = "repo", remap = false)
    Repo ae2craftingtime_test_driver$repo();

    @Invoker(value = "handleGridInventoryEntryMouseClick", remap = false)
    void ae2craftingtime_test_driver$click(GridInventoryEntry entry, int button, ClickType clickType);
}
