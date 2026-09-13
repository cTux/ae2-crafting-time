package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.mc1201.RecurrentPlanEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CraftingPlanSummaryEntry.class)
public class CraftingPlanSummaryEntryMixin implements RecurrentPlanEntry {
    @Unique private boolean ae2craftingtime$recurrent;
    @Override public boolean ae2craftingtime$recurrent() { return ae2craftingtime$recurrent; }
    @Override public void ae2craftingtime$recurrent(boolean recurrent) { ae2craftingtime$recurrent = recurrent; }
}
