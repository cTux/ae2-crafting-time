package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.AEKey;
import appeng.client.gui.widgets.Scrollbar;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalLong;

@Pseudo
@Mixin(targets = "com.almostreliable.merequester.client.abstraction.AbstractRequesterScreen", remap = false)
public abstract class MERequesterScreenMixin {
    @Unique
    private static final float AE2CRAFTINGTIME_TEXT_SCALE = 0.5f;
    @Unique
    private static final int AE2CRAFTINGTIME_LABEL_BACKGROUND = 0xD0202028;
    @Unique
    private static final int AE2CRAFTINGTIME_LABEL_PADDING_X = 2;
    @Unique
    private static final int AE2CRAFTINGTIME_LABEL_HEIGHT = 7;
    @Unique
    private static final int AE2CRAFTINGTIME_STATUS_X = 47;
    @Unique
    private static final int AE2CRAFTINGTIME_STATUS_WIDTH = 118;

    @Shadow
    @Final
    private static int GUI_HEADER_HEIGHT;

    @Shadow
    @Final
    private static int ROW_HEIGHT;

    @Shadow
    @Final
    protected ArrayList<Object> lines;

    @Shadow
    @Final
    private Scrollbar scrollbar;

    @Shadow
    protected int rowAmount;

    @Inject(method = "drawFG", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$drawRequestTtc(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX,
            int mouseY, CallbackInfo ci) {
        if (lines.isEmpty()) {
            return;
        }

        var estimates = new ArrayList<MERequesterEstimate>();
        var scroll = scrollbar.getCurrentScroll();
        for (var row = 0; row < rowAmount && scroll + row < lines.size(); row++) {
            estimates.add(ae2craftingtime$estimate(lines.get(scroll + row)));
        }

        var knownSeconds = estimates.stream().flatMapToLong(estimate -> estimate.seconds().stream()).toArray();
        var minSeconds = knownSeconds.length == 0 ? 0 : java.util.Arrays.stream(knownSeconds).min().orElseThrow();
        var maxSeconds = knownSeconds.length == 0 ? 0 : java.util.Arrays.stream(knownSeconds).max().orElseThrow();
        for (var row = 0; row < estimates.size(); row++) {
            var estimate = estimates.get(row);
            var color = estimate.seconds().isPresent()
                    ? TtcColor.forSeconds(estimate.seconds().getAsLong(), minSeconds, maxSeconds)
                    : 0xE0E0E0;
            var rowIndex = row;
            estimate.label().ifPresent(label -> ae2craftingtime$drawRowBadge(guiGraphics, rowIndex, label, color));
        }

        TimeEstimate.formatTotal(estimates.stream().map(MERequesterEstimate::seconds).toList())
                .ifPresent(eta -> ae2craftingtime$drawBadge(guiGraphics, 160, 6, TtcText.totalTtc(eta), 0xE0E0E0));
    }

    @Unique
    private static void ae2craftingtime$drawRowBadge(GuiGraphics guiGraphics, int row, Component label, int color) {
        var x = AE2CRAFTINGTIME_STATUS_X + AE2CRAFTINGTIME_STATUS_WIDTH / 2;
        var y = GUI_HEADER_HEIGHT + row * ROW_HEIGHT + 12;
        ae2craftingtime$drawBadge(guiGraphics, x, y, label, color);
    }

    @Unique
    private static void ae2craftingtime$drawBadge(GuiGraphics guiGraphics, int centerX, int top, Component label,
            int color) {
        var font = Minecraft.getInstance().font;
        var scaledTextWidth = font.width(label) * AE2CRAFTINGTIME_TEXT_SCALE;
        var labelWidth = (int) Math.ceil(scaledTextWidth) + AE2CRAFTINGTIME_LABEL_PADDING_X * 2;
        var labelLeft = centerX - labelWidth / 2.0f;
        var textX = (int) ((centerX - scaledTextWidth / 2) / AE2CRAFTINGTIME_TEXT_SCALE);
        var textY = (int) ((top + 1) / AE2CRAFTINGTIME_TEXT_SCALE);
        var pose = guiGraphics.pose();

        pose.pushPose();
        pose.scale(AE2CRAFTINGTIME_TEXT_SCALE, AE2CRAFTINGTIME_TEXT_SCALE, AE2CRAFTINGTIME_TEXT_SCALE);
        guiGraphics.fill(
                (int) Math.floor(labelLeft / AE2CRAFTINGTIME_TEXT_SCALE),
                (int) Math.floor((double) top / AE2CRAFTINGTIME_TEXT_SCALE),
                (int) Math.ceil((labelLeft + labelWidth) / AE2CRAFTINGTIME_TEXT_SCALE),
                (int) Math.ceil((double) (top + AE2CRAFTINGTIME_LABEL_HEIGHT) / AE2CRAFTINGTIME_TEXT_SCALE),
                AE2CRAFTINGTIME_LABEL_BACKGROUND);
        guiGraphics.drawString(font, label, textX, textY, color, true);
        pose.popPose();
    }

    @Unique
    private static MERequesterEstimate ae2craftingtime$estimate(Object request) {
        var key = ae2craftingtime$getKey(request);
        var amount = ae2craftingtime$getLong(request, "getAmount");
        if (key.isEmpty() || amount <= 0) {
            return MERequesterEstimate.empty();
        }

        var profileKey = ProfilerBridge.key(key.get());
        var stats = ClientStats.CACHE.get(profileKey);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(profileKey);
            return new MERequesterEstimate(Optional.of(TtcText.noStats()), OptionalLong.empty());
        }

        var normalized = AeKeyAmounts.normalize(key.get(), amount);
        var seconds = TimeEstimate.seconds(normalized, stats.get());
        var label = TimeEstimate.format(normalized, stats.get()).map(eta -> (Component) TtcText.requesterTtc(eta));
        return new MERequesterEstimate(label, seconds);
    }

    @Unique
    private static Optional<AEKey> ae2craftingtime$getKey(Object request) {
        var key = ae2craftingtime$invoke(request, "getKey");
        return key instanceof AEKey aeKey ? Optional.of(aeKey) : Optional.empty();
    }

    @Unique
    private static long ae2craftingtime$getLong(Object request, String methodName) {
        var value = ae2craftingtime$invoke(request, methodName);
        return value instanceof Number number ? number.longValue() : 0;
    }

    @Unique
    private static Object ae2craftingtime$invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Unique
    private record MERequesterEstimate(Optional<Component> label, OptionalLong seconds) {
        private static MERequesterEstimate empty() {
            return new MERequesterEstimate(Optional.empty(), OptionalLong.empty());
        }
    }
}
