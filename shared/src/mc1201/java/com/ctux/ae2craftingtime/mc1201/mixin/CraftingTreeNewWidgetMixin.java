package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.CraftingTreeTtc;
import com.ctux.ae2craftingtime.core.IntegrationRead;
import com.ctux.ae2craftingtime.mc1201.IntegrationLog;
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
        if (!IntegrationLog.available("ae2ct")) return;
        if ((!TtcDetailsKeyMapping.matchesMouse(button) && !TtcDetailsKeyMapping.matchesResetMouse(button))
                || !Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        try {
            if (ae2craftingtime$handleClickedStats(mouseX, mouseY, button)) {
                ci.cancel();
            }
        } catch (IntegrationRead.Failure failure) {
            ae2craftingtime$disable(failure);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void ae2craftingtime$beginFrame(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        ae2craftingtime$colorRoot = null;
        ae2craftingtime$secondsByNode = Map.of();
        ae2craftingtime$colorsByNode = Map.of();
        if (IntegrationLog.treeEnabled()) {
            IntegrationLog.observe("ae2ct", "layout");
        }
    }

    @Inject(
            method = "drawNode(Lnet/minecraft/client/gui/GuiGraphics;Lcom/vcwdfca/ae2ct/tree/LegacyTreeLayout$Entry;)V",
            at = @At("RETURN"),
            require = 0)
    private void ae2craftingtime$drawStats(GuiGraphics guiGraphics, @Coerce Object node, CallbackInfo ci) {
        if (!IntegrationLog.treeEnabled()) {
            return;
        }

        try {
            var data = IntegrationRead.invoke(node, "node", Object.class);
            var stack = data == null ? null : IntegrationRead.invoke(data, "output", GenericStack.class);
            var point = IntegrationRead.invoke(node, "point", Point.class);
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
            IntegrationLog.observe("ae2ct", "node");
        } catch (IntegrationRead.Failure failure) {
            ae2craftingtime$disable(failure);
        }
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
        var additions = new ArrayList<Component>();
        try {
            if (IntegrationLog.treeEnabled()) {
                var data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
                var stack = data == null ? null : IntegrationRead.invoke(data, "output", GenericStack.class);
                if (stack != null) {
                    var seconds = ae2craftingtime$secondsByNode.get(data);
                    if (seconds != null) {
                        TimeEstimate.formatTotal(List.of(OptionalLong.of(seconds)))
                                .ifPresent(eta -> additions.add(TtcText.ttc(eta)));
                    }
                    additions.add(TtcText.detailsHint());
                    additions.add(TtcText.resetHint());
                }
            }

            lines.addAll(additions);
            if (!additions.isEmpty()) IntegrationLog.observe("ae2ct", "tooltip");
        } catch (IntegrationRead.Failure failure) {
            ae2craftingtime$disable(failure);
        }

        screen.drawTooltipWithHeader(guiGraphics, mouseX, mouseY, lines);
    }

    private Object ae2craftingtime$hoveredDataNode(double mouseX, double mouseY) {
        var entry = IntegrationRead.invoke(this, "getMouseEntry", Object.class, mouseX, mouseY);
        if (entry != null) {
            var data = IntegrationRead.invoke(entry, "node", Object.class);
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
        var stack = data == null ? null : IntegrationRead.invoke(data, "output", GenericStack.class);
        var amount = IntegrationRead.invoke(data, "amount", Number.class);
        var craftAmount = amount == null ? 0L : amount.longValue();
        if (stack == null || craftAmount <= 0) {
            return false;
        }

        var key = new ProfileKey(stack.what().getId().toString());
        if (TtcDetailsKeyMapping.matchesResetMouse(button)) {
            StatsChatMessages.reset(key, stack.what().getDisplayName().getString());
            IntegrationLog.observe("ae2ct", "reset");
            return true;
        }
        StatsChatMessages.show(key, stack.what().getDisplayName().getString(),
                AeKeyAmounts.normalize(stack.what(), craftAmount));
        IntegrationLog.observe("ae2ct", "details");
        return true;
    }

    private void ae2craftingtime$refreshColors() {
        var data = IntegrationRead.field(this, "activeData", Object.class);
        if (data == null) {
            data = IntegrationRead.field(this, "baseData", Object.class);
        }
        if (data == null || data == ae2craftingtime$colorRoot) {
            return;
        }

        ae2craftingtime$colorRoot = data;
        var all = data == null ? null : IntegrationRead.invoke(data, "allNodes", List.class);
        var roots = new ArrayList<Object>();
        if (all != null) {
            for (var node : all) {
                roots.add(node);
            }
        }
        var seconds = CraftingTreeTtc.computeSeconds(roots,
                d -> IntegrationRead.invoke(d, "output", GenericStack.class),
                d -> {
                    var value = IntegrationRead.invoke(d, "amount", Number.class);
                    return value == null ? 0L : value.longValue();
                },
                d -> {
                    var processes = IntegrationRead.invoke(d, "inputs", List.class);
                    var result = new ArrayList<Object>();
                    if (processes != null) {
                        for (var process : processes) {
                            var nodes = IntegrationRead.invoke(process, "inputs", List.class);
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
    private void ae2craftingtime$disable(IntegrationRead.Failure failure) {
        IntegrationLog.disable("ae2ct", failure);
        ae2craftingtime$colorRoot = null;
        ae2craftingtime$secondsByNode = Map.of();
        ae2craftingtime$colorsByNode = Map.of();
    }
}
