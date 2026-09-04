package com.ctux.ae2craftingtime.mc1201;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

/**
 * Client-side only. Draws the delayed-craft provider highlight as thick
 * rainbow boxes with matching face diagonals on Minecraft 26.1.
 *
 * <p>Unlike the older line pipeline, this version supports a real line width
 * per vertex, so a single pass at {@link #LINE_WIDTH} (3x the previous
 * {@code 2.0f}) is enough for both edges and diagonals. Called only from the
 * 26.1.2 NeoForge render hook; never touched on a dedicated server.
 */
public final class ProviderHighlightShapes {
    /** Roughly 3x the previous 2.0f outline width. */
    public static final float LINE_WIDTH = 6.0f;

    private static final double EXPAND = 0.002;

    public static void renderThickRainbowBox(PoseStack pose, VertexConsumer consumer, double originX,
            double originY, double originZ, int argb, float lineWidth) {
        var x0 = originX - EXPAND;
        var y0 = originY - EXPAND;
        var z0 = originZ - EXPAND;
        var x1 = originX + 1 + EXPAND;
        var y1 = originY + 1 + EXPAND;
        var z1 = originZ + 1 + EXPAND;
        // 12 cube edges.
        line(pose, consumer, x0, y0, z0, x1, y0, z0, argb, lineWidth);
        line(pose, consumer, x0, y0, z0, x0, y1, z0, argb, lineWidth);
        line(pose, consumer, x0, y0, z0, x0, y0, z1, argb, lineWidth);
        line(pose, consumer, x1, y0, z0, x1, y1, z0, argb, lineWidth);
        line(pose, consumer, x1, y0, z0, x1, y0, z1, argb, lineWidth);
        line(pose, consumer, x0, y1, z0, x1, y1, z0, argb, lineWidth);
        line(pose, consumer, x0, y1, z0, x0, y1, z1, argb, lineWidth);
        line(pose, consumer, x0, y0, z1, x1, y0, z1, argb, lineWidth);
        line(pose, consumer, x0, y0, z1, x0, y1, z1, argb, lineWidth);
        line(pose, consumer, x1, y1, z0, x1, y1, z1, argb, lineWidth);
        line(pose, consumer, x1, y0, z1, x1, y1, z1, argb, lineWidth);
        line(pose, consumer, x0, y1, z1, x1, y1, z1, argb, lineWidth);
        // Face diagonals (an X on each of the 6 faces) in the same color.
        line(pose, consumer, x0, y0, z0, x1, y0, z1, argb, lineWidth);
        line(pose, consumer, x1, y0, z0, x0, y0, z1, argb, lineWidth);
        line(pose, consumer, x0, y1, z0, x1, y1, z1, argb, lineWidth);
        line(pose, consumer, x1, y1, z0, x0, y1, z1, argb, lineWidth);
        line(pose, consumer, x0, y0, z0, x0, y1, z1, argb, lineWidth);
        line(pose, consumer, x0, y1, z0, x0, y0, z1, argb, lineWidth);
        line(pose, consumer, x1, y0, z0, x1, y1, z1, argb, lineWidth);
        line(pose, consumer, x1, y1, z0, x1, y0, z1, argb, lineWidth);
        line(pose, consumer, x0, y0, z0, x1, y1, z0, argb, lineWidth);
        line(pose, consumer, x1, y0, z0, x0, y1, z0, argb, lineWidth);
        line(pose, consumer, x0, y0, z1, x1, y1, z1, argb, lineWidth);
        line(pose, consumer, x1, y0, z1, x0, y1, z1, argb, lineWidth);
    }

    private static void line(PoseStack pose, VertexConsumer consumer, double x0, double y0, double z0, double x1,
            double y1, double z1, int argb, float lineWidth) {
        var last = pose.last();
        var normal = new Vector3f((float) (x1 - x0), (float) (y1 - y0), (float) (z1 - z0)).normalize();
        consumer.addVertex(last, (float) x0, (float) y0, (float) z0).setColor(argb).setNormal(last, normal)
                .setLineWidth(lineWidth);
        consumer.addVertex(last, (float) x1, (float) y1, (float) z1).setColor(argb).setNormal(last, normal)
                .setLineWidth(lineWidth);
    }

    private ProviderHighlightShapes() {
    }
}
