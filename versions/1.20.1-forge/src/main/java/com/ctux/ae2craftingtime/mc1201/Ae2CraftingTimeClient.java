package com.ctux.ae2craftingtime.mc1201;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;

@Mod.EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Ae2CraftingTimeClient {
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new OptionsScreen(parent)));
    }
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ClientOptionsRuntime.initialize(FMLPaths.CONFIGDIR.get());
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        IntegrationLog.required("key-registration", () -> event.register(TtcDetailsKeyMapping.showDetails()));
    }

    private Ae2CraftingTimeClient() {
    }
}
