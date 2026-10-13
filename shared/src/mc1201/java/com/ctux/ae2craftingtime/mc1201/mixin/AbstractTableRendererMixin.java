package com.ctux.ae2craftingtime.mc1201.mixin;

import com.ctux.ae2craftingtime.core.TtcEstimate;
import appeng.client.gui.me.crafting.AbstractTableRenderer;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.me.crafting.CraftConfirmTableRenderer;
import appeng.client.gui.me.crafting.CraftingStatusTableRenderer;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.core.CraftingRowState;
import com.ctux.ae2craftingtime.core.NativeTableLayout;
import com.ctux.ae2craftingtime.core.PresentationHooks;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcComponents;
import com.ctux.ae2craftingtime.mc1201.TtcColorContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(AbstractTableRenderer.class)
public abstract class AbstractTableRendererMixin {
    @Shadow(remap = false) @Final private AEBaseScreen<?> screen;
    @Shadow(remap = false) @Final private int rows;
    @Shadow(remap = false) @Final private float lineHeight;
    @Shadow(remap = false) protected abstract List<Component> getEntryDescription(Object entry);
    @Shadow(remap = false) protected abstract List<Component> getEntryTooltip(Object entry);

    @Unique
    private NativeTableLayout ae2craftingtime$layout() {
        boolean status = screen instanceof CraftingCPUScreen<?>;
        boolean enabled = ClientOptionsRuntime.enabled(status ? OptionFeature.STATUS_ROWS : OptionFeature.PLAN_ROWS);
        return NativeTableLayout.of((int) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale(),
                3, 67, 22, rows, enabled && PresentationHooks.table(status));
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 67), require = 0, remap = false)
    private int ae2craftingtime$cellWidth(int nativeWidth) { return ae2craftingtime$layout().cellWidth(); }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 22), require = 0, remap = false)
    private int ae2craftingtime$cellHeight(int nativeHeight) { return ae2craftingtime$layout().cellHeight(); }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 22f), require = 0, remap = false)
    private float ae2craftingtime$centeringHeight(float nativeHeight) { return ae2craftingtime$layout().cellHeight(); }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 68), require = 0, remap = false)
    private int ae2craftingtime$columnPitch(int nativePitch) { return ae2craftingtime$layout().cellWidth() + 1; }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 23), require = 0, remap = false)
    private int ae2craftingtime$rowPitch(int nativePitch) { return ae2craftingtime$layout().cellHeight() + 1; }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 3, ordinal = 0), require = 0, remap = false)
    private int ae2craftingtime$columnCount(int nativeCount) { return ae2craftingtime$layout().columns(); }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 3, ordinal = 1), require = 0, remap = false)
    private int ae2craftingtime$indexColumns(int nativeCount) { return ae2craftingtime$layout().columns(); }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 0.5f), require = 0, remap = false)
    private float ae2craftingtime$textScale(float nativeScale) { return ae2craftingtime$layout().textScale(); }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 2f, ordinal = 1), require = 0, remap = false)
    private float ae2craftingtime$inverseX(float nativeScale) { return 1f / ae2craftingtime$layout().textScale(); }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 2f, ordinal = 2), require = 0, remap = false)
    private float ae2craftingtime$inverseY(float nativeScale) { return 1f / ae2craftingtime$layout().textScale(); }

    @Redirect(method = "render", at = @At(value = "FIELD",
            target = "Lappeng/client/gui/me/crafting/AbstractTableRenderer;rows:I", opcode = Opcodes.GETFIELD),
            require = 0, remap = false)
    private int ae2craftingtime$visibleRows(AbstractTableRenderer<?> renderer) {
        return ae2craftingtime$layout().rows();
    }

    @Redirect(method = "render", at = @At(value = "FIELD",
            target = "Lappeng/client/gui/me/crafting/AbstractTableRenderer;lineHeight:F", opcode = Opcodes.GETFIELD),
            require = 0, remap = false)
    private float ae2craftingtime$lineHeight(AbstractTableRenderer<?> renderer) {
        return lineHeight * (ae2craftingtime$layout().textScale() / 0.5f);
    }

    @Inject(method = "getScrollableRows(I)I", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void ae2craftingtime$scrollRows(int entries, CallbackInfoReturnable<Integer> cir) {
        if (ae2craftingtime$layout().textScale() == 1f) {
            cir.setReturnValue(ae2craftingtime$layout().scrollableRows(entries));
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/me/crafting/AbstractTableRenderer;getEntryTooltip(Ljava/lang/Object;)Ljava/util/List;"),
            require = 0, remap = false)
    private List<Component> ae2craftingtime$fullRowTooltip(AbstractTableRenderer<?> renderer, Object entry) {
        var nativeLines = getEntryTooltip(entry);
        if (ae2craftingtime$layout().textScale() != 1f) return nativeLines;
        int available = ae2craftingtime$layout().cellWidth() - 23;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        var fullLines = getEntryDescription(entry).stream().filter(line -> font.width(line) > available).toList();
        if (fullLines.isEmpty()) return nativeLines;
        var result = new java.util.ArrayList<>(nativeLines);
        result.addAll(fullLines);
        return result;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
                    remap = true),
            require = 0, remap = false)
    @SuppressWarnings("mapping")
    private int ae2craftingtime$drawTextWithShadow(GuiGraphics guiGraphics, Font font, Component text,
            int x, int y, int color, boolean shadow) {
        var translatable = TtcComponents.translation(text);
        if (translatable != null) {
            var isAe2CraftingTime = translatable.getKey().startsWith("text.ae2craftingtime.");
            if (isAe2CraftingTime && ae2craftingtime$isTtcLine(translatable))
                text = TtcComponents.rowSymbolsInTextColor(text, color);
            if (ae2craftingtime$layout().textScale() == 1f) {
                int widthBefore = font.width(text);
                text = TtcComponents.fitWithEllipsis(text, font, ae2craftingtime$layout().cellWidth() - 23);
                x += widthBefore - font.width(text);
            }
            if (isAe2CraftingTime && ae2craftingtime$isTtcLine(translatable)) {
                var width = font.width(text);
                var scale = ae2craftingtime$layout().textScale() != 1f && CraftingRowState.isWidthLimited(translatable.getKey())
                        ? CraftingRowState.badgeTextScale(width) : 1f;
                if (scale < 1f) {
                    var left = x + width - CraftingRowState.BADGE_TEXT_WIDTH;
                    TtcBadge.fillRoundedRect(guiGraphics, left - 2, y - 2, x + width + 2,
                            y + font.lineHeight + 2, TtcBadge.BACKGROUND);
                    var pose = guiGraphics.pose();
                    pose.pushPose();
                    pose.translate(left, y, 0);
                    pose.scale(scale, 1, 1);
                    guiGraphics.drawString(font, text, 0, 0, color, ClientOptionsRuntime.textShadow());
                    pose.popPose();
                    return x + width;
                }
                int padding = ae2craftingtime$layout().badgeVerticalPadding();
                TtcBadge.fillRoundedRect(guiGraphics, x - 2, y - padding, x + width + 2, y + font.lineHeight + padding,
                        TtcBadge.BACKGROUND);
            }
            return guiGraphics.drawString(font, text, x, y, color,
                    ClientOptionsRuntime.current().textShadow(isAe2CraftingTime, shadow));
        }
        if (ae2craftingtime$layout().textScale() == 1f) {
            int widthBefore = font.width(text);
            text = TtcComponents.fitWithEllipsis(text, font, ae2craftingtime$layout().cellWidth() - 23);
            x += widthBefore - font.width(text);
        }
        return guiGraphics.drawString(font, text, x, y, color, shadow);
    }

    @Unique
    private static boolean ae2craftingtime$isTtcLine(TranslatableContents translatable) {
        return CraftingRowState.isBadge(translatable.getKey());
    }

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beginTtcColors(GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<?> entries, int scrollOffset, CallbackInfo ci) {
        if (!ae2craftingtime$hasTtc((Object) this)) {
            return;
        }

        TtcColorContext.set(colors(entries));
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$endTtcColors(GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<?> entries, int scrollOffset, CallbackInfo ci) {
        if (ae2craftingtime$hasTtc((Object) this)) {
            TtcColorContext.clear();
        }
    }

    private static boolean ae2craftingtime$hasTtc(Object renderer) {
        return renderer instanceof CraftConfirmTableRenderer || renderer instanceof CraftingStatusTableRenderer;
    }

    private static Map<ProfileKey, Integer> colors(List<?> entries) {
        var secondsByKey = new HashMap<ProfileKey, Double>();
        var min = Double.POSITIVE_INFINITY;
        var max = Double.NEGATIVE_INFINITY;

        for (var entry : entries) {
            var seconds = seconds(entry);
            if (seconds.isEmpty()) {
                continue;
            }

            var key = key(entry);
            var value = seconds.get().durationMs();
            secondsByKey.put(key, value);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        var colors = new HashMap<ProfileKey, Integer>();
        for (var entry : secondsByKey.entrySet()) {
            colors.put(entry.getKey(), com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime.ttcColor(entry.getValue(), min, max));
        }
        return colors;
    }

    private static Optional<TtcEstimate> seconds(Object entry) {
        if (entry instanceof CraftingPlanSummaryEntry planEntry) {
            if (planEntry.getCraftAmount() <= 0) {
                return Optional.empty();
            }

            var key = ProfilerBridge.key(planEntry.getWhat());
            var stats = ClientStats.CACHE.get(key);
            if (stats.isEmpty()) {
                return Optional.empty();
            }

            return TimeEstimate.milliseconds(AeKeyAmounts.normalize(planEntry.getWhat(), planEntry.getCraftAmount()),
                    stats.get());
        }

        if (!(entry instanceof CraftingStatusEntry statusEntry)) {
            return Optional.empty();
        }

        var amount = statusEntry.getActiveAmount() + statusEntry.getPendingAmount();
        if (amount <= 0) {
            return Optional.empty();
        }

        var key = ProfilerBridge.key(statusEntry.getWhat());
        if (CraftingRowState.blockReason(statusEntry.getPendingAmount(), ClientStats.blockReason(key)) != null) {
            return Optional.empty();
        }
        if (statusEntry.getActiveAmount() == 0 && statusEntry.getPendingAmount() > 0
                && ClientStats.CACHE.waitingTicks(key).isPresent()) {
            return Optional.empty();
        }
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            return Optional.empty();
        }

        return TimeEstimate.milliseconds(AeKeyAmounts.normalize(statusEntry.getWhat(), amount),
                stats.get());
    }

    private static ProfileKey key(Object entry) {
        if (entry instanceof CraftingPlanSummaryEntry planEntry) {
            return ProfilerBridge.key(planEntry.getWhat());
        }
        return ProfilerBridge.key(((CraftingStatusEntry) entry).getWhat());
    }
}
