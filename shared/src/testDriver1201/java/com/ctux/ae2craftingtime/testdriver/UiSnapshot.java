package com.ctux.ae2craftingtime.testdriver;

import java.util.List;

public record UiSnapshot(
        String screen,
        String menu,
        Rect gui,
        int screenWidth,
        int screenHeight,
        double guiScale,
        long frame,
        int scroll,
        List<Row> rows,
        List<ObservedText> text,
        List<Rect> badges,
        List<Widget> widgets,
        List<Rect> itemCells,
        List<ObservedText> tooltip) {
    public UiSnapshot {
        rows = List.copyOf(rows);
        text = List.copyOf(text);
        badges = List.copyOf(badges);
        widgets = List.copyOf(widgets);
        itemCells = List.copyOf(itemCells);
        tooltip = List.copyOf(tooltip);
    }

    public record Row(String outputId, long craftAmount, Rect cell, List<ObservedText> description) {
        public Row {
            description = List.copyOf(description);
        }
    }

    public record ObservedText(String key, String rendered, List<String> arguments, Rect bounds, Integer color, boolean bold) {
        public ObservedText(String key, String rendered, List<String> arguments, Rect bounds) {
            this(key, rendered, arguments, bounds, null, false);
        }
        public ObservedText {
            arguments = List.copyOf(arguments);
        }
    }

    public record Widget(String type, String state, Rect bounds, List<ObservedText> tooltip) {
        public Widget {
            tooltip = List.copyOf(tooltip);
        }
    }
}
