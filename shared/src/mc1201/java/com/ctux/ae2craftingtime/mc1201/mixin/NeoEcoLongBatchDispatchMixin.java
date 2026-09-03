package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.crafting.inv.ListCraftingInventory;
import com.ctux.ae2craftingtime.mc1201.NeoEcoDispatchObserver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic", remap = false)
public abstract class NeoEcoLongBatchDispatchMixin {
    @Redirect(method = "recordPushedPattern(Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob;Lcn/dancingsnow/neoecoae/impl/crafting/fastpath/ECOExtractedPatternExecution;JZ)V",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)V",
                    ordinal = 0), remap = false)
    private void ae2craftingtime$profileExpectedOutput(ListCraftingInventory inventory, AEKey key, long amount,
            Actionable mode) {
        inventory.insert(key, amount, mode);
        ((NeoEcoDispatchObserver) this).ae2craftingtime$dispatched(key, amount, mode);
    }
}
