package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.testdriver.mixin.ContainerScreenAccessor;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import appeng.api.stacks.AEKey;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UiObservationStore {
    private static final int TABLE_X = 9;
    private static final int TABLE_Y = 19;
    private static final int CELL_WIDTH = 67;
    private static final int CELL_HEIGHT = 22;
    private static final int PITCH_X = 68;
    private static final int PITCH_Y = 23;
    private static final Set<String> WIRELESS_SCREENS = Set.of(
            "appeng.client.gui.me.common.MEStorageScreen",
            "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen",
            "de.mari_023.ae2wtlib.wct.WCTScreen");
    private static Frame active;
    private static volatile UiSnapshot latest;
    private static volatile List<UiSnapshot.ObservedText> wirelessTooltip = List.of();
    private static long sequence;

    public static void reset() {
        active = null;
        latest = null;
        wirelessTooltip = List.of();
    }

    public static void begin(Minecraft minecraft) {
        if (!(minecraft.screen instanceof AEBaseScreen<?> screen)
                || (!(screen instanceof CraftConfirmScreen)
                && !(screen instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?>)
                && !screen.getClass().getName().equals(Ae2NetworkAnalyserFixture.SCREEN)
                && !screen.getClass().getName().equals(RequesterFixture.SCREEN)
                && !CraftingTreeScenario.isScreen(screen.getClass().getName()))) {
            active = null;
            return;
        }
        active = new Frame(screen.getClass().getName(), screen.getMenu().getClass().getName(),
                new Rect(screen.getGuiLeft(), screen.getGuiTop(), ((ContainerScreenAccessor) screen).ae2craftingtime_test_driver$width(),
                        ((ContainerScreenAccessor) screen).ae2craftingtime_test_driver$height()),
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(),
                minecraft.getWindow().getGuiScale());
    }

    public static void rows(List<?> entries, int scroll) {
        if (active == null) {
            return;
        }
        active.scroll = scroll;
        active.rows.clear();
        for (int i = 0; i < entries.size(); i++) {
            if (!(entries.get(i) instanceof CraftingPlanSummaryEntry entry)) {
                continue;
            }
            var visibleIndex = i - scroll * 3;
            if (visibleIndex < 0 || visibleIndex >= 15) {
                continue;
            }
            var cell = new Rect(active.gui.x() + TABLE_X + visibleIndex % 3 * PITCH_X,
                    active.gui.y() + TABLE_Y + visibleIndex / 3 * PITCH_Y, CELL_WIDTH, CELL_HEIGHT);
            active.rows.add(new PendingRow(entry.getWhat().getId().toString(), entry.getCraftAmount(), cell));
            active.itemCells.add(new Rect(cell.x() + CELL_WIDTH - 19, cell.y() + 3, 16, 16));
        }
    }

    public static void description(CraftingPlanSummaryEntry entry, List<Component> components) {
        if (active != null) {
            active.descriptions.put(entry.getWhat().getId().toString(), observed(components, null));
        }
    }

    public static void tooltip(CraftingPlanSummaryEntry entry, List<Component> components) {
        if (active != null) {
            active.tooltip.clear();
            active.tooltip.addAll(observed(components, null));
        }
    }

    public static void text(GuiGraphicsExtractor graphics, Component component, int x, int y, int width, int height) {
        if (active == null) {
            return;
        }
        var observed = observed(component, transformed(graphics, x, y, x + width, y + height));
        if (observed.key().startsWith("text.ae2craftingtime.")) {
            active.text.add(observed);
        }
    }

    public static void fill(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        if (active != null && color == 0xB0000000) {
            active.badges.add(transformed(graphics, x1, y1, x2, y2));
        }
    }

    public static void finish(Minecraft minecraft) {
        if (active == null || minecraft.screen == null) {
            return;
        }
        if (active.screen.equals(RequesterFixture.SCREEN)) {
            var screen = (AEBaseScreen<?>) minecraft.screen;
            screen.getMenu().slots.stream().filter(net.minecraft.world.inventory.Slot::isActive)
                    .map(slot -> new Rect(active.gui.x() + slot.x, active.gui.y() + slot.y, 16, 16))
                    .forEach(active.itemCells::add);
        }
        if (minecraft.screen instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?> screen) {
            var accessor = (com.ctux.ae2craftingtime.testdriver.mixin.CraftingStatusAccessor) screen;
            var status = accessor.ae2craftingtime_test_driver$status();
            if (status != null) {
                active.rows.clear();
                int scroll = accessor.ae2craftingtime_test_driver$scrollbar().getCurrentScroll();
                var entries = status.getEntries();
                for (int i = scroll * 3; i < Math.min(entries.size(), scroll * 3 + 18); i++) {
                    var entry = entries.get(i);
                    int visible = i - scroll * 3;
                    var cell = new Rect(active.gui.x() + 9 + visible % 3 * 68,
                            active.gui.y() + 19 + visible / 3 * 23, 67, 22);
                    active.rows.add(new PendingRow(entry.getWhat().getId().toString(),
                            entry.getActiveAmount() + entry.getPendingAmount(), cell));
                    active.itemCells.add(new Rect(cell.x() + CELL_WIDTH - 19, cell.y() + 3, 16, 16));
                }
            }
        }
        for (var child : minecraft.screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible) {
                var state = widget instanceof TtcSortButton button
                        ? button.getTooltipMessage().stream().map(Component::getString).reduce((a, b) -> a + " | " + b)
                                .orElse("")
                        : "";
                active.widgets.add(new UiSnapshot.Widget(widget.getClass().getName(), state,
                        new Rect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()), List.of()));
            }
        }
        var rows = active.rows.stream().map(row -> new UiSnapshot.Row(row.outputId, row.craftAmount, row.cell,
                active.descriptions.getOrDefault(row.outputId, List.of()))).toList();
        latest = new UiSnapshot(active.screen, active.menu, active.gui, active.screenWidth, active.screenHeight,
                active.guiScale, ++sequence, active.scroll, rows, active.text, merge(active.badges), active.widgets,
                active.itemCells, active.tooltip);
        active = null;
    }

    public static UiSnapshot latest() {
        return latest;
    }

    public static void wirelessTooltip(List<Component> components) {
        if (active != null && (CraftingTreeScenario.isScreen(active.screen)
                || Minecraft.getInstance().screen instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?>)) {
            active.tooltip.clear();
            active.tooltip.addAll(observed(components, null));
        }
        if (Minecraft.getInstance().screen != null
                && isWirelessScreen(Minecraft.getInstance().screen.getClass().getName())) {
            wirelessTooltip = observed(components, null);
        }
    }

    static boolean isWirelessScreen(String className) {
        return WIRELESS_SCREENS.contains(className);
    }

    public static void treeNode(GuiGraphicsExtractor graphics, AEKey key, int x, int y) {
        if (active != null && CraftingTreeScenario.isScreen(active.screen)) {
            var bounds = transformed(graphics, x, y, x + 16, y + 16);
            active.rows.add(new PendingRow(key.getId().toString(), 0, bounds));
            active.itemCells.add(bounds);
        }
    }

    private static Rect transformed(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
        var pose = graphics.pose();
        var from = pose.transformPosition(new org.joml.Vector2f(x1, y1));
        var to = pose.transformPosition(new org.joml.Vector2f(x2, y2));
        return new Rect((int) Math.floor(from.x), (int) Math.floor(from.y),
                (int) Math.ceil(to.x - from.x), (int) Math.ceil(to.y - from.y));
    }

    public static List<UiSnapshot.ObservedText> wirelessTooltip() {
        return wirelessTooltip;
    }

    public static void clearWirelessTooltip() {
        wirelessTooltip = List.of();
    }

    static List<UiSnapshot.ObservedText> observed(List<Component> components, Rect bounds) {
        var result = new ArrayList<UiSnapshot.ObservedText>();
        for (var component : components) {
            result.add(observed(component, bounds));
            result.addAll(observed(component.getSiblings(), bounds));
        }
        return List.copyOf(result);
    }

    private static UiSnapshot.ObservedText observed(Component component, Rect bounds) {
        if (component.getContents() instanceof TranslatableContents translated) {
            var arguments = new ArrayList<String>();
            for (var argument : translated.getArgs()) {
                if (argument instanceof Component nested
                        && nested.getContents() instanceof TranslatableContents nestedTranslation) {
                    arguments.add(nestedTranslation.getKey());
                } else {
                    arguments.add(argument instanceof Component nested ? nested.getString() : String.valueOf(argument));
                }
            }
            return new UiSnapshot.ObservedText(translated.getKey(), component.getString(), arguments, bounds);
        }
        return new UiSnapshot.ObservedText("literal", component.getString(), List.of(), bounds);
    }

    private static List<Rect> merge(List<Rect> rectangles) {
        var merged = new ArrayList<Rect>();
        for (var rectangle : rectangles) {
            var match = -1;
            for (int i = 0; i < merged.size(); i++) {
                if (merged.get(i).overlaps(rectangle)) {
                    match = i;
                    break;
                }
            }
            if (match < 0) {
                merged.add(rectangle);
            } else {
                var old = merged.get(match);
                var left = Math.min(old.x(), rectangle.x());
                var top = Math.min(old.y(), rectangle.y());
                var right = Math.max(old.x() + old.width(), rectangle.x() + rectangle.width());
                var bottom = Math.max(old.y() + old.height(), rectangle.y() + rectangle.height());
                merged.set(match, new Rect(left, top, right - left, bottom - top));
            }
        }
        return List.copyOf(merged);
    }

    private record PendingRow(String outputId, long craftAmount, Rect cell) {
    }

    private static final class Frame {
        private final String screen;
        private final String menu;
        private final Rect gui;
        private final int screenWidth;
        private final int screenHeight;
        private final double guiScale;
        private int scroll;
        private final List<PendingRow> rows = new ArrayList<>();
        private final Map<String, List<UiSnapshot.ObservedText>> descriptions = new LinkedHashMap<>();
        private final List<UiSnapshot.ObservedText> text = new ArrayList<>();
        private final List<Rect> badges = new ArrayList<>();
        private final List<UiSnapshot.Widget> widgets = new ArrayList<>();
        private final List<Rect> itemCells = new ArrayList<>();
        private final List<UiSnapshot.ObservedText> tooltip = new ArrayList<>();

        private Frame(String screen, String menu, Rect gui, int screenWidth, int screenHeight, double guiScale) {
            this.screen = screen;
            this.menu = menu;
            this.gui = gui;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.guiScale = guiScale;
        }
    }

    private UiObservationStore() {
    }
}
