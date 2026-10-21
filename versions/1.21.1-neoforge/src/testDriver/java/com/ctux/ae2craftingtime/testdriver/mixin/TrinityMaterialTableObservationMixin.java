package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.fish_dan_.data_energistics.client.screen.crafting.confirm.TrinityCraftConfirmMaterialTable", remap = false)
public abstract class TrinityMaterialTableObservationMixin {
    @Inject(method = "drawEntry", at = @At("HEAD"), remap = false)
    private void ae2craftingtime_test_driver$row(GuiGraphics graphics, Minecraft minecraft, Font font,
            CraftingPlanSummaryEntry entry, int x, int y, CallbackInfo ci) {
        UiObservationStore.trinityRow(graphics, entry, x, y);
    }
}
