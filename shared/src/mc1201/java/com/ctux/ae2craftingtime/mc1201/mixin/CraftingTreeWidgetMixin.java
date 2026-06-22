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
import com.ctux.ae2craftingtime.mc1201.TtcDetailsKeyMapping;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Mixin(targets = "com.neuvillette.ae2ct.gui.CraftingTreeWidget", remap = false)
@Pseudo
public abstract class CraftingTreeWidgetMixin {
    private static final float TEXT_SCALE = 0.45f;
    private static final int LABEL_BACKGROUND = 0xFFDBDBDB;

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

    private Object ae2craftingtime$colorRoot;
    private Map<Object, Long> ae2craftingtime$secondsByNode = Map.of();
    private Map<Object, Integer> ae2craftingtime$colorsByNode = Map.of();

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2craftingtime$clickStats(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (!TtcDetailsKeyMapping.matchesMouse(button) || !Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        if (ae2craftingtime$showClickedStats(mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void ae2craftingtime$beginFrame(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        ae2craftingtime$colorRoot = null;
    }

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

        ae2craftingtime$refreshColors();
        var seconds = ae2craftingtime$secondsByNode.get(node);
        if (seconds == null) {
            return;
        }

        var x = point.x * spacingX + outputX;
        var y = point.y * spacingY + outputY;
        var text = TimeEstimate.formatTotal(List.of(OptionalLong.of(seconds))).orElseThrow();
        var pose = guiGraphics.pose();
        var font = Minecraft.getInstance().font;
        var color = ae2craftingtime$colorsByNode.getOrDefault(node, TtcColor.DARK_GREEN);
        var textX = (int) ((x - 3 + (24 - font.width(text) * TEXT_SCALE) / 2) / TEXT_SCALE);
        var textY = (int) ((y + 15) / TEXT_SCALE);
        var stripLeft = (int) ((x - 3) / TEXT_SCALE);
        var stripTop = (int) ((y + 14) / TEXT_SCALE);
        var stripRight = (int) ((x + 21) / TEXT_SCALE);
        var stripBottom = (int) ((y + 20) / TEXT_SCALE);

        pose.pushPose();
        pose.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        guiGraphics.fill(stripLeft, stripTop, stripRight, stripBottom, LABEL_BACKGROUND);
        guiGraphics.drawString(font, text, textX, textY, color, false);
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
                lines.add(Component.literal("Ctrl-Click to see TTC details"));
                var seconds = ae2craftingtime$totalSeconds(node);
                if (seconds.isPresent()) {
                    TimeEstimate.formatTotal(List.of(seconds))
                            .ifPresent(eta -> lines.add(Component.literal("TTC: " + eta)));
                }
            }
        }

        screen.drawTooltipWithHeader(guiGraphics, mouseX, mouseY, lines);
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

    private boolean ae2craftingtime$showClickedStats(double mouseX, double mouseY) {
        var node = hoveredNode(mouseX, mouseY);
        if (node == null && Minecraft.getInstance().screen instanceof AEBaseScreen<?> screen) {
            node = hoveredNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
        }
        var stack = node == null ? null : readField(node, "stack", GenericStack.class);
        var amountHelper = node == null ? null : readField(node, "amountHelper", Object.class);
        var craftAmount = amountHelper == null ? null : readField(amountHelper, "craftAmount", Long.class);
        if (stack == null || craftAmount == null || craftAmount <= 0) {
            return false;
        }

        var key = new ProfileKey(stack.what().getId().toString());
        StatsChatMessages.show(key, AeKeyAmounts.normalize(stack.what(), craftAmount));
        return true;
    }

    private void ae2craftingtime$refreshColors() {
        var manager = readField(this, "_nodeManager", Object.class);
        var root = manager == null ? null : readField(manager, "root", Object.class);
        if (root == null || root == ae2craftingtime$colorRoot) {
            return;
        }

        ae2craftingtime$colorRoot = root;
        var secondsByNode = new IdentityHashMap<Object, Long>();
        ae2craftingtime$totalSeconds(root, secondsByNode);
        ae2craftingtime$secondsByNode = secondsByNode;
        var colorsByNode = new IdentityHashMap<Object, Integer>();
        ae2craftingtime$colorSiblingGroup(List.of(root), secondsByNode, colorsByNode);
        ae2craftingtime$colorChildGroups(root, secondsByNode, colorsByNode);
        ae2craftingtime$colorsByNode = colorsByNode;
    }

    private static void ae2craftingtime$colorChildGroups(Object node, Map<Object, Long> secondsByNode,
            Map<Object, Integer> colorsByNode) {
        var subNodes = readField(node, "subNodes", List.class);
        if (subNodes == null) {
            return;
        }

        ae2craftingtime$colorSiblingGroup(subNodes, secondsByNode, colorsByNode);
        for (var subNode : subNodes) {
            ae2craftingtime$colorChildGroups(subNode, secondsByNode, colorsByNode);
        }
    }

    private static void ae2craftingtime$colorSiblingGroup(List<?> nodes, Map<Object, Long> secondsByNode,
            Map<Object, Integer> colorsByNode) {
        long minSeconds = Long.MAX_VALUE;
        long maxSeconds = Long.MIN_VALUE;
        for (var node : nodes) {
            var seconds = secondsByNode.get(node);
            if (seconds != null) {
                minSeconds = Math.min(minSeconds, seconds);
                maxSeconds = Math.max(maxSeconds, seconds);
            }
        }
        for (var node : nodes) {
            var seconds = secondsByNode.get(node);
            if (seconds != null) {
                colorsByNode.put(node, TtcColor.forSeconds(seconds, minSeconds, maxSeconds));
            }
        }
    }

    private OptionalLong ae2craftingtime$totalSeconds(Object node) {
        return ae2craftingtime$totalSeconds(node, new IdentityHashMap<>());
    }

    private static OptionalLong ae2craftingtime$totalSeconds(Object node, Map<Object, Long> cache) {
        if (node == null) {
            return OptionalLong.empty();
        }
        var cached = cache.get(node);
        if (cached != null) {
            return OptionalLong.of(cached);
        }

        long total = 0;
        var self = ae2craftingtime$selfSeconds(node);
        if (self.isPresent()) {
            total += self.getAsLong();
        }

        var subNodes = readField(node, "subNodes", List.class);
        if (subNodes != null) {
            for (var subNode : subNodes) {
                var seconds = ae2craftingtime$totalSeconds(subNode, cache);
                if (seconds.isPresent()) {
                    total += seconds.getAsLong();
                }
            }
        }

        if (total == 0) {
            return OptionalLong.empty();
        }
        cache.put(node, total);
        return OptionalLong.of(total);
    }

    private static OptionalLong ae2craftingtime$selfSeconds(Object node) {
        var stack = readField(node, "stack", GenericStack.class);
        var amountHelper = readField(node, "amountHelper", Object.class);
        var craftAmount = amountHelper == null ? null : readField(amountHelper, "craftAmount", Long.class);
        if (stack == null || craftAmount == null || craftAmount <= 0) {
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
