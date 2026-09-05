package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Mixin(targets = "com.neuvillette.ae2ct.gui.CraftingTreeWidget", remap = false)
@Pseudo
public abstract class CraftingTreeWidgetMixin {
    private static final int EXTRA_SPACING_Y = 8;

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
    private int ae2craftingtime$baseSpacingY;
    private Map<Object, Long> ae2craftingtime$secondsByNode = Map.of();
    private Map<Object, Integer> ae2craftingtime$colorsByNode = Map.of();

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2craftingtime$clickStats(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (!IntegrationLog.available("ae2ct")) return;
        if ((!TtcDetailsKeyMapping.matchesMouse(button) && !TtcDetailsKeyMapping.matchesResetMouse(button))
                || !Ae2CraftingTimeConfig.SHOW_IN_TREE.get()) {
            return;
        }

        try {
            if (ae2craftingtime$handleClickedStats(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        } catch (IntegrationRead.Failure failure) {
            ae2craftingtime$disable(failure);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void ae2craftingtime$beginFrame(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        if (ae2craftingtime$baseSpacingY == 0) {
            ae2craftingtime$baseSpacingY = spacingY;
        }

        ae2craftingtime$colorRoot = null;
        ae2craftingtime$secondsByNode = Map.of();
        ae2craftingtime$colorsByNode = Map.of();
        spacingY = ae2craftingtime$baseSpacingY;
        if (!IntegrationLog.treeEnabled()) return;
        try {
            ae2craftingtime$refreshColors();
            spacingY += EXTRA_SPACING_Y;
            IntegrationLog.observe("ae2ct", "layout");
        } catch (IntegrationRead.Failure failure) {
            ae2craftingtime$disable(failure);
        }
    }

    @Inject(
            method = "drawNode(Lnet/minecraft/client/gui/GuiGraphics;Lcom/neuvillette/ae2ct/api/CraftingTreeHelper$Node;)V",
            at = @At("RETURN"),
            require = 0)
    private void ae2craftingtime$drawStats(GuiGraphics guiGraphics, @Coerce Object node, CallbackInfo ci) {
        if (!IntegrationLog.treeEnabled()) {
            return;
        }

        try {
            var stack = IntegrationRead.field(node, "stack", GenericStack.class);
            var point = IntegrationRead.field(node, "point", Point.class);
            if (stack == null || point == null) {
                return;
            }

            ae2craftingtime$refreshColors();
            var seconds = ae2craftingtime$secondsByNode.get(node);
            if (seconds == null) {
                return;
            }

            var color = ae2craftingtime$colorsByNode.getOrDefault(node, TtcColor.GREEN);
            CraftingTreeTtc.drawBadge(guiGraphics, outputX, outputY, spacingX, spacingY, point, seconds, color);
            IntegrationLog.observe("ae2ct", "node");
        } catch (IntegrationRead.Failure failure) {
            ae2craftingtime$disable(failure);
        }
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
    private void ae2craftingtime$appendTooltipStats(AEBaseScreen<?> screen, GuiGraphics guiGraphics, int mouseX,
            int mouseY, List<Component> lines) {
        var additions = new ArrayList<Component>();
        try {
            if (IntegrationLog.treeEnabled()) {
                var data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
                var stack = data == null ? null : IntegrationRead.field(data, "stack", GenericStack.class);
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
        var manager = IntegrationRead.field(this, "_nodeManager", Object.class);
        if (manager != null) {
            var map = IntegrationRead.field(manager, "map", Map.class);
            if (map != null) {
                return map.get(getMousePoint(mouseX, mouseY));
            }
        }
        return null;
    }

    private boolean ae2craftingtime$handleClickedStats(double mouseX, double mouseY, int button) {
        var data = ae2craftingtime$hoveredDataNode(mouseX, mouseY);
        if (data == null && Minecraft.getInstance().screen instanceof AEBaseScreen<?> screen) {
            data = ae2craftingtime$hoveredDataNode(mouseX + screen.getGuiLeft(), mouseY + screen.getGuiTop());
        }
        var stack = data == null ? null : IntegrationRead.field(data, "stack", GenericStack.class);
        var amountHelper = data == null ? null : IntegrationRead.field(data, "amountHelper", Object.class);
        var craftAmount = amountHelper == null ? null : IntegrationRead.field(amountHelper, "craftAmount", Long.class);
        var amount = craftAmount == null ? 0L : craftAmount;
        if (stack == null || amount <= 0) {
            return false;
        }

        var key = new ProfileKey(stack.what().getId().toString());
        if (TtcDetailsKeyMapping.matchesResetMouse(button)) {
            StatsChatMessages.reset(key, stack.what().getDisplayName().getString());
            IntegrationLog.observe("ae2ct", "reset");
            return true;
        }
        StatsChatMessages.show(key, stack.what().getDisplayName().getString(),
                AeKeyAmounts.normalize(stack.what(), amount));
        IntegrationLog.observe("ae2ct", "details");
        return true;
    }

    private void ae2craftingtime$refreshColors() {
        var manager = IntegrationRead.field(this, "_nodeManager", Object.class);
        var root = manager == null ? null : IntegrationRead.field(manager, "root", Object.class);
        if (root == null || root == ae2craftingtime$colorRoot) {
            return;
        }

        ae2craftingtime$colorRoot = root;
        var seconds = CraftingTreeTtc.computeSeconds(List.of(root),
                d -> IntegrationRead.field(d, "stack", GenericStack.class),
                d -> {
                    var helper = IntegrationRead.field(d, "amountHelper", Object.class);
                    var value = helper == null ? null : IntegrationRead.field(helper, "craftAmount", Long.class);
                    return value;
                },
                d -> {
                    var subNodes = IntegrationRead.field(d, "subNodes", List.class);
                    var result = new ArrayList<Object>();
                    if (subNodes != null) {
                        for (var node : subNodes) {
                            result.add(node);
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
        if (ae2craftingtime$baseSpacingY != 0) spacingY = ae2craftingtime$baseSpacingY;
    }
}
