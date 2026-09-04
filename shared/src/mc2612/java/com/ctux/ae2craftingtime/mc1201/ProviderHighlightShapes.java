package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

/**
 * Client-side only. Draws the delayed-craft provider highlight on Minecraft
 * 26.1: thick rainbow edge boxes plus a red plate on each camera-facing
 * face. Item icons are submitted separately through the 26.1 submit pipeline
 * (see the NeoForge render hook).
 *
 * <p>Unlike the older line pipeline, this version supports a real line width
 * per vertex, so a single pass at {@link #LINE_WIDTH} (3x the previous
 * {@code 2.0f}) is enough for the edges. Called only from the 26.1.2
 * NeoForge render hook; never touched on a dedicated server.
 */
public final class ProviderHighlightShapes {
    /** Roughly 3x the previous 2.0f outline width. */
    public static final float LINE_WIDTH = 6.0f;

    private static final double EXPAND = 0.002;
    private static final float PLATE_HALF_SIZE = 0.36f;
    private static final float PLATE_OFFSET = 0.01f;
    /** Local offset of the item in front of its face plane; also see the hook. */
    public static final float ITEM_OFFSET = 0.03f;
    public static final float ITEM_SCALE = 0.55f;

    public static void renderThickRainbowBox(PoseStack pose, VertexConsumer consumer, double originX,
            double originY, double originZ, int argb, float lineWidth) {
        var x0 = originX - EXPAND;
        var y0 = originY - EXPAND;
        var z0 = originZ - EXPAND;
        var x1 = originX + 1 + EXPAND;
        var y1 = originY + 1 + EXPAND;
        var z1 = originZ + 1 + EXPAND;
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
    }

    /**
     * Emits one red plate quad for a camera-facing face into a
     * {@code debugFilledBox} buffer. The origin is the camera-relative block
     * origin, matching the edge pass.
     */
    public static void renderFacePlate(PoseStack pose, VertexConsumer filled, double originX, double originY,
            double originZ, Direction face, int redArgb) {
        var last = pose.last();
        var centerX = (float) (originX + 0.5);
        var centerY = (float) (originY + 0.5);
        var centerZ = (float) (originZ + 0.5);
        switch (face) {
            case DOWN -> quad(last, filled, centerX - PLATE_HALF_SIZE, centerY - 0.5f - PLATE_OFFSET,
                    centerZ - PLATE_HALF_SIZE, centerX + PLATE_HALF_SIZE, centerY - 0.5f - PLATE_OFFSET,
                    centerZ - PLATE_HALF_SIZE, centerX + PLATE_HALF_SIZE, centerY - 0.5f - PLATE_OFFSET,
                    centerZ + PLATE_HALF_SIZE, centerX - PLATE_HALF_SIZE, centerY - 0.5f - PLATE_OFFSET,
                    centerZ + PLATE_HALF_SIZE, redArgb);
            case UP -> quad(last, filled, centerX - PLATE_HALF_SIZE, centerY + 0.5f + PLATE_OFFSET,
                    centerZ + PLATE_HALF_SIZE, centerX + PLATE_HALF_SIZE, centerY + 0.5f + PLATE_OFFSET,
                    centerZ + PLATE_HALF_SIZE, centerX + PLATE_HALF_SIZE, centerY + 0.5f + PLATE_OFFSET,
                    centerZ - PLATE_HALF_SIZE, centerX - PLATE_HALF_SIZE, centerY + 0.5f + PLATE_OFFSET,
                    centerZ - PLATE_HALF_SIZE, redArgb);
            case NORTH -> quad(last, filled, centerX + PLATE_HALF_SIZE, centerY - PLATE_HALF_SIZE,
                    centerZ - 0.5f - PLATE_OFFSET, centerX - PLATE_HALF_SIZE, centerY - PLATE_HALF_SIZE,
                    centerZ - 0.5f - PLATE_OFFSET, centerX - PLATE_HALF_SIZE, centerY + PLATE_HALF_SIZE,
                    centerZ - 0.5f - PLATE_OFFSET, centerX + PLATE_HALF_SIZE, centerY + PLATE_HALF_SIZE,
                    centerZ - 0.5f - PLATE_OFFSET, redArgb);
            case SOUTH -> quad(last, filled, centerX - PLATE_HALF_SIZE, centerY - PLATE_HALF_SIZE,
                    centerZ + 0.5f + PLATE_OFFSET, centerX + PLATE_HALF_SIZE, centerY - PLATE_HALF_SIZE,
                    centerZ + 0.5f + PLATE_OFFSET, centerX + PLATE_HALF_SIZE, centerY + PLATE_HALF_SIZE,
                    centerZ + 0.5f + PLATE_OFFSET, centerX - PLATE_HALF_SIZE, centerY + PLATE_HALF_SIZE,
                    centerZ + 0.5f + PLATE_OFFSET, redArgb);
            case WEST -> quad(last, filled, centerX - 0.5f - PLATE_OFFSET, centerY - PLATE_HALF_SIZE,
                    centerZ - PLATE_HALF_SIZE, centerX - 0.5f - PLATE_OFFSET, centerY - PLATE_HALF_SIZE,
                    centerZ + PLATE_HALF_SIZE, centerX - 0.5f - PLATE_OFFSET, centerY + PLATE_HALF_SIZE,
                    centerZ + PLATE_HALF_SIZE, centerX - 0.5f - PLATE_OFFSET, centerY + PLATE_HALF_SIZE,
                    centerZ - PLATE_HALF_SIZE, redArgb);
            case EAST -> quad(last, filled, centerX + 0.5f + PLATE_OFFSET, centerY - PLATE_HALF_SIZE,
                    centerZ + PLATE_HALF_SIZE, centerX + 0.5f + PLATE_OFFSET, centerY - PLATE_HALF_SIZE,
                    centerZ - PLATE_HALF_SIZE, centerX + 0.5f + PLATE_OFFSET, centerY + PLATE_HALF_SIZE,
                    centerZ - PLATE_HALF_SIZE, centerX + 0.5f + PLATE_OFFSET, centerY + PLATE_HALF_SIZE,
                    centerZ + PLATE_HALF_SIZE, redArgb);
        }
    }

    /**
     * Orients an identity, camera-relative pose so local +Z points out of the
     * given face and the origin sits {@link #ITEM_OFFSET} in front of its
     * plane, ready for an item submit at half scale.
     */
    public static void orientFaceForItem(PoseStack pose, double originX, double originY, double originZ,
            Direction face) {
        pose.translate(originX + 0.5, originY + 0.5, originZ + 0.5);
        switch (face) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            case SOUTH -> {
            }
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90.0f));
        }
        pose.translate(0.0, 0.0, 0.5 + ITEM_OFFSET);
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer filled, float ax, float ay, float az, float bx,
            float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int argb) {
        filled.addVertex(pose, ax, ay, az).setColor(argb);
        filled.addVertex(pose, bx, by, bz).setColor(argb);
        filled.addVertex(pose, cx, cy, cz).setColor(argb);
        filled.addVertex(pose, dx, dy, dz).setColor(argb);
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

    /**
     * Resolves a highlight output id to an item stack for the face icon.
     * Returns {@link ItemStack#EMPTY} for anything that is not an item, so
     * callers always render at least the red plate.
     */
    public static ItemStack resolveItem(String outputId) {
        if (outputId == null || outputId.isBlank() || outputId.length() > PacketLimits.MAX_OUTPUT_ID_LENGTH) {
            return ItemStack.EMPTY;
        }
        var id = Identifier.tryParse(outputId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var entry = BuiltInRegistries.ITEM.get(id);
        if (entry.isEmpty() || entry.get().value() == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(entry.get().value());
    }

    private ProviderHighlightShapes() {
    }
}
