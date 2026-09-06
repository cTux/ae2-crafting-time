package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderTarget;
import com.ctux.ae2craftingtime.mc1201.ProviderDispatchContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderLogicMixin {
    @Invoker(value = "findAdapter", remap = false)
    protected abstract PatternProviderTarget ae2craftingtime$findAdapter(Direction direction);

    @Invoker(value = "adapterAcceptsAll", remap = false)
    protected abstract boolean ae2craftingtime$adapterAcceptsAll(PatternProviderTarget target, KeyCounter[] input);

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/helpers/patternprovider/PatternProviderLogic;getCraftingLockedReason()Lappeng/api/config/LockCraftingMode;"),
            remap = false)
    private LockCraftingMode ae2craftingtime$observeLock(PatternProviderLogic provider) {
        var reason = provider.getCraftingLockedReason();
        ProviderDispatchContext.lock(provider, reason);
        return reason;
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/implementations/blockentities/ICraftingMachine;acceptsPlans()Z"), remap = false)
    private boolean ae2craftingtime$observeDedicatedMachine(ICraftingMachine machine) {
        var accepts = machine.acceptsPlans();
        ProviderDispatchContext.dedicated(this, accepts);
        return accepts;
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/implementations/blockentities/ICraftingMachine;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z"),
            remap = false)
    private boolean ae2craftingtime$observeDedicatedResult(ICraftingMachine machine, IPatternDetails pattern,
            KeyCounter[] input, Direction direction) {
        var accepted = machine.pushPattern(pattern, input, direction);
        ProviderDispatchContext.dedicatedResult(this, accepted);
        return accepted;
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/helpers/patternprovider/PatternProviderLogic;findAdapter(Lnet/minecraft/core/Direction;)Lappeng/helpers/patternprovider/PatternProviderTarget;"),
            remap = false)
    private PatternProviderTarget ae2craftingtime$observeTarget(PatternProviderLogic provider, Direction direction) {
        var target = ae2craftingtime$findAdapter(direction);
        ProviderDispatchContext.target(provider, target != null);
        return target;
    }

    @ModifyExpressionValue(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/helpers/patternprovider/PatternProviderTarget;containsPatternInput(Ljava/util/Set;)Z"),
            remap = false)
    private boolean ae2craftingtime$observeBlocking(boolean blocked) {
        ProviderDispatchContext.blocked(blocked);
        return blocked;
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/helpers/patternprovider/PatternProviderLogic;adapterAcceptsAll(Lappeng/helpers/patternprovider/PatternProviderTarget;[Lappeng/api/stacks/KeyCounter;)Z"),
            remap = false)
    private boolean ae2craftingtime$observeInputAcceptance(PatternProviderLogic provider,
            PatternProviderTarget target, KeyCounter[] input) {
        var accepts = ae2craftingtime$adapterAcceptsAll(target, input);
        ProviderDispatchContext.acceptsInputs(provider, accepts);
        return accepts;
    }
}
