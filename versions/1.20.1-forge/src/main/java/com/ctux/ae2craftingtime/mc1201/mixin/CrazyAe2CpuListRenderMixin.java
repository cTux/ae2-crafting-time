package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.Point;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.CpuTtcCache;
import com.ctux.ae2craftingtime.core.CpuTtcDisplayOrder;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CPUSelectionList.class, priority = 1100)
public abstract class CrazyAe2CpuListRenderMixin {
    @Shadow
    @Final
    private CraftingStatusMenu menu;

    @Shadow
    @Final
    private Blitter buttonBg;

    @Shadow
    private Rect2i bounds;

    @Shadow
    @Dynamic("CPUSelectionListOrderMixin merged frame")
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$drawnList;

    @Shadow
    @Dynamic("CPUSelectionListOrderMixin merged frame")
    private Map<Integer, CpuTtcCache.CpuView> ae2craftingtime$drawn;

    @Shadow
    @Dynamic("CPUSelectionListOrderMixin merged frame")
    private int ae2craftingtime$drawnScroll;

    @WrapOperation(method = "drawBackgroundLayer", at = @At(value = "INVOKE",
            target = "Ljava/util/List;subList(II)Ljava/util/List;", remap = false),
            order = 10001, require = 1, remap = false)
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$sliceTtcFrame(
            List<CraftingStatusMenu.CraftingCpuListEntry> list, int from, int to,
            Operation<List<CraftingStatusMenu.CraftingCpuListEntry>> original) {
        return CpuTtcClient.ttcOrderActive(menu)
                ? list.subList(from, to)
                : original.call(list, from, to);
    }

    @WrapOperation(method = {"getTooltip", "onMouseUp"}, at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/CPUSelectionList;hitTestCpu(Lappeng/client/Point;)"
                    + "Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuListEntry;", remap = false),
            require = 2, remap = false)
    private CraftingStatusMenu.CraftingCpuListEntry ae2craftingtime$hitTtcFrame(
            CPUSelectionList instance, Point mousePos,
            Operation<CraftingStatusMenu.CraftingCpuListEntry> original) {
        if (!CpuTtcClient.ttcOrderActive(menu)) return original.call(instance, mousePos);
        var index = CpuTtcDisplayOrder.hitIndex(mousePos.getX(), mousePos.getY(), bounds.getX(), bounds.getY(),
                buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), ae2craftingtime$drawnScroll,
                ae2craftingtime$drawnList.size());
        if (index < 0) return null;
        var cpu = ae2craftingtime$drawnList.get(index);
        var drawn = ae2craftingtime$drawn.get(cpu.serial());
        return drawn != null && CpuTtcClient.stillCurrent(menu, drawn) ? cpu : null;
    }
}
