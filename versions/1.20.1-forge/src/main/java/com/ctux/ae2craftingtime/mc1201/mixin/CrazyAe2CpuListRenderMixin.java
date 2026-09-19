package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.Point;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.CpuTtcCache;
import com.ctux.ae2craftingtime.core.CpuTtcDisplayOrder;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @ModifyVariable(method = "drawBackgroundLayer", at = @At("STORE"), ordinal = 0, require = 1, remap = false)
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$sliceTtcFrame(
            List<CraftingStatusMenu.CraftingCpuListEntry> original) {
        if (!CpuTtcClient.ttcOrderActive(menu)) return original;
        var from = Math.min(Math.max(0, ae2craftingtime$drawnScroll), ae2craftingtime$drawnList.size());
        return ae2craftingtime$drawnList.subList(from,
                Math.min(from + original.size(), ae2craftingtime$drawnList.size()));
    }

    @Inject(method = "hitTestCpu", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void ae2craftingtime$hitTtcFrame(Point mousePos,
            CallbackInfoReturnable<CraftingStatusMenu.CraftingCpuListEntry> cir) {
        if (!CpuTtcClient.ttcOrderActive(menu)) return;
        var index = CpuTtcDisplayOrder.hitIndex(mousePos.getX(), mousePos.getY(), bounds.getX(), bounds.getY(),
                buttonBg.getSrcWidth(), buttonBg.getSrcHeight(), ae2craftingtime$drawnScroll,
                ae2craftingtime$drawnList.size());
        if (index < 0) {
            cir.setReturnValue(null);
            return;
        }
        var cpu = ae2craftingtime$drawnList.get(index);
        var drawn = ae2craftingtime$drawn.get(cpu.serial());
        cir.setReturnValue(drawn != null && CpuTtcClient.stillCurrent(menu, drawn) ? cpu : null);
    }
}
