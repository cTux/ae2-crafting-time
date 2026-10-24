package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.testdriver.mixin.ContainerScreenAccessor;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import com.ctux.ae2craftingtime.core.NativeTableLayout;
import com.ctux.ae2craftingtime.core.PresentationHooks;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final Set<String> WIRELESS_SCREENS = Set.of(
            "appeng.client.gui.me.common.MEStorageScreen",
            "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen",
            "de.mari_023.ae2wtlib.wct.WCTScreen");
    private static Frame active;
    private static volatile UiSnapshot latest;
    private static Map<String, Object> latestNativeHover = Map.of();
    private static volatile List<UiSnapshot.ObservedText> wirelessTooltip = List.of();
    private static long sequence;
    private static appeng.menu.me.crafting.CraftingStatus syntheticStatus;

    static appeng.menu.me.crafting.CraftingStatus synthetic(appeng.menu.me.crafting.CraftingStatus status) {
        syntheticStatus = status;
        return status;
    }

    public static void reset() {
        active = null;
        latest = null;
        latestNativeHover = Map.of();
        syntheticStatus = null;
        wirelessTooltip = List.of();
    }

    public static void begin(Minecraft minecraft) {
        TreeAmountObservation.reset();
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
        var layout = tableLayout(false);
        for (int i = 0; i < entries.size(); i++) {
            if (!(entries.get(i) instanceof CraftingPlanSummaryEntry entry)) {
                continue;
            }
            var visibleIndex = i - scroll * layout.columns();
            if (visibleIndex < 0 || visibleIndex >= layout.rows() * layout.columns()) {
                continue;
            }
            var cell = cell(layout, visibleIndex);
            active.rows.add(new PendingRow(entry.getWhat().getId().toString(), entry.getCraftAmount(),
                    entry.getMissingAmount(), cell, entry));
            active.itemCells.add(new Rect(cell.x() + layout.cellWidth() - 19, cell.y() + 3, 16, 16));
        }
    }

    public static void description(CraftingPlanSummaryEntry entry, List<Component> components) {
        if (active != null) {
            // The outer renderer wrapper passes the final decorated list here.
            active.planDescriptions.put(entry, components);
        }
    }

    public static void description(CraftingStatusEntry entry, List<Component> components) {
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

    public static void tooltip(CraftingStatusEntry entry, List<Component> components) {
        if (active != null) {
            active.tooltip.clear();
            active.tooltip.addAll(observed(components, null));
        }
    }

    public static void text(GuiGraphics graphics, Component component, int x, int y, int width, int height) {
        if (active == null) {
            return;
        }
        var observed = CraftingTreeScenario.nodeTtcText(active.screen,
                observed(component, transformed(graphics, x, y, x + width, y + height)));
        if (recordStatusText(component.getString(), observed.bounds())) return;
        var total = planTotalText(active.screen, active.gui, observed);
        if (total != null) {
            active.text.add(total);
            return;
        }
        if (observed.key().startsWith("text.ae2craftingtime.")
                || observed.key().equals("gui.ae2craftingtime.suspended")
                || active.screen.contains("CraftConfirmScreen")
                && (observed.key().equals("gui.ae2.FromStorage") || observed.key().equals("gui.ae2.ToCraft"))
                && active.rows.stream().anyMatch(row -> observed.bounds().inside(row.cell()))
                || active.screen.contains("CraftingStatusScreen")
                || active.cpuCards.stream().anyMatch(card -> observed.bounds().overlaps(card.nameArea))) {
            active.text.add(observed);
        }
    }

    public static void nativeTitle(GuiGraphics graphics, String text, int x, int y, int width, int height,
            Integer color, boolean bold) {
        if (active != null) {
            var bounds = transformed(graphics, x, y, x + width, y + height);
            if (recordStatusText(text, bounds)) return;
            if (recordNativePlanAmount(text, bounds, color, bold)) return;
            // Newer AE2 flattens styled row labels into formatted characters.
            if (active.screen.contains("CraftingStatusScreen") && statusRowText(bounds)) {
                active.text.add(new UiSnapshot.ObservedText("native-status-text", text, List.of(), bounds,
                        color, bold));
                return;
            }
        }
        if (active != null && active.screen.contains("CraftingStatusScreen")
                && new Rect(transformed(graphics, x, y, x + width, y + height).x(),
                        transformed(graphics, x, y, x + width, y + height).y(), 0, 0).inside(active.gui)
                && transformed(graphics, x, y, x + width, y + height).y() < active.gui.y() + 19
                && (text.startsWith("TTC:")
                        || text.startsWith(appeng.core.localization.GuiText.CraftingStatus.text().getString()))) {
            active.text.add(new UiSnapshot.ObservedText("native-title", text, List.of(),
                    transformed(graphics, x, y, x + width, y + height)));
        }
        if (active != null && active.screen.contains("CraftingCPUScreen")
                && text.equals(net.minecraft.client.resources.language.I18n.get("gui.ae2craftingtime.suspended"))) {
            active.text.add(new UiSnapshot.ObservedText("gui.ae2craftingtime.suspended", text, List.of(),
                    transformed(graphics, x, y, x + width, y + height)));
        }
    }

    private static boolean recordNativePlanAmount(String text, Rect bounds, Integer color, boolean bold) {
        if (active == null || !active.screen.contains("CraftConfirmScreen")) return false;
        for (var row : active.rows) {
            if (!bounds.inside(row.cell())) continue;
            var description = active.planDescriptions.get(row.identity());
            if (description == null) continue;
            for (var component : description) {
                var observed = observed(component, bounds);
                if (!(observed.key().equals("gui.ae2.FromStorage") || observed.key().equals("gui.ae2.ToCraft"))
                        || !observed.rendered().equals(text)) continue;
                if (active.text.stream().noneMatch(existing -> existing.key().equals(observed.key())
                        && existing.bounds().equals(bounds)))
                    active.text.add(new UiSnapshot.ObservedText(observed.key(), text, observed.arguments(), bounds,
                            color, bold));
                return true;
            }
        }
        return false;
    }

    private static boolean statusRowText(Rect bounds) {
        var layout = tableLayout(true);
        for (int index = 0; index < layout.rows() * layout.columns(); index++) {
            var cell = cell(layout, index);
            if (bounds.inside(cell)) return true;
        }
        return false;
    }

    private static NativeTableLayout tableLayout(boolean status) {
        return NativeTableLayout.of((int) active.guiScale, 3, 67, 22, status ? 6 : 5,
                ClientOptionsRuntime.enabled(status ? OptionFeature.STATUS_ROWS : OptionFeature.PLAN_ROWS)
                        && PresentationHooks.table(status));
    }

    private static Rect cell(NativeTableLayout layout, int visible) {
        return new Rect(active.gui.x() + TABLE_X + visible % layout.columns() * (layout.cellWidth() + 1),
                active.gui.y() + TABLE_Y + visible / layout.columns() * (layout.cellHeight() + 1),
                layout.cellWidth(), layout.cellHeight());
    }

    private static boolean recordStatusText(String rendered, Rect bounds) {
        var semantic = semanticText(active.descriptions, active.planDescriptions, rendered);
        if (semantic == null) return false;
        if (active.text.stream().noneMatch(line -> line.key().equals(semantic.key()) && bounds.equals(line.bounds())))
            active.text.add(new UiSnapshot.ObservedText(semantic.key(), semantic.rendered(), semantic.arguments(),
                    bounds, semantic.color(), semantic.bold()));
        return true;
    }

    static UiSnapshot.ObservedText semanticText(Map<String, List<UiSnapshot.ObservedText>> descriptions,
            Map<Object, List<Component>> planDescriptions, String rendered) {
        return java.util.stream.Stream.concat(descriptions.values().stream(),
                planDescriptions.values().stream().map(lines -> observed(lines, null))).flatMap(List::stream)
                .filter(line -> line.key().startsWith("text.ae2craftingtime.")
                        && line.rendered().equals(rendered))
                .findFirst().orElse(null);
    }

    static List<UiSnapshot.ObservedText> planDescription(Map<Object, List<Component>> descriptions, Object identity) {
        return observed(descriptions.getOrDefault(identity, List.of()), null);
    }

    static UiSnapshot.ObservedText planTotalText(String screen, Rect gui, UiSnapshot.ObservedText text) {
        // The production footer flattens its translated component to recolor symbols before drawing it.
        if (!screen.endsWith("CraftConfirmScreen") || text.bounds() == null
                || text.bounds().y() != gui.y() + 178 || !text.rendered().startsWith("TTC:")) return null;
        return new UiSnapshot.ObservedText("text.ae2craftingtime.total_ttc", text.rendered(),
                text.arguments(), text.bounds(), text.color(), text.bold());
    }

    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (active != null && color == com.ctux.ae2craftingtime.mc1201.TtcBadge.BACKGROUND) {
            active.badges.add(transformed(graphics, x1, y1, x2, y2));
        }
    }

    public static void beginCpuCards(List<appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry> entries,
            int scroll) {
        if (active == null) return;
        active.cpuCards.clear();
        active.scroll = scroll;
        active.rawCpuSerials = entries.stream().map(
                appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry::serial).toList();
    }

    public static void cpuCard(appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry cpu,
            int selectedSerial, int x, int y, int width, int height) {
        if (active == null) return;
        var job = cpu.currentJob();
        var name = cpu.name() == null ? "CPU #" + cpu.serial() : cpu.name().getString();
        var bounds = new Rect(x, y, width, height);
        active.cpuCards.add(new PendingCpuCard(cpu.serial(), name,
                job == null ? null : job.what().getId().toString(), job == null ? 0 : job.amount(),
                cpu.elapsedTimeNanos(), cpu.serial() == selectedSerial, bounds,
                new Rect(bounds.x() + 2, bounds.y() + 2, Math.max(0, width - 4), 10),
                new Rect(bounds.x() + 2, bounds.y() + height - 12, Math.max(0, width - 4), 10),
                new Rect(bounds.x() + 1, bounds.y() + height - 2, Math.max(0, width - 2), 1)));
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
        UiSnapshot.StatusObservation statusObservation = null;
        if (minecraft.screen instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?> screen) {
            var accessor = (com.ctux.ae2craftingtime.testdriver.mixin.CraftingStatusAccessor) screen;
            var status = accessor.ae2craftingtime_test_driver$status();
            if (status != null) {
                int serial = screen.getMenu() instanceof appeng.menu.me.crafting.CraftingStatusMenu menu
                        ? menu.getSelectedCpuSerial() : -1;
                var card = active.cpuCards.stream().filter(cpu -> cpu.serial == serial).findFirst().orElse(null);
                var synced = ((com.ctux.ae2craftingtime.mc1201.RowStatsJobMenu) screen.getMenu())
                        .ae2craftingtime$rowStatsJob().forContext(
                                com.ctux.ae2craftingtime.mc1201.StatsRequestContext.cpuContext(screen.getMenu()));
                String jobUuid = synced.equals(com.ctux.ae2craftingtime.core.RowStatsJob.NO_JOB)
                        || synced.equals(com.ctux.ae2craftingtime.core.RowStatsJob.UNAVAILABLE_JOB)
                        ? null : synced.toString();
                long completed = status.getStartItemCount() - status.getRemainingItemCount();
                statusObservation = new UiSnapshot.StatusObservation(System.identityHashCode(status),
                        status == syntheticStatus ? "synthetic" : "native",
                        status.getElapsedTime(), status.getStartItemCount(), status.getRemainingItemCount(),
                        completed, status.getStartItemCount() == 0 ? 0
                                : (double) completed / status.getStartItemCount(),
                        serial == -1 ? null : serial, jobUuid, card == null ? null : card.jobId,
                        card == null ? null : card.elapsedNanos);
                active.rows.clear();
                int scroll = accessor.ae2craftingtime_test_driver$scrollbar().getCurrentScroll();
                var entries = status.getEntries();
                var layout = tableLayout(true);
                for (int i = scroll * layout.columns();
                        i < Math.min(entries.size(), (scroll + layout.rows()) * layout.columns()); i++) {
                    var entry = entries.get(i);
                    int visible = i - scroll * layout.columns();
                    var cell = cell(layout, visible);
                    active.rows.add(new PendingRow(entry.getWhat().getId().toString(),
                            entry.getActiveAmount() + entry.getPendingAmount(), 0, cell, null,
                            entry.getStoredAmount(), entry.getActiveAmount(), entry.getPendingAmount()));
                    active.itemCells.add(new Rect(cell.x() + layout.cellWidth() - 19, cell.y() + 3, 16, 16));
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
        var rows = active.rows.stream().map(row -> new UiSnapshot.Row(row.outputId, row.craftAmount,
                row.missingAmount, row.cell,
                row.identity == null ? rowDescription(active.descriptions, active.text, row.outputId, row.cell)
                        : planDescription(active.planDescriptions, row.identity),
                row.storedAmount, row.activeAmount, row.pendingAmount)).toList();
        var mergedBadges = merge(active.badges);
        var cpuCards = active.cpuCards.stream().map(card -> {
            var badge = mergedBadges.stream().filter(rect -> rect.inside(card.bounds)).findFirst().orElse(null);
            var ttc = cpuTtcText(active.text, card.bounds);
            return new UiSnapshot.CpuCard(card.serial, card.name, card.jobId, card.amount, card.elapsedNanos,
                    card.selected, card.bounds, card.nameArea, card.infoArea, card.progressArea, ttc, badge);
        }).toList();
        latestNativeHover = active.nativeHover;
        latest = new UiSnapshot(active.screen, active.menu, active.gui, active.screenWidth, active.screenHeight,
                active.guiScale, ++sequence, active.scroll, rows, active.text, mergedBadges, active.widgets,
                active.itemCells, active.tooltip, cpuCards, active.rawCpuSerials, statusObservation);
        active = null;
    }

    static UiSnapshot.ObservedText cpuTtcText(List<UiSnapshot.ObservedText> drawn, Rect card) {
        var text = drawn.stream().filter(value -> value.bounds() != null && value.bounds().inside(card)
                && (value.key().equals("text.ae2craftingtime.ttc")
                || value.key().equals("literal") && value.bounds().x() >= card.x() + card.width() / 4
                && value.rendered().startsWith(
                        com.ctux.ae2craftingtime.core.TtcSymbols.Symbol.TIME.glyph() + " ")))
                .findFirst().orElse(null);
        if (text == null || !text.key().equals("literal")) return text;
        return new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", text.rendered(),
                text.arguments(), text.bounds(), text.color(), text.bold());
    }

    public static void nativeHover(appeng.client.gui.StackWithBounds hovered) {
        if (active == null || hovered == null) return;
        var bounds = hovered.bounds();
        active.nativeHover = Map.of("outputId", hovered.stack().what().getId().toString(),
                "bounds", new Rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
    }

    public static Map<String, Object> nativeHover(UiSnapshot snapshot) {
        return snapshot == latest ? latestNativeHover : Map.of();
    }

    public static UiSnapshot latest() {
        return latest;
    }

    public static void wirelessTooltip(List<Component> components) {
        if (active != null && (CraftingTreeScenario.isScreen(active.screen)
                || active.screen.equals("com.appliedenhancements.client.plan.AelisCraftConfirmScreen")
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

    static List<UiSnapshot.ObservedText> rowDescription(
            Map<String, List<UiSnapshot.ObservedText>> descriptions,
            List<UiSnapshot.ObservedText> text, String outputId, Rect cell) {
        var recorded = descriptions.get(outputId);
        if (recorded != null && !recorded.isEmpty()) return recorded;
        return text.stream().filter(value -> value.bounds() != null && value.bounds().inside(cell)).toList();
    }

    public static void treeNode(GuiGraphics graphics, AEKey key, int x, int y) {
        if (active != null && CraftingTreeScenario.isScreen(active.screen)) {
            var bounds = transformed(graphics, x, y, x + 16, y + 16);
            active.rows.add(new PendingRow(key.getId().toString(), TreeAmountObservation.amount(key.getId().toString()), 0, bounds));
            active.itemCells.add(bounds);
        }
    }

    private static Rect transformed(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        var pose = graphics.pose().last().pose();
        var from = pose.transformPosition(new org.joml.Vector3f(x1, y1, 0));
        var to = pose.transformPosition(new org.joml.Vector3f(x2, y2, 0));
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
            collect(component, bounds, result, true);
        }
        return List.copyOf(result);
    }

    private static void collect(Component component, Rect bounds, List<UiSnapshot.ObservedText> result,
            boolean includeSiblings) {
        result.add(observed(component, bounds));
        var wrapped = !(component.getContents() instanceof TranslatableContents)
                && com.ctux.ae2craftingtime.mc1201.TtcComponents.translation(component) != null;
        var siblings = component.getSiblings();
        if (includeSiblings || wrapped) {
            for (int i = wrapped ? 2 : 0; i < siblings.size(); i++) {
                collect(siblings.get(i), bounds, result, false);
            }
        }
    }

    private static UiSnapshot.ObservedText observed(Component component, Rect bounds) {
        var translated = com.ctux.ae2craftingtime.mc1201.TtcComponents.translation(component);
        if (translated != null) {
            var style = component.getContents() instanceof TranslatableContents
                    ? component.getStyle() : component.getSiblings().get(1).getStyle().applyTo(component.getStyle());
            var arguments = new ArrayList<String>();
            for (var argument : translated.getArgs()) {
                if (argument instanceof Component nested) {
                    var nestedTranslation = com.ctux.ae2craftingtime.mc1201.TtcComponents.translation(nested);
                    arguments.add(nestedTranslation == null ? nested.getString() : nestedTranslation.getKey());
                } else {
                    arguments.add(String.valueOf(argument));
                }
            }
            return new UiSnapshot.ObservedText(translated.getKey(), component.getString(), arguments, bounds,
                    style.getColor() == null ? null : style.getColor().getValue(), style.isBold());
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

    private record PendingRow(String outputId, long craftAmount, long missingAmount, Rect cell, Object identity,
            long storedAmount, long activeAmount, long pendingAmount) {
        private PendingRow(String outputId, long craftAmount, long missingAmount, Rect cell, Object identity) {
            this(outputId, craftAmount, missingAmount, cell, identity, 0, 0, 0);
        }
        private PendingRow(String outputId, long craftAmount, long missingAmount, Rect cell) {
            this(outputId, craftAmount, missingAmount, cell, null);
        }
    }

    private record PendingCpuCard(int serial, String name, String jobId, long amount, long elapsedNanos,
            boolean selected, Rect bounds, Rect nameArea, Rect infoArea, Rect progressArea) {
    }

    private static final class Frame {
        private Map<String, Object> nativeHover = Map.of();
        private final String screen;
        private final String menu;
        private final Rect gui;
        private final int screenWidth;
        private final int screenHeight;
        private final double guiScale;
        private int scroll;
        private final List<PendingRow> rows = new ArrayList<>();
        private final Map<String, List<UiSnapshot.ObservedText>> descriptions = new LinkedHashMap<>();
        private final Map<Object, List<Component>> planDescriptions = new java.util.IdentityHashMap<>();
        private final List<UiSnapshot.ObservedText> text = new ArrayList<>();
        private final List<Rect> badges = new ArrayList<>();
        private final List<UiSnapshot.Widget> widgets = new ArrayList<>();
        private final List<Rect> itemCells = new ArrayList<>();
        private final List<UiSnapshot.ObservedText> tooltip = new ArrayList<>();
        private final List<PendingCpuCard> cpuCards = new ArrayList<>();
        private List<Integer> rawCpuSerials = List.of();

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
