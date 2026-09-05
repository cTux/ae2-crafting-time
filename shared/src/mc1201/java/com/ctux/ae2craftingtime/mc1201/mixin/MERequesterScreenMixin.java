package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.AEKey;
import appeng.client.gui.widgets.Scrollbar;
import com.ctux.ae2craftingtime.core.RequesterTtcLayout;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ctux.ae2craftingtime.core.IntegrationRead;
import com.ctux.ae2craftingtime.mc1201.IntegrationLog;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

@Pseudo
@Mixin(targets = "com.almostreliable.merequester.client.abstraction.AbstractRequesterScreen", remap = false)
public abstract class MERequesterScreenMixin {
    @Unique
    private static final float AE2CRAFTINGTIME_TEXT_SCALE = 0.5f;
    @Unique
    private static final int AE2CRAFTINGTIME_LABEL_PADDING = 2;
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

    @Inject(method = "addSubWidget", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$reserveTtcSpace(String id, AbstractWidget widget,
            Map<String, AbstractWidget> subWidgets, CallbackInfo ci) {
        var offset = RequesterTtcLayout.statusOffset(id);
        widget.setX(widget.getX() + offset);
        widget.setWidth(widget.getWidth() - offset);
    }

    @Inject(method = "drawFG", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$drawRequestTtc(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX,
            int mouseY, CallbackInfo ci) {
        if (!IntegrationLog.available("merequester") || lines.isEmpty()) {
            return;
        }

        var estimates = new ArrayList<MERequesterEstimate>();
        var scroll = scrollbar.getCurrentScroll();
        try {
            for (var row = 0; row < rowAmount && scroll + row < lines.size(); row++) {
                estimates.add(ae2craftingtime$estimate(lines.get(scroll + row)));
            }
        } catch (IntegrationRead.Failure failure) {
            IntegrationLog.disable("merequester", failure);
            return;
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
            estimate.label().ifPresent(label -> {
                ae2craftingtime$drawRowBadge(guiGraphics, rowIndex, label, color);
                IntegrationLog.observe("merequester", "row");
            });
        }

        TimeEstimate.formatTotal(estimates.stream().map(MERequesterEstimate::seconds).toList())
                .ifPresent(eta -> {
                    ae2craftingtime$drawBadge(guiGraphics, 160, 6, TtcText.totalTtc(eta), 0xE0E0E0,
                            0.5f, AE2CRAFTINGTIME_TEXT_SCALE, AE2CRAFTINGTIME_LABEL_PADDING);
                    IntegrationLog.observe("merequester", "total");
                });
    }

    @Unique
    private static void ae2craftingtime$drawRowBadge(GuiGraphics guiGraphics, int row, Component label, int color) {
        var font = Minecraft.getInstance().font;
        var y = RequesterTtcLayout.rowTop(GUI_HEADER_HEIGHT, ROW_HEIGHT, row);
        ae2craftingtime$drawBadge(guiGraphics, RequesterTtcLayout.BADGE_X, y, label, color, 0.0f,
                RequesterTtcLayout.rowScale(font.width(label), font.lineHeight), 1);
    }

    @Unique
    private static void ae2craftingtime$drawBadge(GuiGraphics guiGraphics, int anchorX, int top, Component label,
            int color, float horizontalAlignment, float scale, int verticalPadding) {
        var font = Minecraft.getInstance().font;
        var scaledTextWidth = font.width(label) * scale;
        var labelWidth = (int) Math.ceil(scaledTextWidth) + AE2CRAFTINGTIME_LABEL_PADDING * 2;
        var labelHeight = (int) Math.ceil(font.lineHeight * scale) + verticalPadding * 2;
        var labelLeft = anchorX - labelWidth * horizontalAlignment;
        var textX = (int) ((labelLeft + AE2CRAFTINGTIME_LABEL_PADDING) / scale);
        var textY = (int) ((top + verticalPadding) / scale);
        var pose = guiGraphics.pose();

        TtcBadge.fillRoundedRect(guiGraphics, (int) Math.floor(labelLeft), top,
                (int) Math.ceil(labelLeft + labelWidth), top + labelHeight, TtcBadge.BACKGROUND);
        pose.pushPose();
        pose.scale(scale, scale, scale);
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
        IntegrationLog.observe("merequester", "request-read");

        var profileKey = ProfilerBridge.key(key.get());
        ClientStatsRequests.request(profileKey);
        var networkAmount = ClientStats.networkAmount(profileKey);
        if (networkAmount.isEmpty() || amount <= networkAmount.getAsLong()) {
            return MERequesterEstimate.empty();
        }
        var stats = ClientStats.CACHE.get(profileKey);
        if (stats.isEmpty()) {
            return new MERequesterEstimate(Optional.of(TtcText.noStats()), OptionalLong.empty());
        }

        var normalized = AeKeyAmounts.normalize(key.get(), amount - networkAmount.getAsLong());
        var seconds = TimeEstimate.seconds(normalized, stats.get());
        var label = TimeEstimate.format(normalized, stats.get()).map(eta -> (Component) TtcText.ttc(eta));
        return new MERequesterEstimate(label, seconds);
    }

    @Unique
    private static Optional<AEKey> ae2craftingtime$getKey(Object request) {
        return Optional.ofNullable(IntegrationRead.invoke(request, "getKey", AEKey.class));
    }

    @Unique
    private static long ae2craftingtime$getLong(Object request, String methodName) {
        var value = IntegrationRead.invoke(request, methodName, Number.class);
        return value == null ? 0 : value.longValue();
    }

    @Unique
    private record MERequesterEstimate(Optional<Component> label, OptionalLong seconds) {
        private static MERequesterEstimate empty() {
            return new MERequesterEstimate(Optional.empty(), OptionalLong.empty());
        }
    }
}
