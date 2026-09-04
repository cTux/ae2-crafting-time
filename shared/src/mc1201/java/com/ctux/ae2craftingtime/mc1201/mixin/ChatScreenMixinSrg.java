package com.ctux.ae2craftingtime.mc1201.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side only. Same chat auto-close as {@link ChatScreenMixin} (see
 * that class for the full rationale), but with the injection remapped for
 * the obfuscated 1.20.1 production mappings (Searge on Forge,
 * intermediary on Fabric). Listed only in the shared 1.20.1 mixin config;
 * the 1.21.1 build excludes this file because its toolchain cannot remap
 * vanilla targets and uses the {@code remap = false} twin instead.
 */
@Mixin(Screen.class)
public abstract class ChatScreenMixinSrg {
    @Inject(method = "handleComponentClicked", at = @At("RETURN"))
    private void ae2craftingtime$closeAfterLocateClick(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || style == null || !((Object) this instanceof ChatScreen)) {
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
