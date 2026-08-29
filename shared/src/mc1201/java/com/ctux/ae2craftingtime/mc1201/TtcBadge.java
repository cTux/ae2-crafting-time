package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.gui.GuiGraphics;

public final class TtcBadge {
    public static final int BACKGROUND = 0xB0000000;

    private TtcBadge() {
    }

    public static void fillRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y2, color);
    }
}
