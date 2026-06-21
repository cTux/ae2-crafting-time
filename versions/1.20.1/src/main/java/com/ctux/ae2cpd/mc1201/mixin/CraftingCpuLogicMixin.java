package com.ctux.ae2cpd.mc1201.mixin;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ctux.ae2cpd.mc1201.ProfilerBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingCpuLogic.class)
public abstract class CraftingCpuLogicMixin {
    @Shadow(remap = false)
    @Final
    private CraftingCPUCluster cluster;

    @Redirect(
            method = "executeCrafting",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"),
            remap = false)
    private boolean ae2cpd$profileStartedPattern(ICraftingProvider provider, IPatternDetails details,
            KeyCounter[] inputHolder) {
        var pushed = provider.pushPattern(details, inputHolder);
        if (pushed) {
            ProfilerBridge.start(details.getPrimaryOutput(), cluster.getLevel().getGameTime());
        }
        return pushed;
    }

    @Inject(method = "insert", at = @At("HEAD"), remap = false)
    private void ae2cpd$profileCompletedOutput(AEKey what, long amount, Actionable type,
            CallbackInfoReturnable<Long> cir) {
        if (type != Actionable.MODULATE || what == null || amount <= 0) {
            return;
        }

        var waiting = ((CraftingCpuLogic) (Object) this).getWaitingFor(what);
        var accepted = Math.min(amount, waiting);
        if (accepted > 0) {
            ProfilerBridge.complete(what, accepted, cluster.getLevel().getGameTime());
        }
    }
}
