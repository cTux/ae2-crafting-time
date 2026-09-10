package com.ctux.ae2craftingtime.testdriver.mixin;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(JoinMultiplayerScreen.class)
public interface JoinMultiplayerScreenAccessor {
    @Accessor("editingServer")
    void ae2craftingtime_test_driver$setEditingServer(ServerData server);

    @Invoker("directJoinCallback")
    void ae2craftingtime_test_driver$directJoinCallback(boolean result);
}
