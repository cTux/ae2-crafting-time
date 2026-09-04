package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ProviderHighlightRender {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        var highlight = ProviderHighlightClient.live();
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        var consumers = minecraft.renderBuffers().bufferSource();
        var rainbow = ProviderHighlightClient.rainbowRgb();
        var alpha = ProviderHighlightClient.pulseAlpha();
        // Click edges first in their own batch: plate and item writes switch the
        // shared fallback builder to other render types, so a cached lines
        // consumer would not survive until the next position.
        if (highlight != null
                && minecraft.level.dimension().location().toString().equals(highlight.dimensionId())) {
            var lines = consumers.getBuffer(RenderType.lines());
            for (var pos : highlight.positions()) {
                ProviderHighlightShapes.renderThickRainbowBox(poseStack, lines, new AABB(pos).inflate(0.002),
                        rainbow[0], rainbow[1], rainbow[2], alpha);
            }
            consumers.endBatch(RenderType.lines());
        }
        // Plates persist while their output still reports a stall.
        for (var plate : ProviderHighlightClient.plates()) {
            if (!minecraft.level.dimension().location().toString().equals(plate.dimensionId())
                    || ClientStats.CACHE.stall(new ProfileKey(plate.outputId())).isEmpty()) {
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

    private ProviderHighlightRender() {
    }
}
