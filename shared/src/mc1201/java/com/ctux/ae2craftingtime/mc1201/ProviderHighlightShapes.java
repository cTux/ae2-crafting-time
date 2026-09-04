package com.ctux.ae2craftingtime.mc1201;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;

/**
 * Client-side only. Draws the delayed-craft provider highlight as thick
 * rainbow boxes with matching face diagonals.
 *
 * <p>Vanilla {@code RenderType.lines()} width is fixed at one pixel on most
 * drivers, so thickness is simulated without touching the vertex format (which
 * differs between 1.20.1 and 1.21.1): edges draw as three nested shells
 * (roughly 2-3x the old single stroke) and each face diagonal draws as a
 * rotated rod whose outline reads as one thick diagonal line. Only
 * {@link LevelRenderer#renderLineBox} plus pose translate/rotate are used, so
 * the same code compiles on 1.20.1 Forge, 1.20.1 Fabric, and 1.21.1 NeoForge.
 * Called only from the per-loader render hooks; never touched on a dedicated
 * server.
 */
public final class ProviderHighlightShapes {
    private static final double[] SHELL_OFFSETS = {0.002, 0.014, 0.026};
    private static final double DIAGONAL_SHELL = 0.014;
    /** Half of the rod width: 0.05 blocks total, visibly thicker than a 1px edge. */
    private static final double ROD_HALF_WIDTH = 0.025;
    /** Half of the rod height along the face normal: keeps the rod off the surface. */
    private static final double ROD_HALF_HEIGHT = 0.004;
    /** Half the face-diagonal length for the inflated shell. */
    private static final double HALF_DIAGONAL = (1.0 + DIAGONAL_SHELL * 2.0) * 0.5 * Math.sqrt(2.0);

    public static void renderThickRainbowBox(PoseStack pose, VertexConsumer consumer, AABB box, float red,
            float green, float blue, float alpha) {
        for (var shell : SHELL_OFFSETS) {
            LevelRenderer.renderLineBox(pose, consumer, box.inflate(shell), red, green, blue, alpha);
        }
        var fat = box.inflate(DIAGONAL_SHELL);
        var centerX = (fat.minX + fat.maxX) * 0.5;
        var centerY = (fat.minY + fat.maxY) * 0.5;
        var centerZ = (fat.minZ + fat.maxZ) * 0.5;
        // An X of rods on each of the 6 faces, in the same color as the edges.
        rodY(pose, consumer, centerX, fat.minY, centerZ, 45.0f, red, green, blue, alpha);
        rodY(pose, consumer, centerX, fat.minY, centerZ, -45.0f, red, green, blue, alpha);
        rodY(pose, consumer, centerX, fat.maxY, centerZ, 45.0f, red, green, blue, alpha);
        rodY(pose, consumer, centerX, fat.maxY, centerZ, -45.0f, red, green, blue, alpha);
        rodZ(pose, consumer, centerX, centerY, fat.minZ, 45.0f, red, green, blue, alpha);
        rodZ(pose, consumer, centerX, centerY, fat.minZ, -45.0f, red, green, blue, alpha);
        rodZ(pose, consumer, centerX, centerY, fat.maxZ, 45.0f, red, green, blue, alpha);
        rodZ(pose, consumer, centerX, centerY, fat.maxZ, -45.0f, red, green, blue, alpha);
        rodX(pose, consumer, fat.minX, centerY, centerZ, 45.0f, red, green, blue, alpha);
        rodX(pose, consumer, fat.minX, centerY, centerZ, -45.0f, red, green, blue, alpha);
        rodX(pose, consumer, fat.maxX, centerY, centerZ, 45.0f, red, green, blue, alpha);
        rodX(pose, consumer, fat.maxX, centerY, centerZ, -45.0f, red, green, blue, alpha);
    }

    private static void rodY(PoseStack pose, VertexConsumer consumer, double centerX, double y, double centerZ,
            float degrees, float red, float green, float blue, float alpha) {
        pose.pushPose();
        pose.translate(centerX, y, centerZ);
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        LevelRenderer.renderLineBox(pose, consumer,
                new AABB(-HALF_DIAGONAL, -ROD_HALF_HEIGHT, -ROD_HALF_WIDTH, HALF_DIAGONAL, ROD_HALF_HEIGHT,
                        ROD_HALF_WIDTH),
                red, green, blue, alpha);
        pose.popPose();
    }

    private static void rodZ(PoseStack pose, VertexConsumer consumer, double centerX, double centerY, double z,
            float degrees, float red, float green, float blue, float alpha) {
        pose.pushPose();
        pose.translate(centerX, centerY, z);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        LevelRenderer.renderLineBox(pose, consumer,
                new AABB(-HALF_DIAGONAL, -ROD_HALF_WIDTH, -ROD_HALF_HEIGHT, HALF_DIAGONAL, ROD_HALF_WIDTH,
                        ROD_HALF_HEIGHT),
                red, green, blue, alpha);
        pose.popPose();
    }

    private static void rodX(PoseStack pose, VertexConsumer consumer, double x, double centerY, double centerZ,
            float degrees, float red, float green, float blue, float alpha) {
        pose.pushPose();
        pose.translate(x, centerY, centerZ);
        pose.mulPose(Axis.XP.rotationDegrees(degrees));
        LevelRenderer.renderLineBox(pose, consumer,
                new AABB(-ROD_HALF_HEIGHT, -HALF_DIAGONAL, -ROD_HALF_WIDTH, ROD_HALF_HEIGHT, HALF_DIAGONAL,
                        ROD_HALF_WIDTH),
                red, green, blue, alpha);
        pose.popPose();
    }

    private ProviderHighlightShapes() {
    }
}
