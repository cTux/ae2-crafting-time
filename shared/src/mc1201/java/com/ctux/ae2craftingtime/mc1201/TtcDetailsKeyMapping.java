package com.ctux.ae2craftingtime.mc1201;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public final class TtcDetailsKeyMapping {
    private static final KeyMapping SHOW_DETAILS = new KeyMapping(
            "key.ae2craftingtime.show_ttc_details",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.categories.ae2craftingtime");

    public static KeyMapping showDetails() {
        return SHOW_DETAILS;
    }

    public static boolean matchesMouse(int button) {
        return SHOW_DETAILS.matchesMouse(button) && Screen.hasControlDown();
    }

    private TtcDetailsKeyMapping() {
    }
}
