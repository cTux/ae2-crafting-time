package com.ctux.ae2craftingtime.testdriver;

public record Rect(int x, int y, int width, int height) {
    public Rect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("negative rectangle size");
        }
    }

    public boolean inside(Rect outer) {
        return x >= outer.x && y >= outer.y && x + width <= outer.x + outer.width
                && y + height <= outer.y + outer.height;
    }

    public boolean overlaps(Rect other) {
        return width > 0 && height > 0 && other.width > 0 && other.height > 0
                && x < other.x + other.width && x + width > other.x
                && y < other.y + other.height && y + height > other.y;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }
}
