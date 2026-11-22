package com.ctux.ae2craftingtime.neoeco.mixin;

import com.ctux.ae2craftingtime.core.TtcEstimate;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusEntry;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuItemEntry", remap = false)
public abstract class NeoEcoStatusEntryMixin implements NeoEcoStatusEntry {
    @Unique private TtcEstimate ae2craftingtime$estimate;

    @Override
    public TtcEstimate ae2craftingtime$estimate() {
        return ae2craftingtime$estimate;
    }

    @Override
    public void ae2craftingtime$estimate(TtcEstimate value) {
        ae2craftingtime$estimate = value;
    }

    @Inject(method = "read", at = @At("RETURN"), require = 0)
    private static void ae2craftingtime$read(CompoundTag tag, HolderLookup.Provider registries,
            CallbackInfoReturnable<Object> callback) {
        if (callback.getReturnValue() instanceof NeoEcoStatusEntry row) {
            row.ae2craftingtime$estimate(NeoEcoStatusPayload.read(tag));
        }
    }

    @Inject(method = "withProcessing", at = @At("RETURN"), require = 0)
    private void ae2craftingtime$copy(CallbackInfoReturnable<Object> callback) {
        if (callback.getReturnValue() instanceof NeoEcoStatusEntry row) {
            row.ae2craftingtime$estimate(ae2craftingtime$estimate);
        }
    }
}
