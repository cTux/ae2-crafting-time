package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Point;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(targets = "com.neuvillette.ae2ct.gui.CraftingTreeWidget", remap = false)
@Pseudo
public abstract class CraftingTreeWidgetMixin {
    @Shadow
    private int outputX;
    @Shadow
    private int outputY;
    @Shadow
    private int spacingX;
    @Shadow
    private int spacingY;

    @Shadow
    protected abstract Point getMousePoint(double mouseX, double mouseY);

    @Inject(
            method = "drawNode(Lnet/minecraft/client/gui/GuiGraphics;Lcom/neuvillette/ae2ct/api/CraftingTreeHelper$Node;)V",
            at = @At("RETURN"),
            require = 0)
    private void ae2craftingtime$drawStats(GuiGraphics guiGraphics, @Coerce Object node, CallbackInfo ci) {
        if (!Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        var stack = readField(node, "stack", GenericStack.class);
        var point = readField(node, "point", Point.class);
        if (stack == null || point == null) {
            return;
        }

        var key = new ProfileKey(stack.what().getId().toString());
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            return;
        }

        var x = point.x * spacingX + outputX;
        var y = point.y * spacingY + outputY;
        var text = format(stats.get());
        var pose = guiGraphics.pose();
        var font = Minecraft.getInstance().font;
        var color = FastColor.ARGB32.color(255, 30, 90, 30);

        pose.pushPose();
        pose.scale(0.45f, 0.45f, 0.45f);
        guiGraphics.drawString(font, text, (int) ((x + 18) / 0.45f), (int) ((y + 23) / 0.45f), color, false);
        pose.popPose();
    }

    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/client/gui/AEBaseScreen;drawTooltipWithHeader(Lnet/minecraft/client/gui/GuiGraphics;IILjava/util/List;)V"),
            require = 0)
    private void ae2craftingtime$appendTooltipStats(AEBaseScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<Component> lines) {
        if (Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            var node = hoveredNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
            var stack = node == null ? null : readField(node, "stack", GenericStack.class);
            if (stack != null) {
                var key = new ProfileKey(stack.what().getId().toString());
                ClientStats.CACHE.get(key).ifPresentOrElse(
                        stats -> addTooltipLines(lines, stats),
                        () -> ClientStatsRequests.request(key));
            }
        }

        screen.drawTooltipWithHeader(guiGraphics, mouseX, mouseY, lines);
    }

    private static String format(ProfileStats stats) {
        var duration = Math.round(stats.averageDurationTicks());
        var rate = stats.amountPerSecond();
        if (stats.unit() == ProfileUnit.MILLIBUCKET) {
            return "avg " + duration + "t | " + compact(rate) + " mB/s";
        }
        return "avg " + duration + "t | " + compact(rate) + "/s";
    }

    private static String compact(double value) {
        if (value >= 100) {
            return Long.toString(Math.round(value));
        }
        if (value >= 10) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private Object hoveredNode(double mouseX, double mouseY) {
        var manager = readField(this, "_nodeManager", Object.class);
        if (manager == null) {
            return null;
        }
        var map = readField(manager, "map", Map.class);
        if (map == null) {
            return null;
        }
        return map.get(getMousePoint(mouseX, mouseY));
    }

    private static void addTooltipLines(List<Component> lines, ProfileStats stats) {
        lines.add(Component.literal("Avg: " + Math.round(stats.averageDurationTicks()) + " ticks / "
                + compact(stats.averageDurationTicks() / 20.0) + " s"));
        lines.add(Component.literal("Throughput: " + compact(stats.amountPerSecond()) + unitSuffix(stats)));
        lines.add(Component.literal("Samples: " + stats.sampleCount()));
        lines.add(Component.literal("Last: " + stats.lastDurationTicks() + " ticks"));
    }

    private static String unitSuffix(ProfileStats stats) {
        return stats.unit() == ProfileUnit.MILLIBUCKET ? " mB/s" : " items/s";
    }

    private static <T> T readField(Object instance, String name, Class<T> type) {
        try {
            Field field;
            try {
                field = instance.getClass().getField(name);
            } catch (NoSuchFieldException ignored) {
                field = instance.getClass().getDeclaredField(name);
                field.setAccessible(true);
            }
            return type.cast(field.get(instance));
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }
}
