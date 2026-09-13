package com.ctux.ae2craftingtime.mc1201.mixin;
import appeng.api.networking.IGrid; import appeng.api.networking.crafting.ICraftingPlan; import appeng.api.networking.security.IActionSource; import appeng.menu.me.crafting.CraftingPlanSummary;
import com.ctux.ae2craftingtime.mc1201.PlanRecurrence; import com.ctux.ae2craftingtime.mc1201.RecurrentPlanEntry;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.At; import org.spongepowered.asm.mixin.injection.Inject; import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CraftingPlanSummary.class) public class CraftingPlanSummaryMixin {
 @Inject(method="fromJob",at=@At("RETURN"),remap=false) private static void mark(IGrid grid,IActionSource source,ICraftingPlan job,CallbackInfoReturnable<CraftingPlanSummary> cir){
  if(!(job instanceof PlanRecurrence r)||r.ae2craftingtime$recurrentKeys().isEmpty())return;
  for(var e:cir.getReturnValue().getEntries())if(e.getMissingAmount()>0&&r.ae2craftingtime$recurrentKeys().contains(e.getWhat()))((RecurrentPlanEntry)e).ae2craftingtime$recurrent(true);
 }
}
