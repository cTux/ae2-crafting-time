package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.stacks.GenericStack;
import com.ctux.ae2craftingtime.core.CraftingJobRecovery;
import com.ctux.ae2craftingtime.mc1201.Ae2LtGrid;
import com.ctux.ae2craftingtime.mc1201.EstimateRecoveryAccess;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.SavedEstimateTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic", remap = false)
public abstract class Ae2LtEstimateMixin implements EstimateRecoveryAccess {
    @Shadow(remap = false) private CompoundTag pendingJobTag;
    @Shadow(remap = false) public abstract ICraftingLink getLastLink();
    @Shadow(remap = false) public abstract GenericStack getFinalJobOutput();
    @Unique private final CraftingJobRecovery ae2craftingtime$recovery = ProfilerBridge.newRecovery();

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("HEAD"), remap = false)
    private void ae2craftingtime$readEstimate(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        ProfilerBridge.readEstimate(this, ae2craftingtime$recovery, SavedEstimateTag.read(data));
    }

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("RETURN"), remap = false)
    private void ae2craftingtime$writeEstimate(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!data.contains("job", 10)) return;
        var job = data.getCompound("job");
        var server = ProfilerBridge.recoveryServer();
        if (pendingJobTag != null && getLastLink() == null) {
            if (!ae2craftingtime$recovery.preservePendingTag(server, ProfilerBridge.trackingEnabled(this)))
                SavedEstimateTag.discard(job);
            return;
        }
        var link = getLastLink();
        var output = getFinalJobOutput();
        var bytes = ProfilerBridge.saveEstimate(this, ae2craftingtime$recovery, server,
                link == null ? null : link.getCraftingID(), output == null ? null : output.what(), Ae2LtGrid.get(this));
        SavedEstimateTag.write(data, bytes);
    }

    @Override
    public void ae2craftingtime$recoverEstimate(boolean acceptedOutput) {
        var link = getLastLink();
        var output = getFinalJobOutput();
        var grid = link == null ? null : Ae2LtGrid.get(this);
        if (acceptedOutput && (link == null || output == null || grid == null)) {
            ae2craftingtime$recovery.missedOutput();
            return;
        }
        ProfilerBridge.restoreEstimate(this, ae2craftingtime$recovery, ProfilerBridge.recoveryServer(),
                link == null ? null : link.getCraftingID(), output == null ? null : output.what(), grid, acceptedOutput);
    }

    @Override public void ae2craftingtime$clearRecovery() { ae2craftingtime$recovery.clear(); }
}
