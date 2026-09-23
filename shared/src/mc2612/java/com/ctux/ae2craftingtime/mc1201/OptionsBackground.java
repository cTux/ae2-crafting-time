package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Minecraft 26.1.2 paints the background inside Screen.render. */
final class OptionsBackground {
    static void render(Screen screen, GuiGraphics graphics) {
        // super.render paints it after this call.
    }

    private OptionsBackground() { }
}
