package com.ctux.ae2craftingtime.mc1201;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/**
 * Client-side only. Holds the latest rainbow edge (dimension, block
 * positions, expiry timestamp) for the per-loader render hooks, plus one
 * persistent red plate per delayed output. Edges and plates have independent
 * lifetimes: edges expire after {@link ProviderLocateCommand#HIGHLIGHT_SECONDS}
 * seconds or when their provider breaks; plates persist until the craft ends,
 * is cancelled, recovers, or the provider breaks. Manual locates touch the
 * edge only; craft-state changes touch plates only. Never touched on a
 * dedicated server.
 */
public final class ProviderHighlightClient {
    public record Highlight(String dimensionId, List<BlockPos> positions, String outputId, long expiresAtMillis) {
        public Highlight {
            outputId = outputId == null ? "" : outputId;
        }
    }

    public record Plate(String dimensionId, List<BlockPos> positions, String outputId,
            long highlightedAtMillis) {
        public Plate {
            positions = positions == null ? List.of() : List.copyOf(positions);
            outputId = outputId == null ? "" : outputId;
        }

        public Plate(String dimensionId, List<BlockPos> positions, String outputId) {
            this(dimensionId, positions, outputId, System.currentTimeMillis());
        }
    }

    private static final int MAX_PLATES = 32;
    private static final LinkedHashMap<String, Plate> PLATES = new LinkedHashMap<>();

    private static volatile Highlight current;

    /**
     * Shows the temporary rainbow edge for one manual locate without touching
     * any red plate. Double-clicks and chat clicks use exactly this, so
     * recovering, finishing, or cancelling before expiry removes only the
     * plate. Empty requests clear the matching edge only, never the plate.
     */
    public static void show(String dimensionId, List<BlockPos> positions, int durationSeconds, String outputId) {
        if (outputId != null && !outputId.isBlank()
                && (durationSeconds <= 0 || positions == null || positions.isEmpty())) {
            clearEdgeFor(outputId);
            return;
        }
        if (dimensionId == null || positions == null || positions.isEmpty() || durationSeconds <= 0) {
            return;
        }
        current = new Highlight(dimensionId, List.copyOf(positions), outputId,
                System.currentTimeMillis() + durationSeconds * 1000L);
    }

    /**
     * Remembers the red plate (background plus item icon) for one output
     * without touching the rainbow edge. The server sends exactly this for
     * automatic delayed pings, so plates appear with no open window and no
     * edge; manual locates use {@link #show} for edge only.
     */
    public static void showPlate(String dimensionId, List<BlockPos> positions, String outputId) {
        if (dimensionId == null || positions == null || positions.isEmpty() || outputId == null
                || outputId.isBlank()) {
            return;
        }
        storePlate(dimensionId, positions, outputId);
    }

    private static void storePlate(String dimensionId, List<BlockPos> positions, String outputId) {
        if (outputId == null || outputId.isBlank()) {
            return;
        }
        PLATES.put(outputId, new Plate(dimensionId, positions, outputId));
        while (PLATES.size() > MAX_PLATES) {
            var eldest = PLATES.keySet().iterator();
            eldest.next();
            eldest.remove();
        }
    }

    public static Highlight live() {
        return liveAt(System.currentTimeMillis());
    }

    static Highlight liveAt(long nowMillis) {
        var highlight = current;
        if (highlight == null || nowMillis >= highlight.expiresAtMillis()) {
            current = null;
            return null;
        }
        return highlight;
    }

    /**
     * Removes one output's persistent red plate without touching any rainbow
     * edge. The server sends an empty highlight for exactly this on craft
     * finish, cancel, and stall recovery, so a rainbow triggered just before
     * survives until its own 15-second expiry or provider break.
     */
    public static void clearFor(String outputId) {
        if (outputId == null || outputId.isBlank()) {
            return;
        }
        PLATES.remove(outputId);
    }

    /**
     * Clears the live rainbow edge when it belongs to the given output,
     * without touching any red plate. Used for empty manual requests; craft
     * state changes must use {@link #clearFor} instead so rainbows survive
     * recovery, finish, and cancel.
     */
    public static void clearEdgeFor(String outputId) {
        if (outputId == null || outputId.isBlank()) {
            return;
        }
        var highlight = current;
        if (highlight != null && outputId.equals(highlight.outputId())) {
            current = null;
        }
    }

    /**
     * Drops positions the {@code keep} predicate rejects (for example broken
     * provider blocks observed during render) from the live edge and every
     * plate in the given dimension. Entries left with no positions disappear;
     * other dimensions are untouched.
     */
    public static void trimPositions(String dimensionId, Predicate<BlockPos> keep) {
        if (dimensionId == null || keep == null) {
            return;
        }
        var highlight = current;
        if (highlight != null && dimensionId.equals(highlight.dimensionId())) {
            var kept = highlight.positions().stream().filter(keep).toList();
            current = kept.isEmpty() ? null
                    : new Highlight(highlight.dimensionId(), kept, highlight.outputId(),
                            highlight.expiresAtMillis());
        }
        var emptied = new ArrayList<String>();
        for (var entry : PLATES.entrySet()) {
            var plate = entry.getValue();
            if (!dimensionId.equals(plate.dimensionId())) {
                continue;
            }
            var kept = plate.positions().stream().filter(keep).toList();
            if (kept.isEmpty()) {
                emptied.add(entry.getKey());
            } else if (kept.size() != plate.positions().size()) {
                entry.setValue(new Plate(plate.dimensionId(), kept, plate.outputId(),
                        plate.highlightedAtMillis()));
            }
        }
        emptied.forEach(PLATES::remove);
    }

    /** Persistent plates for the render hooks to filter by dimension. Server authoritative. */
    public static List<Plate> plates() {
        return new ArrayList<>(PLATES.values());
    }

    /**
     * Legacy snapshot hook kept for loader compatibility. Server-owned craft
     * state is authoritative for plates: snapshots from another CPU, the
     * planning screen, or a closed window must never remove a still-delayed
     * plate, so this is now a no-op. Plates disappear only on explicit server
     * clear (recovery, finish, cancel) or provider break (render trim).
     */
    public static void prunePlates(List<String> requestedKeys) {
        return;
    }

    static void clearPlates() {
        PLATES.clear();
        current = null;
    }

    /**
     * Plate gate for every loader's render hook. Server-owned craft state is
     * authoritative: any stored plate shows until the server explicitly clears
     * it (recovery, finish, cancel) or the provider breaks (render trim).
     * Never consults the general UI stats cache, so opening another CPU, the
     * planning screen, or closing all windows can no longer hide a
     * still-delayed plate. Wall-clock plays no role here: the 15-second window
     * gates edges only.
     */
    public static boolean shouldShowPlates(String outputId) {
        return outputId != null && !outputId.isBlank();
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
