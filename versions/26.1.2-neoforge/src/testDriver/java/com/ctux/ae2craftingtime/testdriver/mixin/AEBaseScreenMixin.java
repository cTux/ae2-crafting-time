package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AEBaseScreen.class, remap = false)
public abstract class AEBaseScreenMixin {
    @Inject(method = "drawTooltipWithHeader(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILjava/util/List;)V",
            at = @At("HEAD"))
    private void ae2craftingtime_test_driver$treeTooltip(GuiGraphicsExtractor graphics, int x, int y,
            List<Component> lines, CallbackInfo ci) {
        UiObservationStore.wirelessTooltip(lines);
    }
}
