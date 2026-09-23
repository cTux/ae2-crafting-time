package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Screen.render does not paint the background through Minecraft 1.21.1. */
final class OptionsBackground {
    static void render(Screen screen, GuiGraphics graphics) {
        screen.renderBackground(graphics);
    }

    private OptionsBackground() { }
}
