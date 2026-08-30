package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.hooks.ticking.TickHandler;
import appeng.me.service.CraftingService;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic", remap = false)
public abstract class Ae2LtTimeWheelCraftingCpuLogicMixin {
    @Unique
    private static Method ae2craftingtime$successfulDispatches;

    @Unique
    private IGrid ae2craftingtime$grid;

    @Unique
    private boolean ae2craftingtime$inserting;

    @Unique
    private boolean ae2craftingtime$deferredFinish;

    @Unique
    private boolean ae2craftingtime$deferredSuccess;

    @Inject(method = "trySubmitJob", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$startJobAccuracy(IGrid grid, ICraftingPlan plan, IActionSource source,
            ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (cir.getReturnValue().successful()) {
            ae2craftingtime$grid = grid;
            ProfilerBridge.startJob(ProfilerBridge.networkId(grid), this, plan, ae2craftingtime$tick(),
                    System.nanoTime());
        }
    }

    @Inject(
            method = "insertWaitingFor(Lcom/moakiee/ae2lt/crafting/timewheel/"
                    + "Ae2LtTimeWheelCraftingCpuLogic$TimeWheelJob;Lappeng/api/stacks/AEKey;J)V",
            at = @At("HEAD"),
            remap = false)
    private void ae2craftingtime$profileExpectedOutput(@Coerce Object activeJob, AEKey what, long amount,
            CallbackInfo ci) {
        if (amount > 0) {
            ProfilerBridge.start(ProfilerBridge.networkId(ae2craftingtime$grid), this, what, amount,
                    ae2craftingtime$tick());
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beginCompletedOutput(AEKey what, long amount, Actionable type,
            CallbackInfoReturnable<Long> cir) {
        if (type == Actionable.MODULATE) {
            ae2craftingtime$inserting = true;
            ae2craftingtime$deferredFinish = false;
        }
    }

    @Inject(method = "insert", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$profileCompletedOutput(AEKey what, long amount, Actionable type,
            CallbackInfoReturnable<Long> cir) {
        if (type != Actionable.MODULATE) {
            return;
        }

        var accepted = cir.getReturnValue();
        if (accepted > 0) {
            ProfilerBridge.complete(ProfilerBridge.networkId(ae2craftingtime$grid), this, what, accepted,
                    ae2craftingtime$tick());
        }
        ae2craftingtime$inserting = false;
        if (ae2craftingtime$deferredFinish) {
            ProfilerBridge.finishJob(this, ae2craftingtime$deferredSuccess, ae2craftingtime$tick(), System.nanoTime());
        }
    }

    @Inject(method = "finishJob", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$finishJob(boolean success, CallbackInfo ci) {
        if (ae2craftingtime$inserting) {
            ae2craftingtime$deferredFinish = true;
            ae2craftingtime$deferredSuccess = success;
        } else {
            ProfilerBridge.finishJob(this, success, ae2craftingtime$tick(), System.nanoTime());
        }
    }

    @Inject(
            method = "tickCraftingLogic(Lappeng/api/networking/energy/IEnergyService;"
                    + "Lappeng/me/service/CraftingService;IJLcom/moakiee/thunderbolt/core/crafting/batch/"
                    + "TickProviderDispatchSchedule;)Lcom/moakiee/ae2lt/crafting/timewheel/"
                    + "Ae2LtTimeWheelCraftingCpuLogic$TickUsage;",
            at = @At("RETURN"),
            remap = false)
    private void ae2craftingtime$trackParallelCapacity(IEnergyService energyService, CraftingService craftingService,
            int maxOps, long maxCopies, @Coerce Object dispatchSchedule, CallbackInfoReturnable<?> cir) {
        ProfilerBridge.updateCapacity(this, Math.min(maxOps, ae2craftingtime$successfulDispatches(cir.getReturnValue())),
                maxOps, ae2craftingtime$tick());
    }

    @Unique
    private static int ae2craftingtime$successfulDispatches(Object usage) {
        try {
            if (ae2craftingtime$successfulDispatches == null) {
                ae2craftingtime$successfulDispatches = usage.getClass().getMethod("successfulDispatches");
            }
            return ((Number) ae2craftingtime$successfulDispatches.invoke(usage)).intValue();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("AE2 Lightning Tech TickUsage API changed", e);
        }
    }

    @Unique
    private long ae2craftingtime$tick() {
        return TickHandler.instance().getCurrentTick();
    }
}
