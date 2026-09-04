package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.shapes.Shapes;
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
        var alpha = (int) (ProviderHighlightClient.pulseAlpha() * 255) << 24 | 0xFF5555;
        for (var pos : highlight.positions()) {
            ShapeRenderer.renderShape(event.getPoseStack(), consumers.getBuffer(RenderTypes.lines()),
                    Shapes.block(), pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, alpha,
                    2.0f);
        }
        consumers.endBatch(RenderTypes.lines());
    }

    private ProviderHighlightRender() {
    }
}
