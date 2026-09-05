package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, value = Dist.CLIENT)
public final class ProviderHighlightRender {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var levelDimension = minecraft.level.dimension().location().toString();
        // A broken provider block drops its edge and plate immediately, in
        // this dimension only.
        ProviderHighlightClient.trimPositions(levelDimension, pos -> {
            if (!minecraft.level.isLoaded(pos)) {
                return true;
            }
            if (minecraft.level.getBlockState(pos).isAir()) {
                return false;
            }
            return minecraft.level.getBlockEntity(pos) != null;
        });
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        var consumers = minecraft.renderBuffers().bufferSource();
        var rainbow = ProviderHighlightClient.rainbowRgb();
        var alpha = ProviderHighlightClient.pulseAlpha();
        // Click edges first in their own batch: plate and item writes switch the
        // shared fallback builder to other render types, so a cached lines
        // consumer would not survive until the next position. Each identity
        // keeps its own edge so two locates within 15 seconds stay independent.
        var edges = ProviderHighlightClient.liveEdges();
        var hasEdge = false;
        var lines = consumers.getBuffer(RenderType.lines());
        for (var highlight : edges) {
            if (!levelDimension.equals(highlight.dimensionId())) {
                continue;
            }
            hasEdge = true;
            for (var pos : highlight.positions()) {
                ProviderHighlightShapes.renderThickRainbowBox(poseStack, lines, new AABB(pos).inflate(0.002),
                        rainbow[0], rainbow[1], rainbow[2], alpha);
            }
        }
        if (hasEdge) {
            consumers.endBatch(RenderType.lines());
        }
        // Plates persist while their output still reports a stall.
        for (var plate : ProviderHighlightClient.plates()) {
            if (!levelDimension.equals(plate.dimensionId())
                    || !ProviderHighlightClient.shouldShowPlates(plate.outputId())) {
                continue;
            }
            var stack = ProviderHighlightShapes.resolveItem(plate.outputId());
            for (var pos : plate.positions()) {
                ProviderHighlightShapes.renderFacePlatesAndIcons(poseStack, consumers, minecraft.level, pos, stack,
                        ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z),
                        LevelRenderer.getLightColor(minecraft.level, pos), alpha);
            }
        }
        consumers.endBatch(RenderType.debugFilledBox());
        poseStack.popPose();
    }

    /**
     * Clears all client highlight state when leaving a world or server so a
     * rainbow cannot survive reconnect and plates never leak into another
     * world with matching coordinates. Red plates return only via
     * server-approved resync.
     */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ProviderHighlightClient.onSessionEnd();
    }

    private ProviderHighlightRender() {
    }
}
