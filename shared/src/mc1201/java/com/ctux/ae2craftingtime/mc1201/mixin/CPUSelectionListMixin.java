package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.CpuTtcCache;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.CpuTtcLayout;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(CPUSelectionList.class)
public abstract class CPUSelectionListMixin {
    @Shadow @Final private CraftingStatusMenu menu;
    @Shadow @Final private Scrollbar scrollbar;
    @Shadow @Final private Blitter buttonBg;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$open(CallbackInfo ci) {
        CpuTtcClient.open(menu);
    }

    @Inject(method = "updateBeforeRender", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$refresh(CallbackInfo ci) {
        var cpus = menu.cpuList.cpus();
        var views = new ArrayList<CpuTtcCache.CpuView>();
        cpus.stream().filter(cpu -> cpu.serial() == menu.getSelectedCpuSerial())
                .findFirst().ifPresent(cpu -> views.add(ae2craftingtime$view(cpu)));
        cpus.stream().skip(scrollbar.getCurrentScroll()).limit(6)
                .map(CPUSelectionListMixin::ae2craftingtime$view).forEach(views::add);
        CpuTtcClient.refresh(views);
    }

    @WrapOperation(method = "drawBackgroundLayer", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/CPUSelectionList;getCpuName(Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuListEntry;)Lnet/minecraft/network/chat/Component;"), remap = false)
    private Component ae2craftingtime$drawTtc(CPUSelectionList instance,
            CraftingStatusMenu.CraftingCpuListEntry cpu, Operation<Component> original,
            @Local(argsOnly = true) GuiGraphics guiGraphics, @Local(ordinal = 0) int x,
            @Local(ordinal = 1) int y) {
        var name = original.call(instance, cpu);
        var eta = TimeEstimate.formatTotal(java.util.List.of(CpuTtcClient.seconds(cpu.serial())));
        if (eta.isEmpty()) return name;
        var font = Minecraft.getInstance().font;
        var text = TtcText.ttc(eta.get());
        var layout = CpuTtcLayout.badge(buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), font.width(text), 0.8);
        var scale = (float) layout.scale();
        var textWidth = layout.textWidth();
        var right = x + buttonBg.getSrcWidth() - 2;
        var top = y + 2;
        TtcBadge.fillRoundedRect(guiGraphics, right - layout.width(), top, right, top + layout.height(), TtcBadge.BACKGROUND);
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(right - textWidth - 2, top + 1, 0);
        pose.scale(scale, scale, 1);
        guiGraphics.drawString(font, text, 0, 0, 0xE0E0E0, true);
        pose.popPose();
        var available = layout.availableNameWidth();
        if (font.width(name) <= available) return name;
        if (available < font.width("...")) return Component.empty();
        return Component.literal(font.plainSubstrByWidth(name.getString(), available - font.width("...")) + "...");
    }

    private static CpuTtcCache.CpuView ae2craftingtime$view(CraftingStatusMenu.CraftingCpuListEntry cpu) {
        var job = cpu.currentJob();
        return job == null ? new CpuTtcCache.CpuView(cpu.serial(), null, 0, 0)
                : new CpuTtcCache.CpuView(cpu.serial(), ProfilerBridge.key(job.what()).outputId(), job.amount(),
                        Math.max(0, cpu.elapsedTimeNanos()));
    }
}
