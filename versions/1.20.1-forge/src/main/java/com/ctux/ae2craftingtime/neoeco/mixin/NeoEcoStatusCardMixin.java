package com.ctux.ae2craftingtime.neoeco.mixin;

import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusEntry;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCards", remap = false)
public abstract class NeoEcoStatusCardMixin {
    @ModifyVariable(method = "item(Ljava/util/List;Lcn/dancingsnow/neoecoae/gui/computation/ComputationCpuItemEntry;Z)"
            + "Lcn/dancingsnow/neoecoae/platform/ui/runtime/gui/ui/utils/ModularUITooltipComponent;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static List<Component> ae2craftingtime$cardTime(List<Component> tooltip, List<Component> ignored,
            @Coerce Object entry, boolean exact) {
        if (!(entry instanceof NeoEcoStatusEntry row)) return tooltip;
        var estimate = NeoEcoStatusText.estimate(row);
        if (estimate == null) return tooltip;
        var lines = new ArrayList<>(tooltip);
        if (lines.isEmpty()) lines.add(row.key().getDisplayName());
        if (lines.size() == 1) lines.add(Component.empty());
        // NeoEco keeps the first and last components as the name and mod label.
        lines.add(lines.size() - 1, estimate);
        return lines;
    }
}
