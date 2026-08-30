package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "cn.dancingsnow.neoecoae.impl.crafting.fastpath.external.GTLCorePatternBufferDispatcher",
        remap = false)
public abstract class NeoEcoGtlPatternBufferDispatcherMixin {
    @Unique
    private static final ThreadLocal<Integer> ae2craftingtime$insertIndex = new ThreadLocal<>();

    @Inject(method = "dispatch", at = @At("HEAD"), remap = false)
    private static void ae2craftingtime$beginDispatch(CallbackInfoReturnable<Long> cir) {
        ae2craftingtime$insertIndex.set(0);
    }

    @Inject(method = "insertScaled", at = @At("HEAD"), remap = false)
    private static void ae2craftingtime$profileExpectedOutputs(ListCraftingInventory inventory, KeyCounter counter,
            long multiplier, CallbackInfo ci) {
        var index = ae2craftingtime$insertIndex.get();
        if (index == null) {
            return;
        }
        ae2craftingtime$insertIndex.set(index + 1);
        if (index != 0) {
            return;
        }

        for (var entry : counter) {
            var amount = entry.getLongValue() <= 0 || multiplier <= 0
                    ? 0
                    : entry.getLongValue() > Long.MAX_VALUE / multiplier
                            ? Long.MAX_VALUE
                            : entry.getLongValue() * multiplier;
            AddonCpuProfilingContext.start(entry.getKey(), amount);
        }
    }

    @Inject(method = "dispatch", at = @At("RETURN"), remap = false)
    private static void ae2craftingtime$finishDispatch(CallbackInfoReturnable<Long> cir) {
        ae2craftingtime$insertIndex.remove();
    }
}
