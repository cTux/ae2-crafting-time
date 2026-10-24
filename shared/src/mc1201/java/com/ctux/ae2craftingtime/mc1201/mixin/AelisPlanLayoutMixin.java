package com.ctux.ae2craftingtime.mc1201.mixin;

import com.ctux.ae2craftingtime.core.AelisPlanTtc;
import com.ctux.ae2craftingtime.mc1201.AelisTtc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "com.appliedenhancements.client.plan.AelisPlanLayout", remap = false)
public abstract class AelisPlanLayoutMixin {
    @Shadow public abstract int width();

    @ModifyConstant(method = "fit", constant = @Constant(intValue = 360), require = 0)
    private static int ae2craftingtime$panelCap(int original, int screenWidth, int screenHeight) {
        return AelisPlanTtc.panelCap(screenWidth, AelisTtc.columnsEnabled());
    }

    @ModifyArg(method = "columns", at = @At(value = "INVOKE",
            target = "Lcom/appliedenhancements/client/plan/AelisPlanLayout$Columns;<init>(IZ)V"),
            index = 0, require = 0)
    private int ae2craftingtime$columnEnd(int right) {
        return right - AelisPlanTtc.reservation(width());
    }
}
