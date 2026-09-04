package com.ctux.ae2craftingtime.mc1201.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side only. Closes the chat after our own locate link is clicked,
 * so the player immediately sees the highlighted provider instead of the
 * chat overlay. Other links behave exactly as before.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("RETURN"))
    private void ae2craftingtime$closeAfterLocateClick(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || style == null) {
            return;
        }
        var click = style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND) {
            return;
        }
        if (!click.getValue().startsWith("/ae2craftingtime locate")) {
            return;
        }
        Minecraft.getInstance().setScreen(null);
    }
}
