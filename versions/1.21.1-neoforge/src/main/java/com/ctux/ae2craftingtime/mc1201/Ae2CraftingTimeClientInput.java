package com.ctux.ae2craftingtime.mc1201;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, value = Dist.CLIENT)
public final class Ae2CraftingTimeClientInput {
    @SubscribeEvent
    public static void showTtcDetails(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && TtcDetailsClick.tryShow(event.getButton())) {
            event.setCanceled(true);
        }
    }

    private Ae2CraftingTimeClientInput() {
    }
}
