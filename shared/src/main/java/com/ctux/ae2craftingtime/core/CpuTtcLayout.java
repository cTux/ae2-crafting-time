package com.ctux.ae2craftingtime.core;

public final class CpuTtcLayout {
    public static Badge badge(int cardWidth, int cardHeight, int textWidth, double nameScale) {
        if (cardWidth < 0 || cardHeight < 0 || textWidth < 0 || nameScale <= 0) {
            throw new IllegalArgumentException("invalid CPU-card dimensions");
        }
        double scale = 0.6;
        if (textWidth > 0) scale = Math.min(scale, Math.max(0, (cardWidth - 8.0) / textWidth));
        scale = Math.min(scale, Math.max(0, (cardHeight - 16.0) / 9.0));
        int scaledTextWidth = (int) Math.ceil(textWidth * scale);
        int width = scaledTextWidth + 4;
        int height = (int) Math.ceil(9 * scale) + 2;
        int availableNameWidth = Math.max(0, (int) ((cardWidth - width - 7) / nameScale));
        return new Badge(scale, scaledTextWidth, width, height, availableNameWidth);
    }

    public record Badge(double scale, int textWidth, int width, int height, int availableNameWidth) { }

    private CpuTtcLayout() { }
}
