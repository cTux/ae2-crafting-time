package com.ctux.ae2craftingtime.mc1201;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

public final class Ae2CraftingTimeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IntegrationLog.required("key-registration", () -> KeyBindingHelper.registerKeyBinding(TtcDetailsKeyMapping.showDetails()));
        IntegrationLog.required("client-network-registration", StatsNetwork::registerClient);
        // Drop rainbows and plates when leaving a world or server so they
        // never leak into another world with matching coordinates. Red plates
        // return only via server-approved resync.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ProviderHighlightClient.onSessionEnd());
        // AFTER_TRANSLUCENT runs after vanilla's world-buffer flush. Own and
        // flush every highlight batch here, including the item's render type.
        var consumers = net.minecraft.client.renderer.MultiBufferSource.immediate(
                new com.mojang.blaze3d.vertex.BufferBuilder(256));
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
            var camera = context.camera().getPosition();
            var poseStack = context.matrixStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
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
            consumers.endBatch();
            poseStack.popPose();
        });
    }
}
