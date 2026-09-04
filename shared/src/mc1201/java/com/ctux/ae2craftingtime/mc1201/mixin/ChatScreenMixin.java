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
 * Client-side only. Closes the chat after our own locate link is clicked,
 * so the player immediately sees the highlighted provider instead of the
 * chat overlay. Other links behave exactly as before.
 *
 * <p>Targets {@code Screen} rather than {@code ChatScreen} because
 * {@code handleComponentClicked} is declared on {@code Screen}: targeting
 * the subclass never resolves and the mixin fails to apply (see the
 * dev-client log for issue #241).
 *
 * <p>Official-names variant for loaders whose toolchain cannot remap
 * vanilla targets (1.21.1 NeoForge): listed only in the 1.21.1 mixin
 * config. The 1.20.1 loaders list {@link ChatScreenMixinSrg} instead, which
 * remaps the same injection for obfuscated production. The two classes are
 * intentionally identical apart from {@code remap}.
 */
@Mixin(Screen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("RETURN"), remap = false)
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
