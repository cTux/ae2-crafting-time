package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.me.helpers.InterestManager;
import appeng.me.helpers.StackWatcher;
import appeng.me.service.StorageService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StorageService.class)
public interface StorageServiceAccessor {
    @Accessor(value = "interestManager", remap = false)
    InterestManager<StackWatcher<IStorageWatcherNode>> ae2craftingtime$interestManager();
}
