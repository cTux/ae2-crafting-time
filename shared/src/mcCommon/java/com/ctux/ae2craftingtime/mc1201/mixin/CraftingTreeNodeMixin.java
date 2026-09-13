package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import com.ctux.ae2craftingtime.core.RecurrentMissing;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingTreeNode.class)
public abstract class CraftingTreeNodeMixin {
    @Shadow private ArrayList<CraftingTreeProcess> nodes;
    @Unique private boolean ae2craftingtime$rejected;

    @WrapOperation(method = "buildChildPatterns", at = @At(value = "INVOKE",
            target = "Lappeng/crafting/CraftingTreeProcess;notRecursive(Lappeng/api/crafting/IPatternDetails;)Z"), remap = false)
    private boolean ae2craftingtime$observe(CraftingTreeProcess parent, IPatternDetails details,
            Operation<Boolean> original) {
        var allowed = original.call(parent, details);
        if (!allowed) ae2craftingtime$rejected = true;
        return allowed;
    }

    @WrapOperation(method = "request", at = @At(value = "INVOKE",
            target = "Lappeng/crafting/CraftingCalculation;addMissing(Lappeng/api/stacks/AEKey;J)V"), remap = false)
    private void ae2craftingtime$record(CraftingCalculation calculation, AEKey key, long amount,
            Operation<Void> original) {
        if (RecurrentMissing.record(ae2craftingtime$rejected, nodes != null && !nodes.isEmpty(), amount)) {
            ((com.ctux.ae2craftingtime.mc1201.RecurrentCalculation) calculation).ae2craftingtime$record(key);
        }
        original.call(calculation, key, amount);
    }
}
