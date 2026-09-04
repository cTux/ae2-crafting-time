package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
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
        var highlight = ProviderHighlightClient.live();
        if (highlight == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.level.dimension().location().toString().equals(highlight.dimensionId())) {
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        var consumers = minecraft.renderBuffers().bufferSource();
        var alpha = ProviderHighlightClient.pulseAlpha();
        for (var pos : highlight.positions()) {
            LevelRenderer.renderLineBox(poseStack, consumers.getBuffer(RenderType.lines()),
                    new AABB(pos).inflate(0.002), 1.0f, 0.33f, 0.33f, alpha);
        }
        consumers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private ProviderHighlightRender() {
    }
}
