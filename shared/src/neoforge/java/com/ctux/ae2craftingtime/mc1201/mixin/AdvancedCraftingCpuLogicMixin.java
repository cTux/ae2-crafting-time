package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.api.networking.energy.IEnergyService;
import appeng.me.service.CraftingService;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic", remap = false)
public abstract class AdvancedCraftingCpuLogicMixin {
    @Shadow
    @Final
    private AdvCraftingCPU cpu;

    @Shadow
    @Final
    private int[] usedOps;

    @Redirect(
            method = "executeCrafting",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)V"),
            remap = false)
    private void ae2craftingtime$profileExpectedOutput(ListCraftingInventory inventory, AEKey what, long amount,
            Actionable type) {
        inventory.insert(what, amount, type);
        if (type == Actionable.MODULATE && amount > 0) {
            ProfilerBridge.start(ProfilerBridge.networkId(cpu.getGrid()), cpu, what, amount,
                    cpu.getLevel().getGameTime());
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$profileCompletedOutput(AEKey what, long amount, Actionable type,
            CallbackInfoReturnable<Long> cir) {
        if (type != Actionable.MODULATE || what == null || amount <= 0) {
            return;
        }

        var waiting = ((AdvCraftingCPULogic) (Object) this).getWaitingFor(what);
        var accepted = Math.min(amount, waiting);
        if (accepted > 0) {
            ProfilerBridge.complete(ProfilerBridge.networkId(cpu.getGrid()), cpu, what, accepted,
                    cpu.getLevel().getGameTime());
        }
    }

    @Inject(method = "finishJob", at = @At("HEAD"), remap = false, require = 0)
    private void ae2craftingtime$clearPendingOutputs(boolean success, CallbackInfo ci) {
        ProfilerBridge.finishJob(cpu, success, cpu.getLevel().getGameTime(), System.nanoTime());
    }

    @Inject(method = "trySubmitJob", at = @At("RETURN"), remap = false, require = 0)
    private void ae2craftingtime$startJobAccuracy(IGrid grid, ICraftingPlan plan, IActionSource source,
            ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (cir.getReturnValue().successful()) {
            ProfilerBridge.startJob(ProfilerBridge.networkId(grid), cpu, plan, cpu.getLevel().getGameTime(),
                    System.nanoTime());
        }
    }

    @Inject(method = "tickCraftingLogic", at = @At("RETURN"), remap = false, require = 0)
    private void ae2craftingtime$trackParallelCapacity(IEnergyService energyService,
            CraftingService craftingService, CallbackInfo ci) {
        var totalSlots = cpu.getCoProcessors() + 1;
        var usedSlots = Math.min(totalSlots, usedOps[0] + usedOps[1] + usedOps[2]);
        ProfilerBridge.updateCapacity(cpu, usedSlots, totalSlots, cpu.getLevel().getGameTime());
    }
}
