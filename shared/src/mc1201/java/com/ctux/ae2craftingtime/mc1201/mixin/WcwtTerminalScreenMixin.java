package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.me.common.GridInventoryEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MEStorageScreen.class)
public abstract class WcwtTerminalScreenMixin {
    private static final String WCWT_SCREEN = "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen";

    @ModifyVariable(method = "renderGridInventoryEntryTooltip", at = @At("STORE"), ordinal = 0)
    private List<Component> ae2craftingtime$appendTtc(List<Component> lines, GuiGraphics guiGraphics,
            GridInventoryEntry entry, int x, int y) {
        if (!getClass().getName().equals(WCWT_SCREEN) || !entry.isCraftable()) {
            return lines;
        }

        var result = new ArrayList<>(lines);
        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.request(key);
        ClientStats.CACHE.get(key).ifPresentOrElse(
                stats -> TimeEstimate.format(AeKeyAmounts.normalize(entry.getWhat(), 1), stats)
                        .ifPresentOrElse(eta -> result.add(TtcText.ttc(eta)),
                                () -> result.add(TtcText.ttcCollectingData())),
                () -> result.add(TtcText.ttcCollectingData()));
        return result;
    }
}
