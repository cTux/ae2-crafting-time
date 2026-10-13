package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.CpuTtcLayout;
import com.ctux.ae2craftingtime.core.PresentationHooks;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import com.ctux.ae2craftingtime.mc1201.TtcComponents;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;

@Mixin(CPUSelectionList.class)
public abstract class CPUSelectionListMixin {
    @Shadow @Final private Blitter buttonBg;

    // A standard STORE injection is complete before the plugin's postApply readiness check.
    @ModifyVariable(method = "drawBackgroundLayer", at = @At("STORE"), ordinal = 0,
            require = 0, remap = false)
    private Component ae2craftingtime$drawTtc(Component name,
            @Local CraftingStatusMenu.CraftingCpuListEntry cpu,
            @Local(argsOnly = true) GuiGraphics guiGraphics, @Local(ordinal = 0) int x,
            @Local(ordinal = 1) int y) {
        if (!com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime.enabled(
                com.ctux.ae2craftingtime.core.OptionFeature.CPU_CARD_TOTAL)) return name;
        var eta = TimeEstimate.total(java.util.List.of(CpuTtcClient.estimate(cpu.serial())));
        var font = Minecraft.getInstance().font;
        int guiScale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        if (guiScale == 1 && !PresentationHooks.cpu()) guiScale = 2;
        if (eta.isEmpty()) return ae2craftingtime$fitName(name, font,
                CpuTtcLayout.badge(buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), 0, 0.8, guiScale).availableNameWidth());
        var text = TtcText.ttc(eta.get());
        var layout = CpuTtcLayout.badge(buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), font.width(text), 0.8, guiScale);
        if (guiScale == 1) text = TtcComponents.fitWithEllipsis(text, font, layout.textWidth());
        var scale = (float) layout.scale();
        var textWidth = layout.textWidth();
        var right = x + buttonBg.getSrcWidth() - 2;
        var top = y + 2;
        TtcBadge.fillRoundedRect(guiGraphics, right - layout.width(), top, right, top + layout.height(), TtcBadge.BACKGROUND);
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(right - textWidth - 2, top + 1, 0);
        pose.scale(scale, scale, 1);
        guiGraphics.drawString(font, text, 0, 0, 0xE0E0E0, ClientOptionsRuntime.textShadow());
        pose.popPose();
        return ae2craftingtime$fitName(name, font, layout.availableNameWidth());
    }

    private static Component ae2craftingtime$fitName(Component name, Font font, int available) {
        if (font.width(name) <= available) return name;
        if (available < font.width("...")) return Component.empty();
        return Component.literal(font.plainSubstrByWidth(name.getString(), available - font.width("...")) + "...");
    }

    @ModifyArg(method = "getTooltip", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/Tooltip;<init>(Ljava/util/List;)V"),
            index = 0, require = 0, remap = false)
    private List<Component> ae2craftingtime$fullTtcTooltip(List<Component> lines,
            @Local CraftingStatusMenu.CraftingCpuListEntry cpu) {
        if (!PresentationHooks.cpu() || !ClientOptionsRuntime.enabled(
                com.ctux.ae2craftingtime.core.OptionFeature.CPU_CARD_TOTAL)
                || Minecraft.getInstance().getWindow().getGuiScale() != 1) return lines;
        var eta = TimeEstimate.total(java.util.List.of(CpuTtcClient.estimate(cpu.serial())));
        if (eta.isEmpty()) return lines;
        var text = TtcText.ttc(eta.get());
        int width = Minecraft.getInstance().font.width(text);
        if (width <= CpuTtcLayout.badge(buttonBg.getSrcWidth(), buttonBg.getSrcHeight(),
                width, 0.8, 1).textWidth()) return lines;
        var expanded = new java.util.ArrayList<>(lines);
        expanded.add(text);
        return expanded;
    }

}
