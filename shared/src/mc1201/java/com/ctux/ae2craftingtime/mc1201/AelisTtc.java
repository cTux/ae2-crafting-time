package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import com.ctux.ae2craftingtime.core.AelisPlanTtc;
import com.ctux.ae2craftingtime.core.IntegrationRead;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Only the contract-selected optional client hooks call this adapter. */
public final class AelisTtc {
    public static boolean enabled() {
        return AelisPlanTtc.ready() && IntegrationLog.available("appliedenhancements")
                && (ClientOptionsRuntime.enabled(OptionFeature.PLAN_ROWS)
                    || ClientOptionsRuntime.enabled(OptionFeature.PLAN_TOTAL));
    }

    public static boolean columnsEnabled() {
        return enabled() && ClientOptionsRuntime.enabled(OptionFeature.PLAN_ROWS);
    }

    private static BigInteger amount(Object row, String field) {
        return IntegrationRead.invoke(row, field, BigInteger.class);
    }

    private static Optional<TtcEstimate> estimate(Object row, boolean visible) {
        var key = IntegrationRead.invoke(row, "key", AEKey.class);
        var profile = ProfilerBridge.key(key);
        var quantity = AelisPlanTtc.normalized(profile.outputId(), key.getAmountPerUnit(), amount(row, "craft"));
        if (quantity.isEmpty()) return Optional.empty();
        var stats = ClientStats.CACHE.get(profile);
        if (stats.isEmpty()) {
            if (visible) ClientStatsRequests.request(profile); else ClientStatsRequests.requestBackground(profile);
        }
        return stats.flatMap(value -> TimeEstimate.milliseconds(quantity.getAsLong(), value));
    }

    private static Component label(Object row) {
        if (amount(row, "craft").signum() <= 0) return Component.literal("—");
        var key = IntegrationRead.invoke(row, "key", AEKey.class);
        if (AelisPlanTtc.normalized(key.getId().toString(), key.getAmountPerUnit(), amount(row, "craft")).isEmpty())
            return TtcText.ttc(Component.translatable("text.ae2craftingtime.unknown").getString());
        return estimate(row, true).<Component>map(eta -> RowTextColor.estimate(eta,
                TtcColorContext.get(ProfilerBridge.key(key)), ClientOptionsRuntime.current()))
                .orElseGet(() -> ClientOptionsRuntime.enabled(OptionFeature.COLLECTING_STATUS)
                        ? RowTextColor.neutral(TtcText.ttcCollectingData(), ClientOptionsRuntime.current()) : Component.empty());
    }

    private static boolean list(Object screen) {
        return IntegrationRead.field(screen, "view", Object.class).toString().equals("LIST");
    }

    public static void draw(Object screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!enabled()) return;
        try {
            var nativeScreen = (CraftConfirmScreen) screen;
            if (nativeScreen.getMenu().getPlan() == null || !list(screen)) return;
            var layout = IntegrationRead.field(screen, "layout", Object.class);
            int width = IntegrationRead.invoke(layout, "width", Integer.class);
            var rows = IntegrationRead.field(screen, "rows", List.class);
            var visible = IntegrationRead.field(screen, "visible", List.class);
            int scroll = IntegrationRead.field(screen, "scroll", Integer.class);
            int count = IntegrationRead.invoke(layout, "visibleRows", Integer.class);
            int right = width - 24;
            int left = right - AelisPlanTtc.COLUMN_WIDTH + 3;
            var font = Minecraft.getInstance().font;
            var estimates = new ArrayList<Optional<TtcEstimate>>();
            var colors = new HashMap<com.ctux.ae2craftingtime.core.ProfileKey, TtcEstimate>();
            boolean incomplete = false;
            for (var row : rows) {
                incomplete |= amount(row, "missing").signum() > 0 || amount(row, "cyclic").signum() > 0;
                if (amount(row, "craft").signum() <= 0) continue;
                var eta = estimate(row, false);
                estimates.add(eta);
                eta.ifPresent(value -> colors.put(ProfilerBridge.key(IntegrationRead.invoke(row, "key", AEKey.class)), value));
            }
            double min = colors.values().stream().mapToDouble(TtcEstimate::durationMs).min().orElse(0);
            double max = colors.values().stream().mapToDouble(TtcEstimate::durationMs).max().orElse(0);
            var palette = new HashMap<com.ctux.ae2craftingtime.core.ProfileKey, Integer>();
            colors.forEach((key, value) -> palette.put(key, ClientOptionsRuntime.ttcColor(value.durationMs(), min, max)));
            TtcColorContext.set(palette);
            if (ClientOptionsRuntime.enabled(OptionFeature.PLAN_ROWS) && AelisPlanTtc.reservation(width) > 0) {
                drawCell(graphics, Component.translatable("text.ae2craftingtime.aelis.ttc_column"), left, 98);
                for (int i = 0; i < count && scroll + i < visible.size(); i++) {
                    int y = 108 + i * 22;
                    var row = visible.get(scroll + i);
                    boolean missing = amount(row, "missing").signum() > 0;
                    boolean hover = AelisPlanTtc.hover(mouseX, mouseY, right, y);
                    graphics.fill(left - 2, y, right + 3, y + 22,
                            AelisPlanTtc.rowBackground(missing, hover, i));
                    drawCell(graphics, label(row), left, y + (22 - font.lineHeight) / 2);
                }
                IntegrationLog.observe("appliedenhancements", "row");
            }
            if (ClientOptionsRuntime.enabled(OptionFeature.PLAN_TOTAL)) {
                incomplete |= estimates.stream().anyMatch(Optional::isEmpty);
                var total = AelisPlanTtc.total(estimates, incomplete);
                Component text = total.<Component>map(eta -> TtcText.totalTtc(eta))
                        .orElseGet(TtcText::ttcCollectingData);
                if (incomplete && total.isPresent()) text = Component.translatable(
                        "text.ae2craftingtime.aelis.partial_total", TtcComponents.estimate(total.get()));
                int color = RowTextColor.planTotalColor(ClientOptionsRuntime.current());
                text = TtcComponents.rowSymbolsInTextColor(text.copy().withStyle(style -> style.withColor(color)), color);
                if (!estimates.isEmpty()) drawText(graphics, text, 18, 63, width - 36);
                IntegrationLog.observe("appliedenhancements", "total");
            }
        } catch (IntegrationRead.Failure failure) {
            IntegrationLog.disable("appliedenhancements", failure);
        }
    }

    private static void drawCell(GuiGraphics graphics, Component text, int left, int y) {
        drawText(graphics, text, AelisPlanTtc.textLeft(left, 64, Minecraft.getInstance().font.width(text)), y, 64);
    }

    private static void drawText(GuiGraphics graphics, Component text, int x, int y, int width) {
        if (text.getString().isEmpty()) return;
        var font = Minecraft.getInstance().font;
        TtcBadge.fillRoundedRect(graphics, x - 1, y - 1, x + Math.min(width, font.width(text)) + 1,
                y + font.lineHeight + 1, TtcBadge.BACKGROUND);
        var shown = font.width(text) <= width ? text : Component.literal(
                font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("…"))) + "…")
                .withStyle(text.getStyle());
        graphics.drawString(font, shown, x, y,
                RowTextColor.planTotalColor(ClientOptionsRuntime.current()), ClientOptionsRuntime.textShadow());
    }

    public static List<Component> tooltip(Object screen, List<Component> original, int x, int y) {
        if (!enabled() || !ClientOptionsRuntime.enabled(OptionFeature.PLAN_ROWS)) return original;
        try {
            if (!list(screen)) return original;
            var row = IntegrationRead.invoke(screen, "rowUnderMouse", Object.class, (double) x, (double) y);
            if (row == null || amount(row, "craft").signum() <= 0) return original;
            var lines = new ArrayList<>(original);
            lines.add(label(row));
            IntegrationLog.observe("appliedenhancements", "tooltip");
            return lines;
        } catch (IntegrationRead.Failure failure) {
            IntegrationLog.disable("appliedenhancements", failure);
            return original;
        }
    }

    private AelisTtc() {}
}
