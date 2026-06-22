package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;

public final class TtcDetailsClick {
    public static boolean tryHandle(int button) {
        if (!TtcDetailsKeyMapping.matchesMouse(button) && !TtcDetailsKeyMapping.matchesResetMouse(button)) {
            return false;
        }

        var minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof StatsClickHandler handler)) {
            return false;
        }

        var window = minecraft.getWindow();
        var mouse = minecraft.mouseHandler;
        var mouseX = mouse.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        var mouseY = mouse.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        return handler.ae2craftingtime$handleClickedStats(mouseX, mouseY, button);
    }

    private TtcDetailsClick() {
    }
}
