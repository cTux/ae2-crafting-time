package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CPUSelectionList.class, priority = 900)
public abstract class CrazyAe2CpuListRenderMixin {
    @Shadow
    @Final
    private CraftingStatusMenu menu;

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
}
