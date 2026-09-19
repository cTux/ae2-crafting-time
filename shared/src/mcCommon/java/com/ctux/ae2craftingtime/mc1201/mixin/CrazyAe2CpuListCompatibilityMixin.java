package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import java.util.List;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CPUSelectionList.class, priority = 900)
public abstract class CrazyAe2CpuListCompatibilityMixin {
    @Shadow
    @Final
    private CraftingStatusMenu menu;

    @Dynamic("Crazy AE2 Addons 2.6.2 handler merged at priority 1000")
    @Inject(method = "sortThenSlice(Ljava/util/List;II)Ljava/util/List;", at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void ae2craftingtime$keepTtcOrder(List<CraftingStatusMenu.CraftingCpuListEntry> list,
            int from, int to, CallbackInfoReturnable<List<CraftingStatusMenu.CraftingCpuListEntry>> cir) {
        if (CpuTtcClient.ttcOrderActive(menu)) {
            cir.setReturnValue(list.subList(from, to));
        }
    }

    @Dynamic("Crazy AE2 Addons 2.6.2 handler merged at priority 1000")
    @Inject(method = "hitTestOnSorted(Lappeng/client/Point;"
            + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void ae2craftingtime$useFrameHitTest(CallbackInfo ci) {
        if (CpuTtcClient.ttcOrderActive(menu)) {
            ci.cancel();
        }
    }
}
