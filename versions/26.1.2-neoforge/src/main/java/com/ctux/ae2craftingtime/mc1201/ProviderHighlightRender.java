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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, value = Dist.CLIENT)
public final class ProviderHighlightRender {
    private static final ItemStackRenderState ITEM_STATE = new ItemStackRenderState();
    private static ItemModelResolver itemResolver;
    private static ModelManager resolverManager;

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentParticles event) {
        var highlight = ProviderHighlightClient.live();
        if (highlight == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.level.dimension().identifier().toString().equals(highlight.dimensionId())) {
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera().position();
        var consumers = minecraft.renderBuffers().bufferSource();
        var rainbow = ProviderHighlightClient.rainbowRgb();
        var pulse = ProviderHighlightClient.pulseAlpha();
        var alpha = (int) (pulse * 255);
        var argb = alpha << 24 | (int) (rainbow[0] * 255) << 16 | (int) (rainbow[1] * 255) << 8
                | (int) (rainbow[2] * 255);
        var redArgb = alpha << 24 | 0xFF2626;
        var lines = consumers.getBuffer(RenderTypes.lines());
        var filled = consumers.getBuffer(RenderTypes.debugFilledBox());
        for (var pos : highlight.positions()) {
            var originX = pos.getX() - camera.x;
            var originY = pos.getY() - camera.y;
            var originZ = pos.getZ() - camera.z;
            ProviderHighlightShapes.renderThickRainbowBox(event.getPoseStack(), lines, originX, originY, originZ,
                    argb, ProviderHighlightShapes.LINE_WIDTH);
            for (var face : ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z)) {
                ProviderHighlightShapes.renderFacePlate(event.getPoseStack(), filled, originX, originY, originZ,
                        face, redArgb);
            }
        }
        consumers.endBatch(RenderTypes.lines());
        consumers.endBatch(RenderTypes.debugFilledBox());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        var highlight = ProviderHighlightClient.live();
        if (highlight == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.level.dimension().identifier().toString().equals(highlight.dimensionId())) {
            return;
        }
        var stack = ProviderHighlightShapes.resolveItem(highlight.outputId());
        if (stack.isEmpty()) {
            return;
        }
        var manager = minecraft.getModelManager();
        if (itemResolver == null || resolverManager != manager) {
            itemResolver = new ItemModelResolver(manager);
            resolverManager = manager;
        }
        itemResolver.updateForTopItem(ITEM_STATE, stack, ItemDisplayContext.FIXED, minecraft.level, null, 0);
        if (ITEM_STATE.isEmpty()) {
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera().position();
        var collector = event.getSubmitNodeCollector();
        var pose = event.getPoseStack();
        for (var pos : highlight.positions()) {
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

    private ProviderHighlightRender() {
    }
}
