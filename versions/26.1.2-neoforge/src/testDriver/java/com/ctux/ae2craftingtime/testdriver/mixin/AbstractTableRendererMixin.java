package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.crafting.AbstractTableRenderer;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractTableRenderer.class)
public abstract class AbstractTableRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void ae2craftingtime_test_driver$rows(GuiGraphicsExtractor graphics, int mouseX, int mouseY, List<?> entries,
            int scrollOffset, CallbackInfo ci) {
        UiObservationStore.rows(entries, scrollOffset);
    }
}
