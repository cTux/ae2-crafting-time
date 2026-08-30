package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TtcBadge {
    public static final int BACKGROUND = 0xB0000000;

    private TtcBadge() {
    }

    public static void fillRoundedRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 2, y1, x2 - 2, y2, color);
        guiGraphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, color);
        guiGraphics.fill(x1, y1 + 2, x2, y2 - 2, color);
    }
}
