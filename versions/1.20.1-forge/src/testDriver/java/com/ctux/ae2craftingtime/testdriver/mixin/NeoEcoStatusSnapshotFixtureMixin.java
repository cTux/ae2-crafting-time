package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuStatusPanel$ServerState", remap = false)
public abstract class NeoEcoStatusSnapshotFixtureMixin {
    @Inject(method = "snapshot", at = @At("RETURN"), cancellable = true, require = 0)
    private void omitPayload(CallbackInfoReturnable<CompoundTag> callback) {
        if (!NeoEcoStatusFixture.omitRowPayload()) return;
        var snapshot = callback.getReturnValue().copy();
        var items = snapshot.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            items.getCompound(index).remove("ae2craftingtime:status_row");
        }
        callback.setReturnValue(snapshot);
    }
}
