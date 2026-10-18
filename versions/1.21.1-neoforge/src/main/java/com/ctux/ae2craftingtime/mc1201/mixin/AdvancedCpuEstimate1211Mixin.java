package com.ctux.ae2craftingtime.mc1201.mixin;

import com.ctux.ae2craftingtime.core.CraftingJobRecovery;
import com.ctux.ae2craftingtime.mc1201.EstimateRecoveryAccess;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.SavedEstimateTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic", remap = false)
public abstract class AdvancedCpuEstimate1211Mixin implements EstimateRecoveryAccess {
    @Shadow @Final private AdvCraftingCPU cpu;
    @Unique private final CraftingJobRecovery ae2craftingtime$recovery = ProfilerBridge.newRecovery();

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("HEAD"), remap = false)
    private void ae2craftingtime$readEstimate(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        ProfilerBridge.readEstimate(cpu, ae2craftingtime$recovery, SavedEstimateTag.read(data));
    }

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("RETURN"), remap = false)
    private void ae2craftingtime$writeEstimate(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!data.contains("job", 10)) return;
        var logic = (AdvCraftingCPULogic) (Object) this;
        var link = logic.getLastLink();
        var output = logic.getFinalJobOutput();
        var bytes = ProfilerBridge.saveEstimate(cpu, ae2craftingtime$recovery, cpu.getLevel().getServer(),
                link == null ? null : link.getCraftingID(), output == null ? null : output.what(), cpu.getGrid());
        SavedEstimateTag.write(data, bytes);
    }

    @Override
    public void ae2craftingtime$recoverEstimate(boolean acceptedOutput) {
        var logic = (AdvCraftingCPULogic) (Object) this;
        var link = logic.getLastLink();
        var output = logic.getFinalJobOutput();
        var grid = cpu.getGrid();
        if (acceptedOutput && (link == null || output == null || grid == null)) {
            ae2craftingtime$recovery.missedOutput();
            return;
        }
        ProfilerBridge.restoreEstimate(cpu, ae2craftingtime$recovery, cpu.getLevel().getServer(),
                link == null ? null : link.getCraftingID(), output == null ? null : output.what(), grid, acceptedOutput);
    }

    @Override public void ae2craftingtime$clearRecovery() { ae2craftingtime$recovery.clear(); }
}
