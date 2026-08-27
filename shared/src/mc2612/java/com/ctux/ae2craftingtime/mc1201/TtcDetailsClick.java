package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

public final class TtcDetailsClick {
    public static boolean tryHandle(MouseButtonEvent event) {
        if (!TtcDetailsKeyMapping.matchesMouse(event) && !TtcDetailsKeyMapping.matchesResetMouse(event)) {
            return false;
        }

        var minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof StatsClickHandler handler)) {
            return false;
        }

        return handler.ae2craftingtime$handleClickedStats(
                event.x(), event.y(), event.button(), TtcDetailsKeyMapping.matchesResetMouse(event));
    }

    private TtcDetailsClick() {
    }
}
