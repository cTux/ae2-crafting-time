package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(Ae2CraftingTime.MOD_ID)
public final class Ae2CraftingTime {
    public static final String MOD_ID = "ae2craftingtime";
    public static final String COMMON_CONFIG_FILE = "ae2craftingtime-common.toml";

    public Ae2CraftingTime(IEventBus modBus, ModContainer modContainer) {
        IntegrationLog.start("1.21.1-neoforge", net.neoforged.fml.loading.FMLEnvironment.dist.isClient(), "neoforge",
                id -> net.neoforged.fml.ModList.get().getModContainerById(id).map(mod -> mod.getModInfo().getVersion().toString()).orElse(null));
        IntegrationLog.required("config-registration", () -> modContainer.registerConfig(ModConfig.Type.COMMON, Ae2CraftingTimeConfig.SPEC, COMMON_CONFIG_FILE));
        modBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getModId().equals(MOD_ID)) IntegrationLog.configuration();
        });
        modBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getModId().equals(MOD_ID)) IntegrationLog.configuration();
        });
        modBus.addListener(StatsNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        IntegrationLog.summary();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(ProviderLocateCommand.build((source, id) ->
                ProviderLocateCommand.locate(source, id, (player, highlight) -> StatsNetwork.sendTo(player,
                        new ProviderHighlightS2C(highlight.networkId(), highlight.dimensionId(),
                                highlight.positions(), highlight.outputId(), highlight.durationSeconds(),
                                highlight.plateOnly())))));
    }

    private void onServerStarted(ServerStartedEvent event) {
        var data = event.getServer().overworld().getDataStorage()
                .computeIfAbsent(Ae2CraftingTimeSavedData.FACTORY, Ae2CraftingTimeSavedData.FILE_ID);
        ProfilerBridge.load(data);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ProfilerBridge.resyncPlatesForPlayer(player);
        }
    }
}
