package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = Ae2CraftingTime.MOD_ID, value = Dist.CLIENT)
public final class ProviderHighlightRender {
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
        var alpha = (int) (ProviderHighlightClient.pulseAlpha() * 255);
        var argb = alpha << 24 | (int) (rainbow[0] * 255) << 16 | (int) (rainbow[1] * 255) << 8
                | (int) (rainbow[2] * 255);
        var lines = consumers.getBuffer(RenderTypes.lines());
        for (var pos : highlight.positions()) {
            ProviderHighlightShapes.renderThickRainbowBox(event.getPoseStack(), lines, pos.getX() - camera.x,
                    pos.getY() - camera.y, pos.getZ() - camera.z, argb, ProviderHighlightShapes.LINE_WIDTH);
        }
        consumers.endBatch(RenderTypes.lines());
    }

    private ProviderHighlightRender() {
    }
}
