package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.TestDriverMod;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void ae2craftingtime_test_driver$afterFrame(float delta, long nanos, boolean tick, CallbackInfo ci) {
        TestDriverMod.afterFrame();
    }
}
