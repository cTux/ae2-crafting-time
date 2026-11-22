package com.ctux.ae2craftingtime.neoeco.mixin;

import com.ctux.ae2craftingtime.core.NeoEcoStatusRows;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusEntry;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuStatusPanel", remap = false)
public abstract class NeoEcoStatusGridMixin {
    @Inject(method = "itemDescription", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ae2craftingtime$itemTime(@Coerce Object entry, boolean full,
            CallbackInfoReturnable<List<Component>> callback) {
        if (full || !(entry instanceof NeoEcoStatusEntry row)) return;
        var estimate = NeoEcoStatusText.estimate(row);
        if (estimate == null) return;
        var lines = callback.getReturnValue();
        var font = Minecraft.getInstance().font;
        if (!NeoEcoStatusRows.fits(lines.size(), font.lineHeight, font.width(estimate))) return;
        var withTime = new ArrayList<>(lines);
        withTime.add(estimate);
        callback.setReturnValue(withTime);
    }

    // The optional add-on's published Forge JAR calls the SRG name directly.
    @WrapOperation(method = "drawItemDescription", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;m_280614_"
                    + "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"),
            require = 0)
    private static int ae2craftingtime$badge(GuiGraphics graphics, Font font, Component text, int x, int y,
            int color, boolean shadow, Operation<Integer> original) {
        if (NeoEcoStatusText.isEstimate(text)) {
            TtcBadge.fillRoundedRect(graphics, x - 2, y, x + font.width(text) + 2, y + font.lineHeight,
                    TtcBadge.BACKGROUND);
        }
        return original.call(graphics, font, text, x, y, color, shadow);
    }
}
