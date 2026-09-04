package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Ae2CraftingTime.MOD_ID)
@SuppressWarnings("removal")
public final class Ae2CraftingTime {
    public static final String MOD_ID = "ae2craftingtime";
    public static final String COMMON_CONFIG_FILE = "ae2craftingtime-common.toml";

    public Ae2CraftingTime() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Ae2CraftingTimeConfig.SPEC, COMMON_CONFIG_FILE);
        StatsNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(ProviderLocateCommand.build((source, id) ->
                ProviderLocateCommand.locate(source, id, (player, record) -> StatsNetwork.sendTo(player,
                        new ProviderHighlightS2C(record.dimensionId(), record.positions(), record.outputId(),
                                ProviderLocateCommand.HIGHLIGHT_SECONDS, false)))));
    }

    private void onServerStarted(ServerStartedEvent event) {
        var data = event.getServer().overworld().getDataStorage()
                .computeIfAbsent(Ae2CraftingTimeSavedData::load, Ae2CraftingTimeSavedData::new,
                        Ae2CraftingTimeSavedData.FILE_ID);
        ProfilerBridge.load(data);
    }
}
