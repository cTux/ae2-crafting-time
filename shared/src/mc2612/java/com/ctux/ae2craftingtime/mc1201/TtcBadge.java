package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TtcBadge {
    public static final int BACKGROUND = 0xB0000000;

    private TtcBadge() {
    }

    public static void fillSmoothRoundRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int radius,
            int color) {
        var r = radius;
        var width = x2 - x1;
        var height = y2 - y1;
        if (r > width / 2) {
            r = width / 2;
        }
        if (r > height / 2) {
            r = height / 2;
        }
        if (r <= 0) {
            guiGraphics.fill(x1, y1, x2, y2, color);
            return;
        }

        for (var y = y1; y < y2; y++) {
            var dy = Math.min(y - y1, (y2 - 1) - y);
            var inset = dy >= r ? 0
                    : (int) Math.round(Math.sqrt((double) r * r - (double) (r - dy) * (r - dy)));
            guiGraphics.fill(x1 + inset, y, x2 - inset, y + 1, color);
        }
    }
}
