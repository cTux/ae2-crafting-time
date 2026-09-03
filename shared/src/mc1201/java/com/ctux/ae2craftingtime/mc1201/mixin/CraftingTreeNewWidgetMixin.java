package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.CraftingTreeTtc;
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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Mixin(targets = "com.vcwdfca.ae2ct.gui.CraftingTreeWidget", remap = false)
@Pseudo
public abstract class CraftingTreeNewWidgetMixin {
    @Shadow
    private int outputX;
    @Shadow
    private int outputY;
    @Shadow
    private int spacingX;
    @Shadow
    private int spacingY;

    private Object ae2craftingtime$colorRoot;
    private Map<Object, Long> ae2craftingtime$secondsByNode = Map.of();
    private Map<Object, Integer> ae2craftingtime$colorsByNode = Map.of();

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2craftingtime$clickStats(double mouseX, double mouseY, int button, CallbackInfo ci) {
        if ((!TtcDetailsKeyMapping.matchesMouse(button) && !TtcDetailsKeyMapping.matchesResetMouse(button))
                || !Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        if (ae2craftingtime$handleClickedStats(mouseX, mouseY, button)) {
            ci.cancel();
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void ae2craftingtime$beginFrame(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        ae2craftingtime$colorRoot = null;
    }

    @Inject(
            method = "drawNode(Lnet/minecraft/client/gui/GuiGraphics;Lcom/vcwdfca/ae2ct/tree/LegacyTreeLayout$Entry;)V",
            at = @At("RETURN"),
            require = 0)
    private void ae2craftingtime$drawStats(GuiGraphics guiGraphics, @Coerce Object node, CallbackInfo ci) {
        if (!Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        var data = CraftingTreeTtc.invoke(node, "node");
        var stack = data == null ? null : (GenericStack) CraftingTreeTtc.invoke(data, "output");
        var point = (Point) CraftingTreeTtc.invoke(node, "point");
        if (stack == null || point == null) {
            return;
        }

        ae2craftingtime$refreshColors();
        var seconds = ae2craftingtime$secondsByNode.get(data);
        if (seconds == null) {
            return;
        }

        var color = ae2craftingtime$colorsByNode.getOrDefault(data, TtcColor.GREEN);
        CraftingTreeTtc.drawBadge(guiGraphics, outputX, outputY, spacingX, spacingY, point, seconds, color);
    }

    @SuppressWarnings("mapping")
    @Redirect(
            method = "updateTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/client/gui/AEBaseScreen;drawTooltipWithHeader(Lnet/minecraft/client/gui/GuiGraphics;IILjava/util/List;)V",
                    remap = true),
            remap = false,
            require = 0)
    private void ae2craftingtime$appendTooltipStats(AEBaseScreen<?> screen, GuiGraphics guiGraphics, int mouseX,
            int mouseY, List<Component> lines) {
        if (Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            var data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
            var stack = data == null ? null : (GenericStack) CraftingTreeTtc.invoke(data, "output");
            if (stack != null) {
                var seconds = ae2craftingtime$secondsByNode.get(data);
                if (seconds != null) {
                    TimeEstimate.formatTotal(List.of(OptionalLong.of(seconds)))
                            .ifPresent(eta -> lines.add(TtcText.ttc(eta)));
                }
                lines.add(TtcText.detailsHint());
                lines.add(TtcText.resetHint());
            }
        }

        screen.drawTooltipWithHeader(guiGraphics, mouseX, mouseY, lines);
    }

    private Object ae2craftingtime$hoveredDataNode(double mouseX, double mouseY) {
        var entry = CraftingTreeTtc.invoke(this, "getMouseEntry", mouseX, mouseY);
        if (entry != null) {
            var data = CraftingTreeTtc.invoke(entry, "node");
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private boolean ae2craftingtime$handleClickedStats(double mouseX, double mouseY, int button) {
        var data = ae2craftingtime$hoveredDataNode(mouseX, mouseY);
        if (data == null && Minecraft.getInstance().screen instanceof AEBaseScreen<?> screen) {
            data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
        }
        var stack = data == null ? null : (GenericStack) CraftingTreeTtc.invoke(data, "output");
        var amount = CraftingTreeTtc.invoke(data, "amount");
        var craftAmount = amount instanceof Number number ? number.longValue() : 0L;
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
        var data = CraftingTreeTtc.readField(this, "activeData");
        if (data == null) {
            data = CraftingTreeTtc.readField(this, "baseData");
        }
        if (data == null || data == ae2craftingtime$colorRoot) {
            return;
        }

        ae2craftingtime$colorRoot = data;
        var all = data == null ? null : (List<?>) CraftingTreeTtc.invoke(data, "allNodes");
        var roots = new ArrayList<Object>();
        if (all != null) {
            for (var node : all) {
                roots.add(node);
            }
        }
        var seconds = CraftingTreeTtc.computeSeconds(roots,
                d -> (GenericStack) CraftingTreeTtc.invoke(d, "output"),
                d -> {
                    var value = CraftingTreeTtc.invoke(d, "amount");
                    return value instanceof Number number ? number.longValue() : 0L;
                },
                d -> {
                    var processes = (List<?>) CraftingTreeTtc.invoke(d, "inputs");
                    var result = new ArrayList<Object>();
                    if (processes != null) {
                        for (var process : processes) {
                            var nodes = (List<?>) CraftingTreeTtc.invoke(process, "inputs");
                            if (nodes != null) {
                                for (var node : nodes) {
                                    result.add(node);
                                }
                            }
                        }
                    }
                    return result;
                });
        ae2craftingtime$secondsByNode = seconds;
        ae2craftingtime$colorsByNode = CraftingTreeTtc.computeColors(seconds);
    }
}
