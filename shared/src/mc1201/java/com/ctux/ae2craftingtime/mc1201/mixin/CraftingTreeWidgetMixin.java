package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.StatsChatMessages;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcDetailsKeyMapping;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Mixin(targets = {
        "com.neuvillette.ae2ct.gui.CraftingTreeWidget",
        "com.vcwdfca.ae2ct.gui.CraftingTreeWidget"
}, remap = false)
@Pseudo
public abstract class CraftingTreeWidgetMixin {
    private static final float TEXT_SCALE = 0.5f;
    private static final int EXTRA_SPACING_Y = 8;
    private static final int LABEL_PADDING_X = 2;
    private static final int LABEL_TOP_OFFSET = 20;
    private static final int LABEL_HEIGHT = 7;

    @Shadow
    private int outputX;
    @Shadow
    private int outputY;
    @Shadow
    private int spacingX;
    @Shadow
    private int spacingY;

    private Object ae2craftingtime$colorRoot;
    private int ae2craftingtime$baseSpacingY;
    private Map<Object, Long> ae2craftingtime$secondsByNode = Map.of();
    private Map<Object, Integer> ae2craftingtime$colorsByNode = Map.of();

    private boolean ae2craftingtime$isNewWidget() {
        return getClass().getName().startsWith("com.vcwdfca.ae2ct");
    }

    private static boolean ae2craftingtime$isNew(Object obj) {
        return obj != null && obj.getClass().getName().startsWith("com.vcwdfca.ae2ct");
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2craftingtime$clickStats(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if ((!TtcDetailsKeyMapping.matchesMouse(button) && !TtcDetailsKeyMapping.matchesResetMouse(button))
                || !Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        if (ae2craftingtime$handleClickedStats(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void ae2craftingtime$beginFrame(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        if (ae2craftingtime$baseSpacingY == 0) {
            ae2craftingtime$baseSpacingY = spacingY;
        }
        if (!ae2craftingtime$isNewWidget()) {
            spacingY = ae2craftingtime$baseSpacingY
                    + (Ae2CraftingTimeConfig.SHOW_IN_TREE.get() ? EXTRA_SPACING_Y : 0);
        }
        ae2craftingtime$colorRoot = null;
    }

    @Inject(
            method = "drawNode(Lnet/minecraft/client/gui/GuiGraphics;Lcom/neuvillette/ae2ct/api/CraftingTreeHelper$Node;)V",
            at = @At("RETURN"),
            require = 0)
    private void ae2craftingtime$drawStatsOld(GuiGraphics guiGraphics, @Coerce Object node, CallbackInfo ci) {
        ae2craftingtime$drawStats(guiGraphics, node);
    }

    @Inject(
            method = "drawNode(Lnet/minecraft/client/gui/GuiGraphics;Lcom/vcwdfca/ae2ct/tree/LegacyTreeLayout$Entry;)V",
            at = @At("RETURN"),
            require = 0)
    private void ae2craftingtime$drawStatsNew(GuiGraphics guiGraphics, @Coerce Object node, CallbackInfo ci) {
        ae2craftingtime$drawStats(guiGraphics, node);
    }

    private void ae2craftingtime$drawStats(GuiGraphics guiGraphics, Object node) {
        if (!Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        Object data;
        GenericStack stack;
        Point point;
        if (ae2craftingtime$isNew(node)) {
            data = ae2craftingtime$invoke(node, "node");
            stack = data == null ? null : ae2craftingtime$resolveStack(data);
            point = (Point) ae2craftingtime$invoke(node, "point");
        } else {
            data = node;
            stack = (GenericStack) ae2craftingtime$readField(node, "stack");
            point = (Point) ae2craftingtime$readField(node, "point");
        }
        if (stack == null || point == null) {
            return;
        }

        ae2craftingtime$refreshColors();
        var seconds = ae2craftingtime$secondsByNode.get(data);
        if (seconds == null) {
            return;
        }

        var text = TimeEstimate.formatTotal(List.of(OptionalLong.of(seconds))).orElse(null);
        if (text == null) {
            return;
        }

        var font = Minecraft.getInstance().font;
        var color = ae2craftingtime$colorsByNode.getOrDefault(data, TtcColor.GREEN);
        var scaledTextWidth = font.width(text) * TEXT_SCALE;
        var labelWidth = (int) Math.ceil(scaledTextWidth) + LABEL_PADDING_X * 2;
        var x = point.x * spacingX + outputX;
        var y = point.y * spacingY + outputY;
        var labelLeft = x + 8 - labelWidth / 2.0f;
        var labelTop = y + LABEL_TOP_OFFSET;
        var textX = (int) ((x + 8 - scaledTextWidth / 2) / TEXT_SCALE);
        var textY = (int) ((labelTop + 1) / TEXT_SCALE);
        var stripLeft = (int) Math.floor(labelLeft / TEXT_SCALE);
        var stripTop = (int) Math.floor(labelTop / TEXT_SCALE);
        var stripRight = (int) Math.ceil((labelLeft + labelWidth) / TEXT_SCALE);
        var stripBottom = (int) Math.ceil((labelTop + LABEL_HEIGHT) / TEXT_SCALE);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        TtcBadge.fillRect(guiGraphics, stripLeft, stripTop, stripRight, stripBottom, TtcBadge.BACKGROUND);
        guiGraphics.drawString(font, text, textX, textY, color, true);
        pose.popPose();
    }

    @SuppressWarnings("mapping")
    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/client/gui/AEBaseScreen;drawTooltipWithHeader(Lnet/minecraft/client/gui/GuiGraphics;IILjava/util/List;)V",
                    remap = true),
            remap = false,
            require = 0)
    private void ae2craftingtime$appendTooltipStats(AEBaseScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<Component> lines) {
        if (Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            var data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
            var stack = data == null ? null : ae2craftingtime$resolveStack(data);
            if (stack != null) {
                ae2craftingtime$totalSeconds(data).ifPresent(seconds -> TimeEstimate.formatTotal(List.of(OptionalLong.of(seconds)))
                        .ifPresent(eta -> lines.add(TtcText.ttc(eta))));
                lines.add(TtcText.detailsHint());
                lines.add(TtcText.resetHint());
            }
        }

        screen.drawTooltipWithHeader(guiGraphics, mouseX, mouseY, lines);
    }

    private Object ae2craftingtime$hoveredDataNode(double mouseX, double mouseY) {
        var entry = ae2craftingtime$invoke(this, "getMouseEntry", mouseX, mouseY);
        if (entry != null) {
            var data = ae2craftingtime$invoke(entry, "node");
            if (data != null) {
                return data;
            }
        }

        var manager = (Object) ae2craftingtime$readField(this, "_nodeManager");
        if (manager != null) {
            var map = (Map<?, ?>) ae2craftingtime$readField(manager, "map");
            if (map != null) {
                return map.get(ae2craftingtime$invoke(this, "getMousePoint", mouseX, mouseY));
            }
        }
        return null;
    }

    private boolean ae2craftingtime$handleClickedStats(double mouseX, double mouseY, int button) {
        var data = ae2craftingtime$hoveredDataNode(mouseX, mouseY);
        if (data == null && Minecraft.getInstance().screen instanceof AEBaseScreen<?> screen) {
            data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
        }
        var stack = data == null ? null : ae2craftingtime$resolveStack(data);
        var craftAmount = data == null ? 0L : ae2craftingtime$resolveCraftAmount(data);
        if (stack == null || craftAmount <= 0) {
            return false;
        }

        var key = new ProfileKey(stack.what().getId().toString());
        if (TtcDetailsKeyMapping.matchesResetMouse(button)) {
            StatsChatMessages.reset(key, stack.what().getDisplayName().getString());
            return true;
        }
        StatsChatMessages.show(key, stack.what().getDisplayName().getString(),
                AeKeyAmounts.normalize(stack.what(), craftAmount));
        return true;
    }

    private void ae2craftingtime$refreshColors() {
        var root = ae2craftingtime$isNewWidget() ? ae2craftingtime$activeDataRoot() : ae2craftingtime$oldRoot();
        if (root == null || root == ae2craftingtime$colorRoot) {
            return;
        }

        ae2craftingtime$colorRoot = root;
        var seconds = new IdentityHashMap<Object, Long>();
        if (ae2craftingtime$isNewWidget()) {
            var data = (Object) ae2craftingtime$readField(this, "activeData");
            if (data == null) {
                data = (Object) ae2craftingtime$readField(this, "baseData");
            }
            var all = data == null ? null : (List<?>) ae2craftingtime$invoke(data, "allNodes");
            if (all != null) {
                for (var n : all) {
                    ae2craftingtime$totalSecondsCached(n, seconds);
                }
            }
        } else if (root != null) {
            ae2craftingtime$totalSecondsCached(root, seconds);
        }
        ae2craftingtime$secondsByNode = seconds;

        var min = Long.MAX_VALUE;
        var max = Long.MIN_VALUE;
        for (var value : seconds.values()) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        var colors = new IdentityHashMap<Object, Integer>();
        for (var entry : seconds.entrySet()) {
            colors.put(entry.getKey(), TtcColor.forSeconds(entry.getValue(), min, max));
        }
        ae2craftingtime$colorsByNode = colors;
    }

    private Object ae2craftingtime$activeDataRoot() {
        var data = (Object) ae2craftingtime$readField(this, "activeData");
        if (data == null) {
            data = (Object) ae2craftingtime$readField(this, "baseData");
        }
        return data == null ? null : ae2craftingtime$invoke(data, "root");
    }

    private Object ae2craftingtime$oldRoot() {
        var manager = (Object) ae2craftingtime$readField(this, "_nodeManager");
        return manager == null ? null : (Object) ae2craftingtime$readField(manager, "root");
    }

    private OptionalLong ae2craftingtime$totalSeconds(Object data) {
        if (data == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(ae2craftingtime$totalSecondsCached(data, new IdentityHashMap<>()));
    }

    private long ae2craftingtime$totalSecondsCached(Object data, Map<Object, Long> cache) {
        var cached = cache.get(data);
        if (cached != null) {
            return cached;
        }

        long total = 0;
        var self = ae2craftingtime$selfSeconds(data);
        if (self.isPresent()) {
            total += self.getAsLong();
        }
        for (var child : ae2craftingtime$children(data)) {
            total += ae2craftingtime$totalSecondsCached(child, cache);
        }

        cache.put(data, total);
        return total;
    }

    private OptionalLong ae2craftingtime$selfSeconds(Object data) {
        var stack = ae2craftingtime$resolveStack(data);
        var craftAmount = ae2craftingtime$resolveCraftAmount(data);
        if (stack == null || craftAmount <= 0) {
            return OptionalLong.empty();
        }

        var key = new ProfileKey(stack.what().getId().toString());
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            ClientStatsRequests.request(key);
            return OptionalLong.empty();
        }

        return TimeEstimate.seconds(AeKeyAmounts.normalize(stack.what(), craftAmount), stats.get());
    }

    private static GenericStack ae2craftingtime$resolveStack(Object data) {
        return ae2craftingtime$isNew(data)
                ? (GenericStack) ae2craftingtime$invoke(data, "output")
                : (GenericStack) ae2craftingtime$readField(data, "stack");
    }

    private static long ae2craftingtime$resolveCraftAmount(Object data) {
        if (ae2craftingtime$isNew(data)) {
            var amount = ae2craftingtime$invoke(data, "amount");
            return amount instanceof Number number ? number.longValue() : 0L;
        }
        var amountHelper = (Object) ae2craftingtime$readField(data, "amountHelper");
        var craftAmount = (Long) ae2craftingtime$readField(amountHelper, "craftAmount");
        return craftAmount == null ? 0L : craftAmount;
    }

    private static List<Object> ae2craftingtime$children(Object data) {
        var result = new ArrayList<Object>();
        if (ae2craftingtime$isNew(data)) {
            var inputs = (List<?>) ae2craftingtime$invoke(data, "inputs");
            if (inputs != null) {
                for (var process : inputs) {
                    var nodes = (List<?>) ae2craftingtime$invoke(process, "inputs");
                    if (nodes != null) {
                        for (var node : nodes) {
                            result.add(node);
                        }
                    }
                }
            }
        } else {
            var subNodes = (List<?>) ae2craftingtime$readField(data, "subNodes");
            if (subNodes != null) {
                for (var node : subNodes) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    private static Object ae2craftingtime$invoke(Object instance, String name, Object... args) {
        if (instance == null) {
            return null;
        }
        for (var type : new Class<?>[] { instance.getClass(), instance.getClass().getSuperclass() }) {
            if (type == null) {
                continue;
            }
            for (var method : type.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    return ae2craftingtime$call(method, instance, args);
                }
            }
            for (var method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    return ae2craftingtime$call(method, instance, args);
                }
            }
        }
        return null;
    }

    private static Object ae2craftingtime$call(Method method, Object instance, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object ae2craftingtime$readField(Object instance, String name) {
        try {
            Field field;
            try {
                field = instance.getClass().getField(name);
            } catch (NoSuchFieldException ignored) {
                field = instance.getClass().getDeclaredField(name);
                field.setAccessible(true);
            }
            return field.get(instance);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }
}
