package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.stacks.AEKey;
import appeng.api.client.AEKeyRendering;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AEKeyRendering.class, remap = false)
public abstract class AEKeyRenderingMixin {
    @Inject(method = "drawInGui", at = @At("RETURN"))
    private static void ae2craftingtime_test_driver$node(Minecraft minecraft, GuiGraphics graphics,
            int x, int y, AEKey key, CallbackInfo ci) {
        UiObservationStore.treeNode(graphics, key, x, y);
    }
}
