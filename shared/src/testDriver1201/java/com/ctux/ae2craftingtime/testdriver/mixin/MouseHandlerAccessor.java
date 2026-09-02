package com.ctux.ae2craftingtime.testdriver.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Invoker("onMove")
    void ae2craftingtime_test_driver$move(long window, double x, double y);
}
