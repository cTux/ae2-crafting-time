package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.crafting.CraftAmountScreen;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftAmountScreen.class)
public interface CraftAmountScreenAccessor {
    @Accessor(value = "next", remap = false)
    Button ae2craftingtime_test_driver$next();
}
