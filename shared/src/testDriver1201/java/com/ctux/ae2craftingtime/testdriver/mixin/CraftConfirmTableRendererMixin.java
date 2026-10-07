package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = CraftConfirmTableRenderer.class, priority = 1100)
public abstract class CraftConfirmTableRendererMixin {
    @WrapMethod(method = "getEntryDescription", require = 0, remap = false)
    private List<Component> ae2craftingtime_test_driver$description(CraftingPlanSummaryEntry entry,
            Operation<List<Component>> original) {
        var lines = original.call(entry);
        UiObservationStore.description(entry, lines);
        return lines;
    }

    @WrapMethod(method = "getEntryTooltip", require = 0, remap = false)
    private List<Component> ae2craftingtime_test_driver$tooltip(CraftingPlanSummaryEntry entry,
            Operation<List<Component>> original) {
        var lines = original.call(entry);
        UiObservationStore.tooltip(entry, lines);
        return lines;
    }
}
