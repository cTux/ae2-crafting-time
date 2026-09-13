package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingPlan;
import com.ctux.ae2craftingtime.mc1201.PlanRecurrence;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CraftingPlan.class)
public class CraftingPlanMixin implements PlanRecurrence {
    @Unique private Set<AEKey> ae2craftingtime$recurrentKeys = Set.of();
    @Override public Set<AEKey> ae2craftingtime$recurrentKeys() { return ae2craftingtime$recurrentKeys; }
    @Override public void ae2craftingtime$recurrentKeys(Set<AEKey> keys) { ae2craftingtime$recurrentKeys = Set.copyOf(keys); }
}
