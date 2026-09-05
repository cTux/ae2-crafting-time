package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, value = Dist.CLIENT)
public final class ProviderHighlightRender {
    private static final ItemStackRenderState ITEM_STATE = new ItemStackRenderState();
    private static ItemModelResolver itemResolver;
    private static ModelManager resolverManager;

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentParticles event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var levelDimension = minecraft.level.dimension().identifier().toString();
        var camera = minecraft.gameRenderer.getMainCamera().position();
        var consumers = minecraft.renderBuffers().bufferSource();
        // A broken provider target drops its edge and plate immediately, in
        // this dimension only. Replacement with another block entity and a
        // surviving host without its provider part both count as broken;
        // unloaded chunks stay unknown so reload never clears intact red.
        ProviderHighlightClient.trimPositions(levelDimension,
                pos -> ProviderBlockTargets.keepForHighlight(minecraft.level, pos));
        var rainbow = ProviderHighlightClient.rainbowRgb();
        var pulse = ProviderHighlightClient.pulseAlpha();
        var alpha = (int) (pulse * 255);
        var argb = alpha << 24 | (int) (rainbow[0] * 255) << 16 | (int) (rainbow[1] * 255) << 8
                | (int) (rainbow[2] * 255);
        var redArgb = alpha << 24 | 0xFF2626;
        // Click edges first in their own batch, then persistent plates. Each
        // identity keeps its own edge so two locates within 15s stay independent.
        var edges = ProviderHighlightClient.liveEdges();
        var hasEdge = false;
        var lines = consumers.getBuffer(RenderTypes.lines());
        for (var highlight : edges) {
            if (!levelDimension.equals(highlight.dimensionId())) {
                continue;
            }
            hasEdge = true;
            for (var pos : highlight.positions()) {
                ProviderHighlightShapes.renderThickRainbowBox(event.getPoseStack(), lines,
                        pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, argb,
                        ProviderHighlightShapes.LINE_WIDTH);
            }
        }
        if (hasEdge) {
            consumers.endBatch(RenderTypes.lines());
        }
        // Plates persist while their output still reports a stall.
        var filled = consumers.getBuffer(RenderTypes.debugFilledBox());
        for (var plate : ProviderHighlightClient.plates()) {
            if (!levelDimension.equals(plate.dimensionId())
                    || !ProviderHighlightClient.shouldShowPlates(plate.outputId())) {
                continue;
            }
            for (var pos : plate.positions()) {
                var originX = pos.getX() - camera.x;
                var originY = pos.getY() - camera.y;
                var originZ = pos.getZ() - camera.z;
                for (var face : ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z)) {
                    ProviderHighlightShapes.renderFacePlate(event.getPoseStack(), filled, originX, originY,
                            originZ, face, redArgb);
                }
            }
        }
        consumers.endBatch(RenderTypes.debugFilledBox());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var levelDimension = minecraft.level.dimension().identifier().toString();
        var camera = minecraft.gameRenderer.getMainCamera().position();
        var collector = event.getSubmitNodeCollector();
        var pose = event.getPoseStack();
        var manager = minecraft.getModelManager();
        if (itemResolver == null || resolverManager != manager) {
            itemResolver = new ItemModelResolver(manager);
            resolverManager = manager;
        }
        for (var plate : ProviderHighlightClient.plates()) {
            if (!levelDimension.equals(plate.dimensionId())
                    || !ProviderHighlightClient.shouldShowPlates(plate.outputId())) {
                continue;
            }
            var stack = ProviderHighlightShapes.resolveItem(plate.outputId());
            if (stack.isEmpty()) {
                continue;
            }
            itemResolver.updateForTopItem(ITEM_STATE, stack, ItemDisplayContext.FIXED, minecraft.level, null, 0);
            if (ITEM_STATE.isEmpty()) {
                continue;
            }
            for (var pos : plate.positions()) {
                var light = LevelRenderer.getLightCoords(minecraft.level, pos);
                for (var face : ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z)) {
                    pose.pushPose();
                    ProviderHighlightShapes.orientFaceForItem(pose, pos.getX() - camera.x, pos.getY() - camera.y,
                            pos.getZ() - camera.z, face);
                    ITEM_STATE.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                }
            }
        }
    }

    /**
     * Clears all client highlight state when leaving a world or server so a
     * rainbow cannot survive reconnect and plates never leak into another
     * world with matching coordinates. Red plates return only via
     * server-approved resync.
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ProviderHighlightClient.onSessionEnd();
    }

    private ProviderHighlightRender() {
    }
}
