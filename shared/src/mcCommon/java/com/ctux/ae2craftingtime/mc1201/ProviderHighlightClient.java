package com.ctux.ae2craftingtime.mc1201;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Client-side only. Holds the latest provider highlight (dimension, block
 * positions, expiry timestamp) for the per-loader render hooks. Never touched
 * on a dedicated server.
 */
public final class ProviderHighlightClient {
    public record Highlight(String dimensionId, List<BlockPos> positions, long expiresAtMillis) {
    }

    private static volatile Highlight current;

    public static void show(String dimensionId, List<BlockPos> positions, int durationSeconds) {
        if (dimensionId == null || positions == null || positions.isEmpty() || durationSeconds <= 0) {
            return;
        }
        current = new Highlight(dimensionId, List.copyOf(positions),
                System.currentTimeMillis() + durationSeconds * 1000L);
    }

    public static Highlight live() {
        var highlight = current;
        if (highlight == null || System.currentTimeMillis() >= highlight.expiresAtMillis()) {
            current = null;
            return null;
        }
        return highlight;
    }

    /**
     * Smooth one-second blink shared by every loader's render hook, so the
     * box reads as an alert rather than a static frame. Ranges from 0.35
     * (dim) to 1.0 (full) opacity.
     */
    public static float pulseAlpha() {
        var phase = (System.currentTimeMillis() % 1000) / 1000.0;
        return 0.35f + 0.65f * (0.5f - 0.5f * (float) Math.cos(phase * Math.PI * 2.0));
    }

    public static void clear() {
        current = null;
    }

    private ProviderHighlightClient() {
    }
}
