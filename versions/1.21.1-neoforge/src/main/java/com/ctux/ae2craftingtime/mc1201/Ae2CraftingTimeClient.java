package com.ctux.ae2craftingtime.mc1201;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, value = Dist.CLIENT)
public final class Ae2CraftingTimeClient {
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        IntegrationLog.required("key-registration", () -> event.register(TtcDetailsKeyMapping.showDetails()));
    }

    private Ae2CraftingTimeClient() {
    }
}
