package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusEntry;
import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCards", remap = false)
public abstract class NeoEcoStatusCardObservationMixin {
    @Inject(method = "item(Ljava/util/List;Lcn/dancingsnow/neoecoae/gui/computation/ComputationCpuItemEntry;Z)"
            + "Lcn/dancingsnow/neoecoae/platform/ui/runtime/gui/ui/utils/ModularUITooltipComponent;",
            at = @At("RETURN"), require = 0)
    private static void observeCard(List<Component> lines, @Coerce Object entry, boolean exact,
            CallbackInfoReturnable<?> callback) {
        if (NeoEcoStatusFixture.observeRows() && entry instanceof NeoEcoStatusEntry row) {
            NeoEcoStatusFixture.hover(row.key(), lines);
        }
    }
}
