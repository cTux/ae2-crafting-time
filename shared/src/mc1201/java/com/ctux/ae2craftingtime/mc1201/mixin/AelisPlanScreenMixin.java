package com.ctux.ae2craftingtime.mc1201.mixin;

import com.ctux.ae2craftingtime.mc1201.AelisTtc;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.appliedenhancements.client.plan.AelisCraftConfirmScreen", remap = false)
public abstract class AelisPlanScreenMixin {
    @Unique private int ae2craftingtime$mouseX;
    @Unique private int ae2craftingtime$mouseY;
    @ModifyArg(method = "drawBG", at = @At(value = "INVOKE",
            target = "Lcom/appliedenhancements/client/plan/AelisCraftConfirmScreen;bevel(Lnet/minecraft/client/gui/GuiGraphics;IIIIIZ)V",
            ordinal = 1), index = 4, require = 0)
    private int ae2craftingtime$summaryHeight(int height) {
        return AelisTtc.enabled() && ClientOptionsRuntime.enabled(OptionFeature.PLAN_TOTAL) ? 32 : height;
    }
    @Inject(method = "drawFG", at = @At("RETURN"), require = 0)
    private void ae2craftingtime$drawTtc(GuiGraphics graphics, int ox, int oy, int mx, int my, CallbackInfo ci) {
        AelisTtc.draw(this, graphics, mx - ox, my - oy);
    }

    @Inject(method = {"render", "m_88315_"}, at = @At("HEAD"), require = 0)
    private void ae2craftingtime$tooltipMouse(GuiGraphics graphics, int x, int y, float ticks, CallbackInfo ci) {
        ae2craftingtime$mouseX = x;
        ae2craftingtime$mouseY = y;
    }

    @ModifyArg(method = {"render", "m_88315_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
            remap = true), index = 1, require = 0)
    private List<Component> ae2craftingtime$ttcTooltip(List<Component> lines) {
        return AelisTtc.tooltip(this, lines, ae2craftingtime$mouseX, ae2craftingtime$mouseY);
    }
}
