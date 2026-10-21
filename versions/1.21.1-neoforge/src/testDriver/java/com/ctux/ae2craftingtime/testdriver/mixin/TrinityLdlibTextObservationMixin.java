package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** LDLib draws its button text through Font.drawInBatch or its layout cache, bypassing GuiGraphics.drawString. */
@Pseudo
@Mixin(targets = "com.lowdragmc.lowdraglib2.gui.LDLibFonts", remap = false)
public abstract class TrinityLdlibTextObservationMixin {
    @Inject(method = "drawText(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;"
            + "Lnet/minecraft/util/FormattedCharSequence;FFIZ)I", at = @At("HEAD"), remap = false, require = 0)
    private static void ae2craftingtime_test_driver$nativeText(GuiGraphics graphics, Font font,
            FormattedCharSequence text, float x, float y, int color, boolean shadow,
            CallbackInfoReturnable<Integer> cir) {
        UiObservationStore.nativeTitle(graphics, font, text, x, y, color);
    }
}
