package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Client-side only. Draws the delayed-craft provider highlight on 1.20.1 and
 * 1.21.1: thick rainbow edge boxes plus a red plate with the stuck output's
 * item icon on each camera-facing face.
 *
 * <p>Vanilla {@code RenderType.lines()} width is fixed at one pixel on most
 * drivers, so edge thickness comes from three nested shells (roughly 2-3x
 * the old single stroke). Plates are thin filled boxes and icons render
 * item-frame style, all through APIs shared by 1.20.1 and 1.21.1. Called
 * only from the per-loader render hooks; never touched on a dedicated
 * server.
 */
public final class ProviderHighlightShapes {
    private static final double[] SHELL_OFFSETS = {0.002, 0.014, 0.026};
    private static final float PLATE_HALF_SIZE = 0.36f;
    private static final float PLATE_MIN_Z = 0.004f;
    private static final float PLATE_MAX_Z = 0.016f;
    private static final float ITEM_Z = 0.03f;
    private static final float ITEM_SCALE = 0.55f;

    public static void renderThickRainbowBox(PoseStack pose, VertexConsumer consumer, AABB box, float red,
            float green, float blue, float alpha) {
        for (var shell : SHELL_OFFSETS) {
            LevelRenderer.renderLineBox(pose, consumer, box.inflate(shell), red, green, blue, alpha);
        }
    }

    /**
     * Draws a red plate with the stuck output's icon on each given face.
     * Faces must already be culled to the camera side. An empty stack draws
     * plates only (for example fluid outputs).
     */
    public static void renderFacePlatesAndIcons(PoseStack pose, MultiBufferSource buffers, Level level, BlockPos pos,
            ItemStack stack, List<Direction> faces, int light, float alpha) {
        var filled = buffers.getBuffer(RenderType.debugFilledBox());
        var items = Minecraft.getInstance().getItemRenderer();
        for (var face : faces) {
            pose.pushPose();
            orientToFace(pose, pos, face);
            LevelRenderer.addChainedFilledBoxVertices(pose, filled, -PLATE_HALF_SIZE, -PLATE_HALF_SIZE, PLATE_MIN_Z,
                    PLATE_HALF_SIZE, PLATE_HALF_SIZE, PLATE_MAX_Z, 1.0f, 0.15f, 0.15f, alpha);
            if (!stack.isEmpty()) {
                pose.pushPose();
                pose.translate(0.0, 0.0, ITEM_Z);
                pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                        level, 0);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    private static void orientToFace(PoseStack pose, BlockPos pos, Direction face) {
        pose.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        switch (face) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            case SOUTH -> {
            }
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90.0f));
        }
        pose.translate(0.0, 0.0, 0.5);
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
        var id = ResourceLocation.tryParse(outputId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private ProviderHighlightShapes() {
    }
}
