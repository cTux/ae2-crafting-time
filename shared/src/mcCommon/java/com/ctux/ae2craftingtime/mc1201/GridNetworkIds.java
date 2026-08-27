package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

final class GridNetworkIds {
    static String fromControllers(String dimensionId, Iterable<BlockPos> controllerAnchors) {
        var anchor = lowestAnchor(controllerAnchors);
        if (anchor == null || dimensionId == null || dimensionId.isBlank()) {
            return "";
        }
        return dimensionId + "|" + anchor.getX() + "," + anchor.getY() + "," + anchor.getZ();
    }

    @Nullable
    private static BlockPos lowestAnchor(Iterable<BlockPos> controllerAnchors) {
        BlockPos lowest = null;
        for (var controller : controllerAnchors) {
            if (controller == null) {
                continue;
            }
            if (lowest == null || compare(controller, lowest) < 0) {
                lowest = controller;
            }
        }
        return lowest;
    }

    private static int compare(BlockPos left, BlockPos right) {
        var byX = Integer.compare(left.getX(), right.getX());
        if (byX != 0) {
            return byX;
        }
        var byY = Integer.compare(left.getY(), right.getY());
        if (byY != 0) {
            return byY;
        }
        return Integer.compare(left.getZ(), right.getZ());
    }

    private GridNetworkIds() {
    }
}
