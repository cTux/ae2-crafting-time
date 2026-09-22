package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.StackWatcher;
import appeng.menu.me.crafting.CraftingPlanSummary;
import com.ctux.ae2craftingtime.mc1201.StoredVariantMenuState;
import com.ctux.ae2craftingtime.testdriver.StoredVariantObservation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StoredVariantMenuState.class, remap = false)
public abstract class StoredVariantServerObservationMixin {
    @Shadow private StackWatcher<IStorageWatcherNode> watcher;
    @Unique private StackWatcher<IStorageWatcherNode> ae2ct$released;

    @Inject(method = "broadcast", at = @At("HEAD"))
    private void broadcasting(CraftingPlanSummary summary, IGrid grid, ServerPlayer player, int menu,
            long revision, boolean valid, CallbackInfo ci) {
        if (valid) StoredVariantObservation.broadcast(this, summary, grid, player, menu, revision);
    }
    @Inject(method = "onStackChange", at = @At("HEAD"))
    private void changed(AEKey key, long amount, CallbackInfo ci) { StoredVariantObservation.changed(this, key, amount); }
    @Inject(method = "refresh", at = @At("HEAD"))
    private void refreshing(CallbackInfo ci) { StoredVariantObservation.refresh(this); }
    @Inject(method = "send", at = @At("HEAD"))
    private void sending(ServerPlayer player, int menu, long revision, java.util.Set<Integer> rows, CallbackInfo ci) {
        StoredVariantObservation.sent(player, menu, revision, rows);
    }
    @Inject(method = "broadcast", at = @At("RETURN"))
    private void registered(CraftingPlanSummary summary, IGrid grid, ServerPlayer player, int menu,
            long revision, boolean valid, CallbackInfo ci) {
        StoredVariantObservation.registered(this, watcher);
    }
    @Inject(method = "releaseWatcher", at = @At("HEAD"))
    private void releasing(CallbackInfo ci) { ae2ct$released = watcher; }
    @Inject(method = "releaseWatcher", at = @At("RETURN"))
    private void released(CallbackInfo ci) { StoredVariantObservation.released(this, ae2ct$released); }
}
