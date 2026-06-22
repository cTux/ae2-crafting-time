package com.ctux.ae2craftingtime.mc1201;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class Ae2CraftingTime implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "ae2craftingtime";
    public static final String COMMON_CONFIG_FILE = "ae2craftingtime-common.toml";

    @Override
    public void onInitialize() {
        Ae2CraftingTimeConfig.load(FabricLoader.getInstance().getConfigDir().resolve(COMMON_CONFIG_FILE));
        StatsNetwork.registerServer();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var data = server.overworld().getDataStorage()
                    .computeIfAbsent(Ae2CraftingTimeSavedData::load, Ae2CraftingTimeSavedData::new,
                            Ae2CraftingTimeSavedData.FILE_ID);
            ProfilerBridge.load(data);
        });
    }

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(TtcDetailsKeyMapping.showDetails());
        StatsNetwork.registerClient();
    }
}
