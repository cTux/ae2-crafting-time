package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

public final class Ae2CraftingTime implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "ae2craftingtime";
    public static final String COMMON_CONFIG_FILE = "ae2craftingtime-common.toml";

    @Override
    public void onInitialize() {
        Ae2CraftingTimeConfig.load(FabricLoader.getInstance().getConfigDir().resolve(COMMON_CONFIG_FILE));
        StatsNetwork.registerServer();
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
                ProviderLocateCommand.build((source, id) -> ProviderLocateCommand.locate(source, id,
                        (player, highlight) -> StatsNetwork.sendTo(player, new ProviderHighlightS2C(
                                highlight.networkId(), highlight.dimensionId(), highlight.positions(),
                                highlight.outputId(), highlight.durationSeconds(), highlight.plateOnly()))))));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var data = server.overworld().getDataStorage()
                    .computeIfAbsent(Ae2CraftingTimeSavedData::load, Ae2CraftingTimeSavedData::new,
                            Ae2CraftingTimeSavedData.FILE_ID);
            ProfilerBridge.load(data);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ProfilerBridge
                .resyncPlatesForPlayer(handler.getPlayer()));
    }

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(TtcDetailsKeyMapping.showDetails());
        StatsNetwork.registerClient();
        // Drop rainbows and plates when leaving a world or server so they
        // never leak into another world with matching coordinates. Red plates
        // return only via server-approved resync.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ProviderHighlightClient.onSessionEnd());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            var levelDimension = minecraft.level.dimension().location().toString();
            // A broken provider target drops its edge and plate immediately, in
            // this dimension only. Replacement with another block entity and a
            // surviving host without its provider part both count as broken;
            // unloaded chunks stay unknown so reload never clears intact red.
            ProviderHighlightClient.trimPositions(levelDimension,
                    pos -> ProviderBlockTargets.keepForHighlight(minecraft.level, pos));
            var highlight = ProviderHighlightClient.live();
            var camera = context.camera().getPosition();
            var poseStack = context.matrixStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            var consumers = context.consumers();
            if (consumers == null) {
                poseStack.popPose();
                return;
            }
            var rainbow = ProviderHighlightClient.rainbowRgb();
            var alpha = ProviderHighlightClient.pulseAlpha();
            // Click edges first: plate and item writes switch the shared fallback
            // builder to other render types, so one pass per type. Each identity
            // keeps its own edge so two locates within 15s stay independent.
            for (var highlight : ProviderHighlightClient.liveEdges()) {
                if (!minecraft.level.dimension().location().toString().equals(highlight.dimensionId())) {
                    continue;
                }
                for (var pos : highlight.positions()) {
                    ProviderHighlightShapes.renderThickRainbowBox(poseStack,
                            consumers.getBuffer(RenderType.lines()), new AABB(pos).inflate(0.002), rainbow[0],
                            rainbow[1], rainbow[2], alpha);
                }
            }
            // Plates persist while their output still reports a stall.
            for (var plate : ProviderHighlightClient.plates()) {
                if (!levelDimension.equals(plate.dimensionId())
                        || !ProviderHighlightClient.shouldShowPlates(plate.outputId())) {
                    continue;
                }
                var stack = ProviderHighlightShapes.resolveItem(plate.outputId());
                for (var pos : plate.positions()) {
                    ProviderHighlightShapes.renderFacePlatesAndIcons(poseStack, consumers, minecraft.level, pos,
                            stack, ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z),
                            LevelRenderer.getLightColor(minecraft.level, pos), alpha);
                }
            }
            if (consumers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource source) {
                source.endBatch(RenderType.lines());
                source.endBatch(RenderType.debugFilledBox());
            }
            poseStack.popPose();
        });
    }
}
