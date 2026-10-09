package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.TreeAmountObservation;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"com.neuvillette.ae2ct.gui.CraftingTreeWidget", "com.vcwdfca.ae2ct.gui.CraftingTreeWidget"}, remap = false)
public abstract class CraftingTreeAmountObservationMixin {
    @Inject(method = "drawNode", at = @At("HEAD"), require = 0)
    private void begin(GuiGraphics graphics, @Coerce Object node, CallbackInfo callback) {
        TreeAmountObservation.record(node);
    }
    @Inject(method = "drawNode", at = @At("RETURN"), require = 0)
    private void end(GuiGraphics graphics, @Coerce Object node, CallbackInfo callback) {
        TreeAmountObservation.clear();
    }
}
