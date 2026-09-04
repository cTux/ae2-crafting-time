package com.ctux.ae2craftingtime.mc1201;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Client-side only. Camera-facing face selection for the provider highlight's
 * red plates and stuck-item icons. Pure block math with no rendering or
 * registry types, so every loader shares it; never touched on a dedicated
 * server.
 */
public final class ProviderFaceIcons {
    /**
     * Returns the block faces pointing toward the camera eye, so hidden faces
     * cost nothing. At most three faces of a block ever qualify.
     */
    public static List<Direction> visibleFaces(BlockPos pos, double cameraX, double cameraY, double cameraZ) {
        var faces = new ArrayList<Direction>(3);
        var centerX = pos.getX() + 0.5;
        var centerY = pos.getY() + 0.5;
        var centerZ = pos.getZ() + 0.5;
        for (var face : Direction.values()) {
            var normalX = face.getStepX();
            var normalY = face.getStepY();
            var normalZ = face.getStepZ();
            var toCameraX = cameraX - (centerX + normalX * 0.5);
            var toCameraY = cameraY - (centerY + normalY * 0.5);
            var toCameraZ = cameraZ - (centerZ + normalZ * 0.5);
            if (normalX * toCameraX + normalY * toCameraY + normalZ * toCameraZ > 0) {
                faces.add(face);
            }
        }
        return faces;
    }

    private ProviderFaceIcons() {
    }
}
