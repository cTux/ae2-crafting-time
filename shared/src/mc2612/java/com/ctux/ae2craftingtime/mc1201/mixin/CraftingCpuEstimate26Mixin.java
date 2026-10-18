package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ctux.ae2craftingtime.core.CraftingJobRecovery;
import com.ctux.ae2craftingtime.mc1201.EstimateRecoveryAccess;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.SavedEstimateValue;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingCpuLogic.class)
public abstract class CraftingCpuEstimate26Mixin implements EstimateRecoveryAccess {
    @Shadow(remap = false) @Final private CraftingCPUCluster cluster;
    @Unique private final CraftingJobRecovery ae2craftingtime$recovery = ProfilerBridge.newRecovery();

    @Inject(method = "readFromNBT(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$readEstimate(ValueInput data, CallbackInfo ci) {
        ProfilerBridge.readEstimate(cluster, ae2craftingtime$recovery, SavedEstimateValue.read(data));
    }

    @ModifyArg(method = "writeToNBT(Lnet/minecraft/world/level/storage/ValueOutput;)V",
            at = @At(value = "INVOKE", target = "Lappeng/crafting/execution/ExecutingCraftingJob;"
                    + "writeToNBT(Lnet/minecraft/world/level/storage/ValueOutput;)V"), index = 0, remap = false)
    private ValueOutput ae2craftingtime$writeEstimate(ValueOutput job) {
        var logic = (CraftingCpuLogic) (Object) this;
        var link = logic.getLastLink();
        var output = logic.getFinalJobOutput();
        SavedEstimateValue.write(job, ProfilerBridge.saveEstimate(cluster, ae2craftingtime$recovery,
                cluster.getLevel().getServer(), link == null ? null : link.getCraftingID(),
                output == null ? null : output.what(), cluster.getGrid()));
        return job;
    }

    @Override
    public void ae2craftingtime$recoverEstimate(boolean acceptedOutput) {
        var logic = (CraftingCpuLogic) (Object) this;
        var link = logic.getLastLink();
        var output = logic.getFinalJobOutput();
        var grid = cluster.getGrid();
        if (acceptedOutput && (link == null || output == null || grid == null)) {
            ae2craftingtime$recovery.missedOutput();
            return;
        }
        ProfilerBridge.restoreEstimate(cluster, ae2craftingtime$recovery, cluster.getLevel().getServer(),
                link == null ? null : link.getCraftingID(), output == null ? null : output.what(), grid, acceptedOutput);
    }

    @Override public void ae2craftingtime$clearRecovery() { ae2craftingtime$recovery.clear(); }
}
