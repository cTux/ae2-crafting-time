package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.me.crafting.CraftingStatusTableRenderer;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.core.CraftingRowState;
import com.ctux.ae2craftingtime.core.CraftingBlockReason;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcColorContext;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import com.ctux.ae2craftingtime.mc1201.IntegrationLog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CraftingStatusTableRenderer.class)
public abstract class CraftingStatusTableRendererMixin {
    @Inject(method = "getEntryDescription", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$appendVisibleTimeToCraft(CraftingStatusEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        var before = cir.getReturnValue().size();
        ae2craftingtime$appendTtc(entry, cir.getReturnValue());
        IntegrationLog.growth("status-row", before, cir.getReturnValue().size());
    }

    @Inject(method = "getEntryTooltip", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$appendTooltipTimeToCraft(CraftingStatusEntry entry,
            CallbackInfoReturnable<List<Component>> cir) {
        if (ae2craftingtime$noSpace(entry)) {
            cir.getReturnValue().addAll(TtcText.noSpaceTooltip());
            IntegrationLog.observe("ae2craftingtime", "status-tooltip");
            return;
        }
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return;
        }

        var reason = ae2craftingtime$blockReason(entry);
        if (reason != null) {
            cir.getReturnValue().addAll(TtcText.blockReasonTooltip(reason));
            IntegrationLog.observe("ae2craftingtime", "status-tooltip");
            return;
        }
        ae2craftingtime$appendStatsTooltip(entry, cir.getReturnValue());
        cir.getReturnValue().add(TtcText.detailsHint().withStyle(ChatFormatting.GRAY));
        cir.getReturnValue().add(TtcText.resetHint().withStyle(ChatFormatting.GRAY));
        IntegrationLog.observe("ae2craftingtime", "status-tooltip");
    }

    private static void ae2craftingtime$appendTtc(CraftingStatusEntry entry, List<Component> lines) {
        if (ae2craftingtime$noSpace(entry)) {
            lines.add(TtcText.noSpace());
            return;
        }
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return;
        }

        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.request(key);
        var reason = ae2craftingtime$blockReason(entry);
        if (reason != null) {
            lines.add(TtcText.blockReason(reason));
            return;
        }
        if (entry.getActiveAmount() == 0 && entry.getPendingAmount() > 0) {
            var waiting = ClientStats.CACHE.waitingTicks(key);
            if (waiting.isPresent()) {
                lines.add(TtcText.waiting()
                        .withStyle(style -> style.withColor(TextColor.fromRgb(0xE0E0E0))));
                return;
            }
        }
        ClientStats.CACHE.get(key).ifPresentOrElse(stats -> {
            var stall = ClientStats.CACHE.stall(key);
            if (stall.isPresent()) {
                lines.add(delayedTtcLine());
                return;
            }
            TimeEstimate.format(AeKeyAmounts.normalize(entry.getWhat(), amount), stats)
                    .ifPresentOrElse(eta -> lines.add(ttcLine(key, eta)),
                            () -> lines.add(TtcText.ttcCollectingData()));
        }, () -> lines.add(TtcText.ttcCollectingData()));
    }

    private static void ae2craftingtime$appendStatsTooltip(CraftingStatusEntry entry, List<Component> lines) {
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        var key = ProfilerBridge.key(entry.getWhat());
        var normalized = AeKeyAmounts.normalize(entry.getWhat(), amount);
        ClientStatsRequests.request(key);
        ClientStats.CACHE.get(key).ifPresent(stats -> {
            var stall = ClientStats.CACHE.stall(key);
            if (stall.isPresent()) {
                lines.addAll(TtcText.stallLines(normalized, entry.getPendingAmount(), stats, stall.get()));
                lines.add(TtcText.locateHint().withStyle(ChatFormatting.GRAY));
            } else {
                lines.addAll(TtcText.statsLines(stats, ClientStats.CACHE.accuracy(key)));
            }
        });
    }

    private static Component delayedTtcLine() {
        return TtcText.ttcDelayed()
                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFF5555)).withBold(true));
    }

    private static boolean ae2craftingtime$noSpace(CraftingStatusEntry entry) {
        return CraftingRowState.noSpace(
                Minecraft.getInstance().screen instanceof CraftingCPUScreen<?> screen
                        && screen.getMenu().isCantStoreItems(),
                entry.getStoredAmount(), entry.getActiveAmount(), entry.getPendingAmount());
    }

    private static CraftingBlockReason ae2craftingtime$blockReason(CraftingStatusEntry entry) {
        return CraftingRowState.blockReason(entry.getPendingAmount(),
                ClientStats.blockReason(ProfilerBridge.key(entry.getWhat())));
    }

    private static Component ttcLine(com.ctux.ae2craftingtime.core.ProfileKey key, String eta) {
        var line = TtcText.ttc(eta);
        var color = TtcColorContext.get(key);
        return color.isPresent()
                ? line.withStyle(style -> style.withColor(TextColor.fromRgb(color.getAsInt())))
                : line;
    }
}
