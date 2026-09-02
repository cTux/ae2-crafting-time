package com.ctux.ae2craftingtime.core;

public final class RequesterTtcLayout {
    public static final int BADGE_X = 46;
    public static final int STATUS_OFFSET = 64;

    public static int statusOffset(String widgetId) {
        return widgetId.startsWith("request_status_") ? STATUS_OFFSET : 0;
    }

    public static int rowTop(int headerHeight, int rowHeight, int row) {
        return headerHeight + row * rowHeight + 12;
    }

    public static float rowScale(int textWidth, int lineHeight) {
        // Leave four horizontal and two vertical pixels for badge padding.
        return Math.min(0.5f, Math.min(60.0f / Math.max(1, textWidth), 5.0f / Math.max(1, lineHeight)));
    }

    private RequesterTtcLayout() {
    }
}
