package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.CpuTtcLayout;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CPUSelectionList.class)
public abstract class CPUSelectionListMixin {
    @Shadow @Final private Blitter buttonBg;

    @WrapOperation(method = "drawBackgroundLayer", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/CPUSelectionList;getCpuName(Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuListEntry;)Lnet/minecraft/network/chat/Component;"), remap = false)
    private Component ae2craftingtime$drawTtc(CPUSelectionList instance,
            CraftingStatusMenu.CraftingCpuListEntry cpu, Operation<Component> original,
            @Local(argsOnly = true) GuiGraphicsExtractor guiGraphics, @Local(ordinal = 0) int x,
            @Local(ordinal = 1) int y) {
        var name = original.call(instance, cpu);
        var eta = TimeEstimate.formatTotal(java.util.List.of(CpuTtcClient.seconds(cpu.serial())));
        var font = Minecraft.getInstance().font;
        if (eta.isEmpty()) return ae2craftingtime$fitName(name, font,
                CpuTtcLayout.badge(buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), 0, 0.666).availableNameWidth());
        var text = TtcText.ttc(eta.get());
        var layout = CpuTtcLayout.badge(buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), font.width(text), 0.666);
        var scale = (float) layout.scale();
        var textWidth = layout.textWidth();
        var right = x + buttonBg.getSrcWidth() - 2;
        var top = y + 2;
        TtcBadge.fillRoundedRect(guiGraphics, right - layout.width(), top, right, top + layout.height(), TtcBadge.BACKGROUND);
        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(right - textWidth - 2, top + 1);
        pose.scale(scale);
        guiGraphics.text(font, text, 0, 0, 0xFFE0E0E0, true);
        pose.popMatrix();
        return ae2craftingtime$fitName(name, font, layout.availableNameWidth());
    }

    private static Component ae2craftingtime$fitName(Component name, Font font, int available) {
        if (font.width(name) <= available) return name;
        if (available < font.width("...")) return Component.empty();
        return Component.literal(font.plainSubstrByWidth(name.getString(), available - font.width("...")) + "...");
    }
}
