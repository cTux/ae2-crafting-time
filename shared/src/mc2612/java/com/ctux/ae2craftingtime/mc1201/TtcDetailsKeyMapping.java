package com.ctux.ae2craftingtime.mc1201;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class TtcDetailsKeyMapping {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath("ae2craftingtime", "controls"));
    private static final KeyMapping SHOW_DETAILS = new KeyMapping(
            "key.ae2craftingtime.show_ttc_details",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            CATEGORY);

    public static KeyMapping.Category category() {
        return CATEGORY;
    }

    public static KeyMapping showDetails() {
        return SHOW_DETAILS;
    }

    public static boolean matchesMouse(MouseButtonEvent event) {
        return SHOW_DETAILS.matchesMouse(event) && event.hasControlDown() && !event.hasAltDown();
    }

    public static boolean matchesResetMouse(MouseButtonEvent event) {
        return SHOW_DETAILS.matchesMouse(event) && event.hasControlDown() && event.hasAltDown();
    }

    private TtcDetailsKeyMapping() {
    }
}
