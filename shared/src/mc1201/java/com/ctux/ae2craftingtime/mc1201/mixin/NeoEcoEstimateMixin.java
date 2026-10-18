package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import com.ctux.ae2craftingtime.core.CraftingJobRecovery;
import com.ctux.ae2craftingtime.mc1201.EstimateRecoveryAccess;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.SavedEstimateTag;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic", remap = false)
public abstract class NeoEcoEstimateMixin implements EstimateRecoveryAccess {
    @Shadow(remap = false) public abstract UUID getCraftingJobId();
    @Shadow(remap = false) public abstract GenericStack getFinalJobOutput();
    @Unique private final CraftingJobRecovery ae2craftingtime$recovery = ProfilerBridge.newRecovery();
    @Unique private IGrid ae2craftingtime$estimateGrid;

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("HEAD"), remap = false)
    private void ae2craftingtime$readEstimate(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        ae2craftingtime$estimateGrid = null;
        ProfilerBridge.readEstimate(this, ae2craftingtime$recovery, SavedEstimateTag.read(data));
    }

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("RETURN"), remap = false)
    private void ae2craftingtime$writeEstimate(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!data.contains("job", 10)) return;
        var output = getFinalJobOutput();
        var bytes = ProfilerBridge.saveEstimate(this, ae2craftingtime$recovery, ProfilerBridge.recoveryServer(),
                getCraftingJobId(), output == null ? null : output.what(), ae2craftingtime$estimateGrid);
        SavedEstimateTag.write(data, bytes);
    }

    @Inject(method = "onRestoredToGrid(Lappeng/api/networking/IGrid;)Z", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$restoredGrid(IGrid grid, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) ae2craftingtime$estimateGrid = grid;
    }

    @Inject(method = "trySubmitJob", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$submittedGrid(IGrid grid, ICraftingPlan plan, IActionSource source,
            ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (cir.getReturnValue().successful()) ae2craftingtime$estimateGrid = grid;
    }

    @Override
    public void ae2craftingtime$recoverEstimate(boolean acceptedOutput) {
        var output = getFinalJobOutput();
        if (acceptedOutput && (getCraftingJobId() == null || output == null || ae2craftingtime$estimateGrid == null)) {
            ae2craftingtime$recovery.missedOutput();
            return;
        }
        ProfilerBridge.restoreEstimate(this, ae2craftingtime$recovery, ProfilerBridge.recoveryServer(),
                getCraftingJobId(), output == null ? null : output.what(), ae2craftingtime$estimateGrid, acceptedOutput);
    }

    @Override public void ae2craftingtime$clearRecovery() { ae2craftingtime$recovery.clear(); }
}
