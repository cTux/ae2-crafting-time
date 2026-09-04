package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
                        (player, record) -> StatsNetwork.sendTo(player, new ProviderHighlightS2C(
                                record.dimensionId(), record.positions(), record.outputId(),
                                ProviderLocateCommand.HIGHLIGHT_SECONDS))))));
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
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            var highlight = ProviderHighlightClient.live();
            if (highlight == null) {
                return;
            }
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null
                    || !minecraft.level.dimension().location().toString().equals(highlight.dimensionId())) {
                return;
            }
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
            // Edges first: plate and item writes switch the shared fallback
            // builder to other render types, so one pass per type.
            for (var pos : highlight.positions()) {
                ProviderHighlightShapes.renderThickRainbowBox(poseStack,
                        consumers.getBuffer(RenderType.lines()), new AABB(pos).inflate(0.002), rainbow[0],
                        rainbow[1], rainbow[2], alpha);
            }
            var stack = ProviderHighlightShapes.resolveItem(highlight.outputId());
            for (var pos : highlight.positions()) {
                ProviderHighlightShapes.renderFacePlatesAndIcons(poseStack, consumers, minecraft.level, pos,
                        stack, ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z),
                        LevelRenderer.getLightColor(minecraft.level, pos), alpha);
            }
            if (consumers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource source) {
                source.endBatch(RenderType.lines());
                source.endBatch(RenderType.debugFilledBox());
            }
            poseStack.popPose();
        });
    }
}
