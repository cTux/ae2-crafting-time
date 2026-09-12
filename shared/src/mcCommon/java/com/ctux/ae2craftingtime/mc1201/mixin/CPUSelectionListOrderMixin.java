package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.CpuTtcCache;
import com.ctux.ae2craftingtime.core.CpuTtcDisplayOrder;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CPUSelectionList.class)
public abstract class CPUSelectionListOrderMixin {
    @Unique
    private static final String AE2CRAFTINGTIME_CPUS =
            "Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuList;cpus()Ljava/util/List;";

    @Shadow
    @Final
    private CraftingStatusMenu menu;

    @Shadow
    @Final
    private Scrollbar scrollbar;

    @Unique
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$displayed = List.of();

    @Unique
    private Map<Integer, CpuTtcCache.CpuView> ae2craftingtime$drawn = Map.of();

    @Unique
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$drawnList = List.of();

    @Unique
    private int ae2craftingtime$drawnScroll = -1;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$open(CallbackInfo ci) {
        CpuTtcClient.open(menu);
    }

    @Inject(method = "updateBeforeRender", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$prepareDisplay(CallbackInfo ci) {
        ae2craftingtime$displayed = CpuTtcClient.displayList(menu);
    }

    @Inject(method = "updateBeforeRender", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$refresh(CallbackInfo ci) {
        CpuTtcClient.refresh(menu, ae2craftingtime$displayed, scrollbar.getCurrentScroll());
    }

    @Inject(method = "drawBackgroundLayer", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beginDraw(CallbackInfo ci) {
        ae2craftingtime$drawnList = List.copyOf(ae2craftingtime$displayed);
        var drawn = new LinkedHashMap<Integer, CpuTtcCache.CpuView>();
        ae2craftingtime$drawnList.forEach(cpu -> drawn.put(cpu.serial(), CpuTtcClient.view(cpu)));
        ae2craftingtime$drawn = Map.copyOf(drawn);
    }

    @WrapOperation(method = "drawBackgroundLayer", at = @At(value = "INVOKE", target = AE2CRAFTINGTIME_CPUS,
            remap = false), remap = false, require = 3)
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$drawList(
            CraftingStatusMenu.CraftingCpuList instance,
            Operation<List<CraftingStatusMenu.CraftingCpuListEntry>> original) {
        return ae2craftingtime$displayed;
    }

    @WrapOperation(method = "hitTestCpu", at = @At(value = "INVOKE", target = AE2CRAFTINGTIME_CPUS,
            remap = false), remap = false, require = 2)
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$hitList(
            CraftingStatusMenu.CraftingCpuList instance,
            Operation<List<CraftingStatusMenu.CraftingCpuListEntry>> original) {
        return ae2craftingtime$drawnScroll < 0 ? List.of() : ae2craftingtime$drawnList;
    }

    @WrapOperation(method = "updateBeforeRender", at = @At(value = "INVOKE", target = AE2CRAFTINGTIME_CPUS,
            remap = false), remap = false, require = 1)
    private List<CraftingStatusMenu.CraftingCpuListEntry> ae2craftingtime$rangeList(
            CraftingStatusMenu.CraftingCpuList instance,
            Operation<List<CraftingStatusMenu.CraftingCpuListEntry>> original) {
        return ae2craftingtime$displayed;
    }

    @WrapOperation(method = "drawBackgroundLayer", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/Scrollbar;getCurrentScroll()I", remap = false),
            remap = false, require = 2)
    private int ae2craftingtime$captureScroll(Scrollbar instance, Operation<Integer> original) {
        ae2craftingtime$drawnScroll = original.call(instance);
        return ae2craftingtime$drawnScroll;
    }

    @WrapOperation(method = "hitTestCpu", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/Scrollbar;getCurrentScroll()I", remap = false),
            remap = false, require = 1)
    private int ae2craftingtime$useDrawnScroll(Scrollbar instance, Operation<Integer> original) {
        return CpuTtcDisplayOrder.inputScroll(ae2craftingtime$drawnScroll);
    }

    @Inject(method = "hitTestCpu", at = @At("RETURN"), cancellable = true, remap = false)
    private void ae2craftingtime$suppressStaleHit(CallbackInfoReturnable<CraftingStatusMenu.CraftingCpuListEntry> cir) {
        var cpu = cir.getReturnValue();
        var drawn = cpu == null ? null : ae2craftingtime$drawn.get(cpu.serial());
        if (drawn == null || !CpuTtcClient.stillCurrent(menu, drawn)) {
            cir.setReturnValue(null);
        }
    }
}
