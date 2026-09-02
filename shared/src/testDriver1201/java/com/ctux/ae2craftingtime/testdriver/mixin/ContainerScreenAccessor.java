package com.ctux.ae2craftingtime.testdriver.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("imageWidth") int ae2craftingtime_test_driver$width();
    @Accessor("imageHeight") int ae2craftingtime_test_driver$height();
}
