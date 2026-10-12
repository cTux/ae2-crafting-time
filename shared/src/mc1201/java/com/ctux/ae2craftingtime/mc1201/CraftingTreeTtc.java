package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.GenericStack;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Point;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class CraftingTreeTtc {
    public static final float TEXT_SCALE = 0.5f;
    public static final int LABEL_PADDING = 2;
    public static final int LABEL_TOP_OFFSET = 19;

    private CraftingTreeTtc() {
    }

    public static void drawBadge(GuiGraphics guiGraphics, int outputX, int outputY, int spacingX, int spacingY,
            Point point, TtcEstimate estimate, int color) {
        if (estimate.durationMs() <= 0) {
            return;
        }

        var font = Minecraft.getInstance().font;
        var display = TtcComponents.estimate(estimate);
        var scaledTextWidth = font.width(display) * TEXT_SCALE;
        var labelWidth = (int) Math.ceil(scaledTextWidth) + LABEL_PADDING * 2;
        var labelHeight = (int) Math.ceil(font.lineHeight * TEXT_SCALE) + LABEL_PADDING * 2;
        var x = point.x * spacingX + outputX;
        var y = point.y * spacingY + outputY;
        var labelLeft = x + 8 - labelWidth / 2.0f;
        var labelTop = y + LABEL_TOP_OFFSET;
        var textX = (int) ((x + 8 - scaledTextWidth / 2) / TEXT_SCALE);
        var textY = (int) ((labelTop + LABEL_PADDING) / TEXT_SCALE);

        TtcBadge.fillRoundedRect(guiGraphics, (int) Math.floor(labelLeft), (int) Math.floor(labelTop),
                (int) Math.ceil(labelLeft + labelWidth), (int) Math.ceil(labelTop + labelHeight),
                TtcBadge.BACKGROUND);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        guiGraphics.drawString(font, display, textX, textY, color, ClientOptionsRuntime.textShadow());
        pose.popPose();
    }

    public static IdentityHashMap<Object, TtcEstimate> computeEstimates(List<Object> roots,
            Function<Object, GenericStack> stackOf,
            Function<Object, Long> craftAmountOf,
            Function<Object, List<Object>> childrenOf) {
        var seconds = new IdentityHashMap<Object, TtcEstimate>();
        if (roots != null) {
            for (var root : roots) {
                addSeconds(root, seconds, stackOf, craftAmountOf, childrenOf);
            }
        }
        return seconds;
    }

    private static void addSeconds(Object data, IdentityHashMap<Object, TtcEstimate> cache,
            Function<Object, GenericStack> stackOf,
            Function<Object, Long> craftAmountOf,
            Function<Object, List<Object>> childrenOf) {
        if (data == null || cache.containsKey(data)) {
            return;
        }

        var estimates = new ArrayList<Optional<TtcEstimate>>();
        var stack = stackOf.apply(data);
        var craftAmount = craftAmountOf.apply(data);
        if (stack != null && craftAmount != null && craftAmount > 0) {
            var key = new ProfileKey(stack.what().getId().toString());
            var stats = ClientStats.CACHE.get(key);
            if (stats.isEmpty()) {
                ClientStatsRequests.request(key);
                estimates.add(Optional.empty());
            } else {
                estimates.add(TimeEstimate.milliseconds(AeKeyAmounts.normalize(stack.what(), craftAmount), stats.get()));
            }
        }

        for (var child : childrenOf.apply(data)) {
            addSeconds(child, cache, stackOf, craftAmountOf, childrenOf);
            var childSeconds = cache.get(child);
            if (childSeconds != null) {
                estimates.add(Optional.of(childSeconds));
            }
        }

        cache.put(data, TimeEstimate.total(estimates).orElse(new TtcEstimate(0, estimates.isEmpty())));
    }

    public static IdentityHashMap<Object, Integer> computeColors(IdentityHashMap<Object, TtcEstimate> seconds) {
        var colors = new IdentityHashMap<Object, Integer>();
        if (seconds == null || seconds.isEmpty()) {
            return colors;
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (var value : seconds.values()) {
            if (value.durationMs() < min) {
                min = value.durationMs();
            }
            if (value.durationMs() > max) {
                max = value.durationMs();
            }
        }

        for (var entry : seconds.entrySet()) {
            colors.put(entry.getKey(), ClientOptionsRuntime.ttcColor(entry.getValue().durationMs(), min, max));
        }
        return colors;
    }

}
