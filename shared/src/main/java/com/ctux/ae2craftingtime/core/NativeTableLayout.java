package com.ctux.ae2craftingtime.core;

/** Geometry shared by AE2's native table and its screen input handlers. */
public record NativeTableLayout(int columns, int cellWidth, int cellHeight, int rows, float textScale) {
    public static NativeTableLayout of(int guiScale, int columns, int cellWidth, int cellHeight,
            int rows, boolean active) {
        if (columns < 1 || cellWidth < 1 || cellHeight < 1 || rows < 1) {
            throw new IllegalArgumentException("invalid table geometry");
        }
        if (guiScale != 1 || !active || rows * (cellHeight + 1) < 55) {
            return new NativeTableLayout(columns, cellWidth, cellHeight, rows, 0.5f);
        }
        return new NativeTableLayout(1, columns * (cellWidth + 1) - 1, 54,
                Math.max(1, rows * (cellHeight + 1) / 55), 1f);
    }

    public int entryAt(int x, int y, int scroll) {
        if (x < 0 || y < 0 || scroll < 0 || x % (cellWidth + 1) >= cellWidth
                || y % (cellHeight + 1) >= cellHeight) {
            return -1;
        }
        int column = x / (cellWidth + 1);
        int row = y / (cellHeight + 1);
        return column < columns && row < rows ? (row + scroll) * columns + column : -1;
    }

    public int scrollableRows(int entries) {
        return Math.max(0, entries / columns + (entries % columns == 0 ? 0 : 1) - rows);
    }

    public int badgeVerticalPadding() {
        return textScale == 1f ? 0 : 2;
    }
}
