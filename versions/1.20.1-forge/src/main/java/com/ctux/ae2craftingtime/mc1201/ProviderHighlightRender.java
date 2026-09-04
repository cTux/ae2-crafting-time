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
        var rainbow = ProviderHighlightClient.rainbowRgb();
        var alpha = ProviderHighlightClient.pulseAlpha();
        var lines = consumers.getBuffer(RenderType.lines());
        var stack = ProviderHighlightShapes.resolveItem(highlight.outputId());
        for (var pos : highlight.positions()) {
            ProviderHighlightShapes.renderThickRainbowBox(poseStack, lines, new AABB(pos).inflate(0.002),
                    rainbow[0], rainbow[1], rainbow[2], alpha);
            ProviderHighlightShapes.renderFacePlatesAndIcons(poseStack, consumers, minecraft.level, pos, stack,
                    ProviderFaceIcons.visibleFaces(pos, camera.x, camera.y, camera.z),
                    LevelRenderer.getLightColor(minecraft.level, pos), alpha);
        }
        consumers.endBatch(RenderType.lines());
        consumers.endBatch(RenderType.debugFilledBox());
        poseStack.popPose();
    }

    private ProviderHighlightRender() {
    }
}
