package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.stacks.AEItemKey;
import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuStatusPanel$ServerState", remap = false)
public abstract class NeoEcoStatusSnapshotFixtureMixin {
    @Inject(method = "snapshot", at = @At("RETURN"), cancellable = true, require = 0)
    private void adjustItemPayload(CallbackInfoReturnable<CompoundTag> callback) {
        boolean omit = NeoEcoStatusFixture.omitRowPayload();
        boolean widen = NeoEcoStatusFixture.widenGoldEstimate();
        if (!omit && !widen) return;
        var snapshot = callback.getReturnValue().copy();
        var items = snapshot.getList("items", Tag.TAG_COMPOUND);
        var gold = AEItemKey.of(Items.GOLD_INGOT).toTagGeneric();
        for (int index = 0; index < items.size(); index++) {
            var row = items.getCompound(index);
            if (omit) {
                row.remove("ae2craftingtime:status_row");
            } else if (row.getCompound("key").equals(gold)) {
                row.getCompound("ae2craftingtime:status_row")
                        .putDouble("millis", NeoEcoStatusFixture.WIDE_MILLIS);
            }
        }
        callback.setReturnValue(snapshot);
    }
}
