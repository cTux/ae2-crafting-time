package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Client-side only. Holds the latest provider highlight (dimension, block
 * positions, expiry timestamp) for the per-loader render hooks, plus one
 * persistent plate per located output. Plates outlive the 15-second edge
 * highlight and render while the output still reports a stall. Never touched
 * on a dedicated server.
 */
public final class ProviderHighlightClient {
    public record Highlight(String dimensionId, List<BlockPos> positions, String outputId, long expiresAtMillis) {
        public Highlight {
            outputId = outputId == null ? "" : outputId;
        }
    }

    public record Plate(String dimensionId, List<BlockPos> positions, String outputId) {
        public Plate {
            positions = positions == null ? List.of() : List.copyOf(positions);
            outputId = outputId == null ? "" : outputId;
        }
    }

    private static final int MAX_PLATES = 32;
    private static final LinkedHashMap<String, Plate> PLATES = new LinkedHashMap<>();

    private static volatile Highlight current;

    public static void show(String dimensionId, List<BlockPos> positions, int durationSeconds, String outputId) {
        if (dimensionId == null || positions == null || positions.isEmpty() || durationSeconds <= 0) {
            return;
        }
        current = new Highlight(dimensionId, List.copyOf(positions), outputId,
                System.currentTimeMillis() + durationSeconds * 1000L);
        if (outputId != null && !outputId.isBlank()) {
            PLATES.put(outputId, new Plate(dimensionId, positions, outputId));
            while (PLATES.size() > MAX_PLATES) {
                var eldest = PLATES.keySet().iterator();
                eldest.next();
                eldest.remove();
            }
        }
    }

    public static Highlight live() {
        var highlight = current;
        if (highlight == null || System.currentTimeMillis() >= highlight.expiresAtMillis()) {
            current = null;
            return null;
        }
        return highlight;
    }

    /** Persistent plates for the render hooks to filter by dimension and stall. */
    public static List<Plate> plates() {
        return new ArrayList<>(PLATES.values());
    }

    /**
     * Drops plates whose output no longer reports a stall after a snapshot
     * for the requested keys was applied. Call right after the client cache
     * replace, from every loader's snapshot handler.
     */
    public static void prunePlates(List<String> requestedKeys) {
        if (requestedKeys == null || requestedKeys.isEmpty() || PLATES.isEmpty()) {
            return;
        }
        for (var id : requestedKeys) {
            if (id != null && ClientStats.CACHE.stall(new ProfileKey(id)).isEmpty()) {
                PLATES.remove(id);
            }
        }
    }

    static void clearPlates() {
        PLATES.clear();
        current = null;
    }

    /**
     * Plate gate for every loader's render hook. Unknown outputs (no cache
     * entry yet, e.g. the CPU screen is closed so no snapshot ever arrived)
     * still show: only a positive entry without a stall hides the plate.
     * prunePlates drops that case once a snapshot arrives.
     */
    public static boolean shouldShowPlates(String outputId) {
        if (outputId == null || outputId.isBlank()) {
            return false;
        }
        var key = new ProfileKey(outputId);
        return ClientStats.CACHE.stall(key).isPresent() || ClientStats.CACHE.get(key).isEmpty();
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

    /** Full rainbow color cycle period in milliseconds. */
    public static final long RAINBOW_PERIOD_MILLIS = 3000L;

    /**
     * Time-cycling rainbow color shared by every loader's render hook, so the
     * highlight contrasts with any environment instead of blending into
     * red-tinted builds. Returns {@code {red, green, blue}} with each
     * component in {@code [0, 1]}.
     */
    public static float[] rainbowRgb() {
        return rainbowRgb(System.currentTimeMillis());
    }

    static float[] rainbowRgb(long timeMillis) {
        var phase = (timeMillis % RAINBOW_PERIOD_MILLIS) / (double) RAINBOW_PERIOD_MILLIS * Math.PI * 2.0;
        return new float[] {0.5f + 0.5f * (float) Math.cos(phase),
                0.5f + 0.5f * (float) Math.cos(phase - Math.PI * 2.0 / 3.0),
                0.5f + 0.5f * (float) Math.cos(phase + Math.PI * 2.0 / 3.0)};
    }

    private ProviderHighlightClient() {
    }
}
