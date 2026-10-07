package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.gui.me.crafting.CraftingStatusTableRenderer;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/** Preserve translated status semantics before AE2 renders flattened styled text. */
@Mixin(value = CraftingStatusTableRenderer.class, priority = 900)
public abstract class CraftingStatusTableRendererObservationMixin {
    @WrapMethod(method = "getEntryDescription", require = 0, remap = false)
    private List<Component> ae2craftingtime_test_driver$description(CraftingStatusEntry entry,
            Operation<List<Component>> original) {
        var lines = original.call(entry);
        UiObservationStore.description(entry, lines);
        return lines;
    }

    @WrapMethod(method = "getEntryTooltip", require = 0, remap = false)
    private List<Component> ae2craftingtime_test_driver$tooltip(CraftingStatusEntry entry,
            Operation<List<Component>> original) {
        var lines = original.call(entry);
        UiObservationStore.tooltip(entry, lines);
        return lines;
    }
}
