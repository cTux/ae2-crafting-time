package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.AEKey;
import appeng.client.gui.widgets.Scrollbar;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
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
    private static final int AE2CRAFTINGTIME_GUI_WIDTH = 195;
    @Unique
    private static final int AE2CRAFTINGTIME_GUI_HEADER_HEIGHT = 20;
    @Unique
    private static final int AE2CRAFTINGTIME_ROW_HEIGHT = 19;
    @Unique
    private static final int AE2CRAFTINGTIME_TTC_X = AE2CRAFTINGTIME_GUI_WIDTH + 4;

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

        var estimates = new ArrayList<OptionalLong>();
        var scroll = scrollbar.getCurrentScroll();
        for (var row = 0; row < rowAmount && scroll + row < lines.size(); row++) {
            var estimate = ae2craftingtime$estimate(lines.get(scroll + row));
            var rowIndex = row;
            estimates.add(estimate.seconds());
            estimate.label().ifPresent(label -> ae2craftingtime$drawLabel(guiGraphics, rowIndex, label));
        }

        TimeEstimate.formatTotal(estimates).ifPresent(eta -> {
            var font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, Component.literal("Total TTC: " + eta), 8,
                    AE2CRAFTINGTIME_GUI_HEADER_HEIGHT + rowAmount * AE2CRAFTINGTIME_ROW_HEIGHT + 8, 0x404040, false);
        });
    }

    @Unique
    private static void ae2craftingtime$drawLabel(GuiGraphics guiGraphics, int row, String label) {
        var font = Minecraft.getInstance().font;
        var y = AE2CRAFTINGTIME_GUI_HEADER_HEIGHT + row * AE2CRAFTINGTIME_ROW_HEIGHT + 5;
        var width = font.width(label);
        guiGraphics.fill(AE2CRAFTINGTIME_TTC_X - 2, y - 1, AE2CRAFTINGTIME_TTC_X + width + 2, y + 9,
                0xA0000000);
        guiGraphics.drawString(font, label, AE2CRAFTINGTIME_TTC_X, y, 0xE0E0E0, false);
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
            return new MERequesterEstimate(Optional.of("No stats"), OptionalLong.empty());
        }

        var normalized = AeKeyAmounts.normalize(key.get(), amount);
        var seconds = TimeEstimate.seconds(normalized, stats.get());
        var label = TimeEstimate.format(normalized, stats.get()).map(eta -> "TTC " + eta);
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
    private record MERequesterEstimate(Optional<String> label, OptionalLong seconds) {
        private static MERequesterEstimate empty() {
            return new MERequesterEstimate(Optional.empty(), OptionalLong.empty());
        }
    }
}
