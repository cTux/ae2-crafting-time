package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CraftConfirmTableRenderer.class)
public abstract class CraftConfirmTableRendererMixin {
    @Inject(method = "getEntryDescription", at = @At("RETURN"), remap = false)
    private void ae2craftingtime_test_driver$description(CraftingPlanSummaryEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        UiObservationStore.description(entry, cir.getReturnValue());
    }

    @Inject(method = "getEntryTooltip", at = @At("RETURN"), remap = false)
    private void ae2craftingtime_test_driver$tooltip(CraftingPlanSummaryEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        UiObservationStore.tooltip(entry, cir.getReturnValue());
    }
}
